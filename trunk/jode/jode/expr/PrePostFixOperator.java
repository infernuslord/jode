/* PrePostFixOperator Copyright (C) 1998-1999 Jochen Hoenicke.
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

public class PrePostFixOperator extends Operator {
    StoreInstruction store;
    boolean postfix;

    public PrePostFixOperator(Type type, int op, 
                              StoreInstruction store, boolean postfix) {
        super(type, op);
	this.store = store;
        this.postfix = postfix;
    }
    
    public int getPriority() {
        return postfix ? 800 : 700;
    }

    public Type getOperandType(int i) {
	return store.getLValueOperandType(i);
    }

    public int getOperandCount() {
        return store.getLValueOperandCount();
    }

    /**
     * Checks if the value of the given expression can change, due to
     * side effects in this expression.  If this returns false, the 
     * expression can safely be moved behind the current expresion.
     * @param expr the expression that should not change.
     */
    public boolean hasSideEffects(Expression expr) {
	return store.hasSideEffects(expr);
    }

    /**
     * Sets the return type of this operator.
     */
    public void setType(Type type) {
        store.setLValueType(type);
        super.setType(store.getLValueType());
    }

    public void setOperandType(Type[] inputTypes) {
        store.setLValueOperandType(inputTypes);
    }

    public void dumpExpression(TabbedPrintWriter writer, 
			       Expression[] operands) 
    throws java.io.IOException {
	boolean needBrace = false;
	int priority = 700;
	if (postfix) {
	    writer.print(getOperatorString());
	    priority = 800;
	}
	if (store.getPriority() < priority) {
	    needBrace = true;
	    writer.print("(");
	}
	store.dumpLValue(writer, operands);
	if (needBrace)
	    writer.print(")");
        if (!postfix)
	    writer.print(getOperatorString());
    }
}
