/* 
 * LocalVariableAnalyzer (c) 1998 Jochen Hoenicke
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
import java.util.*;

public class LocalVariableAnalyzer {
    
    Vector locals;

    LocalInfo[] argLocals;
    LocalVariableTable lvt;
    JodeEnvironment env;
    FieldDefinition mdef;
    int maxlocals;

    LocalVariableAnalyzer(JodeEnvironment env, FieldDefinition mdef, 
			  int maxlocals) {
	this.env = env;
	this.mdef = mdef;
	this.maxlocals = maxlocals;
	locals = new Vector();
    }

    /**
     * Reads the local variable table from the class file
     * if it exists.
     */
    public void read(BinaryCode bc) {
        BinaryAttribute attr = bc.getAttributes();
        while (attr != null) {
            if (attr.getName() == Constants.idLocalVariableTable) {
                DataInputStream stream = 
                    new DataInputStream
                    (new ByteArrayInputStream(attr.getData()));
                try {
                    lvt = new LocalVariableTable(maxlocals);
                    lvt.read(env, stream);
                } catch (IOException ex) {
                    throw new ClassFormatError(ex.toString());
                }
            }
            attr = attr.getNextAttribute();
        }
    }

    /**
     * This method combines to LocalInfos to a single one.
     * It also handles the special cases where one or both LocalInfo
     * are null.
     */
    private LocalInfo combine(int slot, LocalInfo li1, LocalInfo li2) {
        if (li1 == null && li2 == null) {
	    li2 = new LocalInfo(slot);
	    locals.addElement(li2);
	} else if (li1 != null && li2 != null)
            li1.combineWith(li2.getLocalInfo());
        else if (li2 == null)
            li2 = li1;

        return li2.getLocalInfo();
    }

    public void analyze(MethodInstructionHeader mih) {
        {
            Type[] paramTypes = mdef.getType().getArgumentTypes();
            int length = (mdef.isStatic() ? 0 : 1) + paramTypes.length;
            argLocals = new LocalInfo[length];
            int offset = 0;
            if (!mdef.isStatic()) {
                argLocals[0] = new LocalInfo(0);
                argLocals[0].setType(mdef.getClassDefinition().getType());
                offset++;
            }
            for (int i=0; i< paramTypes.length; i++) {
                argLocals[offset+i] = new LocalInfo(i+offset);
                argLocals[offset+i].setType(paramTypes[i]);
            }
        }

	Hashtable done = new Hashtable();
	Stack instrStack = new Stack();
	Stack readsStack = new Stack();
        LocalInfo[] reads = new LocalInfo[maxlocals];

	Enumeration predec = mih.getReturns().elements();
	while (predec.hasMoreElements()) {
	    instrStack.push(predec.nextElement());
	    readsStack.push(reads);
	}

	while (!instrStack.empty()) {
	    InstructionHeader instr = 
		(InstructionHeader) instrStack.pop();
	    LocalInfo[] prevReads =
		(LocalInfo[]) done.get(instr);
	    reads = (LocalInfo[]) readsStack.pop();

//             System.err.println("");
//             System.err.print("Addr: "+instr.getAddress()+ " [");
//             for (int i=0; i< maxlocals; i++) {
//                 if (reads[i] != null) 
//                     System.err.print(", "+reads[i].getName().toString());
//             }
//             System.err.print("] ");

	    if (prevReads != null) {
                boolean changed = false;
		for (int i=0; i<maxlocals; i++) {
		    if (reads[i] != null) {
                        reads[i] = reads[i].getLocalInfo();
                        if (prevReads[i] == null) {
                            changed = true;
                        } else if (prevReads[i].getLocalInfo() != reads[i]) {
                            prevReads[i].combineWith(reads[i]);
                            changed = true;
                        }
		    }
                }
		if (!changed)
		    continue;
	    }
            
            if (instr instanceof MethodInstructionHeader) {
                /* This is the first instruction 
                 */
                for (int i=0; i < argLocals.length; i++) {
                    if (reads[i] != null)
                        argLocals[i].combineWith(reads[i]);
                }
            } else if (instr.getInstruction() instanceof LocalVarOperator) {

                if (Decompiler.isVerbose)
                    System.err.print(".");

                LocalVarOperator op = 
                    (LocalVarOperator)instr.getInstruction();
                int slot = op.getSlot();
                
                LocalInfo li = combine(slot, op.getLocalInfo(), 
                                       reads[slot]);
                
                LocalInfo[] newReads = new LocalInfo[maxlocals];
                System.arraycopy(reads, 0, newReads, 0, maxlocals);
                
                op.setLocalInfo(li);
                if (op.isRead())
                    newReads[slot] = li;
                else
                    newReads[slot] = null;
                
                reads = newReads;
                done.put(instr, reads);
            }
            
            predec = instr.getPredecessors().elements();
            while (predec.hasMoreElements()) {
                instrStack.push(predec.nextElement());
                readsStack.push(reads);
            }
	}
        if (!mdef.isStatic())
            argLocals[0].setName(Constants.idThis);
    }

    public void createLocalInfo(CodeAnalyzer code) {
        // System.err.println("createLocalInfo");
        MethodInstructionHeader mih = code.methodHeader;
        if (lvt == null)
            analyze(mih);
        else {
            int length = (mdef.isStatic() ? 0 : 1) + 
                mdef.getType().getArgumentTypes().length;
	    argLocals = new LocalInfo[length];
            for (int i=0; i < length; i++)
                argLocals[i] = lvt.getLocal(i).getInfo(-1);
            for (InstructionHeader ih = mih.getFirst(); 
                 ih != null; ih = ih.getNextInstruction()) {
                if (ih.getInstruction() instanceof LocalVarOperator) {
                    LocalVarOperator op = 
                        (LocalVarOperator)ih.getInstruction();
                    int slot = op.getSlot();
                    LocalInfo li = 
                        lvt.getLocal(slot).getInfo(ih.getAddress());
                    op.setLocalInfo(li);
                } 
            }
        }
    }

    public LocalInfo getLocal(int i) {
        return argLocals[i];
    }

    public void dumpSource(TabbedPrintWriter writer) 
         throws java.io.IOException
    {
	Enumeration enum = locals.elements();
    VAR:
	while (enum.hasMoreElements()) {
	    LocalInfo li = (LocalInfo) enum.nextElement();
	    if (!li.isShadow()) {
                for (int i=0; i< argLocals.length; i++)
                    if (argLocals[i].getLocalInfo() == li)
                        continue VAR;
		writer.println(env.getTypeString(li.getType(), 
						 li.getName())+";");
            }
	}
    }
}
