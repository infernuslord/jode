/* 
 * CompareBinaryOperator (c) 1998 Jochen Hoenicke
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
import sun.tools.java.Type;

public class CompareBinaryOperator extends SimpleOperator {
    public CompareBinaryOperator(Type type, int op) {
        super(Type.tBoolean, op, 2);
        operandTypes[0] = operandTypes[1] = type;
    }

    public int getPriority() {
        switch (getOperatorIndex()) {
        case 26:
        case 27:
            return 500;
        case 28:
        case 29:
        case 30:
        case 31: 
            return 550;
        }
        throw new RuntimeException("Illegal operator");
    }

    public int getOperandPriority(int i) {
        return getPriority()+i;
    }

    public void setOperandType(Type[] inputTypes) {
        super.setOperandType(inputTypes);
        Type operandType = 
	    MyType.tSubType(MyType.intersection
			    (MyType.tSuperType(operandTypes[0]),
			     MyType.tSuperType(operandTypes[1])));
        operandTypes[0] = operandTypes[1] = operandType;
    }

    public boolean equals(Object o) {
	return (o instanceof CompareBinaryOperator) &&
	    ((CompareBinaryOperator)o).operator == operator;
    }

    public String toString(String[] operands) {
        return operands[0] + getOperatorString() + operands[1];
    }
}
