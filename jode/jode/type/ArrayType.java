/* ArrayType Copyright (C) 1997-1998 Jochen Hoenicke.
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
package jode;
import jode.bytecode.ClassInfo;
import java.util.Vector;

/** 
 * This type represents an array type.
 *
 * @author Jochen Hoenicke 
 */
public class ArrayType extends Type {
    // The interfaces that an array implements:
    final static ClassInfo[] ifaces = {
	// Make sure to list all interfaces, even if some interface
	// implements another (or change code in getGeneralizedType().
	ClassInfo.forName("java.lang.Cloneable"),
	ClassInfo.forName("java.io.Serializable")
    };

    Type elementType;

    public ArrayType(Type elementType) {
        super(TC_ARRAY);
        this.elementType = elementType;
    }

    public Type getElementType() {
        return elementType;
    }

    public Type getBottom() {
        return tArray(elementType.getBottom());
    }

    public Type getTop() {
        return tArray(elementType.getTop());
    }

    public Type getHint() {
	return tArray(elementType.getHint());
    }

    static boolean implementsAllIfaces(ClassInfo[] otherIfaces) {
    big:
        for (int i=0; i < otherIfaces.length; i++) {
            ClassInfo iface = otherIfaces[i];
            for (int j=0; j < ifaces.length; j++) {
                if (iface.implementedBy(ifaces[j]))
                        continue big;
            }
            return false;
        }
        return true;
    }
    
    /**
     * Create the type corresponding to the range from bottomType to this.
     * @param bottomType the start point of the range
     * @return the range type, or tError if not possible.
     */
    public Type createRangeType(Type bottomType) {
        /*  tUnknown , tArray(x) -> <tObject, tArray(x)>
         *  obj      , tArray(x) -> <obj, tArray(x)>
	 *    iff tArray extends and implements obj
         *  tArray(y), tArray(x) -> tArray( <y,x> )
         */
	if (bottomType == tUnknown || bottomType == tObject)
            return tRange(tObject, this);

	if (bottomType instanceof ClassInterfacesType) {
	    ClassInterfacesType bottom = (ClassInterfacesType) bottomType;
	    if (bottom.clazz == null
		&& implementsAllIfaces(bottom.ifaces))
		return tRange(bottom, this);
	    return tError;
	}
        return (bottomType.typecode == TC_ARRAY)
            ? tArray(elementType.createRangeType
		     (((ArrayType)bottomType).elementType))
            : tError;
    }

    /**
     * Returns the common sub type of this and type.
     * @param type the other type.
     * @return the common sub type.
     */
    public Type getSpecializedType(Type type) {
        /*  tArray(x), tUnknown  -> tArray(x)
         *  tArray(x), tObject   -> tArray(x)
         *  tArray(x), tArray(y) -> tArray(x.getSpecialized(y))
         *  tArray(x), other     -> tError
         */
	if (type == tUnknown || type == tObject)
            return this;

	if (type instanceof ClassInterfacesType) {
	    ClassInterfacesType other = (ClassInterfacesType) type;
	    if (other.clazz == null
		&& implementsAllIfaces(other.ifaces))
		return this;
	    return tError;
	}
        return (type.getTypeCode() == TC_ARRAY)
            ?      tArray(elementType.getSpecializedType
                          (((ArrayType)type).elementType))
            :      tError;
    }

    /**
     * Returns the common super type of this and type.
     * @param type the other type.
     * @return the common super type.
     */
    public Type getGeneralizedType(Type type) {
        /*  tArray(x), tUnknown  -> tArray(x)
         *  tArray(x), tClass(y) -> tObject
         *  tArray(x), tArray(y) -> tArray(x.getGeneralized(y))
         *  tArray(x), other     -> tError
         */
	if (type == tUnknown)
            return this;
        if (type.getTypeCode() == TC_ARRAY)
	    return tArray(elementType.getGeneralizedType
                          (((ArrayType)type).elementType));

	if (type.getTypeCode() == TC_CLASS) {
	    ClassInterfacesType other = (ClassInterfacesType) type;
	    if (implementsAllIfaces(other.ifaces)) {
		if (other.clazz == null)
		    return other;
		else
		    return ClassInterfacesType.create(null, other.ifaces);
	    }

	    if (other.implementsAllIfaces(ifaces))
		return ClassInterfacesType.create(null, ifaces);
	    
	    /* Now the more complicated part: find all interfaces, that are
	     * implemented by one interface or class in each group.
	     *
	     * First get all interfaces of this.clazz and this.ifaces.
	     */
	    Vector newIfaces = new Vector();
	iface_loop:
	    for (int i=0; i < ifaces.length; i++) {
		/* Now consider each array interface.  If any clazz or
		 * interface in other implements it, add it to the
		 * newIfaces vector.  */
		if (other.clazz != null 
		    && ifaces[i].implementedBy(other.clazz)) {
		    newIfaces.addElement(ifaces[i]);
		    continue iface_loop;
		}
		for (int j=0; j<other.ifaces.length; j++) {
		    if (ifaces[i].implementedBy(other.ifaces[j])) {
			newIfaces.addElement(ifaces[i]);
			continue iface_loop;
		    }
		}
	    }
	    ClassInfo[] ifaceArray = new ClassInfo[newIfaces.size()];
	    newIfaces.copyInto(ifaceArray);
	    return ClassInterfacesType.create(null, ifaceArray);
	}
	return tError;
    }

    /**
     * Checks if we need to cast to a middle type, before we can cast from
     * fromType to this type.
     * @return the middle type, or null if it is not necessary.
     */
    public Type getCastHelper(Type fromType) {
	Type topType = fromType.getTop();
	switch (topType.getTypeCode()) {
	case TC_ARRAY:
	    if (!elementType.isClassType()
		|| !((ArrayType)topType).elementType.isClassType())
		return tObject;
	    Type middleType = elementType.getCastHelper
		(((ArrayType)topType).elementType);
	    if (middleType != null)
		return tArray(middleType);
	    return null;
	case TC_CLASS:
	    ClassInterfacesType top = (ClassInterfacesType) topType;
	    if (top.clazz == null
		&& implementsAllIfaces(top.ifaces))
		return null;
	    return tObject;
	case TC_UNKNOWN:
	    return null;
	}
	return tObject;
    }

    /**
     * Checks if this type represents a valid type instead of a list
     * of minimum types.
     */
    public boolean isValidType() {
	return elementType.isValidType();
    }

    public boolean isClassType() {
        return true;
    }

    /**
     * Marks this type as used, so that the class is imported.
     */
    public void useType() {
        elementType.useType();
    }

    public String getTypeSignature() {
	return "["+elementType.getTypeSignature();
    }

    public String toString() {
        return elementType.toString()+"[]";
    }

    private static String pluralize(String singular) {
        return singular + 
            ((singular.endsWith("s") || singular.endsWith("x")
              || singular.endsWith("sh") || singular.endsWith("ch")) 
             ? "es" : "s");
    }

    public String getDefaultName() {
	if (elementType instanceof ArrayType)
	    return elementType.getDefaultName();
        return pluralize(elementType.getDefaultName());
    }

    public boolean equals(Object o) {
        if (o == this) 
            return true;
        if (o instanceof ArrayType) {
            ArrayType type = (ArrayType) o;
            return type.elementType.equals(elementType);
        }
        return false;
    }
}
