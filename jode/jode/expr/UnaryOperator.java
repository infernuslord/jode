/* 
 * UnaryOperator (c) 1998 Jochen Hoenicke
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

public class UnaryOperator extends SimpleOperator {
    public UnaryOperator(Type type, int op) {
        super(type, op, 1);
    }
    
    public int getPriority() {
        return 700;
    }

    public int getOperandPriority(int i) {
        return getPriority();
    }

    /**
     * Sets the return type of this operator.
     */
    public void setType(Type type) {
        super.setType(type);
        Type newOpType = type.intersection(operandTypes[0]);
        operandTypes[0] = newOpType;
    }

    public boolean equals(Object o) {
	return (o instanceof UnaryOperator) &&
	    ((UnaryOperator)o).operator == operator;
    }

    public String toString(String[] operands) {
        return getOperatorString() + operands[0];
    }
}
