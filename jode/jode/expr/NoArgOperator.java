/* NoArgOperator Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.AssertError;
import jode.decompiler.TabbedPrintWriter;

public abstract class NoArgOperator extends Operator {

    public NoArgOperator(Type type, int operator) {
        super(type, operator);
    }

    public NoArgOperator(Type type) {
        this(type, 0);
    }

    public int getOperandCount() {
        return 0;
    }

    public int getOperandPriority(int i) {
        throw new AssertError("This operator has no operands");
    }

    public Type getOperandType(int i) {
        throw new AssertError("This operator has no operands");
    }

    public void setOperandType(Type[] types) {
        throw new AssertError("This operator has no operands");
    }
}
