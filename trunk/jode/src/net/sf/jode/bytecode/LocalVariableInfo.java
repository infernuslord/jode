/* LocalVariableInfo Copyright (C) 1999 Jochen Hoenicke.
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

package net.sf.jode.bytecode;
import net.sf.jode.util.UnifyHash;
///#def COLLECTIONS java.util
import java.util.Iterator;
///#enddef

/**
 * A simple class containing the info of the LocalVariableTable
 */
public final class LocalVariableInfo {
    private String name, type;
    private int slot;
    private static LocalVariableInfo anonymous[];
    static {
	grow(5);
    }
    private static final UnifyHash unifier = new UnifyHash();

    private LocalVariableInfo(int slot) {
	this.slot = slot;
    }

    private LocalVariableInfo(int slot, String name, String type) {
	this.slot = slot;
	this.name = name;
	this.type = type;
    }

    private static void grow(int upper) {
	LocalVariableInfo[] newAnon = new LocalVariableInfo[upper];
	int start = 0;
	if (anonymous != null) {
	    start = anonymous.length;
	    System.arraycopy(anonymous, 0, newAnon, 0, start);
	}
	anonymous = newAnon;
	for (int i=start; i< upper; i++)
	    anonymous[i] = new LocalVariableInfo(i);
    }
	
    public static LocalVariableInfo getInfo(int slot) {
	if (slot >= anonymous.length)
	    grow(Math.max(slot + 1, anonymous.length * 2));
	return anonymous[slot];
    }

    public static LocalVariableInfo getInfo(int slot, String name, String type) {
	if (name == null && type == null)
	    return getInfo(slot);
	int hash = slot ^ name.hashCode() ^ type.hashCode();
	Iterator iter = unifier.iterateHashCode(hash);
	while (iter.hasNext()) {
	    LocalVariableInfo lvi = (LocalVariableInfo) iter.next();
	    if (lvi.slot == slot
		&& lvi.name.equals(name)
		&& lvi.type.equals(type))
		return lvi;
	}
	LocalVariableInfo lvi = new LocalVariableInfo(slot, name, type);
	unifier.put(hash, lvi);
	return lvi;
    }
    
    public int getSlot() {
	return slot;
    }

    public String getName() {
	return name;
    }
    
    public String getType() {
	return type;
    }

    public String toString() {
	String result = ""+slot;
	if (name != null)
	    result += " ["+name+","+type+"]";
	return result;
    }
}
