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
import java.lang.reflect.*;
import gnu.bytecode.Attribute;
import gnu.bytecode.CodeAttr;

public class MethodAnalyzer implements Analyzer {
    JodeEnvironment env;
    CodeAnalyzer code = null;
    ClassAnalyzer classAnalyzer;
    boolean isConstructor;
    Method method;
    Constructor constr;
    
    private MethodAnalyzer(ClassAnalyzer cla, Method m, Constructor c,
                          JodeEnvironment e)
    {
        classAnalyzer = cla;
        method = m;
        constr = c;
        isConstructor = (c != null);
        env  = e;

        String name = isConstructor ? "<init>" : m.getName();
        Class[] paramTypes = (isConstructor 
                              ? c.getParameterTypes()
                              : m.getParameterTypes());

        gnu.bytecode.Method mdef = cla.classType.getMethods();
        while (mdef != null) {
            if (mdef.getName().equals(name)) {
                gnu.bytecode.Type[] argtypes = mdef.getParameterTypes();
                if (argtypes.length == paramTypes.length) {
                    int i;
                    for (i=0; i<argtypes.length; i++) {
                        if (!Type.tType(paramTypes[i]).equals(Type.tType(argtypes[i].getSignature())))
                            break;
                    }
                    if (i == argtypes.length) 
                        break;
                }
            }
            mdef = mdef.getNext();
        }
        
        Attribute attr = Attribute.get(mdef, "Code");
        if (attr != null && attr instanceof CodeAttr)
            code = new CodeAnalyzer(this, (CodeAttr) attr, env);
    }

    public MethodAnalyzer(ClassAnalyzer cla, Method method,
                          JodeEnvironment env)
    {
        this(cla, method, null, env);
    }

    public MethodAnalyzer(ClassAnalyzer cla, Constructor constr,
                          JodeEnvironment env)
    {
        this(cla, null, constr, env);
    }

    public int getParamCount() {
	return isConstructor 
            ? (constr.getParameterTypes().length + 1)
            : ((Modifier.isStatic(method.getModifiers()) ? 0 : 1)
               + method.getParameterTypes().length);
    }

    public Type getReturnType() {
        return isConstructor 
            ? Type.tVoid : Type.tType(method.getReturnType());
    }

    public void analyze() 
      throws ClassFormatError
    {
	if (code == null)
	    return;

	int offset = 0;
	if (isConstructor || !Modifier.isStatic(method.getModifiers())) {
	    LocalInfo clazz = code.getParamInfo(0);
	    clazz.setType(Type.tType(this.classAnalyzer.clazz));
	    clazz.setName("this");
	    offset++;
	}
        
	Class[] paramTypes = isConstructor 
            ? constr.getParameterTypes() : method.getParameterTypes();
	for (int i=0; i< paramTypes.length; i++)
	    code.getParamInfo(offset+i).setType(Type.tType(paramTypes[i]));

        Class[] exceptions = isConstructor
            ? constr.getExceptionTypes() : method.getExceptionTypes();
        for (int i= 0; i< exceptions.length; i++)
            env.useClass(exceptions[i]);
    
        if (!isConstructor)
            getReturnType().useType();

	if (!Decompiler.immediateOutput) {
	    if (Decompiler.isVerbose)
		System.err.print((isConstructor 
                                  ? "<init>" : method.getName())+": ");
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
		System.err.print((isConstructor 
                                  ? "<init>" : method.getName())+": ");
	    code.analyze();
	    if (Decompiler.isVerbose)
		System.err.println("");
	}

        writer.println("");
	String modif = Modifier.toString(isConstructor 
                                         ? constr.getModifiers()
                                         : method.getModifiers());
	if (modif.length() > 0)
	    writer.print(modif+" ");
        if (isConstructor && Modifier.isStatic(constr.getModifiers()))
            writer.print(""); /* static block */
        else { 
            if (isConstructor)
                writer.print(env.classString(classAnalyzer.clazz));
            else
                writer.print(getReturnType().toString()
			     + " " + method.getName());
            writer.print("(");
            Class[] paramTypes = isConstructor 
                 ? constr.getParameterTypes() : method.getParameterTypes();
            int offset = (!isConstructor 
                          && Modifier.isStatic(method.getModifiers()))?0:1;
            for (int i=0; i<paramTypes.length; i++) {
                if (i>0)
                    writer.print(", ");
                LocalInfo li;
                if (code == null) {
                    li = new LocalInfo(i+offset);
                    li.setType(Type.tType(paramTypes[i]));
                } else
                    li = code.getParamInfo(i+offset);
                writer.print(li.getType().toString()+" "+li.getName());
            }
            writer.print(")");
        }
        Class[] exceptions = isConstructor
            ? constr.getExceptionTypes() : method.getExceptionTypes();
        if (exceptions != null && exceptions.length > 0) {
            writer.println("");
            writer.print("throws ");
            for (int i= 0; i< exceptions.length; i++) {
                if (exceptions[i] != null) {
                    if (i > 0)
                        writer.print(", ");
                    writer.print(env.classString(exceptions[i]));
                }
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
