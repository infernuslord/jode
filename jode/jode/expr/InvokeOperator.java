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

package jode.decompiler;
import jode.CodeAnalyzer;
import jode.MethodType;
import jode.Type;
import jode.bytecode.ClassInfo;

public final class InvokeOperator extends Operator 
    implements MatchableOperator {
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

    /**
     * Checks if the value of the given expression can change, due to
     * side effects in this expression.  If this returns false, the 
     * expression can safely be moved behind the current expresion.
     * @param expr the expression that should not change.
     */
    public boolean hasSideEffects(Expression expr) {
	return expr.containsConflictingLoad(this);
    }

    /**
     * Checks if the value of the operator can be changed by this expression.
     */
    public boolean matches(Operator loadop) {
        return (loadop instanceof InvokeOperator
		|| loadop instanceof ConstructorOperator
		|| loadop instanceof GetFieldOperator);
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

    /**
     * Checks, whether this is a call of a method from this class.
     * @XXX check, if this class implements the method and if not
     * allow super class
     */
    public boolean isThis() {
        return (classType.equals(Type.tClass(codeAnalyzer.getClazz().
                                             getName())));
    }

    /**
     * Checks, whether this is a call of a method from the super class.
     * @XXX check, if its the first super class that implements the method.
     */
    public boolean isSuperOrThis() {
        return ((jode.ClassInterfacesType)classType).getClazz().superClassOf
            (codeAnalyzer.getClazz());
    }

    public String toString(String[] operands) {
        String object = specialFlag 
            ? (operands[0].equals("this") 
               ? (/* XXX check if this is a private or final method. */
                  isThis() ? "" : "super")
               : (/* XXX check if this is a private or final method. */
                  isThis() ? operands[0] : "NON VIRTUAL " + operands[0]))
            : (methodType.isStatic()
               ? (isThis() ? "" : classType.toString())
               : (operands[0].equals("this") ? "" : operands[0]));

        int arg = methodType.isStatic() ? 0 : 1;
        String method = isConstructor() 
            ? (object.length() == 0 ? "this" : object)
            : (object.length() == 0 ? methodName : object + "." + methodName);

        StringBuffer params = new StringBuffer();
        for (int i=0; i < methodType.getParameterTypes().length; i++) {
            if (i>0)
                params.append(", ");
            params.append(operands[arg++]);
        }
        return method+"("+params+")";
    }

    /* Invokes never equals: they may return different values even if
     * they have the same parameters.
     */
}
