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

package jode.decompiler;
import jode.*;
import jode.bytecode.*;
import jode.flow.FlowBlock;
import jode.flow.TransformExceptionHandlers;

import java.util.BitSet;
import java.util.Stack;
import java.util.Vector;
import java.util.Enumeration;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class CodeAnalyzer implements Analyzer {
    
    FlowBlock methodHeader;
    BytecodeInfo code;
    MethodAnalyzer method;
    public JodeEnvironment env;

    Vector allLocals = new Vector();
    LocalInfo[] param;
    LocalVariableTable lvt;
    
    /**
     * Get the method.
     * @return The method to which this code belongs.
     */
    public MethodAnalyzer getMethod() {return method;}
    
    public CodeAnalyzer(MethodAnalyzer ma, 
			AttributeInfo codeattr, JodeEnvironment e)
    {
        method = ma;
        env  = e;
	DataInputStream stream = new DataInputStream
	    (new ByteArrayInputStream(codeattr.getContents()));

	ConstantPool cpool = ma.classAnalyzer.getConstantPool();
	code = new BytecodeInfo();
	try {
	    code.read(cpool, stream);
	} catch (IOException ex) {
	    ex.printStackTrace(Decompiler.err);
	    code = null;
	    return;
	}
	
	if (Decompiler.useLVT) {
	    AttributeInfo attr = code.findAttribute("LocalVariableTable");
	    if (attr != null) {
                if (Decompiler.showLVT)
                    Decompiler.err.println("Method: "+ma.getName());
		lvt = new LocalVariableTable(code.getMaxLocals(), cpool, attr);
	    }
	}

	int paramCount = method.getParamCount();
	param = new LocalInfo[paramCount];
	for (int i=0; i<paramCount; i++)
	    param[i] = getLocalInfo(0, i);
    }

    public BytecodeInfo getBytecodeInfo() {
	return code;
    }

    public FlowBlock getMethodHeader() {
        return methodHeader;
    }

    void readCode() {
	/* The adjacent analyzation relies on this */
	DeadCodeAnalysis.removeDeadCode(code);
	Handler[] handlers = code.getExceptionHandlers();

	int returnCount;
        TransformExceptionHandlers excHandlers; 
	{
	    /* First create a FlowBlock for every block that has a 
	     * predecessor other than the previous instruction.
	     */
	    for (Instruction instr = code.getFirstInstr();
		 instr != null; instr = instr.nextByAddr) {
		if (instr.prevByAddr == null
		    || instr.prevByAddr.alwaysJumps
		    || instr.preds.size() != 1)
		    instr.tmpInfo = new FlowBlock
			(this, instr.addr, instr.length);
		else
		    instr.tmpInfo = null;
	    }

	    for (int i=0; i < handlers.length; i++) {
		Instruction instr = handlers[i].start;
		if (instr.tmpInfo == null)
		    instr.tmpInfo 
			= new FlowBlock(this, instr.addr, instr.length);
		instr = handlers[i].catcher;
		if (instr.tmpInfo == null)
		    instr.tmpInfo 
			= new FlowBlock(this, instr.addr, instr.length);
	    }

            /* While we read the opcodes into FlowBlocks 
             * we try to combine sequential blocks, as soon as we
             * find two sequential instructions in a row, where the
             * second has no predecessors.
             */
            int mark = 1000;
            FlowBlock lastBlock = null;
	    boolean   lastSequential = false;
	    for (Instruction instr = code.getFirstInstr();
		 instr != null; instr = instr.nextByAddr) {

		jode.flow.StructuredBlock block
		    = Opcodes.readOpcode(instr, this);

                if (jode.Decompiler.isVerbose && instr.addr > mark) {
                    Decompiler.err.print('.');
                    mark += 1000;
                }

                if (lastSequential && instr.tmpInfo == null
		    /* Only merge with previous block, if this is sequential, 
		     * too.  
		     * Why?  doSequentialT1 does only handle sequential blocks.
		     */
		    && !instr.alwaysJumps && instr.succs == null) {
		    
                    lastBlock.doSequentialT1(block, instr.length);

                } else {

		    if (instr.tmpInfo == null)
			instr.tmpInfo = new FlowBlock
			    (this, instr.addr, instr.length);
		    FlowBlock flowBlock = (FlowBlock) instr.tmpInfo;
		    flowBlock.setBlock(block);

		    if (lastBlock != null)
			lastBlock.setNextByAddr(flowBlock);

                    instr.tmpInfo = lastBlock = flowBlock;
		    lastSequential = !instr.alwaysJumps && instr.succs == null;
                }
	    }

	    methodHeader = (FlowBlock) code.getFirstInstr().tmpInfo;
            excHandlers = new TransformExceptionHandlers();
            for (int i=0; i<handlers.length; i++) {
                Type type = null;
                FlowBlock start 
		    = (FlowBlock) handlers[i].start.tmpInfo;
                int endAddr = handlers[i].end.nextByAddr.addr;
                FlowBlock handler
		    = (FlowBlock) handlers[i].catcher.tmpInfo;
                if (handlers[i].type != null)
                    type = Type.tClass(handlers[i].type);
                
                excHandlers.addHandler(start, endAddr, handler, type);
            }
        }

        if (Decompiler.isVerbose)
            Decompiler.err.print('-');
            
        excHandlers.analyze();
        methodHeader.analyze();
    } 

    public void analyze()
    {
	if (code == null)
	    return;
        readCode();
	if (!Decompiler.usePUSH && methodHeader.mapStackToLocal())
	    methodHeader.removePush();
	if (Decompiler.removeOnetimeLocals)
	    methodHeader.removeOnetimeLocals();

        Enumeration enum = allLocals.elements();
        while (enum.hasMoreElements()) {
            LocalInfo li = (LocalInfo)enum.nextElement();
            if (!li.isShadow())
                li.getType().useType();
        }
	for (int i=0; i < param.length; i++) {
	    for (int j=0; j < i; j++) {
		if (param[j].getName().equals(param[i].getName())) {
		    /* A name conflict happened. */
		    param[i].makeNameUnique();
		    break; /* j */
		}
	    }
	}
	methodHeader.makeDeclaration(new jode.flow.VariableSet(param));
    }

    public void dumpSource(TabbedPrintWriter writer) 
         throws java.io.IOException
    {
	if (methodHeader != null)
	    methodHeader.dumpSource(writer);
	else
	    writer.println("COULDN'T DECOMPILE METHOD!");
    }

    public LocalInfo getLocalInfo(int addr, int slot) {
        LocalInfo li = (lvt != null)
            ? lvt.getLocal(slot).getInfo(addr)
            : new LocalInfo(slot);
        if (!allLocals.contains(li))
            allLocals.addElement(li);
        return li;
    }

    /**
     * Checks if the variable set contains a local with the given name.
     */
    public LocalInfo findLocal(String name) {
        Enumeration enum = allLocals.elements();
        while (enum.hasMoreElements()) {
            LocalInfo li = (LocalInfo) enum.nextElement();
            if (li.getName().equals(name))
                return li;
        }
        return null;
    }

    public LocalInfo getParamInfo(int slot) {
	return param[slot];
    }

    public void useClass(String clazz) 
    {
        env.useClass(clazz);
    }

    public String getTypeString(Type type) {
        return method.classAnalyzer.getTypeString(type);
    }

    public ClassAnalyzer getClassAnalyzer() {
	return method.classAnalyzer;
    }

    public ClassInfo getClazz() {
        return method.classAnalyzer.clazz;
    }
}
