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
import sun.tools.java.*;

public class ConstructorOperator extends Operator {
    FieldDefinition field;

    public ConstructorOperator(Type type, FieldDefinition field) {
        super(type, 0);
        this.field = field;
    }

    public int getPriority() {
        return 950;
    }

    public int getOperandCount() {
        return 1 + field.getType().getArgumentTypes().length;
    }

    public int getOperandPriority(int i) {
        if (i == 0)
            return 950;
        return 0;
    }

    public Type getOperandType(int i) {
        if (i == 0)
            return field.getClassDeclaration().getType();  // or subtype? XXX
        return MyType.tSubType(field.getType().getArgumentTypes()[i-1]);
    }

    public void setOperandType(Type types[]) {
    }

    public String toString(String[] operands) {
        StringBuffer result = new StringBuffer(operands[0]).append("(");
        for (int i=0; i < field.getType().getArgumentTypes().length; i++) {
            if (i>0)
                result.append(", ");
            result.append(operands[i+1]);
        }
        return result.append(")").toString();
    }
}
