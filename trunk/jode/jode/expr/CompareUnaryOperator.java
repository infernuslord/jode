/* 
 * CompareUnaryOperator (c) 1998 Jochen Hoenicke
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

public class CompareUnaryOperator extends SimpleOperator {
    boolean objectType;

    public CompareUnaryOperator(Type type, int op) {
        super(Type.tBoolean, op, 1);
        operandTypes[0] = type;
        objectType = (type.isOfType(Type.tUObject));
    }

    public int getPriority() {
        switch (getOperatorIndex()) {
        case 26:
        case 27:
            return 500;
        case 28:
        case 29:
        case 30:
        case 31: 
            return 550;
        }
        throw new RuntimeException("Illegal operator");
    }

    public int getOperandPriority(int i) {
        return getPriority();
    }

    public boolean equals(Object o) {
	return (o instanceof CompareUnaryOperator) &&
	    ((CompareUnaryOperator)o).operator == operator;
    }

    public String toString(String[] operands) {
        return operands[0] + getOperatorString() + (objectType?"null":"0");
    }
}
