/* 
 * NewArrayOperator (c) 1998 Jochen Hoenicke
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

public class NewArrayOperator extends SimpleOperator {
    String baseTypeString;

    public NewArrayOperator(Type arrayType, int dimensions) {
        super(arrayType, 0, dimensions);
        for (int i=0; i< dimensions; i++) {
            operandTypes[i] = Type.tUInt;
        }
    }

    public int getPriority() {
        return 900;
    }

    public int getOperandPriority(int i) {
        return 0;
    }

    public String toString(String[] operands) {
        StringBuffer arrays = new StringBuffer();
        Type flat = type;
        int i = 0;
        while (flat instanceof ArrayType) {
            flat = ((ArrayType)flat).getElementType();
            if (i < getOperandCount())
                arrays.append("[").append(operands[i++]).append("]");
            else
                arrays.append("[]");
        }
        return "new "+flat.toString()+arrays;
    }
}
