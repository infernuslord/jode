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

/**
 * A PrePostFixOperator has one subexpression, namely the StoreInstruction.
 */
public class PrePostFixOperator extends Operator {
    boolean postfix;

    public PrePostFixOperator(Type type, int operatorIndex,
			      LValueExpression lvalue, boolean postfix) {
        super(type);
        this.postfix = postfix;
	setOperatorIndex(operatorIndex);
	initOperands(1);
	setSubExpressions(0, (Operator) lvalue);
    }
    
    public int getPriority() {
        return postfix ? 800 : 700;
    }

    public void updateSubTypes() {
	if (!isVoid())
	    subExpressions[0].setType(type);
    }

    public void updateType() {
	if (!isVoid())
	    updateParentType(subExpressions[0].getType());
    }

    public void dumpExpression(TabbedPrintWriter writer)
    throws java.io.IOException {
	boolean needBrace = false;
	int priority = 700;
	if (!postfix) {
	    writer.print(getOperatorString());
	    priority = 800;
	}
	subExpressions[0].dumpExpression(writer, priority);
        if (postfix)
	    writer.print(getOperatorString());
    }
}
