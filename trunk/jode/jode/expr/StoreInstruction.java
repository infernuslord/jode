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
import jode.GlobalOptions;
import jode.decompiler.TabbedPrintWriter;

public class StoreInstruction extends Operator
    implements CombineableOperator {

    boolean isOpAssign = false;

    public StoreInstruction(LValueExpression lvalue) {
        super(Type.tVoid, ASSIGN_OP);
	initOperands(2);
	setSubExpressions(0, (Operator) lvalue);
    }

    public LValueExpression getLValue() {
	return (LValueExpression) subExpressions[0];
    }

    public void makeOpAssign(int operatorIndex) {
	setOperatorIndex(operatorIndex);
	if (subExpressions[1] instanceof NopOperator)
	    subExpressions[1].type = Type.tUnknown;
	isOpAssign = true;
    }

    /**
     * Makes a non void expression out of this store instruction.
     */
    public void makeNonVoid() {
        if (type != Type.tVoid)
            throw new InternalError("already non void");
	type = subExpressions[0].getType();
    }

    public boolean lvalueMatches(Operator loadop) {
	return getLValue().matches(loadop);
    }

    public int getPriority() {
        return 100;
    }

    public void updateSubTypes() {
	if (!isVoid()) {
	    subExpressions[0].setType(type);
	    subExpressions[1].setType(Type.tSubType(type));
	}
    }

    public void updateType() {

	Type newType;

	if (!isOpAssign) {
	    /* An opassign (+=, -=, etc.) doesn't merge rvalue type. */
	    Type lvalueType = subExpressions[0].getType();
	    Type rvalueType = subExpressions[1].getType();
	    subExpressions[0].setType(Type.tSuperType(rvalueType));
	    subExpressions[1].setType(Type.tSubType(lvalueType));
	}

	if (!isVoid())
	    updateParentType(subExpressions[0].getType());
    }

    public Expression simplify() {
	if (subExpressions[1] instanceof ConstOperator) {
            ConstOperator one = (ConstOperator) subExpressions[1];

            if ((getOperatorIndex() == OPASSIGN_OP+ADD_OP ||
                 getOperatorIndex() == OPASSIGN_OP+SUB_OP)
		&& one.isOne(subExpressions[0].getType())) {
		
                int op = (getOperatorIndex() == OPASSIGN_OP+ADD_OP)
                    ? INC_OP : DEC_OP;
		
                return new PrePostFixOperator
                    (getType(), op, getLValue(), isVoid()).simplify();
            }
        }
	return super.simplify();
    }

    public boolean opEquals(Operator o) {
	return o instanceof StoreInstruction
	    && o.operatorIndex == operatorIndex
	    && o.isVoid() == isVoid();
    }

    public void dumpExpression(TabbedPrintWriter writer)
	throws java.io.IOException
    {
	subExpressions[0].dumpExpression(writer, 950);
	writer.breakOp();
	writer.print(getOperatorString());
	subExpressions[1].dumpExpression(writer, 100);
    }
}
