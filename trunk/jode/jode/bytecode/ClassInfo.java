/* jode.bytecode.ClassInfo Copyright (C) 1997-1998 Jochen Hoenicke.
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
import jode.MethodType;
import java.io.*;
import java.util.*;
import java.lang.reflect.Modifier;

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
public class ClassInfo extends BinaryInfo {
    private String name;

    private static SearchPath classpath;
    private static Hashtable classes = new Hashtable(); // XXX - weak map

    private int status = 0;

    private ConstantPool constantPool;
    private int modifiers = -1;
    private ClassInfo    superclass;
    private ClassInfo[]  interfaces;
    private FieldInfo[]  fields;
    private MethodInfo[] methods;

    public final static ClassInfo javaLangObject = forName("java.lang.Object");
    
    public static void setClassPath(String path) {
        classpath = new SearchPath(path);
	Enumeration enum = classes.elements();
	while (enum.hasMoreElements()) {
	    ClassInfo ci = (ClassInfo) enum.nextElement();
	    ci.status = 0;
	    ci.superclass = null;
	    ci.fields = null;
	    ci.interfaces = null;
	    ci.methods = null;
	    ci.attributes = null;
	}
    }

    public static boolean exists(String name) {
        return classpath.exists(name.replace('.', '/') + ".class");
    }
    
    public static boolean isPackage(String name) {
        return classpath.isDirectory(name.replace('.', '/'));
    }
    
    public static Enumeration getClasses(final String packageName) {
        final Enumeration enum = 
            classpath.listClassFiles(packageName.replace('.','/'));
        return new Enumeration() {
            public boolean hasMoreElements() {
                return enum.hasMoreElements();
            }
            public Object nextElement() {
                String name = (String) enum.nextElement();
                if (!name.endsWith(".class"))
                    throw new jode.AssertError("Wrong file name");
                return ClassInfo.forName(packageName + "."
					 + name.substring(0, name.length()-6));

            }
        };
    }
    
    public static ClassInfo forName(String name) {
        if (name == null)
            return null;
        name = name.replace('/', '.');
        ClassInfo clazz = (ClassInfo) classes.get(name);
        if (clazz == null) {
            clazz = new ClassInfo(name);
            classes.put(name, clazz);
        }
        return clazz;
    }

    public ClassInfo(String name) {
        this.name = name;
    }

    private void readHeader(DataInputStream input, int howMuch) 
	throws IOException {
	if (input.readInt() != 0xcafebabe)
	    throw new ClassFormatException("Wrong magic");
	if (input.readUnsignedShort() > 3) 
	    throw new ClassFormatException("Wrong minor");
	if (input.readUnsignedShort() != 45) 
	    throw new ClassFormatException("Wrong major");
    }

    private ConstantPool readConstants(DataInputStream input, int howMuch)
	throws IOException {
        ConstantPool cpool = new ConstantPool();
        cpool.read(input);
        return cpool;
    }

    private void readNameAndSuper(ConstantPool cpool, 
                                  DataInputStream input, int howMuch)
	throws IOException {
	modifiers = input.readUnsignedShort();
        String name = cpool.getClassName(input.readUnsignedShort());
        if (!this.name.equals(name))
            new ClassFormatException("Class has wrong name: "+name);
        superclass = ClassInfo.forName
            (cpool.getClassName(input.readUnsignedShort()));
    }

    private void readInterfaces(ConstantPool cpool,
                                DataInputStream input, int howMuch)
	throws IOException {
	int count = input.readUnsignedShort();
	interfaces = new ClassInfo[count];
	for (int i=0; i< count; i++) {
            interfaces[i] = ClassInfo.forName
                (cpool.getClassName(input.readUnsignedShort()));
	}
    }

    private void readFields(ConstantPool cpool,
                            DataInputStream input, int howMuch)
	throws IOException {
        if ((howMuch & FIELDS) != 0) {
            int count = input.readUnsignedShort();
            fields = new FieldInfo[count];
            for (int i=0; i< count; i++) {
                fields[i] = new FieldInfo(); 
                fields[i].read(cpool, input, howMuch);
            }
        } else {
            int count = input.readUnsignedShort();
            for (int i=0; i< count; i++) {
                input.readUnsignedShort();  // modifier
                input.readUnsignedShort();  // name
                input.readUnsignedShort();  // type
                skipAttributes(input);
            }
        }
    }

    private void readMethods(ConstantPool cpool, 
                             DataInputStream input, int howMuch)
	throws IOException {
        if ((howMuch & METHODS) != 0) {
            int count = input.readUnsignedShort();
            methods = new MethodInfo[count];
            for (int i=0; i< count; i++) {
                methods[i] = new MethodInfo(); 
                methods[i].read(cpool, input, howMuch);
            }
        } else {
            int count = input.readUnsignedShort();
            for (int i=0; i< count; i++) {
                input.readUnsignedShort();  // modifier
                input.readUnsignedShort();  // name
                input.readUnsignedShort();  // type
                skipAttributes(input);
            }
        }
    }

    public void loadInfoReflection(int howMuch) {

	try {
	    Class clazz = Class.forName(name);
	    modifiers = clazz.getModifiers();
	    if ((howMuch & HIERARCHY) != 0) {
		if (clazz.getSuperclass() == null)
		    superclass = null;
		else
		    superclass = ClassInfo.forName
			(clazz.getSuperclass().getName());
		Class[] ifaces = clazz.getInterfaces();
		interfaces = new ClassInfo[ifaces.length];
		for (int i=0; i<ifaces.length; i++)
		    interfaces[i] = ClassInfo.forName(ifaces[i].getName());
		status |= HIERARCHY;
	    }
	    if ((howMuch & ~HIERARCHY) != 0) {
                jode.Decompiler.err.println
		    ("Can't find class " + name
		     + " in classpath.  Bad things may or may not happen.");
		status |= howMuch;
	    } 
	    
	} catch (ClassNotFoundException ex) {
	    // Nothing helped, ``guess'' the hierarchie
            String message = ex.getMessage();
            if ((howMuch & ~(METHODS|HIERARCHY)) == 0) {
                jode.Decompiler.err.println
		    ("Can't read class " + name + ", types may be incorrect. ("
		     + ex.getClass().getName()
		     + (message != null ? ": " + message : "") + ")");
            } else
                jode.Decompiler.err.println
		    ("Can't read class " + name
		     + "(" + ex.getClass().getName()
		     + (message != null ? ": " + message : "") + ")");
            
            if (name.equals("java.lang.Object"))
                superclass = null;
            else
                superclass = ClassInfo.forName("java.lang.Object");
            interfaces = new ClassInfo[0];
	    modifiers = Modifier.PUBLIC;
            status = FULLINFO;
	}
    }

    public void loadInfo(int howMuch) {
        try {
            DataInputStream input = 
                new DataInputStream(classpath.getFile(name.replace('.', '/')
                                                      + ".class"));
            readHeader(input, howMuch);

            ConstantPool cpool = readConstants(input, howMuch);
            if ((howMuch & CONSTANTS) != 0)
                this.constantPool = cpool;
            readNameAndSuper(cpool, input, howMuch);
            readInterfaces(cpool, input, howMuch);
            if ((howMuch & HIERARCHY) != 0)
                status |= HIERARCHY;
            readFields(cpool, input, howMuch);
            readMethods(cpool, input, howMuch);
            readAttributes(cpool, input, howMuch);
            
            status |= howMuch;

        } catch (IOException ex) {
	    // Try getting the info through the reflection interface
	    // instead.

	    loadInfoReflection(howMuch);
        }
    }

    public ClassInfo getSuperclass() {
        if ((status & HIERARCHY) == 0)
            loadInfo(HIERARCHY);
        return superclass;
    }
    
    public ClassInfo[] getInterfaces() {
        if ((status & HIERARCHY) == 0)
            loadInfo(HIERARCHY);
        return interfaces;
    }

    public int getModifiers() {
        if ((status & HIERARCHY) == 0)
            loadInfo(HIERARCHY);
        return modifiers;
    }

    public boolean isInterface() {
        return Modifier.isInterface(getModifiers());
    }

    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }

    public boolean superClassOf(ClassInfo son) {
        while (son != this && son != null) {
            son = son.getSuperclass();
        }
        return son == this;
    }

    public boolean implementedBy(ClassInfo clazz) {
        while (clazz != this && clazz != null) {
            ClassInfo[] ifaces = clazz.getInterfaces();
            for (int i=0; i< ifaces.length; i++) {
                if (implementedBy(ifaces[i]))
                    return true;
            }
            clazz = clazz.getSuperclass();
        }
        return clazz == this;
    }

    public ConstantPool getConstantPool() {
        if ((status & CONSTANTS) == 0)
            loadInfo(CONSTANTS);
        return constantPool;
    }

    public MethodInfo findMethod(String name, MethodType type) {
        if ((status & METHODS) == 0)
            loadInfo(METHODS);
        for (int i=0; i< methods.length; i++)
            if (methods[i].getName().equals(name)
                && methods[i].getType().equals(type))
                return methods[i];
        return null;
    }

    public MethodInfo[] getMethods() {
        if ((status & METHODS) == 0)
            loadInfo(METHODS);
        return methods;
    }

    public FieldInfo[] getFields() {
        if ((status & FIELDS) == 0)
            loadInfo(FIELDS);
        return fields;
    }
}
