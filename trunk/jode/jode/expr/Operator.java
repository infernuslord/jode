/* Operator Copyright (C) 1998-1999 Jochen Hoenicke.
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

public abstract class Operator extends Expression {
    public final static int ADD_OP     =  1;
    public final static int NEG_OP     =  2;
    public final static int SHIFT_OP   =  6;
    public final static int AND_OP     =  9;
    public final static int ASSIGN_OP  = 12;
    public final static int OPASSIGN_OP= 12;
    public final static int INC_OP     = 24; /* must be even! */
    public final static int DEC_OP     = 25;
    public final static int COMPARE_OP = 26; /* must be even! */
    public final static int EQUALS_OP  = 26;
    public final static int NOTEQUALS_OP = 27;
    public final static int LOG_AND_OP = 32; /* must be even! */
    public final static int LOG_OR_OP  = 33;
    public final static int LOG_NOT_OP = 34;
    static String opString[] = {
        "", " + ", " - ", " * ", " / ", " % ", 
	" << ", " >> ", " >>> ", " & ", " | ", " ^ ",
        " = ", " += ", " -= ", " *= ", " /= ", " %= ", 
	" <<= ", " >>= ", " >>>= ", " &= ", " |= ", " ^= ",
        "++", "--",
        " == "," != "," < "," >= "," > ", " <= ", " && ", " || ",
        "!", "~"
    };

    protected int operator;

    Operator (Type type, int op) {
        super(type);
        this.operator = op;
        if (type == null)
            throw new jode.AssertError("type == null");
    }

    public Expression addOperand(Expression op) {
	return new ComplexExpression
	    (this, new Expression[getOperandCount()]).addOperand(op);
    }

    public Operator getOperator() {
        return this;
    }

    public int getOperatorIndex() {
        return operator;
    }
    public void setOperatorIndex(int op) {
        operator = op;
    }

    public String getOperatorString() {
        return opString[operator];
    }

    /**
     * Get priority of the operator.
     * Currently this priorities are known:
     * <ul><li> 1000 constant
     * </li><li> 950 new, .(field access), []
     * </li><li> 900 new[]
     * </li><li> 800 ++,-- (post)
     * </li><li> 700 ++,--(pre), +,-(unary), ~, !, cast
     * </li><li> 650 *,/, % 
     * </li><li> 610 +,-
     * </li><li> 600 <<, >>, >>> 
     * </li><li> 550 >, <, >=, <=, instanceof
     * </li><li> 500 ==, != 
     * </li><li> 450 & 
     * </li><li> 420 ^ 
     * </li><li> 410 | 
     * </li><li> 350 && 
     * </li><li> 310 || 
     * </li><li> 200 ?:
     * </li><li> 100 =, +=, -=, etc.
     * </li></ul>
     */
    public abstract int getPriority();

    /**
     * Get minimum priority of the nth operand.
     * @see getPriority
     */
    public abstract int getOperandPriority(int i);
    public abstract Type getOperandType(int i);
    public abstract int getOperandCount();
    public abstract void setOperandType(Type[] inputTypes);
    public abstract String toString(String[] operands);

    public String toString()
    {
        String[] operands = new String[getOperandCount()];
        for (int i=0; i< operands.length; i++) {
            operands[i] = "stack_"+(operands.length-i-1);
        }
        return toString(operands);
    }
}

