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

package jode;
import jode.bytecode.ClassInfo;
import jode.bytecode.FieldInfo;
import jode.bytecode.MethodInfo;
import jode.bytecode.ConstantPool;
import jode.bytecode.ClassFormatException;
import jode.decompiler.Expression;
import jode.flow.TransformConstructors;

import java.lang.reflect.Modifier;

public class ClassAnalyzer implements Analyzer {
    JodeEnvironment env;
    Analyzer[] analyzers;
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

    public boolean setFieldInitializer(String fieldName, Expression expr) {
        for (int i=0; i< analyzers.length; i++) {
            if (analyzers[i] instanceof FieldAnalyzer) {
                FieldAnalyzer field = (FieldAnalyzer) analyzers[i];
                if (field.getName().equals(fieldName))
                    return field.setInitializer(expr);
            }
        }
        return false;
    }

    public ClassInfo getClazz() {
        return clazz;
    }

    public void analyze() {
        int numFields = 0;
        int i = 0;
        
        FieldInfo[] fields = clazz.getFields();
        MethodInfo[] methods = clazz.getMethods();
        if (fields == null) {
            /* This means that the class could not be loaded.
             * give up.
             */
            return;
        }

        analyzers = new Analyzer[fields.length + 
                                methods.length];
        for (int j=0; j < fields.length; j++) {
            analyzers[i] = new FieldAnalyzer(this, fields[j], env);
            analyzers[i++].analyze();
        }

        staticConstructor = null;
        java.util.Vector constrVector = new java.util.Vector();
        for (int j=0; j < methods.length; j++) {
            MethodAnalyzer analyzer = 
                new MethodAnalyzer(this, methods[j], env);
            analyzers[i++] = analyzer;

            if (analyzer.isConstructor()) {
                if (analyzer.isStatic())
                    staticConstructor = analyzer;
                else
                    constrVector.addElement(analyzer);
            }
            analyzer.analyze();
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
        if (analyzers == null) {
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

	for (int i=0; i< analyzers.length; i++)
	    analyzers[i].dumpSource(writer);
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
