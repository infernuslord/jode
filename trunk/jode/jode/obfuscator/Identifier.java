/* 
 * Identifier (c) 1998 Jochen Hoenicke
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
package jode.obfuscator;

public class Identifier {
    /**
     * This is a doubly list of identifiers, that must have always
     * have the same names, and same reachable/preserved settings.
     */
    private Identifier right = null;
    private Identifier left  = null;

    private boolean reachable = false;
    private boolean preserved = false;

    /**
     * Returns true, if this identifier is reachable in some way, false if it
     * is dead and can be removed.
     */
    public final boolean isReachable() {
	return reachable;
    }
    /**
     * true, if this identifier must preserve its name, false if the
     * name may be obfuscated.
     */
    public final boolean isPreserved() {
	return preserved;
    }

    /**
     * Marks this identifier as preserved. This will also make the
     * identifier reachable, if it isn't already.
     *
     * You shouldn't call this directly, but use setPreserved instead.
     */
    protected void setSinglePreserved() {
	preserved = true;
    }

    /**
     * Marks this identifier as reachable.  
     *
     * You should override this method for method identifier, which may
     * mark other methods as reachable.
     *
     * You shouldn't call this directly, but use setReachable instead.
     */
    protected void setSingleReachable() {
	reachable = true;
    }

    /**
     * Mark all shadows as reachable.
     */
    public void setReachable() {
	Identifier ptr = this;
	while (ptr != null) {
	    ptr.setSingleReachable();
	    ptr = ptr.left;
	}
	Identifier ptr = right;
	while (ptr != null) {
	    ptr.setSingleReachable();
	    ptr = ptr.right;
	}
    }

    /**
     * Mark all shadows as preserved.
     */
    public void setPreserved() {
	Identifier ptr = this;
	while (ptr != null) {
	    ptr.setSinglePreserved();
	    ptr = ptr.left;
	}
	Identifier ptr = right;
	while (ptr != null) {
	    ptr.setSinglePreserved();
	    ptr = ptr.right;
	}
    }

    public void getRepresentative() {
	Identifier ptr = this;
	while (ptr.left != null)
	    ptr = ptr.left;
	return ptr;
    }

    /**
     * Mark that this identifier and the given identifier must always have
     * the same name.
     */
    public void addShadow(Identifier orig) {
	if (isReachable() && !orig.isReachable())
	    orig.setReachable();
	else if (!isReachable() && orig.isReachable())
	    setReachable();

	if (isPreserved() && !orig.isPreserved())
	    orig.setPreserved();
	else if (!isPreserved() && orig.isPreserved())
	    setPreserved();
	
	Identifier ptr = this;
	while (ptr.right != null)
	    ptr = ptr.right;
	while (orig.left != null)
	    orig = orig.left;
	ptr.right = orig;
	orig.left = ptr;
    }
}
