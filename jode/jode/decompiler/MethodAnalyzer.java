/* 
 * MethodAnalyzer (c) 1998 Jochen Hoenicke
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
import jode.bytecode.MethodInfo;
import jode.bytecode.AttributeInfo;
import jode.bytecode.CodeInfo;

import java.lang.reflect.Modifier;
import java.io.*;

public class MethodAnalyzer implements Analyzer {
    JodeEnvironment env;
    CodeAnalyzer code = null;
    ClassAnalyzer classAnalyzer;
    boolean isConstructor;
    int modifiers;
    String methodName;
    MethodType methodType;
    Type[] exceptions;
    
    public MethodAnalyzer(ClassAnalyzer cla, MethodInfo minfo,
                          JodeEnvironment env) {
        this.classAnalyzer = cla;
        this.env = env;
        this.modifiers = minfo.getModifiers();
        this.methodType = minfo.getType();
        this.methodName = minfo.getName();
        this.isConstructor = 
            methodName.equals("<init>") || methodName.equals("<clinit>");
        
        AttributeInfo codeattr = minfo.findAttribute("Code");
        if (codeattr != null) {
            DataInputStream stream = new DataInputStream
                (new ByteArrayInputStream(codeattr.getContents()));
            CodeInfo codeinfo = new CodeInfo();
            try {
                codeinfo.read(classAnalyzer.getConstantPool(), stream);
                code = new CodeAnalyzer(this, codeinfo, env);
            } catch (IOException ex) {
                ex.printStackTrace();
                code = null;
            }
        }
        
        AttributeInfo excattr = minfo.findAttribute("Exceptions");
        if (excattr == null) {
            exceptions = new Type[0];
        } else {
            DataInputStream stream = new DataInputStream
                (new ByteArrayInputStream(excattr.getContents()));
            try {
                int throwCount = stream.readUnsignedShort();
                this.exceptions = new Type[throwCount];
                for (int t=0; t< throwCount; t++) {
                    int idx = stream.readUnsignedShort();
                    exceptions[t] = Type.tClass(classAnalyzer.getConstantPool()
                                                .getClassName(idx));
                }
            } catch (IOException ex) {
                throw new AssertError("exception attribute too long?");
            }
        }
    }

    public jode.flow.FlowBlock getMethodHeader() {
        return code != null ? code.getMethodHeader() : null;
    }

    public boolean isConstructor() {
        return isConstructor;
    }

    public boolean isStatic() {
        return methodType.isStatic();
    }

    public int getParamCount() {
        int count = isStatic() ? 0 : 1;
        Type[] paramTypes = methodType.getParameterTypes();
        for (int i=0; i< paramTypes.length; i++)
            count += paramTypes[i].stackSize();
	return count;
    }

    public Type getReturnType() {
        return methodType.getReturnType();
    }

    public void analyze() 
      throws ClassFormatError
    {
	if (code == null)
	    return;

	int offset = 0;
	if (!isStatic()) {
	    LocalInfo clazz = code.getParamInfo(0);
	    clazz.setType
                (Type.tClass(this.classAnalyzer.getClazz().getName()));
	    clazz.setName("this");
	    offset++;
	}
        
	Type[] paramTypes = methodType.getParameterTypes();
	for (int i=0; i< paramTypes.length; i++) {
	    code.getParamInfo(offset).setType(paramTypes[i]);
            offset += paramTypes[i].stackSize();
        }

        for (int i= 0; i< exceptions.length; i++)
            exceptions[i].useType();
    
        if (!isConstructor)
            methodType.getReturnType().useType();

	if (!Decompiler.immediateOutput) {
	    if (Decompiler.isVerbose)
		Decompiler.err.print(methodName+": ");
	    code.analyze();
	    if (Decompiler.isVerbose)
		Decompiler.err.println("");
	}
    }
    
    public void dumpSource(TabbedPrintWriter writer) 
         throws IOException
    {
	if (Decompiler.immediateOutput && code != null) {
            // We do the code.analyze() here, to get 
            // immediate output.

	    if (Decompiler.isVerbose)
		Decompiler.err.print(methodName+": ");
	    code.analyze();
	    if (Decompiler.isVerbose)
		Decompiler.err.println("");
	}

        if (isConstructor() && isStatic() 
            && getMethodHeader().getBlock() instanceof jode.flow.EmptyBlock)
            return;

        writer.println("");
	String modif = Modifier.toString(modifiers);
	if (modif.length() > 0)
	    writer.print(modif+" ");
        if (isConstructor && isStatic())
            writer.print(""); /* static block */
        else { 
            if (isConstructor)
                writer.print(env.classString(classAnalyzer.
                                             getClazz().getName()));
            else
                writer.print(getReturnType().toString()
			     + " " + methodName);
            writer.print("(");
            Type[] paramTypes = methodType.getParameterTypes();
            int offset = methodType.isStatic()?0:1;
            for (int i=0; i<paramTypes.length; i++) {
                if (i>0)
                    writer.print(", ");
                LocalInfo li;
                if (code == null) {
                    li = new LocalInfo(offset);
                    li.setType(paramTypes[i]);
                    li.makeNameUnique();
                } else
                    li = code.getParamInfo(offset);
                writer.print(li.getType().toString()+" "+li.getName());
                offset += paramTypes[i].stackSize();
            }
            writer.print(")");
        }
        if (exceptions.length > 0) {
            writer.println("");
            writer.print("throws ");
            for (int i= 0; i< exceptions.length; i++) {
                if (i > 0)
                    writer.print(", ");
                writer.print(exceptions[i].toString());
            }
        }
        if (code != null) {
	    writer.openBrace();
            writer.tab();
            code.dumpSource(writer);
            writer.untab();
	    writer.closeBrace();
        } else
            writer.println(";");
    }
}
