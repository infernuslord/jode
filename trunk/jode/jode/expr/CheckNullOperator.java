/* 
 * CheckNullOperator (c) 1998 Jochen Hoenicke
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
