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
import java.io.*;

public class CodeAnalyzer implements Analyzer, Constants {
    
    BinaryCode bincode;

    MethodInstructionHeader methodHeader;
    MethodAnalyzer method;
    public JodeEnvironment env;
    
    /**
     * Get the method.
     * @return The method to which this code belongs.
     */
    public MethodAnalyzer getMethod() {return method;}
    
    void readCode() 
         throws ClassFormatError
    {
        byte[] code = bincode.getCode();
        InstructionHeader[] instr = new InstructionHeader[code.length];
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
        BinaryExceptionHandler[] handlers = bincode.getExceptionHandlers();
	methodHeader = new MethodInstructionHeader(env, instr, handlers);
    }

	/*
        tryAddrs.put(new Integer(handler.startPC), handler);
        references[handler.startPC]++;
        catchAddrs.put(new Integer(handler.handlerPC), handler);
        references[handler.handlerPC]++;
	*/

    public void dumpSource(TabbedPrintWriter writer) 
         throws java.io.IOException
    {
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

    static Transformation[] exprTrafos = {
        new RemoveNop(),
        new CombineCatchLocal(),
        new CreateExpression(),
        new CreatePostIncExpression(),
        new CreateAssignExpression(),
        new CreateNewConstructor(),
        new CombineIfGotoExpressions(),
        new CreateIfThenElseOperator(),
        new CreateConstantArray()
    };

    static Transformation[] simplifyTrafos = { new SimplifyExpression() };
    static Transformation[] blockTrafos = { 
        new CreateDoWhileStatements(),
        new CreateTryCatchStatements(),
        new CreateIfStatements(),
        new CreateBreakStatement(),
        new CreateWhileStatements(),
        new CreateSwitchStatements()
    };

    public void analyze()
    {
        methodHeader.doTransformations(exprTrafos);
        methodHeader.doTransformations(simplifyTrafos);
        methodHeader.doTransformations(blockTrafos);
    }

    public String getTypeString(Type type) {
        return env.getTypeString(type);
    }

    public ClassDefinition getClassDefinition() {
        return env.getClassDefinition();
    }
}

