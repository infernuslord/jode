/* ClassInfo Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.GlobalOptions;
import jode.type.Type;
import java.io.*;
import java.util.*;
///#ifdef JDK12
///import java.lang.ref.WeakReference;
///import java.lang.ref.ReferenceQueue;
///#endif
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * This class does represent a class similar to java.lang.Class.  You
 * can get the super class and the interfaces.
 *
 * The main difference to java.lang.Class is, that the objects are builded
 * from a stream containing the .class file, and that it uses the 
 * <code>Type</code> to represent types instead of Class itself.
 *
 * @author Jochen Hoenicke
 */
public class ClassInfo extends BinaryInfo {

    private static SearchPath classpath;
///#ifdef JDK12
///    private static final Map classes = new HashMap();
///    private static final ReferenceQueue queue = new ReferenceQueue();
///#else
    private static final Hashtable classes = new Hashtable();
///#endif

    private int status = 0;

    private int modifiers = -1;
    private String name;
    private ClassInfo    superclass;
    private ClassInfo[]  interfaces;
    private FieldInfo[]  fields;
    private MethodInfo[] methods;
    private InnerClassInfo[] outerClasses;
    private InnerClassInfo[] innerClasses;
    private InnerClassInfo[] extraClasses;
    private String sourceFile;

    public final static ClassInfo javaLangObject = forName("java.lang.Object");
    
    public static void setClassPath(String path) {
        classpath = new SearchPath(path);
///#ifdef JDK12
///	java.lang.ref.Reference died;
///	while ((died = queue.poll()) != null) {
///	    classes.values().remove(died);
///	}
///	Iterator i = classes.values().iterator();
///	while (i.hasNext()) {
///	    ClassInfo ci = (ClassInfo) ((WeakReference)i.next()).get();
///	    if (ci == null) {
///		i.remove();
///		continue;
///	    }
///#else
	Enumeration enum = classes.elements();
	while (enum.hasMoreElements()) {
	    ClassInfo ci = (ClassInfo) enum.nextElement();
///#endif
	    ci.status = 0;
	    ci.superclass = null;
	    ci.fields = null;
	    ci.interfaces = null;
	    ci.methods = null;
	    ci.unknownAttributes = null;
	}
    }

    public static boolean exists(String name) {
        return classpath.exists(name.replace('.', '/') + ".class");
    }
    
    public static boolean isPackage(String name) {
        return classpath.isDirectory(name.replace('.', '/'));
    }
    
    public static Enumeration getClassesAndPackages(final String packageName) {
        final Enumeration enum = 
            classpath.listFiles(packageName.replace('.','/'));
        return new Enumeration() {
            public boolean hasMoreElements() {
                return enum.hasMoreElements();
            }
            public Object nextElement() {
                String name = (String) enum.nextElement();
                if (!name.endsWith(".class"))
		    // This is a package
		    return name;
                return name.substring(0, name.length()-6);

            }
        };
    }
    
    public static ClassInfo forName(String name) {
//          name = name.replace('/', '.');
	if (name == null
	    || name.indexOf(';') != -1
	    || name.indexOf('[') != -1
	    || name.indexOf('/') != -1)
	    throw new IllegalArgumentException("Illegal class name: "+name);
///#ifdef JDK12
///	java.lang.ref.Reference died;
///	while ((died = queue.poll()) != null) {
///	    classes.values().remove(died);
///	}
///	WeakReference ref = (WeakReference) classes.get(name);
///	ClassInfo clazz = (ref == null) ? null : (ClassInfo) ref.get();
///#else
	ClassInfo clazz = (ClassInfo) classes.get(name);
///#endif
        if (clazz == null) {
            clazz = new ClassInfo(name);
///#ifdef JDK12
///            classes.put(name, new WeakReference(clazz, queue));
///#else
            classes.put(name, clazz);
///#endif
        }
        return clazz;
    }

    private ClassInfo(String name) {
        this.name = name;
    }

