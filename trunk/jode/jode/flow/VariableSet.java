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
public class VariableSet extends java.util.Vector {
    /**
     * Creates a new empty variable set
     */
    public VariableSet() {
        super(0, 0);
    }

    /**
     * Adds a local variable to the variable set.
     * @param li The local variable of type LocalInfo.
     */
    public void addElement(LocalInfo li) {
        super.addElement((Object)li);
    }

    /**
     * Merges the current VariableSet with another.  For all slots occuring
     * in both variable sets, all corresponding LocalInfos are merged.
     * The variable sets are not changed (use union for this).
     * @return The merged variables.
     * @param vs  the other variable set.
     */
    public VariableSet merge(VariableSet vs) {
        VariableSet merged = new VariableSet();
        for (int i=0; i<elementCount; i++) {
            LocalInfo li1 = ((LocalInfo) elementData[i]).getLocalInfo();
            boolean didMerge = false;
            for (int j=0; j<vs.elementCount; j++) {
                LocalInfo li2 = ((LocalInfo) vs.elementData[j]).getLocalInfo();
                if (li1.getSlot() == li2.getSlot()) {
                    li1.combineWith(li2);
                    didMerge = true;
                }
            }
            if (didMerge)
                merged.addElement(li1);
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
        for (int i=0; i<elementCount; i++) {
            LocalInfo li1 = ((LocalInfo) elementData[i]).getLocalInfo();
            for (int j=0; j<vs.elementCount; j++) {
                LocalInfo li2 = ((LocalInfo) vs.elementData[j]).getLocalInfo();
                if (li1.getSlot() == li2.getSlot()) {
                    if (!intersection.contains(li1))
                        intersection.addElement(li1);
                    if (!intersection.contains(li2))
                        intersection.addElement(li2);
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
        for (int i=0; i<elementCount; i++) {
            LocalInfo li1 = ((LocalInfo) elementData[i]).getLocalInfo();
            for (int j=0; j<vs.elementCount; j++) {
                LocalInfo li2 = ((LocalInfo) vs.elementData[j]).getLocalInfo();
                if (li1.getLocalInfo() == li2.getLocalInfo()) {
                    if (!intersection.contains(li1))
                        intersection.addElement(li1);
                    if (!intersection.contains(li2))
                        intersection.addElement(li2);
                }
            }
        }
        return intersection;
    }           

    /**
     * Union the other variable set to the current.
     */
    public void unionExact(VariableSet vs) {
        int oldSize = elementCount;
    iloop:
        for (int i=0; i< vs.elementCount; i++) {
            LocalInfo li2 = ((LocalInfo) vs.elementData[i]).getLocalInfo();
            /* check if this particular local info was already in the set */
            for (int j=0; j< oldSize; j++) {
                LocalInfo li1 = ((LocalInfo) elementData[j]).getLocalInfo();
                if (li1 == li2)
                    /* Yes it was, take next variable */
                    continue iloop;
            }
            addElement(li2);
        }
    }

    /**
     * Add the other variable set to the current, except when the slot
     * is already in the current set.  
     */
    public void add(VariableSet vs) {
        int oldSize = elementCount;
    iloop:
        for (int i=0; i< vs.elementCount; i++) {
            LocalInfo li2 = (LocalInfo) vs.elementData[i];
            /* check if this slot was already overwritten by this block */
            for (int j=0; j< oldSize; j++) {
                LocalInfo li1 = (LocalInfo) elementData[j];
                if (li1.getSlot() == li2.getSlot())
                    /* Yes it was, take next variable */
                    continue iloop;
            }
            addElement(li2.getLocalInfo());
        }
    }

    /**
     * Add the other variable set to the current, except when the slot
     * is already in the current set.  
     */
    public void addExact(VariableSet vs) {
        int oldSize = elementCount;
    iloop:
        for (int i=0; i< vs.elementCount; i++) {
            LocalInfo li2 = ((LocalInfo) vs.elementData[i]).getLocalInfo();
            /* check if this slot was already overwritten by this block */
            for (int j=0; j< oldSize; j++) {
                LocalInfo li1 = (LocalInfo) elementData[j];
                if (li1.getLocalInfo() == li2)
                    /* Yes it was, take next variable */
                    continue iloop;
            }
            addElement(li2);
        }
    }

    /**
     * Add the variables in gen to the current set, unless there are
     * variables in kill using the same slot.
     * @param gen The gen set.
     * @param kill The kill set.
     */
    public void mergeGenKill(VariableSet gen, VariableSet kill) {
    iloop:
        for (int i=0; i< gen.elementCount; i++) {
            LocalInfo li2 = ((LocalInfo) gen.elementData[i]).getLocalInfo();
            /* check if this slot was already overwritten (kill set) */
            for (int j=0; j< kill.elementCount; j++) {
                LocalInfo li1 = (LocalInfo) kill.elementData[j];
                if (li2.getSlot() == li1.getSlot())
                    /* Yes it was, take next variable */
                    continue iloop;
            }
            addElement(li2);
        }
    }

    /**
     * Subtract the other variable set from this one.  This removes
     * every variable from this set, that uses a slot in the other
     * variable set.
     * @param vs The other variable set.
     */
    public void subtract(VariableSet vs) {
        /* We count from top to bottom to have easier reorganization.
         * Note, that the variables have not to be in any particular
         * order.  */
        int newCount = elementCount;
        for (int i=newCount-1; i>=0; i--) {
            LocalInfo li1 = (LocalInfo) elementData[i];
            for (int j=0; j<vs.elementCount; j++) {
                LocalInfo li2 = (LocalInfo) vs.elementData[j];
                if (li1.getSlot() == li2.getSlot()) {
                    /* remove the element from this variable list. */
                    newCount--;
                    elementData[i] = elementData[newCount];
                    /* break the j-loop */
                    break;
                }
            }
        }
        /* Now set the new size */
        setSize(newCount);
    }

    /**
     * Subtract the other variable set from this one.  This removes
     * every variable from this set, that is in the other
     * variable set.
     * @param vs The other variable set.
     */
    public void subtractExact(VariableSet vs) {
        /* We count from top to bottom to have easier reorganization.
         * Note, that the variables have not to be in any particular
         * order.  */
        int newCount = elementCount;
        for (int i=newCount-1; i>=0; i--) {
            LocalInfo li1 = (LocalInfo) elementData[i];
            for (int j=0; j<vs.elementCount; j++) {
                LocalInfo li2 = (LocalInfo) vs.elementData[j];
                if (li1.getLocalInfo() == li2.getLocalInfo()) {
                    /* remove the element from this variable list. */
                    newCount--;
                    elementData[i] = elementData[newCount];
                    /* break the j-loop */
                    break;
                }
            }
        }
        /* Now set the new size */
        setSize(newCount);
    }
}




