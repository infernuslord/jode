/* 
 * LocalLoadOperator (c) 1998 Jochen Hoenicke
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

public class LocalLoadOperator extends ConstOperator 
implements LocalVarOperator {
    LocalInfo local;

    public LocalLoadOperator(Type type, LocalInfo local) {
        super(type, "");
        this.local = local;
        local.setOperator(this);
    }

    public boolean isRead() {
        return true;
    }

    public boolean isWrite() {
        return false;
    }

//     public void setLocalInfo(LocalInfo local) {
//         local.setType(type);
// 	this.local = local;
//     }

    public LocalInfo getLocalInfo() {
	return local.getLocalInfo();
    }

    public void updateType() {
        super.setType(local.getType());
        if (parent != null)
            parent.updateType();
    }

    public Type getType() {
//  	System.err.println("LocalLoad.getType of "+local.getName()+": "+local.getType());
	return local.getType();
    }

    public void setType(Type type) {
// 	System.err.println("LocalLoad.setType of "+local.getName()+": "+local.getType());
	super.setType(local.setType(type));
    }

//     public int getSlot() {
//         return slot;
//     }

    public String toString(String[] operands) {
        return local.getName().toString();
    }

    public boolean equals(Object o) {
        return (o instanceof LocalLoadOperator &&
                ((LocalLoadOperator) o).local.getSlot() == local.getSlot());
    }
}