    protected void readAttribute(String name, int length,
				 ConstantPool cp,
				 DataInputStream input, 
				 int howMuch) throws IOException {
	if ((howMuch & ALL_ATTRIBUTES) != 0 && name.equals("SourceFile")) {
	    if (length != 2)
		throw new ClassFormatException("SourceFile attribute"
					       + " has wrong length");
	    sourceFile = cp.getUTF8(input.readUnsignedShort());
	} else if ((howMuch & (OUTERCLASSES | INNERCLASSES)) != 0
		   && name.equals("InnerClasses")) {
	    int count = input.readUnsignedShort();
	    int innerCount = 0, outerCount = 0, extraCount = 0;
	    InnerClassInfo[] innerClassInfo = new InnerClassInfo[count];
	    for (int i=0; i< count; i++) {
		int innerIndex = input.readUnsignedShort();
		int outerIndex = input.readUnsignedShort();
		int nameIndex = input.readUnsignedShort();
		String inner = cp.getClassName(innerIndex);
		String outer = 
		    outerIndex != 0 ? cp.getClassName(outerIndex) : null;
		String innername = 
		    nameIndex != 0 ? cp.getUTF8(nameIndex) : null;
		int access = input.readUnsignedShort();
		InnerClassInfo ici = new InnerClassInfo 
		    (inner, outer, innername, access);

		if (outer != null && outer.equals(getName()))
		    innerClassInfo[innerCount++] = ici;
		else
		    innerClassInfo[count - (++extraCount)] = ici;
	    }
	    {
		String lastOuterName = getName();
		for (int i = count - extraCount; i < count; i++) {
		    InnerClassInfo ici = innerClassInfo[i];
		    if (ici.inner.equals(lastOuterName)) {
			for (int j = i; j > count - extraCount; j--)
			    innerClassInfo[j] = innerClassInfo[j-1];
			innerClassInfo[count-extraCount] = ici;
			extraCount--;
			outerCount++;
			lastOuterName = ici.outer;
		    }
		}
	    }
	    if (innerCount > 0) {
		innerClasses = new InnerClassInfo[innerCount];
		System.arraycopy(innerClassInfo, 0, 
				 innerClasses, 0, innerCount);
	    } else
		innerClasses = null;

	    if (outerCount > 0) {
		outerClasses = new InnerClassInfo[outerCount];
		System.arraycopy(innerClassInfo, innerCount, 
				 outerClasses, 0, outerCount);
	    } else 
		outerClasses = null;

	    if (extraCount > 0) {
		extraClasses = new InnerClassInfo[extraCount];
		System.arraycopy(innerClassInfo, innerCount + outerCount, 
				 extraClasses, 0, extraCount);
	    } else
		extraClasses = null;

	    if (length != 2 + 8 * count)
		throw new ClassFormatException
		    ("InnerClasses attribute has wrong length");
	} else
	    super.readAttribute(name, length, cp, input, howMuch);
    }

    public void read(DataInputStream input, int howMuch) throws IOException {
	/* header */
	if (input.readInt() != 0xcafebabe)
	    throw new ClassFormatException("Wrong magic");
	if (input.readUnsignedShort() > 3) 
	    throw new ClassFormatException("Wrong minor");
	if (input.readUnsignedShort() != 45) 
	    throw new ClassFormatException("Wrong major");

	/* constant pool */
        ConstantPool cpool = new ConstantPool();
        cpool.read(input);

	/* always read modifiers, name, super, ifaces */
	{
	    status |= HIERARCHY;
	    modifiers = input.readUnsignedShort();
	    String className = cpool.getClassName(input.readUnsignedShort());
	    if (!name.equals(className))
		throw new ClassFormatException("wrong name " + className);
	    String superName = cpool.getClassName(input.readUnsignedShort());
	    superclass = superName != null ? ClassInfo.forName(superName) : null;
	    int count = input.readUnsignedShort();
	    interfaces = new ClassInfo[count];
	    for (int i=0; i< count; i++) {
		interfaces[i] = ClassInfo.forName
		    (cpool.getClassName(input.readUnsignedShort()));
	    }
	}	    

	/* fields */
        if ((howMuch & FIELDS) != 0) {
            int count = input.readUnsignedShort();
            fields = new FieldInfo[count];
            for (int i=0; i< count; i++) {
                fields[i] = new FieldInfo(this); 
                fields[i].read(cpool, input, howMuch);
            }
        } else {
	    byte[] skipBuf = new byte[6];
            int count = input.readUnsignedShort();
            for (int i=0; i< count; i++) {
		input.readFully(skipBuf); // modifier, name, type
                skipAttributes(input);
            }
        }

	/* methods */
        if ((howMuch & METHODS) != 0) {
            int count = input.readUnsignedShort();
            methods = new MethodInfo[count];
            for (int i=0; i< count; i++) {
                methods[i] = new MethodInfo(this); 
                methods[i].read(cpool, input, howMuch);
            }
        } else {
	    byte[] skipBuf = new byte[6];
            int count = input.readUnsignedShort();
            for (int i=0; i< count; i++) {
		input.readFully(skipBuf); // modifier, name, type
                skipAttributes(input);
            }
        }

	/* attributes */
	readAttributes(cpool, input, howMuch);
    }

