/* 
 * ClassAnalyzer (c) 1998 Jochen Hoenicke
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
import jode.bytecode.ClassInfo;
import jode.bytecode.FieldInfo;
import jode.bytecode.MethodInfo;
import jode.bytecode.ConstantPool;
import jode.bytecode.ClassFormatException;
import jode.expr.Expression;
import jode.flow.TransformConstructors;
import java.util.NoSuchElementException;
import java.lang.reflect.Modifier;

public class ClassAnalyzer implements Analyzer {
    JodeEnvironment env;
    FieldAnalyzer[] fields;
    MethodAnalyzer[] methods;
    MethodAnalyzer staticConstructor;
    MethodAnalyzer[] constructors;

    ClassInfo clazz;
    ClassAnalyzer parent;
    
    public ClassAnalyzer(ClassAnalyzer parent, ClassInfo clazz, 
                         JodeEnvironment env)
    {
        clazz.loadInfo(clazz.FULLINFO);
        this.parent = parent;
        this.clazz = clazz;
        this.env  = env;
    }

    public FieldAnalyzer getField(String fieldName, Type fieldType) {
        for (int i=0; i< fields.length; i++) {
	    if (fields[i].getName().equals(fieldName)
		&& fields[i].getType().equals(fieldType))
		return fields[i];
        }
	throw new NoSuchElementException
	    ("Field "+fieldType+" "+clazz.getName()+"."+fieldName);
    }
    
    public MethodAnalyzer getMethod(String methodName, MethodType methodType) {
        for (int i=0; i< methods.length; i++) {
	    if (methods[i].getName().equals(methodName)
		&& methods[i].getType().equals(methodType))
		return methods[i];
        }
	throw new NoSuchElementException
	    ("Method "+methodType+" "+clazz.getName()+"."+methodName);
    }
    
    public ClassInfo getClazz() {
        return clazz;
    }

    public void analyze() {
        int numFields = 0;
        int i = 0;
        
        FieldInfo[] finfos = clazz.getFields();
        MethodInfo[] minfos = clazz.getMethods();
        if (finfos == null) {
            /* This means that the class could not be loaded.
             * give up.
             */
            return;
        }

	fields = new FieldAnalyzer[finfos.length];
	methods = new MethodAnalyzer[minfos.length];
        for (int j=0; j < finfos.length; j++) {
            fields[j] = new FieldAnalyzer(this, finfos[j], env);
            fields[j].analyze();
        }

        staticConstructor = null;
        java.util.Vector constrVector = new java.util.Vector();
        for (int j=0; j < methods.length; j++) {
            methods[j] = new MethodAnalyzer(this, minfos[j], env);

            if (methods[j].isConstructor()) {
                if (methods[j].isStatic())
                    staticConstructor = methods[j];
                else
                    constrVector.addElement(methods[j]);
            }
	    // First analyze only synthetic methods.
	    if (methods[j].isSynthetic())
		methods[j].analyze();
        }
        for (int j=0; j < methods.length; j++) {
	    // Now analyze the remaining methods
	    if (!methods[j].isSynthetic())
		methods[j].analyze();
	}
        constructors = new MethodAnalyzer[constrVector.size()];
        if (constructors.length > 0) {
            constrVector.copyInto(constructors);
            TransformConstructors.transform(this, false, constructors);
        }
        if (staticConstructor != null)
            TransformConstructors.transform(this, true, new MethodAnalyzer[] 
                                            { staticConstructor });

	env.useClass(clazz.getName());
        if (clazz.getSuperclass() != null)
            env.useClass(clazz.getSuperclass().getName());
        ClassInfo[] interfaces = clazz.getInterfaces();
        for (int j=0; j< interfaces.length; j++)
            env.useClass(interfaces[j].getName());
    }

    public void dumpSource(TabbedPrintWriter writer) throws java.io.IOException
    {
        if (fields == null) {
            /* This means that the class could not be loaded.
             * give up.
             */
            return;
        }
        String modif = Modifier.toString(clazz.getModifiers() 
                                         & ~Modifier.SYNCHRONIZED);
        if (modif.length() > 0)
            writer.print(modif + " ");
        writer.print(clazz.isInterface() 
                     ? ""/*interface is in modif*/ : "class ");
	writer.println(env.classString(clazz.getName()));
	writer.tab();
        ClassInfo superClazz = clazz.getSuperclass();
	if (superClazz != null && 
            superClazz != ClassInfo.javaLangObject) {
	    writer.println("extends "+env.classString(superClazz.getName()));
        }
        ClassInfo[] interfaces = clazz.getInterfaces();
	if (interfaces.length > 0) {
	    writer.print(clazz.isInterface() ? "extends " : "implements ");
	    for (int i=0; i < interfaces.length; i++) {
		if (i > 0)
		    writer.print(", ");
		writer.print(env.classString(interfaces[i].getName()));
	    }
            writer.println("");
	}
	writer.untab();
	writer.openBrace();
	writer.tab();

	for (int i=0; i< fields.length; i++)
	    fields[i].dumpSource(writer);
	for (int i=0; i< methods.length; i++)
	    methods[i].dumpSource(writer);
	writer.untab();
	writer.closeBrace();
    }

    public ConstantPool getConstantPool() {
        return clazz.getConstantPool();
    }
        
    public String getTypeString(Type type) {
        return type.toString();
    }

    public String getTypeString(Type type, String name) {
        return type.toString() + " " + name;
    }
}
