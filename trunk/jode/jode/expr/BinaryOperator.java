/* 
 * BinaryOperator (c) 1998 Jochen Hoenicke
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
import jode.Type;

public class BinaryOperator extends Operator {
    protected Type operandType;

    public BinaryOperator(Type type, int op) {
        super(type, op);
        operandType = type;
    }
    
    public int getOperandCount() {
        return 2;
    }

    public int getPriority() {
        switch (operator) {
        case 1: case 2:
            return 610;
        case 3: case 4: case 5:
            return 650;
        case 6: case 7: case 8:
            return 600;
        case 9: 
            return 450;
        case 10:
            return 410;
        case 11:
            return 420;
        case 12: case 13: case 14: case 15: case 16: case 17: 
        case 18: case 19: case 20: case 21: case 22: case 23:
            return 100;
        case LOG_OR_OP:
            return 310;
        case LOG_AND_OP:
            return 350;
        }
        throw new RuntimeException("Illegal operator");
    }

    public int getOperandPriority(int i) {
        return getPriority() + i;
    }

    public Type getOperandType(int i) {
        return operandType;
    }

    public void setOperandType(Type[] inputTypes) {
        operandType = operandType
            .intersection(inputTypes[0]).intersection(inputTypes[1]);
        type = operandType;
    }

    /**
     * Sets the return type of this operator.
     */
    public void setType(Type newType) {
        type = operandType = operandType.intersection(newType);
    }

    public boolean equals(Object o) {
	return (o instanceof BinaryOperator) &&
	    ((BinaryOperator)o).operator == operator;
    }

    public String toString(String[] operands) {
        return operands[0] + getOperatorString() + operands[1];
    }
}
