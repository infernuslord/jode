/* BinaryOperator Copyright (C) 1998-1999 Jochen Hoenicke.
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

public class BinaryOperator extends SimpleOperator {

    public BinaryOperator(Type type, int op) {
        super(type, op, 2);
    }
    
    public int getPriority() {
        switch (operator) {
        case 1: case 2:
            return 610;
        case 3: case 4: case 5:
            return 650;
        case 6: case 7: case 8:
            return 600;
        case 9: 
            return 450;
        case 10:
            return 410;
        case 11:
            return 420;
        case 12: case 13: case 14: case 15: case 16: case 17: 
        case 18: case 19: case 20: case 21: case 22: case 23:
            return 100;
        case LOG_OR_OP:
            return 310;
        case LOG_AND_OP:
            return 350;
        }
        throw new RuntimeException("Illegal operator");
    }

    public int getOperandPriority(int i) {
        return getPriority() + i;
    }

    public Type getOperandType(int i) {
        return type;
    }

    public void setOperandType(Type[] inputTypes) {
	setType(inputTypes[0].intersection(inputTypes[1]));
    }

    public boolean equals(Object o) {
	return (o instanceof BinaryOperator) &&
	    ((BinaryOperator)o).operator == operator;
    }

    public void dumpExpression(TabbedPrintWriter writer, 
			       Expression[] operands)
	throws java.io.IOException {
	operands[0].dumpExpression(writer, getPriority());
	writer.print(getOperatorString());
	operands[1].dumpExpression(writer, getPriority()+1);
    }
}
