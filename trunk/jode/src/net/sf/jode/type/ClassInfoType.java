/* ClassInfoType Copyright (C) 1998-2002 Jochen Hoenicke.
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

package net.sf.jode.type;
import net.sf.jode.bytecode.ClassInfo;
import net.sf.jode.GlobalOptions;

import java.lang.reflect.Modifier;
import java.io.IOException;
import java.util.Vector;
import java.util.Stack;
import java.util.Hashtable;

/**
 * This class is the type representing a class loaded from a ClassPath.<p>
 *
 * @author Jochen Hoenicke */
public class ClassInfoType extends ClassType {
    ClassInfo clazz;
    ClassType superClass = null;
    ClassType[] interfaces = null;

    public ClassInfo getClazz() {
        return clazz;
    }

    public ClassInfoType(ClassInfo clazz) {
        super(TC_CLASS, clazz.getName());
	this.clazz = clazz;
	try {
	    clazz.load(ClassInfo.HIERARCHY);
	} catch (IOException ex) {
	    clazz.guess(ClassInfo.HIERARCHY);
	    GlobalOptions.err.println
		("Can't get full class hierarchy for "+clazz+
		 " types may be incorrect.");
	    GlobalOptions.err.println(ex.toString());
	}
    }

    public boolean isUnknown() {
	return clazz.isGuessed();
    }

    public boolean isFinal() {
	return Modifier.isFinal(clazz.getModifiers());
    }

    public boolean isInterface() {
	return clazz.isInterface();
    }

    public ClassType getSuperClass() {
	if (clazz.isInterface())
	    return null;
	if (superClass == null) {
	    ClassInfo superInfo = clazz.getSuperclass();
	    if (superInfo == null)
		return null;
	    superClass = Type.tClass(superInfo);
	}
	return superClass;
    }

    public ClassType[] getInterfaces() {
	if (interfaces == null) {
	    ClassInfo[] ifaceInfos = clazz.getInterfaces();
	    if (ifaceInfos.length == 0)
		interfaces = EMPTY_IFACES;
	    else {
		interfaces = new ClassType[ifaceInfos.length];
		for (int i=0; i < interfaces.length; i++)
		    interfaces[i] = Type.tClass(ifaceInfos[i]);
	    }
	}
	return interfaces;
    }

    public ClassInfo getClassInfo() {
	return clazz;
    }

    public boolean equals(Object o) {
	if (o instanceof ClassInfoType)
	    return ((ClassInfoType) o).clazz == clazz;
	return super.equals(o);
    }
}




