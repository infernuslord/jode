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

    Expression parent = null;

    public Expression(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public Expression getParent() {
        return parent;
    }

    public void setType(Type newType) {
        type = type.intersection(newType);
    }

    public void updateType() {
        if (parent != null)
            parent.updateType();
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
     * @param e The store expression, must be of type void.
     * @return 1, if it can, 0, if no match was found and -1, if a
     * conflict was found.  You may wish to check for >0.
     */
    public int canCombine(Expression e) {
        return containsMatchingLoad(e)? 1 : 0;
    }

    /**
     * Checks if this expression contains a load, that matches the
     * given Expression (which should be a
     * StoreInstruction/IIncOperator).
     * @param e The store expression.
     * @return if this expression contains a matching load.
     */
    public boolean containsMatchingLoad(Expression e) {
        if (e instanceof IIncOperator
            && ((IIncOperator)e.getOperator()).matches(getOperator()))
            return true;
        else if (e instanceof ComplexExpression
                 && e.getOperator() instanceof StoreInstruction
                 && ((StoreInstruction) e.getOperator())
                 .matches(getOperator()))
            return true;
	return false;
    }

    /**
     * Combines the given Expression (which should be a StoreInstruction)
     * into this expression.  You must only call this if
     * canCombine returns the value 1.
     * @param e The store expression.
     * @return The combined expression.
     */
    public Expression combine(Expression e) {
        if (e.getOperator() instanceof IIncOperator) {
            if (((IIncOperator)e.getOperator()).matches(getOperator())) {
                ((IIncOperator)e.getOperator()).makeNonVoid();
                /* Do not call setType, we don't want to intersect. */
                e.type = e.getOperator().getType();
                return e;
            }
        } else {
            if (((StoreInstruction)e.getOperator()).matches(getOperator())) {
                ((StoreInstruction)e.getOperator()).makeNonVoid();
                /* Do not call setType, we don't want to intersect. */
                e.type = e.getOperator().getType();
                return e;
            }
        }
        return null;
    }

    public Expression simplify() {
        return this;
    }
    public Expression simplifyString() {
        return this;
    }

    static Expression EMPTYSTRING = new ConstOperator(Type.tString, "\"\"");

    public Expression simplifyStringBuffer() {
        return null;
    }

    public abstract Operator getOperator();

    public void makeInitializer() {
    }

    public boolean isConstant() {
        return true;
    }

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
