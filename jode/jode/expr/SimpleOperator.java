/* SimpleOperator Copyright (C) 1998-1999 Jochen Hoenicke.
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

public abstract class SimpleOperator extends Operator {
    protected Type[] operandTypes;

    public SimpleOperator(Type type, int operator, 
                          int operandCount) {
        super(type, operator);
        operandTypes = new Type[operandCount];
        for (int i=0; i< operandCount; i++) {
            operandTypes[i] = type;
        }
    }

    public int getOperandCount() {
        return operandTypes.length;
    }

    public Type getOperandType(int i) {
        return operandTypes[i];
    }

    public void setOperandType(Type[] t) {
        for (int i=0; i< operandTypes.length; i++) {
            operandTypes[i] = operandTypes[i].intersection(t[i]);
        }
    }
}
