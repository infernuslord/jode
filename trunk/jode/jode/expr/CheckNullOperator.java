/* CheckNullOperator Copyright (C) 1999 Jochen Hoenicke.
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
import jode.decompiler.LocalInfo;

/**
 * This is a pseudo operator, which represents the check against null
 * that jikes and javac generates for inner classes:
 *
 * <pre>
 *   outer.new Inner()
 * </pre>
 * is translated by javac to
 * <pre>
 *   new Outer$Inner(outer ((void) DUP.getClass()));
 * </pre>
 * and by jikes to
 * <pre>
 *   new Outer$Inner(outer (DUP == null ? throw null));
 * </pre>
 */

public class CheckNullOperator extends Operator {

    Type operandType;
    LocalInfo local;

    public CheckNullOperator(Type type, LocalInfo li) {
        super(type, 0);
        operandType = type;
	local = li;
	local.setType(type);
    }

    public int getOperandCount() {
	return 1;
    }

    public int getPriority() {
        return 200;
    }

    public int getOperandPriority(int i) {
        return 0;
    }

    public Type getOperandType(int i) {
        return operandType;
    }

    public void setOperandType(Type[] inputTypes) {
        operandType = operandType.intersection(inputTypes[0]);
        type = operandType;
	local.setType(type);
    }

    /**
     * Sets the return type of this operator.
     */
    public void setType(Type newType) {
        type = operandType = operandType.intersection(newType);
	local.setType(type);
    }

    public String toString(String[] operands) {
	/* There is no way to produce exactly the same code.
	 * This is a good approximation.
	 * op.getClass will throw a null pointer exception if operands[0]
	 * is null, otherwise return something not equal to null.
	 * The bad thing is that this isn't atomar.
	 */
	return ("(" + local.getName() + " = "
		+ operands[0] + ").getClass() != null ? "
		+ local.getName() + " : null");
    }
}
