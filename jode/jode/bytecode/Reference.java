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
 * This class represents a field or method reference.
 */
public class Reference {
    /**
     * The class info.
     */
    private final String className;
    /**
     * The member name.  Don't make this a MethodInfo, since the clazz
     * may not be readable.
     */
    private final String memberName;
    /**
     * The member type.
     */
    private final String memberType;

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
	    reference = new Reference(className, name, type);
///#ifdef JDK12
///	    references.put(sig, new WeakReference(reference));
///#else
            references.put(reference, reference);
///#endif
        }
	return reference;
    }

    private Reference(String className, String name, String type) {
	this.className = className.intern();
	this.memberName = name.intern();
	this.memberType = type.intern();
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
}
