/* LocalStoreOperator Copyright (C) 1998-1999 Jochen Hoenicke.
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

public class LocalStoreOperator extends StoreInstruction 
    implements LocalVarOperator {
    LocalInfo local;

    public LocalStoreOperator(Type lvalueType, LocalInfo local, int operator) {
        super(lvalueType, operator);
        this.local = local;
        local.setType(lvalueType);
        local.setOperator(this);
    }

    public boolean isRead() {
        return operator != ASSIGN_OP;
    }

    public boolean isWrite() {
        return true;
    }

    public void updateType() {
        if (parent != null)
            parent.updateType();
    }

    public LocalInfo getLocalInfo() {
	return local.getLocalInfo();
    }

    public Type getLValueType() {
	return local.getType();
    }

    public void setLValueType(Type type) {
	local.setType(type);
    }

    public boolean matches(Operator loadop) {
        return loadop instanceof LocalLoadOperator && 
            ((LocalLoadOperator)loadop).getLocalInfo().getSlot()
            == local.getSlot();
    }

    public int getLValuePriority() {
        return 1000;
    }

    public int getLValueOperandCount() {
        return 0;
    }

    public int getLValueOperandPriority(int i) {
        /* shouldn't be called */
        throw new RuntimeException("LocalStoreOperator has no operands");
    }

    public Type getLValueOperandType(int i) {
        /* shouldn't be called */
        throw new RuntimeException("LocalStoreOperator has no operands");
    }

    public void setLValueOperandType(Type []t) {
        /* shouldn't be called */
        throw new RuntimeException("LocalStoreOperator has no operands");
    }

    public void dumpLValue(TabbedPrintWriter writer, Expression[] operands) {
	writer.print(local.getName());
    }
}

