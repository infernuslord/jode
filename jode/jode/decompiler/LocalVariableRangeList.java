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
        int startAddr;
        int endAddr;
        MyLocalInfo next;
        
        MyLocalInfo(int slot, int s, int e, String n, Type t) {
            super (slot);
            startAddr = s;
            endAddr = e;
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
        MyLocalInfo prev = null;
        MyLocalInfo next = list;
        while (next != null && next.endAddr < li.startAddr) {
            prev = next;
            next = next.next;
        }
	/* prev.endAddr < li.startAddr <= next.endAddr
	 */
        if (next != null && li.endAddr >= next.startAddr) {
	    if (next.getType().equals(li.getType())
		&& next.getName().equals(li.getName())) {
		/* Same type, same name and overlapping range.
		 * This is the same local: extend next to the common
		 * range and don't add li.
		 */
		next.startAddr = Math.min(next.startAddr, li.startAddr);
		next.endAddr   = Math.max(next.endAddr, li.endAddr);
		return;
	    }
            Decompiler.err.println("warning: non disjoint locals");
	}
        li.next = next;
        if (prev == null)
            list = li;
        else
            prev.next = li;
    }

    private LocalInfo find(int addr) {
        MyLocalInfo li = list;
        while (li != null && li.endAddr < addr)
            li = li.next;
        if (li == null || li.startAddr > addr /* XXX addr+1? weired XXX */) {
            LocalInfo temp = new LocalInfo(slot);
            return temp;
        }
        return li;
    }

    public void addLocal(int startAddr, int endAddr,
                         String name, Type type) {
        MyLocalInfo li = new MyLocalInfo(slot,startAddr,endAddr,name,type);
        add (li);
    }

    public LocalInfo getInfo(int addr) {
        return find(addr);
    }
}
