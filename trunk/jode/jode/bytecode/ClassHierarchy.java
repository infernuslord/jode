/* jode.bytecode.ClassHierarchy Copyright (C) 1997-1998 Jochen Hoenicke.
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
package jode.bytecode;
import jode.SearchPath;
import java.io.*;
import java.util.Hashtable;

/**
 * This class does represent a class similar to java.lang.Class.  You
 * can get the super class and the interfaces.
 *
 * The main difference to java.lang.Class is, that the objects are builded
 * from a stream containing the .class file, and that it uses the 
 * <code>jode.Type</code> to represent types instead of Class itself.
 *
 * @author Jochen Hoenicke
 */
public class ClassHierarchy {
    private String name;
    private int modifiers = -1;
    private ClassHierarchy superclass;
    private ClassHierarchy[] interfaces;
    private static SearchPath classpath;
    private ConstantPool constantPool;
    private static Hashtable classes = new Hashtable(); // XXX - weak map

    public final static ClassHierarchy javaLangObject = 
        ClassHierarchy.forName("java.lang.Object");
    
    public static void setClassPath(SearchPath path) {
        classpath = path;
    }

    
    public static ClassHierarchy forName(String name) {
        if (name == null)
            return null;
        name = name.replace('/', '.');
        ClassHierarchy clazz = (ClassHierarchy) classes.get(name);
        if (clazz == null) {
            clazz = new ClassHierarchy(name);
            classes.put(name, clazz);
        }
        return clazz;
    }

    public ClassHierarchy(String name) {
        this.name = name;
    }

    private void readHeader(DataInputStream input) 
	throws IOException {
	if (input.readInt() != 0xcafebabe)
	    throw new ClassFormatException("Wrong magic");
	if (input.readUnsignedShort() > 3) 
	    throw new ClassFormatException("Wrong minor");
	if (input.readUnsignedShort() != 45) 
	    throw new ClassFormatException("Wrong major");
    }

    private void readConstants(DataInputStream input)
	throws IOException {
	constantPool = new ConstantPool();
        constantPool.read(input);
    }

    private void readNameAndSuper(DataInputStream input)
	throws IOException {
	modifiers = input.readUnsignedShort();
        String name = constantPool.getClassName(input.readUnsignedShort());
        if (!this.name.equals(name))
            new ClassFormatException("Class has wrong name: "+name);
        superclass = ClassHierarchy.forName
            (constantPool.getClassName(input.readUnsignedShort()));
    }

    private void readInterfaces(DataInputStream input)
	throws IOException {
	int count = input.readUnsignedShort();
	interfaces = new ClassHierarchy[count];
	for (int i=0; i< count; i++) {
            interfaces[i] = ClassHierarchy.forName
                (constantPool.getClassName(input.readUnsignedShort()));
	}
    }

    private void loadHierarchy() {
        try {
            DataInputStream input = 
                new DataInputStream(classpath.getFile(name.replace('.', '/')
                                                      + ".class"));
            readHeader(input);
            readConstants(input);
            readNameAndSuper(input);
            readInterfaces(input);

            constantPool = null;  // Now allow clean up
        } catch (IOException ex) {
            String message = ex.getLocalizedMessage();
            System.err.println("Can't read class " + name 
                               + ", types may be incorrect. ("
                               + ex.getClass().getName()
                               + (message != null ? ": " + message : "")+")");
            
            if (name.equals("java.lang.Object"))
                superclass = null;
            else
                superclass = ClassHierarchy.forName("java.lang.Object");
            interfaces = new ClassHierarchy[0];
        }
    }

    public ClassHierarchy getSuperclass() {
        if (interfaces == null)
            loadHierarchy();
        return superclass;
    }
    
    public ClassHierarchy[] getInterfaces() {
        if (interfaces == null)
            loadHierarchy();
        return interfaces;
    }

    public int getModifiers() {
        return modifiers;
    }

    public boolean isInterface() {
        if (interfaces == null)
            loadHierarchy();
        return java.lang.reflect.Modifier.isInterface(modifiers);
    }

    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }

    public boolean superClassOf(ClassHierarchy son) {
        while (son != this && son != null) {
            son = son.getSuperclass();
        }
        return son == this;
    }

    public boolean implementedBy(ClassHierarchy clazz) {
        while (clazz != this && clazz != null) {
            ClassHierarchy[] ifaces = clazz.getInterfaces();
            for (int i=0; i< ifaces.length; i++) {
                if (implementedBy(ifaces[i]))
                    return true;
            }
            clazz = clazz.getSuperclass();
        }
        return clazz == this;
    }
}
