/* StoreInstruction Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.type.Type;
import jode.decompiler.TabbedPrintWriter;

public abstract class StoreInstruction extends Operator
    implements CombineableOperator {

    public String lvCasts;
    Type lvalueType;
    Type rvalueType = null;

    public StoreInstruction(Type type, int operator) {
        super(Type.tVoid, operator);
        lvalueType = type;
        lvCasts = lvalueType.toString();
    }

    public void makeOpAssign(int operator, Type rvalueType) {
	setOperatorIndex(operator);
	this.rvalueType = rvalueType;
    }

    public Type getType() {
        return type == Type.tVoid ? type : getLValueType();
    }

    public Type getLValueType() {
        return lvalueType;
    }

    /**
     * Checks if the value of the given expression can change, due to
     * side effects in this expression.  If this returns false, the 
     * expression can safely be moved behind the current expresion.
     * @param expr the expression that should not change.
     */
    public boolean hasSideEffects(Expression expr) {
	return expr.containsConflictingLoad(this);
    }

    /**
     * Makes a non void expression out of this store instruction.
     */
    public void makeNonVoid() {
        if (type != Type.tVoid)
            throw new jode.AssertError("already non void");
        type = lvalueType;
        if (parent != null && parent.getOperator() == this)
            parent.type = lvalueType;
    }

    public abstract boolean matches(Operator loadop);
    public abstract int getLValueOperandCount();
    public abstract Type getLValueOperandType(int i);
    public abstract void setLValueOperandType(Type [] t);

    /**
     * Sets the type of the lvalue (and rvalue).
     */
    public void setLValueType(Type type) {
        lvalueType = lvalueType.intersection(type);
    }

    public int getPriority() {
        return 100;
    }

    public Type getOperandType(int i) {
        if (i == getLValueOperandCount()) {
	    if (getOperatorIndex() == ASSIGN_OP)
		/* In a direct assignment, lvalueType is rvalueType */
		return getLValueType(); 
	    else
		return rvalueType;
        } else
            return getLValueOperandType(i);
    }

    public void setOperandType(Type[] t) {
        int count = getLValueOperandCount();
        if (count > 0)
            setLValueOperandType(t);
	if (getOperatorIndex() == ASSIGN_OP)
	    /* In a direct assignment, lvalueType is rvalueType */
	    setLValueType(t[count]);
	else
	    rvalueType = rvalueType.intersection(t[count]);
    }

    public int getOperandCount() {
        return 1 + getLValueOperandCount();
    }

    public abstract void dumpLValue(TabbedPrintWriter writer, 
				    Expression[] operands)
	throws java.io.IOException;

    public void dumpExpression(TabbedPrintWriter writer, Expression[] operands)
	throws java.io.IOException
    {
	dumpLValue(writer, operands);
	writer.print(getOperatorString());
	operands[getLValueOperandCount()].dumpExpression(writer, 100);
    }
}
