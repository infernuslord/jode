/* ArrayLoadOperator Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.ArrayType;

public class ArrayLoadOperator extends SimpleOperator {
    String value;

    public ArrayLoadOperator(Type type) {
        super(type, 0, 2);
        operandTypes[0] = Type.tArray(type);
        operandTypes[1] = Type.tInt;
    }

    public int getPriority() {
        return 950;
    }

    public int getOperandPriority(int i) {
        return (i==0)?950:0;
    }

    /**
     * Sets the return type of this operator.
     */
    public void setType(Type type) {
        if (!type.equals(this.type)) {
            super.setType(type);
            operandTypes[0] = Type.tArray(type);
        }
    }

    public void setOperandType(Type[] t) {
        super.setOperandType(t);
        if (operandTypes[0] == Type.tError)
            type = Type.tError;
	else if (operandTypes[0] instanceof ArrayType)
            type = type.intersection
                (((ArrayType)operandTypes[0]).getElementType());
        else
            throw new jode.AssertError("No Array type: "+operandTypes[0]);
    }

    public String toString(String[] operands) {
        return operands[0]+"["+operands[1]+"]";
    }
}
