/* 
 * InvokeOperator (c) 1998 Jochen Hoenicke
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
import gnu.bytecode.CpoolRef;

public final class InvokeOperator extends Operator {
    CodeAnalyzer codeAnalyzer;
    boolean specialFlag;
    MethodType methodType;
    String methodName;
    Type classType;

    public InvokeOperator(CodeAnalyzer codeAnalyzer,
                          boolean specialFlag, Type classType, 
                          MethodType methodType, String methodName) {
        super(Type.tUnknown, 0);
        this.methodType = methodType;
        this.methodName = methodName;
        this.classType = classType;
        this.type = methodType.getReturnType();
        this.codeAnalyzer  = codeAnalyzer;
        this.specialFlag = specialFlag;
        if (methodType.isStatic())
            classType.useType();
    }

    public boolean isStatic() {
        return methodType.isStatic();
    }

    public MethodType getMethodType() {
        return methodType;
    }

    public String getMethodName() {
        return methodName;
    }

    public Type getClassType() {
        return classType;
    }

    public int getPriority() {
        return 950;
    }

    public int getOperandCount() {
        return (methodType.isStatic()?0:1) 
            + methodType.getParameterTypes().length;
    }

    public int getOperandPriority(int i) {
        if (!methodType.isStatic() && i == 0)
            return 950;
        return 0;
    }

    public Type getOperandType(int i) {
        if (!methodType.isStatic()) {
            if (i == 0)
                return getClassType();
            i--;
        }
        return methodType.getParameterTypes()[i];
    }

    public void setOperandType(Type types[]) {
    }

    public boolean isConstructor() {
        return methodName.equals("<init>");
    }

    public boolean isThis() {
        Class clazz = codeAnalyzer.method.classAnalyzer.getClazz();
        return (classType.equals(Type.tType(clazz)));
    }

    public boolean isSuperOrThis() {
        Class clazz = codeAnalyzer.method.classAnalyzer.getClazz();
        while (clazz != null 
               && !classType.equals(Type.tType(clazz))) {
            clazz = clazz.getSuperclass();
        }
        return (clazz != null);
    }

    public String toString(String[] operands) {
        String object = null;
        if (specialFlag) {
            Class clazz = codeAnalyzer.method.classAnalyzer.clazz;
            if (operands[0].equals("this")) {
                object = "";
                while (clazz != null 
                       && !classType.equals(Type.tType(clazz))) {
                    object = "super";
                    clazz = clazz.getSuperclass();
                }
                
                if (clazz == null)
                    object = "NON VIRTUAL this";
            } else if (classType.equals(Type.tType(clazz)))
                object = operands[0];
            else
                object = "NON VIRTUAL "+operands[0];
        }
            
        object = (object != null) ? object
            : methodType.isStatic()
            ? (classType.equals(Type.tType(codeAnalyzer.getClazz()))
               ? ""
               : classType.toString())
            : (operands[0].equals("this") 
               ? (specialFlag &&
                  classType.equals(Type.tType(codeAnalyzer.getClazz()
                                              .getSuperclass()))
                  ? "super"
                  : "")
               : operands[0]);

        int arg = methodType.isStatic() ? 0 : 1;
        String method = isConstructor() 
            ? (object.length() == 0 ? "this" : object)
            : (object.length() == 0 ? "" : object + ".") + methodName;

        StringBuffer params = new StringBuffer();
        for (int i=0; i < methodType.getParameterTypes().length; i++) {
            if (i>0)
                params.append(", ");
            params.append(operands[arg++]);
        }
        return method+"("+params+")";
    }

    public boolean equals(Object o) {
	return o instanceof InvokeOperator &&
	    ((InvokeOperator)o).classType.equals(classType) &&
	    ((InvokeOperator)o).methodName.equals(methodName) &&
	    ((InvokeOperator)o).methodType.equals(methodType) &&
	    ((InvokeOperator)o).specialFlag == specialFlag;
    }
}
