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
import java.lang.reflect.Modifier;
import gnu.bytecode.Attribute;
import gnu.bytecode.CodeAttr;
import gnu.bytecode.CpoolClass;
import gnu.bytecode.Method;
import gnu.bytecode.MiscAttr;
import gnu.bytecode.Spy;

public class MethodAnalyzer implements Analyzer {
    JodeEnvironment env;
    CodeAnalyzer code = null;
    ClassAnalyzer classAnalyzer;
    boolean isConstructor;
    int modifiers;
    String methodName;
    MethodType methodType;
    Type[] exceptions;
    
    public MethodAnalyzer(ClassAnalyzer cla, Method mdef,
                          JodeEnvironment env) {
        this.classAnalyzer = cla;
        this.env = env;
        this.modifiers = Spy.getModifiers(mdef);
        this.methodType = new MethodType(mdef.getStaticFlag(), 
                                         mdef.getSignature());
        this.methodName = mdef.getName();
        this.isConstructor = 
            methodName.equals("<init>") || methodName.equals("<clinit>");
        
        Attribute codeattr = Attribute.get(mdef, "Code");
        if (codeattr != null && codeattr instanceof CodeAttr)
            code = new CodeAnalyzer(this, (CodeAttr) codeattr, env);
        
        Attribute excattr = Attribute.get(mdef, "Exceptions");
        if (excattr == null) {
            exceptions = new Type[0];
        } else {
            java.io.DataInputStream stream = Spy.getAttributeStream
                ((MiscAttr) excattr);
            try {
                int throwCount = stream.readUnsignedShort();
                this.exceptions = new Type[throwCount];
                for (int t=0; t< throwCount; t++) {
                    int idx = stream.readUnsignedShort();
                    CpoolClass cpcls = (CpoolClass) 
                        classAnalyzer.getConstant(idx);
                    exceptions[t] = Type.tClass(cpcls.getName().getString());
                }
            } catch (java.io.IOException ex) {
                throw new AssertError("exception attribute too long?");
            }
        }
    }

    public int getParamCount() {
	return (methodType.isStatic() ? 0 : 1)
            + methodType.getParameterTypes().length;
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
	if (!methodType.isStatic()) {
	    LocalInfo clazz = code.getParamInfo(0);
	    clazz.setType(Type.tType(this.classAnalyzer.clazz));
	    clazz.setName("this");
	    offset++;
	}
        
	Type[] paramTypes = methodType.getParameterTypes();
	for (int i=0; i< paramTypes.length; i++)
	    code.getParamInfo(offset+i).setType(paramTypes[i]);

        for (int i= 0; i< exceptions.length; i++)
            exceptions[i].useType();
    
        if (!isConstructor)
            methodType.getReturnType().useType();

	if (!Decompiler.immediateOutput) {
	    if (Decompiler.isVerbose)
		System.err.print(methodName+": ");
	    code.analyze();
	    if (Decompiler.isVerbose)
		System.err.println("");
	}
    }
    
    public void dumpSource(TabbedPrintWriter writer) 
         throws java.io.IOException
    {
	if (Decompiler.immediateOutput && code != null) {
            // We do the code.analyze() here, to get 
            // immediate output.

	    if (Decompiler.isVerbose)
		System.err.print(methodName+": ");
	    code.analyze();
	    if (Decompiler.isVerbose)
		System.err.println("");
	}

        writer.println("");
	String modif = Modifier.toString(modifiers);
	if (modif.length() > 0)
	    writer.print(modif+" ");
        if (isConstructor && methodType.isStatic())
            writer.print(""); /* static block */
        else { 
            if (isConstructor)
                writer.print(env.classString(classAnalyzer.clazz));
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
                    li = new LocalInfo(i+offset);
                    li.setType(paramTypes[i]);
                } else
                    li = code.getParamInfo(i+offset);
                writer.print(li.getType().toString()+" "+li.getName());
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
            writer.println(" {");
            writer.tab();
            code.dumpSource(writer);
            writer.untab();
            writer.println("}");
        } else
            writer.println(";");
    }
}
