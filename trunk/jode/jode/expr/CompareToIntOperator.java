/* CompareToIntOperator Copyright (C) 1998-1999 Jochen Hoenicke.
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

public class CompareToIntOperator extends SimpleOperator {
    public CompareToIntOperator(Type type, int lessGreater) {
        super(Type.tInt, 0, 2);
        operandTypes[0] = operandTypes[1] = type;
    }

    public int getPriority() {
        return 499;
    }

    public int getOperandPriority(int i) {
        return 550;
    }

    public void setOperandType(Type[] inputTypes) {
        super.setOperandType(inputTypes);
        Type operandType = operandTypes[0].intersection(operandTypes[1]);
        operandTypes[0] = operandTypes[1] = operandType;
    }

    public boolean equals(Object o) {
	return (o instanceof CompareToIntOperator);
    }

    public String toString(String[] operands) {
        return operands[0] + " <=> " + operands[1];
    }
}
