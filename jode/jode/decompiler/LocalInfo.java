/* LocalInfo Copyright (C) 1998-1999 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id$
 */

package jode.decompiler;
import java.util.Enumeration;
import java.util.Vector;
import jode.GlobalOptions;
import jode.type.Type;
import jode.expr.LocalVarOperator;

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
    private static int nextAnonymousSlot = -1;
    private int slot;
    private boolean nameIsGenerated = false;
    private boolean isUnique;
    private String name;
    private Type type;
    private LocalInfo shadow;
    private Vector operators = new Vector();
    private Vector hints = new Vector();

    /**
     * Create a new local info with an anonymous slot.
     */
    public LocalInfo() {
        name = null;
        type = Type.tUnknown;
        this.slot = nextAnonymousSlot--;
    }

    /**
     * Create a new local info.
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

    public void addHint(LocalVarEntry entry) {
	getLocalInfo().hints.addElement(entry);
    }

    public int getUseCount() {
	return getLocalInfo().operators.size();
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
		if (!nameIsGenerated)
		    shadow.name = li.name;
//  		GlobalOptions.err.println("combining "+name+"("+type+") and "
//  				       +li.name+"("+li.type+")");
                li.setType(type);


                boolean needTypeUpdate = !li.type.equals(type);

                java.util.Enumeration enum = operators.elements();
                while (enum.hasMoreElements()) {
                    LocalVarOperator lvo = 
                        (LocalVarOperator) enum.nextElement();
                    if (needTypeUpdate) {
                        if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_TYPES) != 0)
                            GlobalOptions.err.println("updating " + lvo);
                        lvo.updateType();
                    }
                    shadow.operators.addElement(lvo);
                }

		enum = hints.elements();
		while (enum.hasMoreElements()) {
		    Object entry = enum.nextElement();
		    if (!shadow.hints.contains(entry))
			shadow.hints.addElement(entry);
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
     * Returns true if the local already has a name.
     */
    public boolean hasName() {
	return getLocalInfo().name != null;
    }

    public String guessName() {
        if (shadow != null) {
            while (shadow.shadow != null) {
                shadow = shadow.shadow;
            }
            return shadow.guessName();
        }
	if (name == null) {
	    Enumeration enum = hints.elements();
	    while (enum.hasMoreElements()) {
		LocalVarEntry entry = (LocalVarEntry) enum.nextElement();
		if (type.isOfType(entry.getType())) {
		    name = entry.getName();
		    setType(entry.getType());
		    return name;
		}
	    }
	    nameIsGenerated = true;
            if (jode.Decompiler.prettyLocals && type != null) {
                name = type.getHint().getDefaultName();
            } else {
                name = type.getHint().getDefaultName()
		    + (slot >= 0 ? "_" + slot : "") + "_" + serialnr++ + "_";
                isUnique = true;
            }
	}
	return name;
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
	    return "local_" + slot + "_"+ Integer.toHexString(hashCode());
	}
        return name;
    }

    public boolean isNameGenerated() {
	return nameIsGenerated;
    }

    /**
     * Get the slot of this local.
     */
    public int getSlot() {
        /* The slot may change when shadowing for anonymous locals */
        return getLocalInfo().slot;
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
            li.name = name + "_" + serialnr++ + "_";
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
    public Type setType(Type otherType) {
        LocalInfo li = getLocalInfo();
        Type newType = li.type.intersection(otherType);
	if (newType == Type.tError
	    && otherType != Type.tError && li.type != Type.tError) {
	    GlobalOptions.err.println("Type error in local " + getName()+": "
				   + li.type + " and " + otherType);
	    Thread.dumpStack();
	}
        else if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_TYPES) != 0)
            GlobalOptions.err.println(getName()+" setType, new: "+newType
				   + " old: "+li.type);

        if (!li.type.equals(newType)) {
            li.type = newType;
            java.util.Enumeration enum = li.operators.elements();
            while (enum.hasMoreElements()) {
                LocalVarOperator lvo = (LocalVarOperator) enum.nextElement();
                if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_TYPES) != 0)
                    GlobalOptions.err.println("updating "+lvo);
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
