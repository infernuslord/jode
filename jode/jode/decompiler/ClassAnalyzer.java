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
import java.lang.reflect.Modifier;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import gnu.bytecode.ConstantPool;
import gnu.bytecode.CpoolEntry;
import gnu.bytecode.CpoolValue1;
import gnu.bytecode.CpoolValue2;
import gnu.bytecode.CpoolString;
import jode.flow.TransformConstructors;

public class ClassAnalyzer implements Analyzer {
    JodeEnvironment env;
    Analyzer[] analyzers;
    MethodAnalyzer staticConstructor;
    MethodAnalyzer[] constructors;

    Class clazz;
    gnu.bytecode.ClassType classType;
    ClassAnalyzer parent;
    
    public ClassAnalyzer(ClassAnalyzer parent, Class clazz, 
                         JodeEnvironment env)
    {
        this.parent = parent;
        this.clazz = clazz;
        this.env  = env;
        try {
            this.classType = gnu.bytecode.ClassFileInput.
                readClassType(env.getClassStream(clazz));
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
        }
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

    public Class getClazz() {
        return clazz;
    }

    public void analyze() {
        int numFields = 0;
        int i = 0;
        
        Field[] fields = clazz.getDeclaredFields(); 

        analyzers = new Analyzer[fields.length + 
                                classType.getMethodCount()];
        for (int j=0; j< fields.length; j++) {
            analyzers[i] = new FieldAnalyzer(this, fields[j], env);
            analyzers[i++].analyze();
        }

        staticConstructor = null;
        java.util.Vector constrVector = new java.util.Vector();
        for (gnu.bytecode.Method method = classType.getMethods();
             method != null; method = method.getNext()) {
            MethodAnalyzer analyzer = new MethodAnalyzer(this, method, env);
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

	env.useClass(clazz);
        if (clazz.getSuperclass() != null)
            env.useClass(clazz.getSuperclass());
        Class[] interfaces = clazz.getInterfaces();
        for (int j=0; j< interfaces.length; j++)
            env.useClass(interfaces[j]);
    }

    public void dumpSource(TabbedPrintWriter writer) throws java.io.IOException
    {
//         if (cdef.getSource() != null)
//             writer.println("/* Original source: "+cdef.getSource()+" */");

        String modif = Modifier.toString(clazz.getModifiers() 
                                         & ~Modifier.SYNCHRONIZED);
        if (modif.length() > 0)
            writer.print(modif + " ");
        writer.print(clazz.isInterface() 
                     ? ""/*interface is in modif*/ 
                     : "class ");
	writer.println(env.classString(clazz));
	writer.tab();
        Class superClazz = clazz.getSuperclass();
	if (superClazz != null && superClazz != Object.class) {
	    writer.println("extends "+env.classString(superClazz));
        }
        Class[] interfaces = clazz.getInterfaces();
	if (interfaces.length > 0) {
	    writer.print(clazz.isInterface() ? "extends " : "implements ");
	    for (int i=0; i < interfaces.length; i++) {
		if (i > 0)
		    writer.print(", ");
		writer.print(env.classString(interfaces[i]));
	    }
            writer.println("");
	}
	writer.untab();
	writer.println("{");
	writer.tab();

	for (int i=0; i< analyzers.length; i++)
	    analyzers[i].dumpSource(writer);
	writer.untab();
	writer.println("}");
    }

    public CpoolEntry getConstant(int i) {
        return classType.getConstant(i);
    }

    public Type getConstantType(int i) 
         throws ClassFormatError
    {
        CpoolEntry constant = getConstant(i);
        switch(constant.getTag()) {
        case ConstantPool.INTEGER: {
            int value = ((CpoolValue1)constant).getValue();
            return ((value < Short.MIN_VALUE || value > Character.MAX_VALUE) 
                    ? Type.tInt
                    : (value < Byte.MIN_VALUE || value > Byte.MAX_VALUE) 
                    ? Type.tRange(Type.tInt, Type.tChar)
                    : Type.tUInt);
        }
        case ConstantPool.FLOAT  : return Type.tFloat ;
        case ConstantPool.LONG   : return Type.tLong  ;
        case ConstantPool.DOUBLE : return Type.tDouble;
        case ConstantPool.STRING : return Type.tString;
        default:
            throw new ClassFormatError("invalid constant type: "
                                       + constant.getTag());
        }
    }

    private static String quoted(String str) {
        StringBuffer result = new StringBuffer("\"");
        for (int i=0; i< str.length(); i++) {
            switch (str.charAt(i)) {
            case '\t':
                result.append("\\t");
                break;
            case '\n':
                result.append("\\n");
                break;
            case '\\':
                result.append("\\\\");
                break;
            case '\"':
                result.append("\\\"");
                break;
            default:
                result.append(str.charAt(i));
            }
        }
        return result.append("\"").toString();
    }

    public String getConstantString(int i) 
    {
        CpoolEntry constant = classType.getConstant(i);
        switch (constant.getTag()) {
        case ConstantPool.INTEGER: 
            return Integer.toString(((CpoolValue1)constant).getValue());
        case ConstantPool.FLOAT:
            return Float.toString
                (Float.intBitsToFloat(((CpoolValue1)constant).getValue()));
        case ConstantPool.LONG:
            return Long.toString(((CpoolValue2)constant).getValue());
        case ConstantPool.DOUBLE:
            return Double.toString
                (Double.longBitsToDouble(((CpoolValue2)constant).getValue()));
        case ConstantPool.STRING: 
            return quoted(((CpoolString)constant).getString().getString());
        }
        throw new AssertError("unknown constant type");
    }
        
    public String getTypeString(Type type) {
        return type.toString();
    }

    public String getTypeString(Type type, String name) {
        return type.toString() + " " + name;
    }
}

