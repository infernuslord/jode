/* UnfoundClassType Copyright (C) 1997-1998 Jochen Hoenicke.
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
import java.util.Vector;
import java.util.Stack;

/**
 * This class represents a type aproximation, consisting of multiple
 * interfaces and a class type.<p>
 *
 * If this is the bottom boundary, this specifies, which class our
 * type must extend and which interfaces it must implement.
 *
 * If this is the top boundary, this gives all interfaces and classes
 * that may extend the type.  I.e. the type may be one of the
 * interfaces or the class type or any of their super types.
 *
 * @author Jochen Hoenicke */
public class UnfoundClassType extends Type {

    String clazzName;

    public UnfoundClassType(String clazzName) {
        super(TC_UCLASS);
        this.clazzName = clazzName;
    }

    /**
     * Create the type corresponding to the range from bottomType to
     * this.  Checks if the given type range may be not empty.  This
     * means, that bottom.clazz is extended by this.clazz and that all
     * interfaces in bottom are implemented by an interface or by
     * clazz.
     * @param bottom the start point of the range
     * @return the range type, or tError if range is empty.  
     */
    public Type createRangeType(Type bottomType) {

        /* Unknown classes are only compatible to tObject and themself.
         */

        if (bottomType == tUnknown || bottomType == tObject)
            return  tRange(tObject, this);
        
        if (!bottomType.equals(this))
            return tError;

        return this;
    }

    /**
     * Returns the specialized type of this and type.
     * We have two classes and multiple interfaces.  The result 
     * should be the object that extends both objects
     * and the union of all interfaces.
     */
    public Type getSpecializedType(Type type) {
        return (type.typecode == TC_UNKNOWN 
                || type == tObject || type.equals(this)) ? this : tError;
    }

    /**
     * Returns the generalized type of this and type.  We have two
     * classes and multiple interfaces.  The result should be the
     * object that is the the super class of both objects and all
     * interfaces, that one class or interface of each type 
     * implements.  */
    public Type getGeneralizedType(Type type) {
        int code = type.typecode;
        if (code == TC_UNKNOWN || type.equals(this))
            return this;
        if (code == TC_ARRAY || code == TC_CLASS || code == TC_UCLASS)
            return tObject;
        return tError;
    }

    /**
     * Marks this type as used, so that the class is imported.
     */
    public void useType() {
        env.useClass(clazzName);
    }

    public String toString()
    {
        return env.classString(clazzName);
    }

    public boolean isClassType() {
        return true;
    }

    public String getDefaultName() {
        String name = clazzName;
        int dot = name.lastIndexOf('.');
        if (dot >= 0)
            name = name.substring(dot+1);
        if (Character.isUpperCase(name.charAt(0)))
            return name.toLowerCase();
        else
            return name+"_var";
    }

    public boolean equals(Object o) {
        return o == this
            || (o instanceof UnfoundClassType
                && ((UnfoundClassType)o).clazzName.equals(clazzName));
    }
}
