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
import sun.tools.java.*;

public class InvokeOperator extends Operator {
    CodeAnalyzer codeAnalyzer;
    boolean staticFlag;
    boolean specialFlag;
    FieldDefinition field;

    public InvokeOperator(CodeAnalyzer codeAnalyzer,
                          boolean staticFlag, boolean specialFlag, 
                          FieldDefinition field) {
        super(Type.tType(field.getType().getReturnType().getTypeSignature()), 
              0);
        this.codeAnalyzer  = codeAnalyzer;
        this.staticFlag = staticFlag;
        this.specialFlag = specialFlag;
        this.field = field;
    }

    public boolean isStatic() {
        return staticFlag;
    }

    public FieldDefinition getField() {
        return field;
    }

    public Type getClassType() {
        return Type.tClass(field.getClassDeclaration().getName().toString());
    }

    public int getPriority() {
        return 950;
    }

    public int getOperandCount() {
        return (staticFlag?0:1) + field.getType().getArgumentTypes().length;
    }

    public int getOperandPriority(int i) {
        if (!staticFlag && i == 0)
            return 950;
        return 0;
    }

    public Type getOperandType(int i) {
        if (!staticFlag) {
            if (i == 0)
                return Type.tSubType(getClassType());
            i--;
        }
        return Type.tSubType(Type.tType(field.getType().
                                        getArgumentTypes()[i].
                                        getTypeSignature()));
    }

    public void setOperandType(Type types[]) {
    }

    public boolean isConstructor() {
        return field.isConstructor();
    }

    public String toString(String[] operands) {
        String object;
        int arg = 0;
        if (staticFlag) {
            if (field.getClassDefinition() == codeAnalyzer.getClassDefinition())
                object = "";
            else
                object = codeAnalyzer.
                    getTypeString(getClassType());
        } else {
            if (operands[arg].equals("this")) {
                if (specialFlag
                    && (field.getClassDeclaration()
                        == codeAnalyzer.getClassDefinition().getSuperClass()))
//                         || (field.getClassDeclaration().getName() 
//                             == Constants.idJavaLangObject 
//                             && codeAnalyzer.getClassDefinition()
//                             .getSuperClass() == null)))
                    object = "super";
                else if (specialFlag)
                    object = "(("+codeAnalyzer.getTypeString(getClassType())
                        + ") this)";
                else
                    object = "";
            } else {
                if (specialFlag)
                    object = "((" + codeAnalyzer.getTypeString(getClassType())
                        + ") " + operands[arg]+")";
                else
                    object = operands[arg];
            }
            arg++;
        }
        String method;
        if (isConstructor()) {
            if (object.length() == 0)
                method = "this";
            else
                method = object;
        } else {
            if (object.length() == 0)
                method = field.getName().toString();
            else
                method = object+"."+field.getName().toString();
        }
        StringBuffer params = new StringBuffer();
        for (int i=0; i < field.getType().getArgumentTypes().length; i++) {
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
