/* 
 * AssignOperator (c) 1998 Jochen Hoenicke
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
import sun.tools.java.Type;

public class AssignOperator extends Operator {
    StoreInstruction store;

    public AssignOperator(int op, StoreInstruction store) {
        super(store.getLValueType(), op);
        this.store = store;
    }

    public StoreInstruction getStore() {
        return store;
    }

    public int getPriority() {
	return store.getPriority();
    }
    
    public int getOperandCount() {
        return store.getOperandCount();
    }

    public int getOperandPriority(int i) {
        return store.getOperandPriority(i);
    }

    public Type getOperandType(int i) {
        return store.getOperandType(i);
    }

    /**
     * Sets the return type of this operator.
     * @return true if the operand types changed
     */
    public boolean setType(Type type) {
        boolean result = store.setLValueType(type);
        super.setType(store.getLValueType());
        return result;
    }

    /**
     * Overload this method if the resulting type depends on the input types
     */
    public void setOperandType(Type[] inputTypes) {
        store.setOperandType(inputTypes);
        this.type = store.getLValueType();
    }
    
    public String toString(String[] operands) {
        return store.toString(operands);
    }
}
