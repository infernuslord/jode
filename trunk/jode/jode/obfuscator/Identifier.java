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
import jode.Obfuscator;
import java.io.*;

public abstract class Identifier {
    /**
     * This is a doubly list of identifiers, that must have always
     * have the same names, and same preserved settings.
     */
    private Identifier right = null;
    private Identifier left  = null;

    private boolean reachable = false;
    private boolean preserved = false;

    private String alias = null;

    public Identifier(String alias) {
	this.alias = alias;
    }

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
	if (getParent() != null)
	    getParent().setReachable();
    }

    /**
     * Mark all shadows as reachable.
     */
    public void setReachable() {
	if (!reachable) {
	    reachable = true;
	    setSingleReachable();
	}
    }

    /**
     * Mark all shadows as preserved.
     */
    public void setPreserved() {
	if (!preserved) {
	    preserved = true;
	    Identifier ptr = this;
	    while (ptr != null) {
		ptr.setSinglePreserved();
		ptr = ptr.left;
	    }
	    ptr = right;
	    while (ptr != null) {
		ptr.setSinglePreserved();
		ptr = ptr.right;
	    }
	}
    }

    public Identifier getRepresentative() {
	Identifier ptr = this;
	while (ptr.left != null)
	    ptr = ptr.left;
	return ptr;
    }

    public final boolean isRepresentative() {
	return left == null;
    }

    public final void setAlias(String name) {
	getRepresentative().alias = name;
    }

    public final String getAlias() {
	return getRepresentative().alias;
    }

    /**
     * Mark that this identifier and the given identifier must always have
     * the same name.
     */
    public void addShadow(Identifier orig) {
	if (isPreserved() && !orig.isPreserved())
	    orig.setPreserved();
	else if (!isPreserved() && orig.isPreserved())
	    setPreserved();
	
	Identifier ptr = this;
	while (ptr.right != null)
	    ptr = ptr.right;

	/* Check if orig is already on the ptr chain */
	Identifier check = orig;
	while (check.right != null)
	    check = check.right;
	if (check == ptr)
	    return;

	while (orig.left != null)
	    orig = orig.left;
	ptr.right = orig;
	orig.left = ptr;
    }

    static int serialnr = 0;
    public void buildTable(int renameRule) {
	if (isPreserved()) {
	    if (Obfuscator.isDebugging)
		Obfuscator.err.println(toString() + " is preserved");
	} else if (isRepresentative()
		   && renameRule != Obfuscator.RENAME_NONE) {

	    if (renameRule == Obfuscator.RENAME_UNIQUE)
		setAlias("xxx" + serialnr++);
	    else {
		StringBuffer newAlias = new StringBuffer();
	    next_alias:
		for (;;) {
		okay:
		    do {
			for (int pos = 0; pos < newAlias.length(); pos++) {
			    char c = newAlias.charAt(pos);
			    if (renameRule == Obfuscator.RENAME_WEAK) {
				if (c == '9') {
				    newAlias.setCharAt(pos, 'A');
				    break okay;
				} else if (c == 'Z') {
				    newAlias.setCharAt(pos, 'a');
				    break okay;
				} else if (c != 'z') {
				    newAlias.setCharAt(pos, (char)(c+1));
				    break okay;
				}
				newAlias.setCharAt(pos, '0');
			    } else {
				while (c++ < 255) {
				    if (Character.isJavaIdentifierPart(c)) {
					newAlias.setCharAt(pos, c);
					break okay;
				    }
				}
				newAlias.setCharAt(pos, '0');
			    }
			}
			newAlias.insert(0, renameRule == Obfuscator.RENAME_WEAK
					? 'A': '0');
		    } while (false);
		    Identifier ptr = this;
		    while (ptr != null) {
			if (ptr.conflicting(newAlias.toString()))
			    continue next_alias;
			ptr = ptr.right;
		    }
		    alias = newAlias.toString();
		    return;
		}
	    }
	}
    }

    public void writeTable(PrintWriter out) throws IOException {
	if (getName() != getAlias())
	    out.println("" + getFullAlias() + " = " + getName());
    }

    public abstract Identifier getParent();
    public abstract String getName();
    public abstract String getType();
    public abstract String getFullName();
    public abstract String getFullAlias();
    public abstract boolean conflicting(String newAlias);
}
