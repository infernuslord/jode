/* 
 * LocalStoreOperator (c) 1998 Jochen Hoenicke
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
import jode.decompiler.LocalInfo;

public class LocalStoreOperator extends StoreInstruction 
implements LocalVarOperator {
    LocalInfo local;

    public LocalStoreOperator(Type lvalueType, LocalInfo local, int operator) {
        super(lvalueType, operator);
        this.local = local;
        local.setType(lvalueType);
        local.setOperator(this);
    }

    public boolean isRead() {
        return operator != ASSIGN_OP;
    }

    public boolean isWrite() {
        return true;
    }

    public void updateType() {
        if (parent != null)
            parent.updateType();
    }

    public LocalInfo getLocalInfo() {
	return local.getLocalInfo();
    }

    public Type getLValueType() {
	return local.getType();
    }

    public void setLValueType(Type type) {
	local.setType(type);
    }

    public boolean matches(Operator loadop) {
        return loadop instanceof LocalLoadOperator && 
            ((LocalLoadOperator)loadop).getLocalInfo().getSlot()
            == local.getSlot();
    }

    public int getLValueOperandCount() {
        return 0;
    }

    public int getLValueOperandPriority(int i) {
        /* shouldn't be called */
        throw new RuntimeException("LocalStoreOperator has no operands");
    }

    public Type getLValueOperandType(int i) {
        /* shouldn't be called */
        throw new RuntimeException("LocalStoreOperator has no operands");
    }

    public void setLValueOperandType(Type []t) {
        /* shouldn't be called */
        throw new RuntimeException("LocalStoreOperator has no operands");
    }

    public String getLValueString(String[] operands) {
        return local.getName().toString();
    }
}

