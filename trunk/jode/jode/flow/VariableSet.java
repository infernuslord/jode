/* VariableSet (c) 1998 Jochen Hoenicke
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
package jode.flow;
import jode.LocalInfo;

/**
 * This class represents a set of Variables, which are mainly used in
 * the in/out sets of StructuredBlock.  The type of the Variables is
 * LocalInfo. <p>
 *
 * It defines some Helper-Function, like intersecting, merging, union
 * and difference.  <p>
 *
 * Note that a variable set can contain LocalInfos that use the same
 * slot, but are different.
 */
public class VariableSet implements Cloneable {
    LocalInfo[] locals;
    int count;

    /**
     * Creates a new empty variable set
     */
    public VariableSet() {
        locals = null;
        count = 0;
    }

    /**
     * Creates a new pre initialized variable set
     */
    public VariableSet(LocalInfo[] locals) {
        count = locals.length;
        this.locals = locals;
    }

    public final void grow(int size) {
        if (locals != null) {
            size += count;
            if (size > locals.length) {
                int nextSize = locals.length * 2;
//                 System.err.println("wanted: "+size+" next: "+nextSize);
                LocalInfo[] newLocals
                    = new LocalInfo[nextSize > size ? nextSize : size];
                System.arraycopy(locals, 0, newLocals, 0, count);
                locals = newLocals;
            }
        } else if (size > 0)
            locals = new LocalInfo[size];
    }

    /**
     * Adds a local info to this variable set.  It doesn't check for
     * duplicates.
     */
    public void addElement(LocalInfo li) {
        grow(1);
        locals[count++] = li;
    }

    /**
     * Checks if the variable set contains the given local info.
     */
    public boolean contains(LocalInfo li) {
        li = li.getLocalInfo();
        for (int i=0; i<count;i++)
            if (locals[i].getLocalInfo() == li)
                return true;
        return false;
    }

    /**
     * Checks if the variable set contains a local with the given name.
     */
    public LocalInfo findLocal(String name) {
        for (int i=0; i<count;i++)
            if (locals[i].getName().equals(name))
                return locals[i];
        return null;
    }

    /**
     * Removes a local info from this variable set.  
     */
    public void removeElement(LocalInfo li) {
        li = li.getLocalInfo();
        for (int i=0; i<count;i++)
            if (locals[i].getLocalInfo() == li)
                locals[i] = locals[--count];
    }

    /**
     * Removes everything from this variable set.  
     */
    public void removeAllElements() {
        locals = null;
        count = 0;
    }

