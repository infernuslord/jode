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

/** 
 * This type represents an array type.
 *
 * @author Jochen Hoenicke 
 */
public class ArrayType extends Type {
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

    /**
     * Create the type corresponding to the range from bottomType to this.
     * @param bottomType the start point of the range
     * @return the range type, or tError if not possible.
     */
    public Type createRangeType(Type bottomType) {
        /*  tUnknown , tArray(x) -> <tObject, tArray(x)>
         *  tObject  , tArray(x) -> <tObject, tArray(x)>
         *  tArray(y), tArray(x) -> tArray( <y,x> )
         */
        return (bottomType == tUnknown || bottomType == tObject) 
            ?      tRange(tObject, this)
            :  (bottomType.typecode == TC_ARRAY)
            ?      tArray(elementType.createRangeType
                          (((ArrayType)bottomType).elementType))
            :      tError;
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
        return (type == tUnknown || type == tObject) 
            ?      this 
            :  (type.getTypeCode() == TC_ARRAY)
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
        return (type == tUnknown) 
            ?      this 
            :  (type.getTypeCode() == TC_CLASS) 
            ?      tObject
            :  (type.getTypeCode() == TC_ARRAY) 
            ?      tArray(elementType.getGeneralizedType
                          (((ArrayType)type).elementType))
            :      tError;
    }

    public String toString() {
        return elementType.toString()+"[]";
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
