/* 
 * Expression (c) 1998 Jochen Hoenicke
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

public abstract class Expression extends Instruction {

    ComplexExpression parent = null;

    public Expression(Type type) {
        super (type);
    }

    /**
     * Get the number of operands.
     * @return The number of stack entries this expression needs.
     */
    public abstract int getOperandCount();

    public Expression negate() {
        Operator negop = 
            new UnaryOperator(Type.tBoolean, Operator.LOG_NOT_OP);
        return new ComplexExpression(negop, new Expression[] { this });
    }

    public Expression tryToCombine(Expression e) {
	if (e instanceof ComplexExpression
            && e.getOperator() instanceof StoreInstruction) {
            ComplexExpression ce = (ComplexExpression) e;
	    StoreInstruction store = (StoreInstruction) e.getOperator();
	    if (store.matches(getOperator()) 
                && ce.subExpressions.length == 1) {
                return new ComplexExpression
                    (new AssignOperator(store.getOperatorIndex(), store),
                     ce.subExpressions);
	    }
	}
	return null;
    }

    Expression simplifyStringBuffer() {
        return null;
    }

    public abstract Operator getOperator();

    String toString(int minPriority) {
        String result = toString();
        if (getOperator().getPriority() < minPriority)
            return "("+result+")";
        return result;
    }

    public boolean isVoid() {
        return getType() == Type.tVoid;
    }
}
