/* SimpleSet Copyright (C) 1998-1999 Jochen Hoenicke.
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

package jode.util;
import java.util.Dictionary;
import java.util.Enumeration;
///#ifdef JDK12
///import java.util.AbstractSet;
///import java.util.Iterator;
///#endif

public class SimpleSet
///#ifdef JDK12
///    extends AbstractSet
///#endif
    implements Cloneable
{
    Object[] elementObjects = new Object[2];
    int count = 0;

    public int size() {
        return count;
    }

    public boolean add(Object element) {
	if (element == null)
	    throw new NullPointerException();

	for (int i=0; i< count; i++) {
	    if (element.equals(elementObjects[i]))
		return false;
	}
	
	if (count == elementObjects.length) {
            Object[] newArray = new Object[(count+1)*3/2];
            System.arraycopy(elementObjects,0,newArray,0,count);
            elementObjects = newArray;
        }
        elementObjects[count++] = element;
	return true;
    }
	
    public Enumeration elements() {
        return new ArrayEnum(count, elementObjects);
    }

    public Object clone() {
        try {
            SimpleSet other = (SimpleSet) super.clone();
            if (count > 0) {
                other.elementObjects = new Object[count];
                System.arraycopy(elementObjects, 0, 
				 other.elementObjects, 0, count);
            }
            return other;
        } catch (CloneNotSupportedException ex) {
            throw new jode.AssertError("Clone?");
        }
    }

///#ifdef JDK12
///    public Iterator iterator() {
///	return new Iterator() {
///	    int pos = 0;
///
///	    public boolean hasNext() {
///		return pos < count;
///	    }
///	    
///	    public Object next() {
///		return elementObjects[pos++];
///	    }
///	  
///	    public void remove() {
///		if (pos < count)
///		    System.arraycopy(elementObjects, pos, 
///				     elementObjects, pos-1, count - pos);
///		count--;
///		pos--;
///	    }
///	};
///    }
///#else

    public boolean isEmpty() {
	return count == 0;
    }

    public boolean addAll(SimpleSet other) {
	boolean changed = false;
	for (int i=0; i < other.count; i++) {
	    changed |= add(other.elementObjects[i]);
	}
	return changed;
    }

    public boolean contains(Object element) {
	for (int i=0; i < count; i++) {
            if (elementObjects[i].equals(element))
		return true;
	}
	return false;
    }

    public boolean remove(Object element) {
        for (int i=0; i< count; i++) {
            if (elementObjects[i].equals(element)) {
                count--;
                if (i < count)
                    elementObjects[i] = elementObjects[count];
		return true;
            }
        }
	return false;
    }
    
    public boolean retainAll(SimpleSet other) {
	int low = 0;
        for (int high=0; high < count; high++) {
	    if (other.contains(elementObjects[high]))
		elementObjects[low++] = elementObjects[high];
	}
	if (count == low)
	    return false;
	count = low;
        return true;
    }

    public boolean removeAll(SimpleSet other) {
	int low = 0;
        for (int high=0; high < count; high++) {
	    if (!other.contains(elementObjects[high]))
		elementObjects[low++] = elementObjects[high];
	}
	if (count == low)
	    return false;
	count = low;
        return true;
    }

    public String toString() {
	StringBuffer sb = new StringBuffer("{");
	String komma = "";
	for (int i=0; i< count; i++) {
	    sb.append(komma).append(elementObjects[i].toString());
	    komma = ", ";
	}
	return sb.append("}").toString();
    }
///#endif
}
