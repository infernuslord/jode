/* 
 * ArrayLoadOperator (c) 1998 Jochen Hoenicke
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

package jode.expr;
import jode.Type;
import jode.ArrayType;

public class ArrayLoadOperator extends SimpleOperator {
    String value;

    public ArrayLoadOperator(Type type) {
        super(type, 0, 2);
        operandTypes[0] = Type.tArray(type);
        operandTypes[1] = Type.tInt;
    }

    public int getPriority() {
        return 950;
    }

    public int getOperandPriority(int i) {
        return (i==0)?950:0;
    }

    /**
     * Sets the return type of this operator.
     */
    public void setType(Type type) {
        if (!type.equals(this.type)) {
            super.setType(type);
            operandTypes[0] = Type.tArray(type);
        }
    }

    public void setOperandType(Type[] t) {
        super.setOperandType(t);
        if (operandTypes[0] == Type.tError)
            type = Type.tError;
	else if (operandTypes[0] instanceof ArrayType)
            type = type.intersection
                (((ArrayType)operandTypes[0]).getElementType());
        else
            throw new jode.AssertError("No Array type: "+operandTypes[0]);
    }

    public String toString(String[] operands) {
        return operands[0]+"["+operands[1]+"]";
    }
}
