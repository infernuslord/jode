/* CompareBinaryOperator Copyright (C) 1998-1999 Jochen Hoenicke.
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

public class CompareBinaryOperator extends SimpleOperator {
    public CompareBinaryOperator(Type type, int op) {
        super(Type.tBoolean, op, 2);
        operandTypes[0] = operandTypes[1] = type;
    }

    public int getPriority() {
        switch (getOperatorIndex()) {
        case 26:
        case 27:
            return 500;
        case 28:
        case 29:
        case 30:
        case 31: 
            return 550;
        }
        throw new RuntimeException("Illegal operator");
    }

    public int getOperandPriority(int i) {
        return getPriority()+i;
    }

    public void setOperandType(Type[] inputTypes) {
        super.setOperandType(inputTypes);
        operandTypes[0] = operandTypes[1] = 
            operandTypes[0].intersection(operandTypes[1]);
    }

    public boolean equals(Object o) {
	return (o instanceof CompareBinaryOperator) &&
	    ((CompareBinaryOperator)o).operator == operator;
    }

    public String toString(String[] operands) {
        return operands[0] + getOperatorString() + operands[1];
    }
}
