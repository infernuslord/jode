/* 
 * ConstructorOperator (c) 1998 Jochen Hoenicke
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

public class ConstructorOperator extends Operator {
    MethodType methodType;
    Type classType;

    public ConstructorOperator(Type type, MethodType methodType, 
                               boolean isVoid) {
        super(isVoid ? Type.tVoid : type, 0);
        this.classType  = type;
        this.methodType = methodType;
    }

    public int getPriority() {
        return 950;
    }

    public int getOperandCount() {
        return methodType.getParameterTypes().length;
    }

    public int getOperandPriority(int i) {
        return 0;
    }

    public Type getClassType() {
        return classType;
    }

    public Type getOperandType(int i) {
        return methodType.getParameterTypes()[i];
    }

    public void setOperandType(Type types[]) {
    }

    public Expression simplifyStringBuffer() {
        return (getClassType() == Type.tStringBuffer)
            ? EMPTYSTRING : null;
    }

    public String toString(String[] operands) {
        StringBuffer result = 
            new StringBuffer("new ").append(classType.toString()).append("(");
        for (int i=0; i < methodType.getParameterTypes().length; i++) {
            if (i>0)
                result.append(", ");
            result.append(operands[i]);
        }
        return result.append(")").toString();
    }
}
