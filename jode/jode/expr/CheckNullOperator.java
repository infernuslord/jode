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

package jode.decompiler;
import jode.Type;

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

public class CheckNullOperator extends SimpleOperator {

    public CheckNullOperator(Type type) {
        super(type, 0, 1);
        operandTypes[0] = type;
    }

    public int getPriority() {
        return 200;
    }

    public int getOperandPriority(int i) {
        return 0;
    }

    public String toString(String[] operands) {
	/* There is no way to produce exactly the same code.
	 * This is a good approximation.
	 * op.getClass will throw a null pointer exception if operands[0]
	 * is null, otherwise return something not equal to null.
	 * The bad thing is that this isn't atomar.
	 */
	return "/*CHECK NULL*/ " + 
	    operands[0] + ".getClass() != null ? " + operands[0] + " : null";
    }
}
