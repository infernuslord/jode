/* ReferenceType Copyright (C) 1997-1998 Jochen Hoenicke.
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
import java.util.Stack;

/**
 * This is an abstrace super class of all reference types.  Reference
 * types are ClassInterfacesType, ArrayType and NullType. <p>
 *
 * @author Jochen Hoenicke 
 */
public abstract class ReferenceType extends Type {
    public ReferenceType(int typecode) {
	super(typecode);
    }
    
    /**
     * Returns the specialized type of this and type. 
     * The result should be the Type that extends both Types
     * (this and type).  This correspondents to the common bottom Type
     * in a range type.
     * @param type the other type.
     * @return the specialized type.
     */
    public abstract Type getSpecializedType(Type typ);
    /**
     * Returns the generalized type of this and type.  The result
     * should be the types that both types extends or implements. This
     * correspondents to the common top Type in a range type.
     * @param type the other type.
     * @return the generalized type
     */
    public abstract Type getGeneralizedType(Type typ);
    public abstract Type createRangeType(ReferenceType bottom);

    /**
     * Tells if all otherIfaces, are implemented by at least one
     * ifaces or by clazz.
     * 
     * This is a useful function for generalizing/specializing interface
     * types or arrays.
     * @param clazz The clazz, can be null.
     * @param ifaces The ifaces.
     * @param otherifaces The other ifaces, that must be implemented.
     * @return true, if all otherIfaces are implemented.
     */
    protected static boolean implementsAllIfaces(ClassInfo clazz,
						 ClassInfo[] ifaces,
						 ClassInfo[] otherIfaces) {
    big:
        for (int i=0; i < otherIfaces.length; i++) {
            ClassInfo iface = otherIfaces[i];
            if (clazz != null && iface.implementedBy(clazz))
                continue big;
            for (int j=0; j < ifaces.length; j++) {
                if (iface.implementedBy(ifaces[j]))
                        continue big;
            }
            return false;
        }
        return true;
    }

    public Type getSuperType() {
	return (this == tObject) ? tObject : tRange(tObject, this);
    }

    public Type getSubType() {
        return tRange(this, tNull);
    }

    /**
     * Intersect this type with another type and return the new type.
     * @param type the other type.
     * @return the intersection, or tError, if a type conflict happens.
     */
    public Type intersection(Type type) {
	if (type == tError)
	    return type;
	if (type == Type.tUnknown)
	    return this;

	Type newBottom = getSpecializedType(type);
	Type newTop    = getGeneralizedType(type);
	Type result;
	if (newTop.equals(newBottom))
	    result = newTop;
	else if (newTop instanceof ReferenceType
		 && newBottom instanceof ReferenceType)
	    result = ((ReferenceType) newTop)
		.createRangeType((ReferenceType) newBottom);
	else
	    result = tError;

        if (Decompiler.isTypeDebugging) {
	    Decompiler.err.println("intersecting "+ this +" and "+ type + 
                                   " to " + result);
	}	    
        return result;
    }
}
