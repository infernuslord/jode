/* SimpleDictionary Copyright (C) 1998-1999 Jochen Hoenicke.
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

package jode.flow;
import java.util.Dictionary;
import java.util.Enumeration;

public class SimpleDictionary extends Dictionary {
    Object[] keyObjects = new Object[2];
    Object[] elementObjects = new Object[2];
    int count = 0;

    public int size() {
        return count;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public Enumeration keys() {
        return new ArrayEnum(count, keyObjects);
    }

    public Enumeration elements() {
        return new ArrayEnum(count, elementObjects);
    }
    
    public Object get(Object key) {
        for (int i=0; i< count; i++) {
            if (keyObjects[i].equals(key))
                return elementObjects[i];
        }
        return null;
    }

    public Object put(Object key, Object element) {
        for (int i=0; i< count; i++) {
            if (keyObjects[i].equals(key)) {
                Object old = elementObjects[i];
                elementObjects[i] = element;
                return old;
            }
        }
        if (count == keyObjects.length) {
            Object[] newArray = new Object[count*3/2];
            System.arraycopy(keyObjects,0,newArray,0,count);
            keyObjects = newArray;
            newArray = new Object[count*3/2];
            System.arraycopy(elementObjects,0,newArray,0,count);
            elementObjects = newArray;
        }
        keyObjects[count] = key;
        elementObjects[count] = element;
        count++;
        return null;
    }

    public Object remove(Object key) {
        for (int i=0; i< count; i++) {
            if (keyObjects[i].equals(key)) {
                Object old = elementObjects[i];
                count--;
                if (i < count) {
                    keyObjects[i] = keyObjects[count];
                    elementObjects[i] = elementObjects[count];
                }
                return old;
            }
        }
        return null;
    }
}
