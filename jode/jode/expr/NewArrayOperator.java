/* NewArrayOperator Copyright (C) 1998-1999 Jochen Hoenicke.
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

public class NewArrayOperator extends SimpleOperator {
    String baseTypeString;

    public NewArrayOperator(Type arrayType, int dimensions) {
        super(arrayType, 0, dimensions);
        for (int i=0; i< dimensions; i++) {
            operandTypes[i] = Type.tUInt;
        }
    }

    public int getPriority() {
        return 900;
    }

    public int getOperandPriority(int i) {
        return 0;
    }

    public String toString(String[] operands) {
        StringBuffer arrays = new StringBuffer();
        Type flat = type;
        int i = 0;
        while (flat instanceof ArrayType) {
            flat = ((ArrayType)flat).getElementType();
            if (i < getOperandCount())
                arrays.append("[").append(operands[i++]).append("]");
            else
                arrays.append("[]");
        }
        return "new "+flat.toString()+arrays;
    }
}