    public void reserveSmallConstants(GrowableConstantPool gcp) {
	for (int i=0; i < fields.length; i++)
	    fields[i].reserveSmallConstants(gcp);

	for (int i=0; i < methods.length; i++)
	    methods[i].reserveSmallConstants(gcp);
    }

    public void prepareWriting(GrowableConstantPool gcp) {
	gcp.putClassName(name);
	gcp.putClassName(superclass.getName());
	for (int i=0; i < interfaces.length; i++)
	    gcp.putClassName(interfaces[i].getName());

	for (int i=0; i < fields.length; i++)
	    fields[i].prepareWriting(gcp);

	for (int i=0; i < methods.length; i++)
	    methods[i].prepareWriting(gcp);

	if (sourceFile != null) {
	    gcp.putUTF8("SourceFile");
	    gcp.putUTF8(sourceFile);
	}
	if (outerClasses != null || innerClasses != null
	    || extraClasses != null) {
	    gcp.putUTF8("InnerClasses");
	    int outerCount = outerClasses != null ? outerClasses.length : 0;
	    for (int i=outerCount; i-- > 0;) {
		gcp.putClassName(outerClasses[i].inner);
		if (outerClasses[i].outer != null)
		    gcp.putClassName(outerClasses[i].outer);
		if (outerClasses[i].name != null)
		    gcp.putUTF8(outerClasses[i].name);
	    }
	    int innerCount = innerClasses != null ? innerClasses.length : 0;
	    for (int i=0; i< innerCount; i++) {
		gcp.putClassName(innerClasses[i].inner);
		if (innerClasses[i].outer != null)
		    gcp.putClassName(innerClasses[i].outer);
		if (innerClasses[i].name != null)
		    gcp.putUTF8(innerClasses[i].name);
	    }
	    int extraCount = extraClasses != null ? extraClasses.length : 0;
	    for (int i=0; i< extraCount; i++) {
		gcp.putClassName(extraClasses[i].inner);
		if (extraClasses[i].outer != null)
		    gcp.putClassName(extraClasses[i].outer);
		if (extraClasses[i].name != null)
		    gcp.putUTF8(extraClasses[i].name);
	    }
	}
        prepareAttributes(gcp);
    }

    protected int getKnownAttributeCount() {
	int count = 0;
	if (sourceFile != null)
	    count++;
	if (innerClasses != null || outerClasses != null
	    || extraClasses != null)
	    count++;
	return count;
    }

    public void writeKnownAttributes(GrowableConstantPool gcp,
				     DataOutputStream output) 
	throws IOException {
	if (sourceFile != null) {
	    output.writeShort(gcp.putUTF8("SourceFile"));
	    output.writeInt(2);
	    output.writeShort(gcp.putUTF8(sourceFile));
	}
	if (outerClasses != null || innerClasses != null
	    || extraClasses != null) {
	    output.writeShort(gcp.putUTF8("InnerClasses"));
	    int outerCount = (outerClasses != null) ? outerClasses.length : 0;
	    int innerCount = (innerClasses != null) ? innerClasses.length : 0;
	    int extraCount = (extraClasses != null) ? extraClasses.length : 0;
	    int count = outerCount + innerCount + extraCount;
	    output.writeInt(2 + count * 8);
	    output.writeShort(count);
	    for (int i=outerCount; i-- > 0; ) {
		output.writeShort(gcp.putClassName(outerClasses[i].inner));
		output.writeShort(outerClasses[i].outer != null ? 
				  gcp.putClassName(outerClasses[i].outer) : 0);
		output.writeShort(outerClasses[i].name != null ?
				  gcp.putUTF8(outerClasses[i].name) : 0);
		output.writeShort(outerClasses[i].modifiers);
	    }
	    for (int i=0; i< innerCount; i++) {
		output.writeShort(gcp.putClassName(innerClasses[i].inner));
		output.writeShort(innerClasses[i].outer != null ? 
				  gcp.putClassName(innerClasses[i].outer) : 0);
		output.writeShort(innerClasses[i].name != null ?
				  gcp.putUTF8(innerClasses[i].name) : 0);
		output.writeShort(innerClasses[i].modifiers);
	    }
	    for (int i=0; i< extraCount; i++) {
		output.writeShort(gcp.putClassName(extraClasses[i].inner));
		output.writeShort(extraClasses[i].outer != null ? 
				  gcp.putClassName(extraClasses[i].outer) : 0);
		output.writeShort(extraClasses[i].name != null ?
				  gcp.putUTF8(extraClasses[i].name) : 0);
		output.writeShort(extraClasses[i].modifiers);
	    }
	}
    }

