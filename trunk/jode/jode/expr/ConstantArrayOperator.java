/* 
 * ConstantArrayOperator (c) 1998 Jochen Hoenicke
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

public class ConstantArrayOperator extends SimpleOperator {

    public ConstantArrayOperator(Type type, int size) {
        super(type, 0, size);
        for (int i=0; i< size; i++)
            operandTypes[i] = ((ArrayType)type).getElementType();
    }

    public int getPriority() {
        return 200;
    }

    public int getOperandPriority(int i) {
        return 0;
    }

    public String toString(String[] operands) {
        StringBuffer result 
            = new StringBuffer("new "+type+" {");
        for (int i=0; i< getOperandCount(); i++) {
            if (i>0)
                result.append(", ");
            result.append(operands[i]);
        }
        return result.append("}").toString();
    }
}
