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

public abstract class Expression {
    protected Type type;

    ComplexExpression parent = null;

    public Expression(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type newType) {
        this.type = newType;
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

    /**
     * Checks if the given Expression (which should be a StoreInstruction)
     * can be combined into this expression.
     * @param e The store expression.
     * @return 1, if it can, 0, if no match was found and -1, if a
     * conflict was found.  You may wish to check for >0.
     */
    public int canCombine(Expression e) {
	if (e instanceof ComplexExpression
            && e.getOperator() instanceof StoreInstruction) {
	    StoreInstruction store = (StoreInstruction) e.getOperator();
	    if (store.matches(getOperator()))
                return 1;
	}
	return 0;
    }

    /**
     * Combines the given Expression (which should be a StoreInstruction)
     * into this expression.  You must only call this if
     * canCombine returns the value 1.
     * @param e The store expression.
     * @return The combined expression.
     */
    public Expression combine(Expression e) {
        StoreInstruction store = (StoreInstruction) e.getOperator();
        ((ComplexExpression)e).operator
            = new AssignOperator(store.getOperatorIndex(), store);
        return e;
    }

    public Expression simplify() {
        return this;
    }

    Expression simplifyStringBuffer() {
        return null;
    }

    public abstract Operator getOperator();

    public abstract String toString();

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
