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
///#endif
import java.util.*;

/**
 * This class represents a field or method reference
 *
 * For simplicity currently most fields are public.  You shouldn't change
 * many of them, though.
 */
public class Reference {
    /**
     * The class info.
     */
    final String className;
    /**
     * The member name.  Don't make this a MethodInfo, since the clazz
     * may not be readable.
     */
    final String memberName;
    /**
     * The member type.
     */
    final String memberType;
    /**
     * The cached hash code
     */
    final int cachedHashCode;

///#ifdef JDK12
///    private static final Map references = new WeakHashMap();
///#else
    private static final Hashtable references = new Hashtable();
///#endif

    public static Reference getReference(String className, 
					 String name, String type) {
	Reference reference = new Reference(className, name, type);
///#ifdef JDK12
///	WeakReference ref = (WeakReference) references.get(reference);
///	Reference cachedRef = (ref == null) ? null : (Reference) ref.get();
///#else
	Reference cachedRef = (Reference) references.get(reference);
///#endif
        if (cachedRef == null) {
///#ifdef JDK12
///	    references.put(reference, new WeakReference(reference));
///#else
            references.put(reference, reference);
///#endif
	    return reference;
        }
	return cachedRef;
    }

    private Reference(String className, String name, String type) {
	this.className = className.intern();
	this.memberName = name.intern();
	this.memberType = type.intern();
	this.cachedHashCode = 
	    className.hashCode() ^ name.hashCode() ^ type.hashCode();
    }

    public String getClazz() {
	return className;
    }

    public String getName() {
	return memberName;
    }

    public String getType() {
	return memberType;
    }
	
    public String toString() {
	return className + " " + memberName + " " + memberType;
    }

    public boolean equals(Object o) {
	if (o instanceof Reference) {
	    Reference other = (Reference) o;
	    return other.cachedHashCode == cachedHashCode
		&& other.className.equals(className)
		&& other.memberName.equals(memberName)
		&& other.memberType.equals(memberType);
	}
	return false;
    }

    public int hashCode() {
	return cachedHashCode;
    }
}
