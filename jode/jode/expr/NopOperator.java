/* NopOperator Copyright (C) 1998-1999 Jochen Hoenicke.
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

/**
 * A NopOperator takes one or zero arguments and returns it again.  It
 * is mainly used as placeholder when the real operator is not yet
 * known (e.g. in SwitchBlock).  But there also exists a nop opcode in
 * the java virtual machine (The compiler can't create such a opcode,
 * though).
 *
 * @author Jochen Hoenicke */
public class NopOperator extends SimpleOperator {
    public NopOperator(Type type) {
	super(type, 0, 1);
    }

    public NopOperator() {
        super(Type.tVoid, 0, 0);
    }

    public int getPriority() {
        return 1000;
    }

    public Expression addOperand(Expression op) {
	op.setType(type.getSubType());
	return op;
    }

    public int getOperandPriority(int i) {
        return 0;
    }

    public boolean isConstant() {
	return false;
    }

    public boolean equals(Object o) {
	return (o instanceof NopOperator);
    }

    public String toString(String[] operands) {
        if (type == Type.tVoid)
            return "/* nop */";
        return operands[0];
    }
}
