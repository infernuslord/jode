/* RangeType Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.AssertError;
import jode.GlobalOptions;
import java.util.Hashtable;

/**
 * This class represents an object type which isn't fully known.  
 * The real object type lies in a range of types between topType
 * and bottomType. <p>
 *
 * For a totally unknown type topType is tObject and bottomType is
 * null.  It is always garanteed that topType is an Array or an Object
 * and that bottomType is null or an Array or an Object. <p>
 *
 * The hintType gives a hint which type it probably is.  It is used to
 * generate the type declaration.
 *
 * @author Jochen Hoenicke
 * @date 98/08/06
 */
public class RangeType extends Type {
    final ReferenceType bottomType;
    final ReferenceType topType;
    public RangeType(ReferenceType bottomType, 
		     ReferenceType topType) {
        super(TC_RANGE);
	if (bottomType == tNull)
	    throw new jode.AssertError("bottom is NULL");
	this.bottomType = bottomType;
	this.topType    = topType;
    }

    public ReferenceType getBottom() {
        return bottomType;
    }

    public ReferenceType getTop() {
        return topType;
    }

    public Type getHint() {
	return topType == tNull && bottomType.equals(bottomType.getHint()) 
	    ? bottomType.getHint(): topType.getHint();
    }

    public Type getSuperType() {
	return topType.getSuperType();
    }

    public Type getSubType() {
        return bottomType.getSubType();
    }

//      /**
//       * Create the type corresponding to the range from bottomType to this.
//       * @param bottomType the start point of the range
//       * @return the range type, or tError if not possible.
//       */
//      public ReferenceType createRangeType(ReferenceType bottomType) {
//          throw new AssertError("createRangeType called on RangeType");
//      }

//      /**
//       * Returns the common sub type of this and type.
//       * @param type the other type.
//       * @return the common sub type.
//       */
//      public ReferenceType getSpecializedType(ReferenceType type) {
//          throw new AssertError("getSpecializedType called on RangeType");
//      }

//      /**
//       * Returns the common super type of this and type.
//       * @param type the other type.
//       * @return the common super type.
//       */
//      public Type getGeneralizedType(Type type) {
//          throw new AssertError("getGeneralizedType called on RangeType");
//      }
	    
    /**
     * Checks if we need to cast to a middle type, before we can cast from
     * fromType to this type.
     * @return the middle type, or null if it is not necessary.
     */
    public Type getCastHelper(Type fromType) {
	return topType.getCastHelper(fromType);
    }

    public String getTypeSignature() {
        if (topType.isClassType() || !bottomType.isValidType())
            return topType.getTypeSignature();
        else
            return bottomType.getTypeSignature();
    }

    public Class getTypeClass() throws ClassNotFoundException {
        if (topType.isClassType() || !bottomType.isValidType())
            return topType.getTypeClass();
        else
            return bottomType.getTypeClass();
    }

    public String toString()
    {
	if (topType == tNull)
	    return "<" + bottomType + "-NULL>";
	return "<" + bottomType + "-" + topType + ">";
    }

    public String getDefaultName() {
	throw new AssertError("getDefaultName() not called on Hint");
    }

    public int hashCode() {
	int hashcode = topType.hashCode();
	return (hashcode << 16 | hashcode >>> 16) ^ bottomType.hashCode();
    }

    public boolean equals(Object o) {
        if (o instanceof RangeType) {
            RangeType type = (RangeType) o;
            return topType.equals(type.topType) 
                && bottomType.equals(type.bottomType);
        }
        return false;
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

	Type top, bottom, result;
	bottom = bottomType.getSpecializedType(type);
	top = topType.getGeneralizedType(type);
	if (top.equals(bottom))
	    result = top;
	else if (top instanceof ReferenceType
		 && bottom instanceof ReferenceType)
	    result = ((ReferenceType)top)
		.createRangeType((ReferenceType)bottom);
	else
	    result = tError;

        if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_TYPES) != 0) {
	    GlobalOptions.err.println("intersecting "+ this +" and "+ type + 
                                   " to " + result);
	}	    
        return result;
    }
}

