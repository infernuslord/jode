/* 
 * SimpleOperator (c) 1998 Jochen Hoenicke
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
