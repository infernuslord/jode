/* 
 * ArrayStoreOperator (c) 1998 Jochen Hoenicke
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
import jode.ArrayType;

public class ArrayStoreOperator extends StoreInstruction {
    Type indexType;

    public ArrayStoreOperator(Type type, int operator) {
        super(type, operator);
        indexType = Type.tInt;
    }

    public ArrayStoreOperator(Type type) {
        this(type, ASSIGN_OP);
    }


    public boolean matches(Operator loadop) {
        return loadop instanceof ArrayLoadOperator;
    }

    public int getLValueOperandCount() {
        return 2;
    }

    public int getLValueOperandPriority(int i) {
        if (i == 0)
            return 950;
        else
            return 0;
    }

    public Type getLValueOperandType(int i) {
        if (i == 0)
            return Type.tArray(lvalueType);
        else
            return indexType;
    }

    public void setLValueOperandType(Type[] t) {
        indexType = indexType.intersection(t[1]);
        Type arrayType = t[0].intersection(Type.tArray(lvalueType));
        if (arrayType == Type.tError)
            lvalueType = Type.tError;
	else
            lvalueType = ((ArrayType)arrayType).getElementType();
    }

    public String getLValueString(String[] operands) {
        return operands[0]+"["+operands[1]+"]";
    }
}
