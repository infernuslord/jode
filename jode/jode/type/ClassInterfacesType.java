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

    ClassDeclaration clazz;
    ClassDeclaration ifaces[];
    
    public ClassInterfacesType(Type clazzType) {
        super(104, "{"+clazzType.getTypeSignature()+"}");
	ClassDeclaration cdecl = 
            new ClassDeclaration(clazzType.getClassName());
	try {
            if (cdecl.getClassDefinition(env).isInterface()) {
                clazz = new ClassDeclaration(Constants.idJavaLangObject);
                ifaces = new ClassDeclaration[1];
                ifaces[0] = cdecl;
            } else {
                clazz = cdecl;
                ifaces = new ClassDeclaration[0];
            }
        } catch (ClassNotFound ex) {
            throw new AssertError(ex.toString());
        }
    }

    public ClassInterfacesType(ClassDeclaration clazz, 
                               ClassDeclaration[] ifaces) {
        super(104, "{}");
        StringBuffer sig = new StringBuffer("{");
        if (clazz != null) 
            sig.append(clazz.getType().getTypeSignature());
        for (int i=0;i < ifaces.length; i++)
            sig.append(ifaces[i].getType().getTypeSignature());
        sig.append("}");
        typeSig = sig.toString();
        this.clazz = clazz;
        this.ifaces = ifaces;
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
        try {
            if (bottom.clazz != null
                && bottom.clazz.getType() != tObject) {
                /* The searched type must be a class type.
                 */
                if (top.ifaces.length != 0
                    || !bottom.clazz.getClassDefinition(env).
                        superClassOf(env,top.clazz))
                    return tError;

                /* All interfaces must be implemented by top.clazz
                 */
                for (int i=0; i < bottom.ifaces.length; i++) {
                    if (!bottom.ifaces[i].getClassDefinition(env).
                        implementedBy(env,top.clazz))
                        return tError;
                }
                return new ClassRangeType
                    (bottom, new ClassInterfacesType(top.clazz,
                                                     new ClassDeclaration[0]));

            } else {
            
                /* Now bottom.clazz is null, find all top.class/interfaces 
                 * that implement all bottom.ifaces.
                 */
                ClassDeclaration clazz = top.clazz;
                if (clazz != null) {
                    for (int i=0; i < bottom.ifaces.length; i++) {
                        ClassDefinition idef = 
                            bottom.ifaces[i].getClassDefinition(env);
                        if (!idef.implementedBy(env, clazz)) {
                            clazz = null;
                            break;
                        }
                    }
                }
                Vector ifaces = new Vector();
            big_loop:
                for (int j=0; j < top.ifaces.length; j++) {
                    for (int i=0; i < bottom.ifaces.length; i++) {
                        ClassDefinition idef = 
                            bottom.ifaces[i].getClassDefinition(env);
                        if (!idef.implementedBy(env, top.ifaces[j]))
                            continue big_loop;
                    }
                    ifaces.addElement(top.ifaces[j]);
                }
                ClassDeclaration[] ifaceArray = 
                    new ClassDeclaration[ifaces.size()];
                ifaces.copyInto(ifaceArray);
                return new ClassRangeType
                    (bottom, new ClassInterfacesType(clazz, ifaceArray));
            }
	} catch (ClassNotFound ex) {
            return tError;
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

        ClassDeclaration clazz = null;
        Vector ifaces = new Vector();


        /* First determine the clazz, one of the two classes must be a sub
         * class of the other or null.
         */
        try {
            if (t1.clazz == null)
                clazz = t2.clazz;
            else if (t2.clazz == null)
                clazz = t1.clazz;
            else if (t1.clazz.
                     getClassDefinition(env).superClassOf(env, t2.clazz))
                clazz = t2.clazz;
            else if (t2.clazz.
                     getClassDefinition(env).superClassOf(env, t1.clazz))
                clazz = t1.clazz;
            else
                return tError;

            /* The interfaces are simply the union of both interfaces set. 
             * But we can simplify this, if an interface is implemented by
             * another or by the class, we can omit it.
             */
        big_loop_t1:
            for (int i=0; i< t1.ifaces.length; i++) {
                ClassDeclaration iface = t1.ifaces[i];
                ClassDefinition idef = iface.getClassDefinition(env);
                if (clazz != null && idef.implementedBy(env, clazz)) {
                    continue big_loop_t1;
                }
                for (int j=0; j<t2.ifaces.length; j++) {
                    if (idef.implementedBy(env, t2.ifaces[j])) {
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
                ClassDeclaration iface = t2.ifaces[i];
                ClassDefinition idef = iface.getClassDefinition(env);
                if (clazz != null && idef.implementedBy(env, clazz)) {
                    continue big_loop_t2;
                }
                for (int j=0; j<ifaces.size(); j++) {
                    if (idef.implementedBy
                        (env, (ClassDeclaration) ifaces.elementAt(j))) {
                        continue big_loop_t2;
                    }
                }

                /* This interface is not implemented by any of the other
                 * ifaces.  Add it to the interfaces vector.
                 */
                ifaces.addElement(iface);
            }
            if (clazz.getType() == tObject && ifaces.size() > 0)
                /* Every interface implies tObject, so remove it */
                clazz = null;
            
        } catch (ClassNotFound ex) {
            return tError;
        }

        ClassDeclaration[] ifaceArray = new ClassDeclaration[ifaces.size()];
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

        ClassDeclaration clazz;
        Vector ifaces = new Vector();

        try {
            /* First the easy part, determine the clazz */
            if (t1.clazz == null)
                clazz = t2.clazz;
            else if (t2.clazz == null)
                clazz = t1.clazz;
            else {
                clazz = t1.clazz;
                
                while(clazz != null) {
                    ClassDefinition cdef = clazz.getClassDefinition(env);
                    if (cdef.superClassOf(env, t2.clazz))
                        break;
                    clazz = cdef.getSuperClass(env);
                }
                if (clazz.getType() == tObject)
                    clazz = null;
            }

            /* Now the more complicated part: find all interfaces, that are
             * implemented by one interface or class in each group.
             *
             * First get all interfaces of t1.clazz and t1.ifaces.
             */
            
            Stack allIfaces = new Stack();
            if (t1.clazz != null) {
                ClassDeclaration clazzIfaces[] = 
                    t1.clazz.getClassDefinition(env).getInterfaces();
                for (int i=0; i<clazzIfaces.length; i++)
                    allIfaces.push(clazzIfaces[i]);
            }
            for (int i=0; i<t1.ifaces.length; i++)
            allIfaces.push(t1.ifaces[i]);
            
            /* Now consider each interface.  If any clazz or interface
             * in t2 implements it, add it to the ifaces vector.
             * Otherwise consider all sub interfaces.
             */
        iface_loop:
            while (!allIfaces.isEmpty()) {
                ClassDeclaration iface = (ClassDeclaration) allIfaces.pop();
                ClassDefinition idef = iface.getClassDefinition(env);
                if (clazz != null && idef.implementedBy(env, clazz))
                    /* We can skip this, as clazz does already imply it.
                     */
                    continue iface_loop;

                if (t2.clazz != null && idef.implementedBy(env, t2.clazz)) {
                    ifaces.addElement(iface);
                    continue iface_loop;
                }
                for (int i=0; i<t2.ifaces.length; i++) {
                    if (idef.implementedBy(env, t2.ifaces[i])) {
                        ifaces.addElement(iface);
                        continue iface_loop;
                    }
                }

                /* This interface is not implemented by any of the other
                 * ifaces.  Try its parent interfaces now.
                 */
                ClassDeclaration clazzIfaces[] = idef.getInterfaces();
                for (int i=0; i<clazzIfaces.length; i++)
                    allIfaces.push(clazzIfaces[i]);
            }
                
        } catch (ClassNotFound ex) {
            return tError;
        }

        ClassDeclaration[] ifaceArray = new ClassDeclaration[ifaces.size()];
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
                return clazz.getType().typeString(string, flag1, flag2);
            else if (ifaces.length > 0)
                return ifaces[0].getType().typeString(string, flag1, flag2);
            else
                return tError.typeString(string,flag1,flag2);
        }
    }
}

