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
import java.util.Enumeration;
import java.util.Vector;

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
    private boolean isUnique;
    private String name;
    private Type type;
    private LocalInfo shadow;
    private Vector operators = new Vector();

    /* The current implementation may use very much stack.  This
     * should be changed someday.
     */

    /**
     * Create a new local info.  The name will be a string
     * @param slot  The slot of this variable.
     */
    public LocalInfo(int slot) {
        name = null;
        type = Type.tUnknown;
        this.slot = slot;
    }

    public void setOperator(LocalVarOperator operator) {
        getLocalInfo().operators.addElement(operator);
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
            getLocalInfo().combineWith(li);
        } else {
            if (this != li) {
                shadow = li;
//                 System.err.println("combining "+name+"("+type+") and "
//                                    +li.name+"("+li.type+")");
                li.setType(type);


                boolean needTypeUpdate = !li.type.equals(type);

                java.util.Enumeration enum = operators.elements();
                while (enum.hasMoreElements()) {
                    LocalVarOperator lvo = 
                        (LocalVarOperator) enum.nextElement();
                    if (needTypeUpdate) {
                        if (Decompiler.isTypeDebugging)
                            System.err.println("updating " + lvo + " in "
                                               + ((Expression)lvo).parent);
                        lvo.updateType();
                    }
                    shadow.operators.addElement(lvo);
                }

                /* Clear unused fields, to allow garbage collection.
                 */
                type = null;
                name = null;
                operators = null;
            }
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
    public String getName() {
        if (shadow != null) {
            while (shadow.shadow != null) {
                shadow = shadow.shadow;
            }
            return shadow.getName();
        }
        if (name == null) {
            name = "local_"+slot+"__"+serialnr+++"_";
            isUnique = true;
        }
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
    public void setName(String name) {
        LocalInfo li = getLocalInfo();
        li.name = name;
    }

    /**
     * Set the name of this local.
     */
    public void makeNameUnique() {
        LocalInfo li = getLocalInfo();
        String name = li.getName();
        if (!li.isUnique) {
            li.name = name + "__"+serialnr+++"_";
            li.isUnique = true;
        }
    }

    /**
     * Get the type of this local.
     */
    public Type getType() {
        return getLocalInfo().type;
    }

    /**
     * Sets a new information about the type of this local.  
     * The type of the local is may be made more specific by this call.
     * @param  The new type information to be set.
     * @return The new type of the local.
     */
    public Type setType(Type newType) {
        LocalInfo li = getLocalInfo();
        newType = li.type.intersection(newType);
        if (Decompiler.isTypeDebugging)
            System.err.println(getName()+" setType, new: "+newType
                               + " old: "+li.type);
        if (!li.type.equals(newType)) {
            li.type = newType;
            java.util.Enumeration enum = li.operators.elements();
            while (enum.hasMoreElements()) {
                LocalVarOperator lvo = (LocalVarOperator) enum.nextElement();
                if (Decompiler.isTypeDebugging)
                    System.err.println("updating "+lvo+" in "
                                       + ((Expression)lvo).parent);
                lvo.updateType();
            }
        }
        return li.type;
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
