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
import jode.flow.FlowBlock;
import jode.flow.Jump;
import jode.flow.StructuredBlock;
import jode.flow.RawTryCatchBlock;

import java.util.Stack;
import java.util.Vector;
import java.util.Enumeration;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import gnu.bytecode.CodeAttr;
import gnu.bytecode.Attribute;
import gnu.bytecode.LocalVarsAttr;
import gnu.bytecode.CpoolClass;

public class CodeAnalyzer implements Analyzer {
    
    FlowBlock methodHeader;
    MethodAnalyzer method;
    public JodeEnvironment env;

    Vector allLocals = new Vector();
    jode.flow.VariableSet param;
    LocalVariableTable lvt;
    
    /**
     * Get the method.
     * @return The method to which this code belongs.
     */
    public MethodAnalyzer getMethod() {return method;}
    
    void readCode(CodeAttr bincode) 
         throws ClassFormatError
    {

        LocalVarsAttr attr = 
            (LocalVarsAttr) Attribute.get(bincode, "LocalVariableTable");
        if (attr != null)
            lvt = new LocalVariableTable(bincode.getMaxLocals(), 
                                         method.classAnalyzer, attr);

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

        short[] handlers = gnu.bytecode.Spy.getExceptionHandlers(bincode);
        for (int i=0; i<handlers.length; i += 4) {
            FlowBlock startBlock = instr[handlers[i + 0]];

            Type type = null;
            if (handlers[i + 3 ] != 0) {
                CpoolClass cpcls = (CpoolClass)
                    method.classAnalyzer.getConstant(handlers[i + 3]);
                type = Type.tClass(cpcls.getName().getString());
            }

            instr[handlers[i + 0]] =             
                new RawTryCatchBlock(type,
                                     new Jump(instr[handlers[i + 1]]),
                                     new Jump(instr[handlers[i + 2]])
                                     ).chainTo(instr[handlers[i + 0]]);
        }

        for (int addr=0; addr<instr.length; ) {
            instr[addr].resolveJumps(instr);
            addr = instr[addr].getNextAddr();
        }
	methodHeader = instr[0];
        methodHeader.makeStartBlock();

	int paramCount = method.getParamCount();
	param = new jode.flow.VariableSet();
	for (int i=0; i<paramCount; i++)
	    param.addElement(getLocalInfo(0, i));
    }

    public void dumpSource(TabbedPrintWriter writer) 
         throws java.io.IOException
    {
	methodHeader.makeDeclaration(param);
        methodHeader.dumpSource(writer);
    }

    public CodeAnalyzer(MethodAnalyzer ma, CodeAttr bc, JodeEnvironment e)
         throws ClassFormatError
    {
        method = ma;
        env  = e;
        readCode(bc);
    }

    public LocalInfo getLocalInfo(int addr, int slot) {
        LocalInfo li = (lvt != null)
            ? lvt.getLocal(slot).getInfo(addr)
            : new LocalInfo(slot);
        if (!allLocals.contains(li))
            allLocals.addElement(li);
        return li;
    }

    public LocalInfo getParamInfo(int slot) {
	return (LocalInfo) param.elementAt(slot);
    }

    public void analyze()
    {
        methodHeader.analyze();
        Enumeration enum = allLocals.elements();
        while (enum.hasMoreElements()) {
            LocalInfo li = (LocalInfo)enum.nextElement();
            if (!li.isShadow())
                li.getType().useType();
        }
    }

    public void useClass(Class clazz) 
    {
        env.useClass(clazz);
    }

    public String getTypeString(Type type) {
        return method.classAnalyzer.getTypeString(type);
    }

    public Class getClazz() {
        return method.classAnalyzer.clazz;
    }
}

