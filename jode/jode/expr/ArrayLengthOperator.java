/* 
 * ArrayLengthOperator (c) 1998 Jochen Hoenicke
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

public class ArrayLengthOperator extends Operator {

    Type arrayType;

    public ArrayLengthOperator() {
        super(Type.tInt, 0);
        arrayType = Type.tArray(Type.tUnknown);
    }

    public int getPriority() {
        return 950;
    }

    public int getOperandCount() {
        return 1;
    }

    public int getOperandPriority(int i) {
        return 900;
    }

    public Type getOperandType(int i) {
        return arrayType;
    }

    public void setOperandType(Type[] types) {
        arrayType = arrayType.intersection(types[0]);
    }

    public String toString(String[] operands) {
        return operands[0] + ".length";
    }
}
