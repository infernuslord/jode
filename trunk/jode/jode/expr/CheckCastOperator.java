/* 
 * CheckCastOperator (c) 1998 Jochen Hoenicke
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

package jode;

public class CheckCastOperator extends SimpleOperator {
    Type castType;

    public CheckCastOperator(Type type) {
        super(type, 0, 1);
        /* The following is wrong.  The operand must not
         * be a super type of the given type, but any type
         * especially if type is an interface.
         *
         * If operand is of class type, it is probably a
         * super type, but who knows?
         *
         * operandTypes[0] = MyType.tSuperType(type);
         *
         * The forgiving solution:
         */
        castType = type;
        operandTypes[0] = Type.tUnknown;
    }

    public int getPriority() {
        return 700;
    }

    public int getOperandPriority(int i) {
        return getPriority();
    }

    public String toString(String[] operands) {
        return "(" + castType.toString() + ")" + operands[0];
    }
}
