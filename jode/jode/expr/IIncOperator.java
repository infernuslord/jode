/* IIncOperator Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.decompiler.LocalInfo;
import jode.decompiler.TabbedPrintWriter;

public class IIncOperator extends NoArgOperator 
    implements LocalVarOperator, CombineableOperator {
    String value;
    LocalInfo local;

    public IIncOperator(LocalInfo local, String value, int operator) {
        super(Type.tVoid, operator);
        this.local = local;
	this.value = value;
        local.setType(Type.tUInt);
        local.setOperator(this);
    }

    public String getValue() {
	return value;
    }

    public boolean isRead() {
        return true;
    }

    public boolean isWrite() {
        return true;
    }

    public void updateType() {
        if (parent != null)
            parent.updateType();
    }

    public LocalInfo getLocalInfo() {
	return local;
    }

    public int getPriority() {
        return 100;
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
        type = local.getType();
    }

    public boolean matches(Operator loadop) {
        return loadop instanceof LocalLoadOperator && 
            ((LocalLoadOperator)loadop).getLocalInfo().getLocalInfo()
            == local.getLocalInfo();
    }

    public Expression simplify() {
        if (value.equals("1")) {
            int op = (getOperatorIndex() == OPASSIGN_OP+ADD_OP)
                ? INC_OP : DEC_OP;

            return new LocalPrePostFixOperator
                (getType(), op, this, isVoid()).simplify();
        }
        return super.simplify();
    }

    public void dumpExpression(TabbedPrintWriter writer, 
			       Expression[] operands)
	throws java.io.IOException {
	writer.print(local.getName() + getOperatorString() + value);
    }
}
