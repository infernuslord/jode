/* LocalVariableRangeList Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.Decompiler;
import jode.type.Type;

public class LocalVariableRangeList {

    class MyLocalInfo extends LocalInfo {
        int start;
        int length;
        MyLocalInfo next;
        
        MyLocalInfo(int slot, int s, int l, String n, Type t) {
            super (slot);
            start = s;
            length = l;
            setName(n);
            setType(t);
            next = null;
        }
    }

    MyLocalInfo list = null;
    int slot;

    LocalVariableRangeList(int slot) {
        this.slot = slot;
    }

    private void add(MyLocalInfo li) {
        MyLocalInfo before = null;
        MyLocalInfo after = list;
        while (after != null && after.start < li.start) {
            before = after;
            after = after.next;
        }
        if (after != null && li.start + li.length > after.start) {
	    if (after.getType().equals(li.getType())
		&& after.getName().equals(li.getName())) {
		/* Same type, same name and overlapping range.
		 * This is the same local: extend after to the common
		 * range and don't add li.
		 */
		after.length += after.start - li.start;
		after.start = li.start;
		if (li.length > after.length)
		    after.length = li.length;
		return;
	    }
            Decompiler.err.println("warning: non disjoint locals");
	}
        li.next = after;
        if (before == null)
            list = li;
        else
            before.next = li;
    }

    private LocalInfo find(int addr) {
        MyLocalInfo li = list;
        while (li != null && addr >= li.start+li.length)
            li = li.next;
        if (li == null || li.start > addr+1 /* XXX weired XXX */) {
            LocalInfo temp = new LocalInfo(slot);
            return temp;
        }
        return li;
    }

    public void addLocal(int start, int length,
                         String name, Type type) {
        MyLocalInfo li = new MyLocalInfo(slot,start,length,name,type);
        add (li);
    }

    public LocalInfo getInfo(int addr) {
        return find(addr);
    }
}
