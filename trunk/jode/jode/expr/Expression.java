/* Expression Copyright (C) 1998-1999 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id$
 */

package jode.expr;
import jode.Type;
import jode.Decompiler;

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

    public void setType(Type otherType) {
	Type newType = type.intersection(otherType);
	if (newType == Type.tError 
	    && type != Type.tError && otherType != Type.tError)
	    Decompiler.err.println("Type error in "+this+": "
				   +"merging "+type+" and "+otherType);
	type = newType;
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

    public abstract Expression addOperand(Expression op);

    public Expression negate() {
        Operator negop = 
            new UnaryOperator(Type.tBoolean, Operator.LOG_NOT_OP);
        return new ComplexExpression(negop, new Expression[] { this });
    }

    /**
     * Checks if the value of the given expression can change, due to
     * side effects in this expression.  If this returns false, the 
     * expression can safely be moved behind the current expresion.
     * @param expr the expression that should not change.
     */
    public boolean hasSideEffects(Expression expr) {
	// Most expression don't have side effects.
	return false;
    }

    /**
     * Checks if the given Expression (which should be a CombineableOperator)
     * can be combined into this expression.
     * @param e The store expression, must be of type void.
     * @return 1, if it can, 0, if no match was found and -1, if a
     * conflict was found.  You may wish to check for >0.
     */
    public int canCombine(Expression e) {
// 	jode.Decompiler.err.println("Try to combine "+e+" into "+this);
        return containsMatchingLoad(e)? 1 : 0;
    }

    /**
     * Checks if this expression contains a load, that matches the
     * given Expression (which should be a StoreInstruction/IIncOperator).
     * @param e The store expression.
     * @return if this expression contains a matching load.
     * @exception ClassCastException, if e.getOperator 
     * is not a CombineableOperator.
     */
    public boolean containsMatchingLoad(Expression e) {
        return ((CombineableOperator)e.getOperator()).matches(getOperator());
    }

    /**
     * Checks if this expression contains a conflicting load, that
     * matches the given CombineableOperator.  The sub expressions are
     * not checked.
     * @param op The combineable operator.
     * @return if this expression contains a matching load.  */
    public boolean containsConflictingLoad(MatchableOperator op) {
        return op.matches(getOperator());
    }

    /**
     * Combines the given Expression (which should be a StoreInstruction)
     * into this expression.  You must only call this if
     * canCombine returns the value 1.
     * @param e The store expression, 
     * the operator must be a CombineableOperator.
     * @return The combined expression.
     * @exception ClassCastException, if e.getOperator 
     * is not a CombineableOperator.
     */
    public Expression combine(Expression e) {
	CombineableOperator op = (CombineableOperator) e.getOperator();
	if (op.matches(getOperator())) {
	    op.makeNonVoid();
	    /* Do not call setType, we don't want to intersect. */
	    e.type = e.getOperator().getType();
	    return e;
        }
        return null;
    }

    /** 
     * This method should remove local variables that are only written
     * and read one time directly after another.  <br>
     *
     * In this case this is a non void LocalStoreOperator, whose local
     * isn't used in other places.
     * @return an expression where the locals are removed.
     */
    public Expression removeOnetimeLocals() {
	return this;
    }

    public Expression simplify() {
        return this;
    }
    public Expression simplifyString() {
        return this;
    }

    public static Expression EMPTYSTRING = new ConstOperator("");

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
