/* IfThenElseOperator Copyright (C) 1998-1999 Jochen Hoenicke.
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

public class IfThenElseOperator extends SimpleOperator {
    public IfThenElseOperator(Type type) {
        super(type, 0, 3);
        operandTypes[0] = Type.tBoolean;
    }
    
    public int getOperandCount() {
        return 3;
    }

    public int getPriority() {
        return 200;
    }

    public int getOperandPriority(int i) {
        switch (i) {
        case 0:
            return 201;
        case 1:
            return 0;
        case 2:
            return 200;
        default:
            throw new jode.AssertError("ifthenelse with operand "+i);
        }
    }

    public void setOperandType(Type[] inputTypes) {
        super.setOperandType(inputTypes);
        Type operandType = 
            type.intersection(operandTypes[1]).intersection(operandTypes[2]);
        type = operandTypes[1] = operandTypes[2] = operandType;
    }

    /**
     * Sets the return type of this operator.
     * @return true if the operand types changed
     */
    public void setType(Type newType) {
        Type operandType = 
            type.intersection(operandTypes[1]).intersection(newType);
        if (!type.equals(operandType)) {
            type = operandTypes[1] = operandTypes[2] = operandType;
        }
    }

    public boolean equals(Object o) {
	return (o instanceof IfThenElseOperator);
    }

    public String toString(String[] operands) {
        return operands[0] + " ? "+operands[1]+" : "+ operands[2];
    }
}