    public void write(DataOutputStream out) throws IOException {
	GrowableConstantPool gcp = new GrowableConstantPool();
	reserveSmallConstants(gcp);
	prepareWriting(gcp);

	out.writeInt(0xcafebabe);
	out.writeShort(3);
	out.writeShort(45);
	gcp.write(out);

	out.writeShort(modifiers);
	out.writeShort(gcp.putClassName(name));
	out.writeShort(gcp.putClassName(superclass.getName()));
	out.writeShort(interfaces.length);
	for (int i=0; i < interfaces.length; i++)
	    out.writeShort(gcp.putClassName(interfaces[i].getName()));

	out.writeShort(fields.length);
	for (int i=0; i < fields.length; i++)
	    fields[i].write(gcp, out);

	out.writeShort(methods.length);
	for (int i=0; i < methods.length; i++)
	    methods[i].write(gcp, out);

        writeAttributes(gcp, out);
    }

    public void loadInfoReflection(Class clazz, int howMuch) 
	throws SecurityException {
	if ((howMuch & HIERARCHY) != 0) {
	    modifiers = clazz.getModifiers();
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
	if ((howMuch & FIELDS) != 0 && fields == null) {
	    Field[] fs;
	    try {
		fs = clazz.getDeclaredFields();
	    } catch (SecurityException ex) {
		fs = clazz.getFields();
		GlobalOptions.err.println
		    ("Could only get public fields of class "
		     + name + ".");
	    }
	    fields = new FieldInfo[fs.length];
	    for (int i = fs.length; --i >= 0; ) {
		String type = Type.getSignature(fs[i].getType());
		fields[i] = new FieldInfo
		    (this, fs[i].getName(), type, fs[i].getModifiers());
	    }
	}
	if ((howMuch & METHODS) != 0 && methods == null) {
	    Method[] ms;
	    try {
		ms = clazz.getDeclaredMethods();
	    } catch (SecurityException ex) {
		ms = clazz.getMethods();
		GlobalOptions.err.println
		    ("Could only get public methods of class "
		     + name + ".");
	    }
	    methods = new MethodInfo[ms.length];
	    for (int i = ms.length; --i >= 0; ) {
		String type = Type.getSignature
		    (ms[i].getParameterTypes(), ms[i].getReturnType());
		methods[i] = new MethodInfo
		    (this, ms[i].getName(), type, ms[i].getModifiers());
	    }
	}
	if ((howMuch & INNERCLASSES) != 0 && innerClasses == null) {
	    Class[] is;
	    try {
		is = clazz.getDeclaredClasses();
	    } catch (SecurityException ex) {
		is = clazz.getClasses();
		GlobalOptions.err.println
		    ("Could only get public methods of class "
		     + name + ".");
	    }
	    if (is.length > 0) {
		innerClasses = new InnerClassInfo[is.length];
		for (int i = is.length; --i >= 0; ) {
		    String inner = is[i].getName();
		    int dollar = inner.lastIndexOf('$');
		    String name = inner.substring(dollar+1);
		    innerClasses[i] = new InnerClassInfo
			(inner, getName(), name, is[i].getModifiers());
		}
	    }
	}
	if ((howMuch & INNERCLASSES) != 0 && outerClasses == null) {
	    int count = 0;
	    Class declarer = clazz.getDeclaringClass();
	    while (declarer != null) {
		count++;
		declarer = declarer.getDeclaringClass();
	    }
	    if (count > 0) {
		outerClasses = new InnerClassInfo[count];
		Class current = clazz;
		for (int i = 0; i < count; i++) {
		    declarer = current.getDeclaringClass();
		    String name = current.getName();
		    int dollar = name.lastIndexOf('$');
		    outerClasses[i] = new InnerClassInfo
			(name, declarer.getName(), 
			 name.substring(dollar+1), current.getModifiers());
		    current = declarer;
		}
	    }
	}
	status |= howMuch;
    }
    
    public void loadInfo(int howMuch) {
        try {
            DataInputStream input = 
                new DataInputStream(classpath.getFile(name.replace('.', '/')
                                                      + ".class"));
	    read(input, howMuch);            
            status |= howMuch;

        } catch (IOException ex) {
	    String message = ex.getMessage();
            if ((howMuch & ~(FIELDS|METHODS|HIERARCHY
			     |INNERCLASSES|OUTERCLASSES)) != 0) {
		GlobalOptions.err.println
		    ("Can't read class " + name + ".");
		ex.printStackTrace(GlobalOptions.err);
		throw new NoClassDefFoundError(name);
	    }
	    // Try getting the info through the reflection interface
	    // instead.
	    Class clazz = null;
	    try {
		clazz = Class.forName(name);
	    } catch (ClassNotFoundException ex2) {
	    } catch (NoClassDefFoundError ex2) {
	    }
	    try {
		if (clazz != null)
		    loadInfoReflection(clazz, howMuch);
		return;
	    } catch (SecurityException ex2) {
		GlobalOptions.err.println
		    (ex2+" while collecting info about class " + name + ".");
	    }
	    
	    // Give a warning and ``guess'' the hierarchie, methods etc.
	    GlobalOptions.err.println
		("Can't read class " + name + ", types may be incorrect. ("
		 + ex.getClass().getName()
		 + (message != null ? ": " + message : "") + ")");
	    
	    if ((howMuch & HIERARCHY) != 0) {
		modifiers = Modifier.PUBLIC;
		if (name.equals("java.lang.Object"))
		    superclass = null;
		else
		    superclass = ClassInfo.forName("java.lang.Object");
		interfaces = new ClassInfo[0];
	    }
	    if ((howMuch & METHODS) != 0)
		methods = new MethodInfo[0];
	    if ((howMuch & FIELDS) != 0)
		fields = new FieldInfo[0];
	    status |= howMuch;
        }
    }

    public String getName() {
        return name;
    }

    public String getJavaName() {
	/* Don't load attributes for class names not containing a
	 * dollar sign.
	 */
	if (name.indexOf('$') == -1)
	    return getName();
	if (getOuterClasses() != null) {
	    int last = outerClasses.length-1;
	    StringBuffer sb = 
		new StringBuffer(outerClasses[last].outer != null 
				 ? outerClasses[last].outer : "METHOD");
	    for (int i=last; i >= 0; i--)
		sb.append(".").append(outerClasses[i].name != null 
				      ? outerClasses[i].name : "ANONYMOUS");
	    return sb.toString();
	}
	return getName();
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

    public FieldInfo findField(String name, String typeSig) {
        if ((status & FIELDS) == 0)
            loadInfo(FIELDS);
        for (int i=0; i< methods.length; i++)
            if (fields[i].getName().equals(name)
                && fields[i].getType().equals(typeSig))
                return fields[i];
        return null;
    }

    public MethodInfo findMethod(String name, String typeSig) {
        if ((status & METHODS) == 0)
            loadInfo(METHODS);
        for (int i=0; i< methods.length; i++)
            if (methods[i].getName().equals(name)
                && methods[i].getType().equals(typeSig))
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

    public InnerClassInfo[] getOuterClasses() {
        if ((status & OUTERCLASSES) == 0)
            loadInfo(OUTERCLASSES);
        return outerClasses;
    }

    public InnerClassInfo[] getInnerClasses() {
        if ((status & INNERCLASSES) == 0)
            loadInfo(INNERCLASSES);
        return innerClasses;
    }

    public InnerClassInfo[] getExtraClasses() {
        if ((status & INNERCLASSES) == 0)
            loadInfo(INNERCLASSES);
        return extraClasses;
    }

    public void setName(String newName) {
	name = newName;
    }

    public void setSuperclass(ClassInfo newSuper) {
	superclass = newSuper;
    }
    
    public void setInterfaces(ClassInfo[] newIfaces) {
        interfaces = newIfaces;
    }

    public void setModifiers(int newModifiers) {
        modifiers = newModifiers;
    }

    public void setMethods(MethodInfo[] mi) {
        methods = mi;
    }

    public void setFields(FieldInfo[] fi) {
        fields = fi;
    }

    public void setOuterClasses(InnerClassInfo[] oc) {
        outerClasses = oc;
    }

    public void setInnerClasses(InnerClassInfo[] ic) {
        innerClasses = ic;
    }

    public void setExtraClasses(InnerClassInfo[] ec) {
        extraClasses = ec;
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

    public String toString() {
        return name;
    }
}
