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
import sun.tools.java.*;
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
public class ClassInterfacesType extends MyType {

    Class clazz;
    Class ifaces[];

    public final static Class cObject = new Object().getClass();
    
    public ClassInterfacesType(Type clazzType) {
        super(104, "{"+clazzType.getTypeSignature()+"}");
	try {
            Class cdecl = 
                Class.forName(clazzType.getClassName().toString());
            if (cdecl.isInterface()) {
                clazz = cObject;
                ifaces = new Class[1];
                ifaces[0] = cdecl;
            } else {
                clazz = cdecl;
                ifaces = new Class[0];
            }
        } catch (ClassNotFoundException ex) {
            throw new AssertError(ex.toString());
        }
    }

    public ClassInterfacesType(Class clazz, 
                               Class[] ifaces) {
        super(104, "{}");
        StringBuffer sig = new StringBuffer("{");
        if (clazz != null) 
            sig.append("L").append(clazz.getName()).append(";");
        for (int i=0;i < ifaces.length; i++)
            sig.append("L").append(ifaces[i].getName()).append(";");
        sig.append("}");
        typeSig = sig.toString();
        this.clazz = clazz;
        this.ifaces = ifaces;
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
     * Checks if the given type range may be not empty.
     * This means, that bottom.clazz is extended by top.clazz
     * and that all interfaces in bottom are implemented by an
     * interface or by top.clazz. <p>
     * 
     * if that is true, a class range type is created, otherwise
     * tError is returned.
     */
    public final static Type createRangeType(ClassInterfacesType bottom,
                                             ClassInterfacesType top) {
        if (bottom.clazz != null
            && bottom.clazz != cObject) {
            /* The searched type must be a class type.
             */
            if (top.ifaces.length != 0
                || !superClassOf(bottom.clazz,top.clazz))
                return tError;
            
            /* All interfaces must be implemented by top.clazz
             */
            for (int i=0; i < bottom.ifaces.length; i++) {
                if (!implementedBy(bottom.ifaces[i], top.clazz))
                    return tError;
                }
            return new ClassRangeType
                (bottom, new ClassInterfacesType(top.clazz,
                                                 new Class[0]));
            
        } else {
            
            /* Now bottom.clazz is null, find all top.class/interfaces 
             * that implement all bottom.ifaces.
             */
            Class clazz = top.clazz;
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
            for (int j=0; j < top.ifaces.length; j++) {
                for (int i=0; i < bottom.ifaces.length; i++) {
                    if (!implementedBy(bottom.ifaces[i], top.ifaces[j]))
                        continue big_loop;
                }
                ifaces.addElement(top.ifaces[j]);
            }
            Class[] ifaceArray = 
                new Class[ifaces.size()];
                ifaces.copyInto(ifaceArray);
                return new ClassRangeType
                    (bottom, new ClassInterfacesType(clazz, ifaceArray));
        }
    }
    
    /**
     * Returns the specialized type of t1 and t2.
     * We have two classes and multiple interfaces.  The result 
     * should be the object that is the the child of both objects
     * and the union of all interfaces.
     */
    public static Type getSpecializedType(ClassInterfacesType t1, 
                                          ClassInterfacesType t2) {

        Class clazz = null;
        Vector ifaces = new Vector();


        /* First determine the clazz, one of the two classes must be a sub
         * class of the other or null.
         */

        if (t1.clazz == null)
            clazz = t2.clazz;
        else if (t2.clazz == null)
            clazz = t1.clazz;
        else if (superClassOf(t1.clazz, t2.clazz))
            clazz = t2.clazz;
        else if (superClassOf(t2.clazz, t1.clazz))
            clazz = t1.clazz;
        else
            return tError;

        /* The interfaces are simply the union of both interfaces set. 
         * But we can simplify this, if an interface is implemented by
         * another or by the class, we can omit it.
         */
    big_loop_t1:
        for (int i=0; i< t1.ifaces.length; i++) {
            Class iface = t1.ifaces[i];
            if (clazz != null && implementedBy(iface, clazz)) {
                continue big_loop_t1;
            }
            for (int j=0; j<t2.ifaces.length; j++) {
                if (implementedBy(iface, t2.ifaces[j])) {
                    continue big_loop_t1;
                }
            }

            /* This interface is not implemented by any of the other
             * ifaces.  Add it to the interfaces vector.
             */
            ifaces.addElement(iface);
        }
    big_loop_t2:
        for (int i=0; i< t2.ifaces.length; i++) {
            Class iface = t2.ifaces[i];
            if (clazz != null && implementedBy(iface, clazz)) {
                continue big_loop_t2;
            }
            for (int j=0; j<ifaces.size(); j++) {
                if (implementedBy(iface, (Class) ifaces.elementAt(j))) {
                    continue big_loop_t2;
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
        return new ClassInterfacesType(clazz, ifaceArray);
    }

    /**
     * Returns the generalized type of t1 and t2.
     * We have two classes and multiple interfaces.  The result 
     * should be the object that is the the parent of both objects and
     * all interfaces, that one class or interface of t1 and of t2
     * implements.
     */
    public static Type getGeneralizedType(ClassInterfacesType t1, 
                                          ClassInterfacesType t2) {

        Class clazz;
        Vector ifaces = new Vector();

        /* First the easy part, determine the clazz */
        if (t1.clazz == null)
            clazz = t2.clazz;
        else if (t2.clazz == null)
            clazz = t1.clazz;
        else {
            clazz = t1.clazz;
                
            while(clazz != null) {
                if (superClassOf(clazz, t2.clazz))
                    break;
                clazz = clazz.getSuperclass();
            }
            if (clazz == cObject)
                clazz = null;
        }

        /* Now the more complicated part: find all interfaces, that are
             * implemented by one interface or class in each group.
             *
             * First get all interfaces of t1.clazz and t1.ifaces.
             */
            
        Stack allIfaces = new Stack();
        if (t1.clazz != null) {
            Class c = t1.clazz;
            while (clazz != c) {
                Class clazzIfaces[] = c.getInterfaces();
                for (int i=0; i<clazzIfaces.length; i++)
                    allIfaces.push(clazzIfaces[i]);
                c = c.getSuperclass();
            }
        }
        for (int i=0; i<t1.ifaces.length; i++)
            allIfaces.push(t1.ifaces[i]);
            
            /* Now consider each interface.  If any clazz or interface
             * in t2 implements it, add it to the ifaces vector.
             * Otherwise consider all sub interfaces.
             */
    iface_loop:
        while (!allIfaces.isEmpty()) {
            Class iface = (Class) allIfaces.pop();
            if (clazz != null && implementedBy(iface, clazz))
                /* We can skip this, as clazz does already imply it.
                 */
                continue iface_loop;

            if (t2.clazz != null && implementedBy(iface, t2.clazz)) {
                ifaces.addElement(iface);
                continue iface_loop;
            }
            for (int i=0; i<t2.ifaces.length; i++) {
                if (implementedBy(iface, t2.ifaces[i])) {
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
        return new ClassInterfacesType(clazz, ifaceArray);
    }

    public String typeString(String string, boolean flag1, boolean flag2)
    {
        if (jode.Decompiler.isTypeDebugging) {
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
            return sb.append("}").append(string).toString();
        } else {
            if (clazz != null)
                return clazz.toString() + string;
            else if (ifaces.length > 0)
                return ifaces[0].toString() + string;
            else
                return tError.toString() + string;
        }
    }
}
