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
import jode.bytecode.ClassInfo;
import jode.bytecode.ConstantPool;
import jode.bytecode.AttributeInfo;
import jode.bytecode.CodeInfo;
import jode.bytecode.Opcodes;
import jode.flow.FlowBlock;
import jode.flow.TransformExceptionHandlers;

import java.util.Stack;
import java.util.Vector;
import java.util.Enumeration;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class CodeAnalyzer implements Analyzer {
    
    FlowBlock methodHeader;
    CodeInfo code;
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
    
    public CodeAnalyzer(MethodAnalyzer ma, CodeInfo bc, JodeEnvironment e)
         throws ClassFormatError
    {
        code = bc;
        method = ma;
        env  = e;

        AttributeInfo attr = code.findAttribute("LocalVariableTable");
        if (attr != null)
            lvt = new LocalVariableTable(bc.getMaxLocals(), 
                                         method.classAnalyzer, attr);

	int paramCount = method.getParamCount();
	param = new LocalInfo[paramCount];
	for (int i=0; i<paramCount; i++)
	    param[i] = getLocalInfo(0, i);
    }

    public FlowBlock getMethodHeader() {
        return methodHeader;
    }

    private final static int SEQUENTIAL   = 1;
    private final static int PREDECESSORS = 2;
    /**
     * @param code The code array.
     * @param handlers The exception handlers.
     */
    void readCode(byte[] code, int[] handlers)
         throws ClassFormatError
    {
        ConstantPool cpool = method.classAnalyzer.getConstantPool();
        byte[] flags = new byte[code.length];
        int[] lengths = new int[code.length];
        try {
            DataInputStream stream = 
                new DataInputStream(new ByteArrayInputStream(code));
	    for (int addr = 0; addr < code.length; ) {
                int[] succs = Opcodes.getSizeAndSuccs(addr, stream);
                if  (succs.length == 2 
                     && succs[1] == addr + succs[0])
                    flags[addr] |= SEQUENTIAL;
                lengths[addr] = succs[0];
                addr += succs[0];
                for (int i=1; i<succs.length; i++)
                    if (succs[i] != addr)
                        flags[succs[i]] |= PREDECESSORS;
	    }
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new ClassFormatError(ex.getMessage());
        }
        for (int i=0; i<handlers.length; i += 4) {
            int start = handlers[i + 0];
            int handler = handlers[i + 2];
            if (start < 0) start += 65536;
            if (handler < 0) handler += 65536;
            flags[start]   |= PREDECESSORS;
            flags[handler] |= PREDECESSORS;
        }

        FlowBlock[] instr = new FlowBlock[code.length];
	int returnCount;
        TransformExceptionHandlers excHandlers; 
        try {
            DataInputStream stream = 
                new DataInputStream(new ByteArrayInputStream(code));

            /* While we read the opcodes into FlowBlocks 
             * we try to combine sequential blocks, as soon as we
             * find two sequential instructions in a row, where the
             * second has no predecessors.
             */
            int mark = 1000;
            FlowBlock lastBlock = null;
	    for (int addr = 0; addr < code.length; ) {
		jode.flow.StructuredBlock block
                    = Opcodes.readOpcode(cpool, addr, stream, this);

                if (jode.Decompiler.isVerbose && addr > mark) {
                    Decompiler.err.print('.');
                    mark += 1000;
                }

                if (lastBlock != null && flags[addr] == SEQUENTIAL) {

                    lastBlock.doSequentialT1(block, lengths[addr]);

                } else {
                    
                    instr[addr] = new FlowBlock(this, addr, 
                                                lengths[addr], block);
                    lastBlock =  ((flags[addr] & SEQUENTIAL) == 0) 
                        ? null : instr[addr];

                }
                addr += lengths[addr];
	    }

            for (int addr=0; addr<instr.length; ) {
                instr[addr].resolveJumps(instr);
                addr = instr[addr].getNextAddr();
            }
            
	    methodHeader = instr[0];
	    methodHeader.markReachable();
            excHandlers = new TransformExceptionHandlers();
            for (int i=0; i<handlers.length; i += 4) {
                Type type = null;
                int start   = handlers[i + 0];
                int end     = handlers[i + 1];
                int handler = handlers[i + 2];
                if (start < 0) start += 65536;
                if (end < 0) end += 65536;
                if (handler < 0) handler += 65536;
                if (handlers[i + 3 ] != 0)
                    type = Type.tClass(cpool.getClassName(handlers[i + 3]));
                
                excHandlers.addHandler(instr[start], end, 
				       instr[handler], type);
		instr[handler].markReachable();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new ClassFormatError(ex.getMessage());
        }

	FlowBlock.removeDeadCode(methodHeader);

        if (Decompiler.isVerbose)
            Decompiler.err.print('-');
            
        excHandlers.analyze();
        methodHeader.analyze();
    } 

    public void analyze()
    {
        byte[] codeArray = code.getCode();
        int[] handlers = code.getExceptionHandlers();
        readCode(codeArray, handlers);
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
        methodHeader.dumpSource(writer);
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

    public ClassInfo getClazz() {
        return method.classAnalyzer.clazz;
    }
}
