/* ClassInterfacesType Copyright (C) 1997-1998 Jochen Hoenicke.
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
public class ClassInterfacesType extends Type {

    Class clazz;
    Class ifaces[];

    public final static Class cObject = Object.class;
    
    public ClassInterfacesType(String clazzName) {
        super(TC_CLASS);
	try {
            Class clazz = Class.forName(clazzName);
            if (clazz.isInterface()) {
                this.clazz = null;
                ifaces = new Class[] {clazz};
            } else {
                this.clazz = clazz;
                ifaces = new Class[0];
            }
        } catch (ClassNotFoundException ex) {
            throw new AssertError(ex.toString());
        }
    }

    public ClassInterfacesType(Class clazz) {
        super(TC_CLASS);
        if (clazz.isInterface()) {
            this.clazz = null;
            ifaces = new Class[] { clazz };
        } else {
            this.clazz = clazz;
            ifaces = new Class[0];
        }
    }

    public ClassInterfacesType(Class clazz, Class[] ifaces) {
        super(TC_CLASS);
        this.clazz = clazz;
        this.ifaces = ifaces;
    }

    private static Type create(Class clazz, Class[] ifaces) {
        /* Make sure that every {java.lang.Object} equals tObject */
        if (ifaces.length == 0 && (clazz == cObject || clazz == null)) 
            return tObject;
        return new ClassInterfacesType(clazz, ifaces);
    }

    public final static boolean superClassOf(Class parent, Class clazz) {
        while (clazz != parent && clazz != null) {
            clazz = clazz.getSuperclass();
        }
        return clazz == parent;
    }

    public final static boolean implementedBy(Class iface, Class clazz) {
        while (clazz != iface && clazz != null) {
            Class[] ifaces = clazz.getInterfaces();
            for (int i=0; i< ifaces.length; i++) {
                if (implementedBy(iface, ifaces[i]))
                    return true;
            }
            clazz = clazz.getSuperclass();
        }
        return clazz != null;
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

        if (bottomType == tUnknown || bottomType == tObject)
            return (this == tObject) ? tObject : tRange(tObject, this);
        
        if (bottomType.typecode != TC_CLASS)
            return tError;

        ClassInterfacesType bottom = (ClassInterfacesType) bottomType;

        if (bottom.clazz != null
            && bottom.clazz != cObject) {
            /* The searched type must be a class type.
             */
            if (this.ifaces.length != 0
                || !superClassOf(bottom.clazz,this.clazz))
                return tError;
            
            /* All interfaces must be implemented by this.clazz
             */
            for (int i=0; i < bottom.ifaces.length; i++) {
                if (!implementedBy(bottom.ifaces[i], this.clazz))
                    return tError;
                }

            if (bottom.clazz == this.clazz
                && bottom.ifaces.length == 0)
                return bottom;

            return tRange(bottom, create(this.clazz, new Class[0]));
            
        } else {
            
            /* Now bottom.clazz is null (or tObject), find all
             * classes/interfaces that implement all bottom.ifaces.  
             */
            Class clazz = this.clazz;
            if (clazz != null) {
                for (int i=0; i < bottom.ifaces.length; i++) {
                    if (!implementedBy(bottom.ifaces[i], clazz)) {
                        clazz = null;
                        break;
                    }
                }
            }
            Vector ifaces = new Vector();
        big_loop:
            for (int j=0; j < this.ifaces.length; j++) {
                for (int i=0; i < bottom.ifaces.length; i++) {
                    if (!implementedBy(bottom.ifaces[i], this.ifaces[j]))
                        continue big_loop;
                }
                ifaces.addElement(this.ifaces[j]);
            }

            /* If bottom and the new top are the same single interface
             * return it.
             */
            if (clazz == null 
                && bottom.ifaces.length == 1 && ifaces.size() == 1 
                && bottom.ifaces[0] == ifaces.elementAt(0))
                return bottom;

            Class[] ifaceArray = new Class[ifaces.size()];
            ifaces.copyInto(ifaceArray);
            return tRange(bottom, create(clazz, ifaceArray));
        }
    }
    
    /**
     * Returns the specialized type of this and type.
     * We have two classes and multiple interfaces.  The result 
     * should be the object that is the the child of both objects
     * and the union of all interfaces.
     */
    public Type getSpecializedType(Type type) {

        if (type.getTypeCode() == TC_UNKNOWN)
            return this;
        if (type.getTypeCode() == TC_ARRAY && this == tObject)
            return type;
        if (type.getTypeCode() != TC_CLASS)
            return tError;

        ClassInterfacesType other = (ClassInterfacesType) type;
        Class clazz;
        Vector ifaces = new Vector();

        /* First determine the clazz, one of the two classes must be a sub
         * class of the other or null.
         */

        if (this.clazz == null)
            clazz = other.clazz;
        else if (other.clazz == null)
            clazz = this.clazz;
        else if (superClassOf(this.clazz, other.clazz))
            clazz = other.clazz;
        else if (superClassOf(other.clazz, this.clazz))
            clazz = this.clazz;
        else
            return tError;

        /* The interfaces are simply the union of both interfaces set. 
         * But we can simplify this, if an interface is implemented by
         * another or by the class, we can omit it.
         */
    big_loop_this:
        for (int i=0; i< this.ifaces.length; i++) {
            Class iface = this.ifaces[i];
            if (clazz != null && implementedBy(iface, clazz)) {
                continue big_loop_this;
            }
            for (int j=0; j<other.ifaces.length; j++) {
                if (implementedBy(iface, other.ifaces[j])) {
                    continue big_loop_this;
                }
            }

            /* This interface is not implemented by any of the other
             * ifaces.  Add it to the interfaces vector.
             */
            ifaces.addElement(iface);
        }
    big_loop_other:
        for (int i=0; i< other.ifaces.length; i++) {
            Class iface = other.ifaces[i];
            if (clazz != null && implementedBy(iface, clazz)) {
                continue big_loop_other;
            }
            for (int j=0; j<ifaces.size(); j++) {
                if (implementedBy(iface, (Class) ifaces.elementAt(j))) {
                    continue big_loop_other;
                }
            }

            /* This interface is not implemented by any of the other
             * ifaces.  Add it to the interfaces vector.
             */
            ifaces.addElement(iface);
        }
        if (clazz == cObject && ifaces.size() > 0)
            /* Every interface implies tObject, so remove it */
            clazz = null;
            
        Class[] ifaceArray = new Class[ifaces.size()];
        ifaces.copyInto(ifaceArray);
        return create(clazz, ifaceArray);
    }

    /**
     * Returns the generalized type of this and type.
     * We have two classes and multiple interfaces.  The result 
     * should be the object that is the the parent of both objects and
     * all interfaces, that one class or interface of this and of type
     * implements.
     */
    public Type getGeneralizedType(Type type) {

        Class clazz;
        Vector ifaces = new Vector();

        if (type.getTypeCode() == TC_UNKNOWN)
            return this;
        if (type.getTypeCode() == TC_ARRAY)
            return tObject;
        if (type.getTypeCode() != TC_CLASS)
            return tError;

        ClassInterfacesType other = (ClassInterfacesType) type;

        /* First the easy part, determine the clazz */
        if (this.clazz == null || other.clazz == null)
            clazz = null;
        else {
            clazz = this.clazz;
                
            while(clazz != null) {
                if (superClassOf(clazz, other.clazz))
                    break;
                clazz = clazz.getSuperclass();
            }
            if (clazz == cObject)
                clazz = null;
        }

        /* Now the more complicated part: find all interfaces, that are
         * implemented by one interface or class in each group.
         *
         * First get all interfaces of this.clazz and this.ifaces.
         */
            
        Stack allIfaces = new Stack();
        if (this.clazz != null) {
            Class c = this.clazz;
            while (clazz != c) {
                Class clazzIfaces[] = c.getInterfaces();
                for (int i=0; i<clazzIfaces.length; i++)
                    allIfaces.push(clazzIfaces[i]);
                c = c.getSuperclass();
            }
        }
        for (int i=0; i<this.ifaces.length; i++)
            allIfaces.push(this.ifaces[i]);
            
            /* Now consider each interface.  If any clazz or interface
             * in other implements it, add it to the ifaces vector.
             * Otherwise consider all sub interfaces.
             */
    iface_loop:
        while (!allIfaces.isEmpty()) {
            Class iface = (Class) allIfaces.pop();
            if ((clazz != null && implementedBy(iface, clazz))
                || ifaces.contains(iface))
                /* We can skip this, as clazz or ifaces already imply it.
                 */
                continue iface_loop;

            if (other.clazz != null && implementedBy(iface, other.clazz)) {
                ifaces.addElement(iface);
                continue iface_loop;
            }
            for (int i=0; i<other.ifaces.length; i++) {
                if (implementedBy(iface, other.ifaces[i])) {
                    ifaces.addElement(iface);
                    continue iface_loop;
                }
            }

            /* This interface is not implemented by any of the other
             * ifaces.  Try its parent interfaces now.
             */
            Class clazzIfaces[] = iface.getInterfaces();
            for (int i=0; i<clazzIfaces.length; i++)
                allIfaces.push(clazzIfaces[i]);
        }
                
        Class[] ifaceArray = new Class[ifaces.size()];
        ifaces.copyInto(ifaceArray);
        return create(clazz, ifaceArray);
    }

    /**
     * Marks this type as used, so that the class is imported.
     */
    public void useType() {
        if (!jode.Decompiler.isTypeDebugging) {
            if (clazz != null && clazz != cObject)
                env.useClass(clazz);
            else if (ifaces.length > 0)
                env.useClass(ifaces[0]);
        }
    }

    public String toString()
    {
        if (jode.Decompiler.isTypeDebugging) {
            if (this == tObject)
                return "<tObject>";
            StringBuffer sb = new StringBuffer("{");
            String comma = "";
            if (clazz != null) {
                sb = sb.append(clazz.getName());
                comma = ", ";
            }
            for (int i=0; i< ifaces.length; i++) {
                sb.append(comma).append(ifaces[i].getName());
                comma = ", ";
            }
            return sb.append("}").toString();
        } else {
            if (clazz != null && clazz != cObject)
                return env.classString(clazz);
            else if (ifaces.length > 0)
                return env.classString(ifaces[0]);
            else if (clazz == cObject)
                return env.classString(clazz);
            else
                return "{<error>}";
        }
    }

    public boolean isClassType() {
        return true;
    }

    public boolean equals(Object o) {
        if (o == this) 
            return true;
        if (o instanceof ClassInterfacesType) {
            ClassInterfacesType type = (ClassInterfacesType) o;
            if (type.clazz == clazz
                && type.ifaces.length == ifaces.length) {
                big_loop:
                for (int i=0; i< type.ifaces.length; i++) {
                    for (int j=0; j<ifaces.length; j++) {
                        if (type.ifaces[i] == ifaces[j])
                            continue big_loop;
                    }
                    return false;
                }
                return true;
            }
        }
        return false;
    }
}
