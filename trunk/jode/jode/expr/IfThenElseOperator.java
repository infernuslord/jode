/* 
 * IfThenElseOperator (c) 1998 Jochen Hoenicke
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

public class IfThenElseOperator extends SimpleOperator {
    public IfThenElseOperator(Type type) {
        super(type, 0, 3);
        operandTypes[0] = Type.tBoolean;
    }
    
    public int getOperandCount() {
        return 3;
    }

    public int getPriority() {
        return 200;
    }

    public int getOperandPriority(int i) {
        switch (i) {
        case 0:
            return 201;
        case 1:
            return 0;
        case 2:
            return 200;
        default:
            throw new AssertError("ifthenelse with operand "+i);
        }
    }

    public void setOperandType(Type[] inputTypes) {
        super.setOperandType(inputTypes);
        Type operandType = 
            MyType.intersection(operandTypes[1],operandTypes[2]);
        type = operandTypes[1] = operandTypes[2] = operandType;
    }

    /**
     * Sets the return type of this operator.
     * @return true if the operand types changed
     */
    public boolean setType(Type newType) {
        Type operandType = 
            MyType.intersection(operandTypes[1], newType);
        if (type != operandType) {
            type = operandTypes[1] = operandTypes[2] = operandType;
            return true;
        }
        return false;
    }

    public boolean equals(Object o) {
	return (o instanceof IfThenElseOperator);
    }

    public String toString(String[] operands) {
        return operands[0] + " ? "+operands[1]+" : "+ operands[2];
    }
}
