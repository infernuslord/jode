/* 
 * CodeAnalyzer (c) 1998 Jochen Hoenicke
 *
 * You may distribute under the terms of the GNU General Public License.
 *
 * IN NO EVENT SHALL JOCHEN HOENICKE BE LIABLE TO ANY PARTY FOR DIRECT,
 * INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT OF
 * THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF JOCHEN HOENICKE 
 * HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * JOCHEN HOENICKE SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS"
 * BASIS, AND JOCHEN HOENICKE HAS NO OBLIGATION TO PROVIDE MAINTENANCE,
 * SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * $Id$
 */

package jode;
import sun.tools.java.*;
import java.util.Stack;
import java.io.*;
import jode.flow.FlowBlock;
import jode.flow.Jump;
import jode.flow.StructuredBlock;
import jode.flow.RawTryCatchBlock;

public class CodeAnalyzer implements Analyzer, Constants {
    
    BinaryCode bincode;

    FlowBlock methodHeader;
    MethodAnalyzer method;
    public JodeEnvironment env;

    jode.flow.VariableSet param;
    LocalVariableTable lvt;
    
    /**
     * Get the method.
     * @return The method to which this code belongs.
     */
    public MethodAnalyzer getMethod() {return method;}
    
    void readCode() 
         throws ClassFormatError
    {

        BinaryAttribute attr = bincode.getAttributes();
        while (attr != null) {
            if (attr.getName() == Constants.idLocalVariableTable) {
                DataInputStream stream = 
                    new DataInputStream
                    (new ByteArrayInputStream(attr.getData()));
                try {
                    lvt = new LocalVariableTable(bincode.getMaxLocals());
                    lvt.read(env, stream);
                } catch (IOException ex) {
                    throw new ClassFormatError(ex.toString());
                }
            }
            attr = attr.getNextAttribute();
        }

        byte[] code = bincode.getCode();
        FlowBlock[] instr = new FlowBlock[code.length];
	int returnCount;
        try {
            DataInputStream stream = 
                new DataInputStream(new ByteArrayInputStream(code));
	    for (int addr = 0; addr < code.length; ) {
		instr[addr] = Opcodes.readOpcode(addr, stream, this);

		addr = instr[addr].getNextAddr();
	    }
        } catch (IOException ex) {
            throw new ClassFormatError(ex.toString());
        }
        for (int addr=0; addr<instr.length; ) {
            instr[addr].resolveJumps(instr);
            addr = instr[addr].getNextAddr();
        }
	methodHeader = instr[0];
        methodHeader.makeStartBlock();

        BinaryExceptionHandler[] handlers = bincode.getExceptionHandlers();
        for (int i=0; i<handlers.length; i++) {
            StructuredBlock tryBlock = instr[handlers[i].startPC].getBlock();
            while (tryBlock instanceof RawTryCatchBlock
                   && (((RawTryCatchBlock) tryBlock).getCatchAddr()
                       > handlers[i].handlerPC)) {

                tryBlock = ((RawTryCatchBlock)tryBlock).getTryBlock();
            }
            
            Type type = 
                (handlers[i].exceptionClass != null)? 
                handlers[i].exceptionClass.getType() : null;
            
            new RawTryCatchBlock(type, tryBlock, 
                                 new Jump(instr[handlers[i].endPC]),
                                 new Jump(instr[handlers[i].handlerPC]));
        }

	int paramCount = method.mdef.getType().getArgumentTypes().length 
	    + (method.mdef.isStatic() ? 0 : 1);
	param = new jode.flow.VariableSet();
	for (int i=0; i<paramCount; i++)
	    param.addElement(getLocalInfo(-1, i));
    }

    public void dumpSource(TabbedPrintWriter writer) 
         throws java.io.IOException
    {
	methodHeader.makeDeclaration(param);
        methodHeader.dumpSource(writer);
    }

    public CodeAnalyzer(MethodAnalyzer ma, BinaryCode bc, JodeEnvironment e)
         throws ClassFormatError
    {
        method = ma;
        env  = e;
	bincode = bc;
        readCode();
    }

    public LocalInfo getLocalInfo(int addr, int slot) {
        if (lvt != null)
            return lvt.getLocal(slot).getInfo(addr);
        else
            return new LocalInfo(slot);
    }

    public LocalInfo getParamInfo(int slot) {
	return (LocalInfo) param.elementAt(slot);
    }

    static jode.flow.Transformation[] exprTrafos = {
        new jode.flow.RemoveEmpty(),
        new jode.flow.CreateExpression(),
        new jode.flow.CreatePostIncExpression(),
        new jode.flow.CreateAssignExpression(),
        new jode.flow.CreateNewConstructor(),
        new jode.flow.CombineIfGotoExpressions(),
        new jode.flow.CreateIfThenElseOperator(),
        new jode.flow.CreateConstantArray(),
        new jode.flow.SimplifyExpression()
    };

    public void analyze()
    {
        /* XXX optimize */
        Stack todo = new Stack();
        FlowBlock flow = methodHeader;
        try {
            jode.TabbedPrintWriter writer = null;
            if (Decompiler.isFlowDebugging) {
                writer = new jode.TabbedPrintWriter(System.err, "    ");
            }
        analyzation:
            while (true) {

                if (Decompiler.isFlowDebugging) {
                    writer.println("before Transformation: ");
                    writer.tab();
                    flow.dumpSource(writer);
                    writer.untab();
                }

                /* First do some non flow transformations. */
                int i=0;
                while (i < exprTrafos.length) {
                    if (exprTrafos[i].transform(flow))
                        i = 0;
                    else
                        i++;
                }
            
                if (Decompiler.isFlowDebugging) {
                    writer.println("after Transformation: ");
                    writer.tab();
                    flow.dumpSource(writer);
                    writer.untab();
                }

                if (flow.doT2(todo)) {

                    if (Decompiler.isFlowDebugging) {
                        writer.println("after T2: ");
                        writer.tab();
                        flow.dumpSource(writer);
                        writer.untab();
                    }

                    /* T2 transformation succeeded.  This may
                     * make another T1 analysis in the previous
                     * block possible.  
                     */
                    if (!todo.isEmpty())
                        flow = (FlowBlock) todo.pop();
                }

                FlowBlock succ = flow.getSuccessor();
                while (true) {
                    if (succ == null) {
                        /* the Block has no successor where t1 is applicable.
                         *
                         * If everything is okay the stack should be empty now,
                         * and the program is transformed correctly.
                         */
                        if (todo.isEmpty())
                            break analyzation;
                            
                        /* Otherwise pop the last flow block from stack and
                         * try another successor.
                         */
                        succ = flow;
                        flow = (FlowBlock) todo.pop();
                    } else if (flow.doT1(succ)) {
                        /* T1 transformation succeeded. */

                        if (Decompiler.isFlowDebugging) {
                            writer.println("after T1: ");
                            writer.tab();
                            flow.dumpSource(writer);
                            writer.untab();
                        }

                        if (Decompiler.isVerbose)
                            System.err.print(".");

                        continue analyzation;
                    } else if (!todo.contains(succ) && succ != flow) {
                        /* succ wasn't tried before, succeed with
                         * successor and put flow on the stack.  
                         */
                        todo.push(flow);
                        flow = succ;
                        continue analyzation;
                    }
                
                    /* Try the next successor.
                     */
                    succ = flow.getSuccessor(succ);
                }
            }
        } catch (java.io.IOException ioex) {
        }
    }

    public String getTypeString(Type type) {
        return env.getTypeString(type);
    }

    public ClassDefinition getClassDefinition() {
        return env.getClassDefinition();
    }
}

