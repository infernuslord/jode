/* 
 * LocalInfo (c) 1998 Jochen Hoenicke
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
import sun.tools.java.*;

/**
 * The LocalInfo represents a local variable of a method.
 * The problem is that two different local variables may use the same
 * slot.  The current strategie is to make the range of a local variable 
 * as small as possible.<p>
 *
 * There may be more than one LocalInfo for a single local variable,
 * because the algorithm begins with totally disjunct variables and
 * then unifies locals.  One of the local is then a shadow object which
 * calls the member functions of the other local.<p>
 */
public class LocalInfo {
    private static int serialnr = 0;
    private int slot;
    private Identifier name;
    private Type type;
    private LocalInfo shadow;

    /* The current implementation may use very much stack.  This
     * should be changed someday.
     */

    /**
     * Create a new local info.  The name will be an identifier
     * of the form local_x__yyy, where x is the slot number and
     * yyy a unique number.
     * @param slot  The slot of this variable.
     */
    public LocalInfo(int slot) {
        name = null;
        type = MyType.tUnknown;
        this.slot = slot;
    }

    /**
     * Combines the LocalInfo with another.  This will make this
     * a shadow object to the other local info.  That is all member
     * functions will use the new local info instead of data in this
     * object. <p>
     * If this is called with ourself nothing will happen.
     * @param li the local info that we want to shadow.
     */
    public void combineWith(LocalInfo li) {
        li = li.getLocalInfo();
        if (shadow != null) {
            while (shadow.shadow != null) {
                shadow = shadow.shadow;
            }
            shadow.combineWith(li);
        }
        if (this != li) {
            shadow = li;
            li.setType(type);
        }
    }

    /**
     * Get the real LocalInfo.  This may be different from the
     * current object if this is a shadow local info.
     */
    public LocalInfo getLocalInfo() {
        if (shadow != null) {
            while (shadow.shadow != null) {
                shadow = shadow.shadow;
            }
            return shadow;
        }
        return this;
    }

    /**
     * Get the name of this local.
     */
    public Identifier getName() {
        if (shadow != null) {
            while (shadow.shadow != null) {
                shadow = shadow.shadow;
            }
            return shadow.getName();
        }
        if (name == null)
            name = Identifier.lookup("local_"+slot+"__"+serialnr++);
        return name;
    }

    /**
     * Get the slot of this local.
     */
    public int getSlot() {
        /* The slot does not change when shadowing */
        return slot;
    }

    /**
     * Set the name of this local.
     */
    public void setName(Identifier name) {
        if (shadow != null) 
            shadow.setName(name);
        else
            this.name = name;
    }

    /**
     * Get the type of this local.
     */
    public Type getType() {
        if (shadow != null) {
            while (shadow.shadow != null) {
                shadow = shadow.shadow;
            }
            return shadow.getType();
        }
        return type;
    }

    /**
     * Sets a new information about the type of this local.  
     * The type of the local is may be made more specific by this call.
     * @param  The new type information to be set.
     * @return The new type of the local.
     */
    public Type setType(Type newType) {
        if (shadow != null) {
            while (shadow.shadow != null) {
                shadow = shadow.shadow;
            }
            return shadow.setType(newType);
        }
        this.type = MyType.intersection(this.type, newType);
        if (this.type == MyType.tError)
            System.err.println("Type error in "+getName());
        return this.type;
    }

    public boolean isShadow() {
	return (shadow != null);
    }

    public boolean equals(Object obj) {
        return (obj instanceof LocalInfo
                && ((LocalInfo)obj).getLocalInfo() == getLocalInfo());
    }

    public String toString() {
        return getName().toString();
    }
}
