/* Reference Copyright (C) 1999-2002 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; see the file COPYING.LESSER.  If not, write to
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
 * This class represents a field or method reference.  It consists of
 * the class name the method/field name and the type signature.
 */
public class Reference {
    /**
     * A reference consists of a class name, a member name and a type.
     */
    private final String clazz, name, type;

    private static final UnifyHash unifier = new UnifyHash();

    public static Reference getReference(String className, 
					 String name, String type) {
	int hash = className.hashCode() ^ name.hashCode() ^ type.hashCode();
	Iterator iter = unifier.iterateHashCode(hash);
	while (iter.hasNext()) {
	    Reference ref = (Reference) iter.next();
	    if (ref.clazz.equals(className)
		&& ref.name.equals(name)
		&& ref.type.equals(type))
		return ref;
	}
	Reference ref = new Reference(className, name, type);
	unifier.put(hash, ref);
	return ref;
    }

    private Reference(String clazz, String name, String type) {
	this.clazz = clazz;
	this.name  = name;
	this.type  = type;
    }

    public String getClazz() {
	return clazz;
    }

    public String getName() {
	return name;
    }

    public String getType() {
	return type;
    }
	
    public String toString() {
	String classStr = clazz;
	if (clazz.startsWith("L"))
	    classStr = clazz.substring(1, clazz.length() - 1)
		.replace('/', '.');
	return classStr + "." + name + " " + type;
    }
}
