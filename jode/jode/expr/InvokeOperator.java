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
    boolean staticFlag;
    boolean specialFlag;
    MethodType methodType;
    Type classType;
    CpoolRef field;

    public InvokeOperator(CodeAnalyzer codeAnalyzer,
                          boolean staticFlag, boolean specialFlag, 
                          CpoolRef field) {
        super(Type.tError, 0);
        methodType = new MethodType(field.getNameAndType().
                                    getType().getString());
        classType = Type.tClass(field.getCpoolClass().getName().getString());
        setType(methodType.getReturnType());
        this.codeAnalyzer  = codeAnalyzer;
        this.staticFlag = staticFlag;
        this.specialFlag = specialFlag;
        this.field = field;
    }

    public boolean isStatic() {
        return staticFlag;
    }

    public CpoolRef getField() {
        return field;
    }

    public MethodType getMethodType() {
        return methodType;
    }

    public Type getClassType() {
        return classType;
    }

    public int getPriority() {
        return 950;
    }

    public int getOperandCount() {
        return (staticFlag?0:1) + methodType.getArgumentTypes().length;
    }

    public int getOperandPriority(int i) {
        if (!staticFlag && i == 0)
            return 950;
        return 0;
    }

    public Type getOperandType(int i) {
        if (!staticFlag) {
            if (i == 0)
                return getClassType();
            i--;
        }
        return methodType.getArgumentTypes()[i];
    }

    public void setOperandType(Type types[]) {
    }

    public boolean isConstructor() {
        return field.getNameAndType().getName().getString().equals("<init>");
    }

    public String toString(String[] operands) {
        String object = 
            staticFlag
            ? ((field.getCpoolClass().getName().getString()
                .replace(java.io.File.separatorChar, '.')
                .equals(codeAnalyzer.getClazz().getName()))
               ? ""
               : codeAnalyzer.getTypeString(getClassType()))
            : (operands[0].equals("this") 
               ? ((specialFlag &&
                   (field.getCpoolClass().getName().getString()
                    .replace(java.io.File.separatorChar, '.')
                    .equals(codeAnalyzer.getClazz()
                            .getSuperclass().getName())))
                  ? "super"
                  : "")
               : operands[0]);

        int arg = staticFlag ? 0 : 1;
        String method;
        if (isConstructor())
            method = (object.length() == 0 ? "this" : object);
        else
            method = (object.length() == 0 ? "" : object + ".")
                + field.getNameAndType().getName().getString();

        StringBuffer params = new StringBuffer();
        for (int i=0; i < methodType.getArgumentTypes().length; i++) {
            if (i>0)
                params.append(", ");
            params.append(operands[arg++]);
        }
        return method+"("+params+")";
    }

    public boolean equals(Object o) {
	return (o instanceof InvokeOperator) &&
	    ((InvokeOperator)o).field == field &&
	    ((InvokeOperator)o).staticFlag == staticFlag &&
	    ((InvokeOperator)o).specialFlag == specialFlag;
    }
}