    public java.util.Enumeration elements() {
        return new ArrayEnum(count, locals);
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public Object clone() {
        try {
            VariableSet other = (VariableSet) super.clone();
            if (count > 0) {
                other.locals = new LocalInfo[count];
                System.arraycopy(locals, 0, other.locals, 0, count);
            }
            return other;
        } catch (CloneNotSupportedException ex) {
            throw new jode.AssertError("Clone?");
        }
    }

    /**
     * Merges the current VariableSet with another.  For all slots occuring
     * in both variable sets, all corresponding LocalInfos are merged.
     * The variable sets are not changed (use union for this).
     * @return The merged variables.
     * @param vs the other variable set.  */
    public VariableSet merge(VariableSet vs) {
        VariableSet merged = new VariableSet();
        merged.grow(Math.min(count,vs.count));
    big_loop:
        for (int i=0; i<count; i++) {
            LocalInfo li1 = locals[i];
            int slot = li1.getSlot();
            boolean didMerge = false;
            for (int k=0; k< merged.count; k++) {
                if (slot == merged.locals[k].getSlot()) {
                    /* This slot was already merged. */
                    li1.combineWith(merged.locals[k]);
                    continue big_loop;
                }
            }
            for (int j=0; j<vs.count; j++) {
                if (li1.getSlot() == vs.locals[j].getSlot()) {
                    li1.combineWith(vs.locals[j]);
                    didMerge = true;
                }
            }
            if (didMerge)
                merged.locals[merged.count++] = li1;
        }
        return merged;
    }

    /**
     * Intersects the current VariableSet with another and returns the
     * intersection.  The existing VariableSet are not changed.  
     * @param vs the other variable set.  
     */
    public VariableSet intersect(VariableSet vs) {
        VariableSet intersection = new VariableSet();
        intersection.grow(Math.min(count, vs.count));
    big_loop:
        for (int i=0; i<count; i++) {
            LocalInfo li = locals[i];
            int slot = li.getSlot();
            for (int j=0; j<vs.count; j++) {
                if (slot == vs.locals[j].getSlot()) {
                    for (int k=0; k<intersection.count; k++) {
                        if (slot == intersection.locals[k].getSlot())
                            continue big_loop;
                    }
                    intersection.locals[intersection.count++] 
                        = li.getLocalInfo();
                    continue big_loop;
                }
            }
        }
        return intersection;
    }

    /**
     * Intersects the current VariableSet with another and returns the
     * intersection.  The existing VariableSet are not changed.  
     * @param vs the other variable set.  
     */
    public VariableSet intersectExact(VariableSet vs) {
        VariableSet intersection = new VariableSet();
        intersection.grow(Math.min(count, vs.count));
    big_loop:
        for (int i=0; i<count; i++) {
            LocalInfo li1 = locals[i].getLocalInfo();
            for (int j=0; j<vs.count; j++) {
                if (li1 == vs.locals[j].getLocalInfo()) {
                    if (!intersection.contains(li1))
                        intersection.locals[intersection.count++] = li1;
                    continue big_loop;
                }
            }
        }
        return intersection;
    }           

    /**
     * Union the other variable set to the current.
     */
    public void unionExact(VariableSet vs) {
        grow(vs.count);
    big_loop:
        for (int i=0; i< vs.count; i++) {
            LocalInfo li2 = (vs.locals[i]).getLocalInfo();
            /* check if this particular local info is already in the set */
            for (int j=0; j< count; j++) {
                LocalInfo li1 = (locals[j]).getLocalInfo();
                if (li1 == li2)
                    /* Yes it is, take next variable */
                    continue big_loop;
            }
            locals[count++] = li2;
        }
    }

    /**
     * Add the other variable set to the current, except when the slot
     * is already in the current set.  
     */
    public void add(VariableSet vs) {
        grow(vs.count);
    big_loop:
        for (int i=0; i< vs.count; i++) {
            LocalInfo li2 = vs.locals[i];
            int slot2 = li2.getSlot();
            /* check if this slot is already in the current set */
            for (int j=0; j< count; j++) {
                if (locals[j].getSlot() == slot2)
                    /* Yes it is, take next variable */
                    continue big_loop;
            }
            locals[count++] = li2.getLocalInfo();
        }
    }

    /**
     * Add the variables in gen to the current set, unless there are
     * variables in kill using the same slot.
     * @param gen The gen set.
     * @param kill The kill set.
     */
    public void mergeGenKill(VariableSet gen, VariableSet kill) {
        grow(gen.count);
    big_loop:
        for (int i=0; i< gen.count; i++) {
            LocalInfo li2 = gen.locals[i];
            int slot = li2.getSlot();
            /* check if this slot is in the kill set) */
            for (int j=0; j< kill.count; j++) {
                LocalInfo li1 = kill.locals[j];
                if (slot == kill.locals[j].getSlot())
                    /* Yes it is, take next variable */
                    continue big_loop;
            }
            locals[count++] = li2.getLocalInfo();
        }
    }

    /**
     * Subtract the other variable set from this one.  This removes
     * every variable from this set, that uses a slot in the other
     * variable set.
     * @param vs The other variable set.
     */
    public void subtract(VariableSet vs) {
    big_loop:
        for (int i=0; i < count;) {
            LocalInfo li1 = locals[i];
            int slot = li1.getSlot();
            for (int j=0; j<vs.count; j++) {
                if (slot == vs.locals[j].getSlot()) {
                    /* remove the element from this variable list. */
                    locals[i] = locals[--count].getLocalInfo();
                    continue big_loop;
                }
            }
            i++;
        }
    }

    /**
     * Subtract the other variable set from this one.  This removes
     * every variable from this set, that is in the other
     * variable set.
     * @param vs The other variable set.
     */
    public void subtractExact(VariableSet vs) {
    big_loop:
        for (int i=0; i < count;) {
            LocalInfo li1 = locals[i].getLocalInfo();
            for (int j=0; j<vs.count; j++) {
                if (li1 == vs.locals[j].getLocalInfo()) {
                    /* remove the element from this variable list. */
                    locals[i] = locals[--count].getLocalInfo();
                    continue big_loop;
                }
            }
            i++;
        }
    }

    public String toString() {
        StringBuffer result = new StringBuffer("[");
        for (int i=0; i < count; i++) {
            if (i>0)
                result.append(", ");
            result.append(locals[i].getName());
        }
        return result.append("]").toString();
    }
}
