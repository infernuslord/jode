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
import java.util.Vector;
import java.util.Enumeration;
import jode.Type;

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
    String className;
    /**
     * The member name.  Don't make this a MethodInfo, since the clazz
     * may not be readable.
     */
    String memberName;
    /**
     * The member type.
     */
    String memberType;
    /**
     * The cached hash code
     */
    int cachedHashCode;

    public Reference(String className, String name, String type) {
	this.className = className.intern();
	this.memberName = name.intern();
	this.memberType = type.intern();
	cachedHashCode = 
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
	
    public void setClazz(String name) {
	className = name;
    }

    public void setName(String name) {
	memberName = name;
    }

    public void setType(String type) {
	memberType = type;
    }
	
    public String toString() {
	return className + "." + memberName + "." + memberType;
    }

    public boolean equals(Object o) {
	if (o instanceof Reference) {
	    Reference other = (Reference) o;
	    return other.className.equals(className)
		&& other.memberName.equals(memberName)
		&& other.memberType.equals(memberType);
	}
	return false;
    }

    public int hashCode() {
	return cachedHashCode;
    }
}
