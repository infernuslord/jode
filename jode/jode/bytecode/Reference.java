/* Reference Copyright (C) 1999 Jochen Hoenicke.
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

package jode.bytecode;
///#ifdef JDK12
///import java.lang.ref.WeakReference;
///import java.lang.ref.ReferenceQueue;
///import java.lang.ref.HashMap;
///#else
import java.util.Hashtable;
///#endif

/**
 * This class represents a field or method reference.
 */
public class Reference {
    /**
     * The reference string.  This is the class name, the member name and
     * the member type, all separated by a space.
     */
    private final String sig;
    /**
     * The position of the first and second space in the reference
     * string.
     */
    private final int firstSpace, secondSpace;

///#ifdef JDK12
///    private static final Map references = new HashMap();
///#else
    private static final Hashtable references = new Hashtable();
///#endif

    public static Reference getReference(String className, 
					 String name, String type) {
	String sig = className+" "+name+" "+type;
///#ifdef JDK12
///	WeakReference ref = (WeakReference) references.get(sig);
///	Reference reference = (ref == null) ? null : (Reference) ref.get();
///#else
	Reference reference = (Reference) references.get(sig);
///#endif
        if (reference == null) {
	    sig = sig.intern();
	    int firstSpace = className.length();
	    int secondSpace = firstSpace + name.length() + 1;
	    reference = new Reference(sig, firstSpace, secondSpace);
///#ifdef JDK12
///	    references.put(sig, new WeakReference(reference));
///#else
            references.put(sig, reference);
///#endif
        }
	return reference;
    }

    private Reference(String sig, int first, int second) {
	this.sig = sig;
	this.firstSpace = first;
	this.secondSpace = second;
    }

    public String getClazz() {
	return sig.substring(0, firstSpace);
    }

    public String getName() {
	return sig.substring(firstSpace + 1, secondSpace);
    }

    public String getType() {
	return sig.substring(secondSpace + 1);
    }
	
    public String toString() {
	return sig;
    }
}
