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

package jode;

public class IIncOperator extends NoArgOperator 
implements LocalVarOperator {
    String value;
    LocalInfo local;

    public IIncOperator(LocalInfo local, String value, int operator) {
        super(Type.tVoid, operator);
        this.local = local;
	this.value = value;
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

    public boolean matches(Operator loadop) {
        return loadop instanceof LocalLoadOperator && 
            ((LocalLoadOperator)loadop).getLocalInfo().getLocalInfo()
            == local.getLocalInfo();
    }

    public String toString(String[] operands) {
        return local.getName().toString() + 
	    getOperatorString() + value;
    }
}
