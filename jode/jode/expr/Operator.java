/* 
 * Operator (c) 1998 Jochen Hoenicke
 *
 * You may distribute under the terms of the GNU General Public License.
 *
 * IN NO EVENT SHALL JOCHEN HOENICKE BE LIABLE TO ANY PARTY FOR DIRECT,
 * INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT OF
 * THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF JOCHEN HOENICKE 
 * HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * JOCHEN HOENICKE SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS"
 * BASIS, AND JOCHEN HOENICKE HAS NO OBLIGATION TO PROVIDE MAINTENANCE,
 * SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
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

