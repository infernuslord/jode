/* SystemClassType Copyright (C) 1998-1999 Jochen Hoenicke.
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

package jode.type;
import jode.bytecode.ClassInfo;
import java.util.Vector;
import java.util.Stack;
import java.util.Hashtable;
import java.io.IOException;

/**
 * This class represents the type of a system class, i.e. the classes
 * from package java.lang, that need special handling, like
 * Object, String, StringBuffer, etc.
 *
 * @author Jochen Hoenicke 
 */
public class SystemClassType extends ClassType {
    ClassType superType;
    ClassType[] ifacesTypes;
    boolean isFinal, isInterface;

    /**
     * @param className The name of this system class, must be interned.
     */
    public SystemClassType(String className, 
			   ClassType superType, 
			   ClassType[] ifacesTypes,
			   boolean isFinal, boolean isInterface) {
	super(TC_SYSCLASS, className);
	this.superType = superType;
	this.ifacesTypes = ifacesTypes;
	this.isFinal = isFinal;
	this.isInterface = isInterface;
    }

    public boolean isInterface() {
	return isInterface;
    }

    public boolean isFinal() {
	return isFinal;
    }

    public boolean isUnknown() {
	return false;
    }

    public ClassType getSuperClass() {
	return superType;
    }

    public ClassType[] getInterfaces() {
	return ifacesTypes;
    }
}
