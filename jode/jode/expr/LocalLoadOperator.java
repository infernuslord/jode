/* LocalLoadOperator Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.Decompiler;
import jode.Type;
import jode.decompiler.LocalInfo;

public class LocalLoadOperator extends NoArgOperator
implements LocalVarOperator {
    LocalInfo local;

    public LocalLoadOperator(Type type, LocalInfo local) {
        super(type);
        this.local = local;
        local.setType(type);
        local.setOperator(this);
    }

    public boolean isRead() {
        return true;
    }

    public boolean isWrite() {
        return false;
    }

    public boolean isConstant() {
        return false;
    }

    public LocalInfo getLocalInfo() {
	return local.getLocalInfo();
    }

    public void updateType() {
        if (Decompiler.isTypeDebugging)
            Decompiler.err.println("local "+local.getName()+" changed: "
                               +type+" to "+local.getType()
                               +" in "+parent);
        super.setType(local.getType());
        if (parent != null)
            parent.updateType();
    }

    public int getPriority() {
        return 1000;
    }

    public Type getType() {
//  	Decompiler.err.println("LocalLoad.getType of "+local.getName()+": "+local.getType());
	return local.getType();
    }

    public void setType(Type type) {
// 	Decompiler.err.println("LocalLoad.setType of "+local.getName()+": "+local.getType());
	super.setType(local.setType(type));
    }

//     public int getSlot() {
//         return slot;
//     }

    public String toString(String[] operands) {
        return local.getName().toString();
    }

    public boolean equals(Object o) {
        return (o instanceof LocalLoadOperator &&
                ((LocalLoadOperator) o).local.getSlot() == local.getSlot());
    }
}

