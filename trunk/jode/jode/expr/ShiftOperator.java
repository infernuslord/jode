/* 
 * ShiftOperator (c) 1998 Jochen Hoenicke
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

/**
 * ShiftOpcodes are special, because their second operand is an UIndex
 */
public class ShiftOperator extends BinaryOperator {
    protected Type shiftType;

    public ShiftOperator(Type type, int op) {
        super(type, op);
        shiftType = Type.tInt;
    }

    public Type getOperandType(int i) {
        return (i==0)?operandType:shiftType;
    }

    public void setOperandType(Type[] inputTypes) {
        operandType = operandType.intersection(inputTypes[0]);
        shiftType   = shiftType  .intersection(inputTypes[1]);
    }
}
