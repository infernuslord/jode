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
import jode.flow.TransformExceptionHandlers;

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
    
    TransformExceptionHandlers handler;
    FlowBlock methodHeader;
    CodeAttr code;
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
    
    public CodeAnalyzer(MethodAnalyzer ma, CodeAttr bc, JodeEnvironment e)
         throws ClassFormatError
    {
        code = bc;
        method = ma;
        env  = e;

        LocalVarsAttr attr = 
            (LocalVarsAttr) Attribute.get(bc, "LocalVariableTable");
        if (attr != null)
            lvt = new LocalVariableTable(bc.getMaxLocals(), 
                                         method.classAnalyzer, attr);

	int paramCount = method.getParamCount();
	param = new jode.flow.VariableSet();
	for (int i=0; i<paramCount; i++)
	    param.addElement(getLocalInfo(0, i));
    }

    void readCode(CodeAttr bincode) 
         throws ClassFormatError
    {
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

        handler = new TransformExceptionHandlers(instr);
        short[] handlers = gnu.bytecode.Spy.getExceptionHandlers(bincode);

        for (int i=0; i<handlers.length; i += 4) {
            Type type = null;
            if (handlers[i + 3 ] != 0) {
                CpoolClass cpcls = (CpoolClass)
                    method.classAnalyzer.getConstant(handlers[i + 3]);
                type = Type.tClass(cpcls.getName().getString());
            }

            handler.addHandler(handlers[i + 0], handlers[i + 1],
                               handlers[i + 2], type);
        }
	methodHeader = instr[0];
    }

    public void analyze()
    {
        readCode(code);
        handler.analyze();
        methodHeader.analyze();
        Enumeration enum = allLocals.elements();
        while (enum.hasMoreElements()) {
            LocalInfo li = (LocalInfo)enum.nextElement();
            if (!li.isShadow())
                li.getType().useType();
        }
    }

    public void dumpSource(TabbedPrintWriter writer) 
         throws java.io.IOException
    {
	methodHeader.makeDeclaration(param);
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

    public LocalInfo getParamInfo(int slot) {
	return (LocalInfo) param.elementAt(slot);
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

