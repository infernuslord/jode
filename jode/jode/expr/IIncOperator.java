/* 
 * IIncOperator (c) 1998 Jochen Hoenicke
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
import jode.LocalInfo;

public class IIncOperator extends NoArgOperator 
implements LocalVarOperator, CombineableOperator {
    String value;
    LocalInfo local;

    public IIncOperator(LocalInfo local, String value, int operator) {
        super(Type.tVoid, operator);
        this.local = local;
	this.value = value;
        local.setType(Type.tUInt);
        local.setOperator(this);
    }

    public String getValue() {
	return value;
    }

    public boolean isRead() {
        return true;
    }

    public boolean isWrite() {
        return true;
    }

    public void updateType() {
        if (parent != null)
            parent.updateType();
    }

    public LocalInfo getLocalInfo() {
	return local;
    }

    public int getPriority() {
        return 100;
    }


    /**
     * Checks if the value of the given expression can change, due to
     * side effects in this expression.  If this returns false, the 
     * expression can safely be moved behind the current expresion.
     * @param expr the expression that should not change.
     */
    public boolean hasSideEffects(Expression expr) {
	return expr.containsConflictingLoad(this);
    }

    /**
     * Makes a non void expression out of this store instruction.
     */
    public void makeNonVoid() {
        if (type != Type.tVoid)
            throw new jode.AssertError("already non void");
        type = local.getType();
    }

    public boolean matches(Operator loadop) {
        return loadop instanceof LocalLoadOperator && 
            ((LocalLoadOperator)loadop).getLocalInfo().getLocalInfo()
            == local.getLocalInfo();
    }

    public Expression simplify() {
        if (value.equals("1")) {
            int op = (getOperatorIndex() == OPASSIGN_OP+ADD_OP)
                ? INC_OP : DEC_OP;

            return new LocalPrePostFixOperator
                (local.getType(), op, this, isVoid()).simplify();
        }
        return super.simplify();
    }

    public String toString(String[] operands) {
        return local.getName().toString() + 
	    getOperatorString() + value;
    }
}
