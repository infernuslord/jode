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
import jode.util.UnifyHash;

import java.io.DataInputStream;
import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Enumeration;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

///#def COLLECTIONS java.util
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
///#enddef
///#def COLLECTIONEXTRA java.lang
import java.lang.Comparable;
///#enddef

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**   
 * Accesses, creates or modifies java bytecode classes or interfaces.
 * This class represents a class or interface, it can't be used for
 * primitive or array types.  Every class/interface is associated with
 * a class path, which is used to load the class in memory.
 *
 * <h3>Creating a class</h3>
 * You create a new ClassInfo, by calling {@link
 * ClassPath#getClassInfo}.  The resulting ClassInfo is empty and you
 * now have two different possibilities to fill it with informations:
 * You load the class from its classpath (from which it was created)
 * or you build it from scratch by setting its contents with the 
 * various <code>setXXX</code> methods.
 *
 * <h3>Changing a class</h3> 
 * Even if the class is already filled with information you can change
 * it.  You can, for example, set another array of methods, change the
 * modifiers, or rename the class.  Use the various
 * <code>setXXX</code> methods.
 *
 * <h3>The components of a class</h3>
 * A class consists of several components:
 * <dl>
 * <dt>name</dt><dd>
 *   The name of the class.  The name is already set, when you create
 *   a new ClassInfo with getClassInfo.  If you change this name this
 *   has some consequences, read the description of the {@link
 *   #setName} method.
 * </dd>
 * <dt>class name</dt><dd>
 *   The short java name of this class, i.e. the name that appears
 *   behind the "class" keyword in the java source file.  While 
 *   <code>getClassName()</code> also works for top level classes,
 *   setClassName() must only be called on inner classes and will not
 *   change the bytecode name.<br>
 *
 *   E.g.: The ClassName of <code>java.util.Map$Entry</code> is
 *   <code>Entry</code>.  If you change its ClassName to
 *   <code>Yrtne</code> and save it, it will still be in a file called
 *   <code>Map$Entry.class</code>, but a debugger would call it
 *   <code>java.util.Map.Yrtne</code>.  Note that you should also save
 *   <code>Map</code>, because it also has a reference to the
 *   ClassName.
 * </dd>
 * <dt>modifiers</dt><dd>
 *   There is a set of access modifiers (AKA access flags) attached to
 *   each class.  They are represented as integers (bitboard) and can
 *   be conveniently accessed via
 *   <code>java.lang.reflect.Modifier</code>. <br> <br>
 *
 *   Inner classes can have more modifiers than normal classes.  To be
 *   backwards compatible this was implemented by Sun by having the real 
 *   modifiers for inner classes at a special location, while the old
 *   location has only the modifiers that were allowed previously.
 *   This package knows about this and always returns the real modifiers.  
 *   The old modifiers are checked if they match the new extended ones.<br>
 *   <b>TODO:</b> Check that reflection returns the new modifiers!
 * </dd>
 * <dt>superclass</dt><dd>
 *   Every class except java.lang.Object has a super class.  The super class
 *   is created in the same classpath as the current class.  Interfaces
 *   always have <code>java.lang.Object</code> as their super class.
 * </dd>
 * <dt>interfaces</dt><dd>
 *   Every class (resp. interfaces) can implement (resp. extend) 
 *   zero or more interfaces.
 * </dd>
 * <dt>fields</dt><dd>
 *   Fields are represented as {@link FieldInfo} objects.
 * </dd>
 * <dt>methods</dt><dd>
 *   Fields are represented as {@link MethodInfo} objects.
 * </dd>
 * <dt>method scoped</dt><dd>
 *   True if this class is an anonymous or method scoped class.
 * </dd>
 * <dt>outer class</dt><dd>
 *   the class of which this class is the inner.  It returns null for
 *   top level classes and for method scoped classes. <br>
 * </dd>
 * <dt>classes</dt><dd>
 *   the inner classes which is an array of ClassInfo.  This doesn't
 *   include method scoped classes.<br>
 * </dd>
 * <dt>extra classes</dt><dd>
 *   this field tells only, for which additional classes the class
 *   files contains an InnerClass Attribute.  This can be the method
 *   scoped classes or the inner inner classes, but there is no
 *   promise. <br>
 * </dd>
 * <dt>source file</dt><dd>
 *   The name of source file.  The JVM uses this field when a stack
 *   trace is produced.
 * </dd>
 * </dl>
 *
 * <h3>inner classes</h3> 
 * Inner classes are supported as far as the information is present in
 * the bytecode.  But you can always ignore this inner information,
 * and access inner classes by their bytecode name,
 * e.g. <code>java.util.Map$Entry</code>.  There are four different types
 * of classes:
 * <dl>
 * <dt>normal package scoped classes</dt><dd>
 *   A class is package scoped if, and only if
 *   <code>getOuterClass()</code> returns <code>null</code> and
 *   <code>isMethodScoped()</code> returns <code>false</code>.
 * </dd>
 * <dt>inner class scoped classes</dt><dd>
 *   A class is class scoped if, and only if
 *   <code>getOuterClass()</code> returns not <code>null</code>.
 *
 *   The bytecode name (<code>getName()</code>) of an inner class is
 *   most times of the form <code>Package.Outer$Inner</code>.  But
 *   ClassInfo also supports differently named classes, as long as the
 *   InnerClass attribute is present.  The method
 *   <code>getClassName()</code> returns the name of the inner class
 *   (<code>Inner</code> in the above example).  
 *
 *   You can get all inner classes of a class with the
 *   <code>getClasses</code> method.
 * </dd>
 * <dt>named method scoped classes</dt><dd>
 *   A class is a named method scoped class if, and only if
 *   <code>isMethodScoped()</code> returns <code>true</code> and
 *   <code>getClassName()</code> returns not <code>null</code>.  In
 *   that case <code>getOuterClass()</code> returns <code>null</code>,
 *   too.<br><br>
 *
 *   The bytecode name (<code>getName()</code>) of an method scoped class is
 *   most times of the form <code>Package.Outer$Number$Inner</code>.  But
 *   ClassInfo also supports differently named classes, as long as the
 *   InnerClass attribute is present.  <br><br>
 *
 *   There's no way to get the method scoped classes of a method, except
 *   by analyzing its instructions.  
 * </dd>
 * <dt>anonymous classes</dt><dd>
 *   A class is an anonymous class if, and only if
 *   <code>isMethodScoped()</code> returns <code>true</code> and
 *   <code>getClassName()</code> returns <code>null</code>.  In that
 *   case <code>getOuterClass()</code> returns <code>null</code>,
 *   too.<br><br>
 *
 *   The bytecode name (<code>getName()</code>) of an method scoped class is
 *   most times of the form <code>Package.Outer$Number</code>.  But
 *   ClassInfo also supports differently named classes, as long as the
 *   InnerClass attribute is present.  <br><br>
 *
 *   There's no way to get the anonymous classes of a method, except
 *   by analyzing its instructions.  
 * </dd>
 * </dl>
 *
 * <hr>
 * <h3>open questions...</h3>
 *
 * I represent most types as <code>java/lang/String</code> (type
 * signatures); this is convenient since java bytecode does the same.
 * On the other hand a class type should be represented as
 * <code>jode/bytecode/ClassInfo</code> class.  There should be a
 * method to convert to it, but I need a ClassPath for this.  Should
 * the method be in ClassInfo (I don't think so), should an instance
 * of TypeSignature have a ClassPath as member variable, or should
 * getClassInfo() take a ClassPath parameter?  What about arrays?
 * <br>
 *
 * Should load(HIERARCHY) also try to load hierarchy of super class.
 * If yes, should it return an IOException if super class can't be
 * found?  Or should <code>getSuperclass</code> throw the IOException
 * instead (I don't like that).  <br>
 * <b>Current Solution:</b> <code>superClassOf</code> and 
 * <code>implementedBy</code> can throw an IOException, getSuperclass not.
 *
 * @author Jochen Hoenicke */
public final class ClassInfo extends BinaryInfo implements Comparable {

    private static ClassPath defaultClasspath;
    
    private int status = 0;

    private boolean modified = false;
    private ClassPath classpath;

    private int modifiers = -1;
    private String name;
    private String className;
    private boolean methodScoped;
    private ClassInfo    superclass;
    private ClassInfo    outerClass;
    private ClassInfo[]  interfaces;
    private ClassInfo[]  innerClasses;
    private ClassInfo[]  extraClasses;
    private FieldInfo[]  fields;
    private MethodInfo[] methods;
    private String sourceFile;

    /** 
     * This constant can be used as parameter to drop.  It specifies
     * that no information at all should be kept for the current class.
     *
     * @see #load 
     */
    public static final int NONE               = 0;
    /** 
     * This constant can be used as parameter to load.  It specifies
     * that at least the outer class information should be loaded, i.e.
     * the outer class, the class name.  It is the
     * only information that is loaded recursively:  It is also 
     * automatically loaded for the outer class and it is loaded for
     * all inner and extra classes, if these fields are loaded.
     * The reason for the recursive load is simple:  In java bytecode
     * a class contains the outer class information for all outer, 
     * inner and extra classes, so we can create this information 
     * without the need to read the outer class.  We also need this
     * information for outer and inner classes when writing a class.
     *
     * @see #load 
     */
    public static final int OUTERCLASS         = 5;
    /** 
     * This constant can be used as parameter to load.  It specifies
     * that at least the hierarchy information, i.e. the
     * superclass/interfaces fields and the modifiers 
     * of this class should be loaded.
     *
     * @see #load 
     */
    public static final int HIERARCHY          = 10;
    /** 
     * This constant can be used as parameter to load.  It specifies
     * that all public fields, methods and inner class declarations
     * should be loaded.  It doesn't load method bodies.
     *
     * @see #load 
     */
    public static final int PUBLICDECLARATIONS = 20;
    /** 
     * This constant can be used as parameter to load.  It specifies
     * that all the fields, methods and inner class declaration
     * should be loaded.  It doesn't load method bodies.
     *
     * @see #load 
     */
    public static final int DECLARATIONS       = 30;
    /** 
     * This constant can be used as parameter to load.  It specifies
     * that everything in the class except non-standard attributes
     * should be loaded.
     *
     * @see #load 
     */
    public static final int ALMOSTALL          = 90;
    /** 
     * This constant can be used as parameter to load.  It specifies
     * that everything in the class should be loaded.
     *
     * @see #load 
     */
    public static final int ALL                = 100;

    /**
     * @deprecated
     */
    public static void setClassPath(String path) {
        setClassPath(new ClassPath(path));
    }

    /**
     * @deprecated
     */
    public static void setClassPath(ClassPath path) {
	defaultClasspath= path;
    }

    /**
     * @deprecated
     */
    public static boolean exists(String name) {
        return defaultClasspath.existsClass(name);
    }
    
    /**
     * @deprecated
     */
    public static boolean isPackage(String name) {
        return defaultClasspath.isDirectory(name.replace('.', '/'));
    }
    
    /**
     * @deprecated
     */
    public static Enumeration getClassesAndPackages(String packageName) {
	return defaultClasspath.listClassesAndPackages(packageName);
    }

    /**
     * @deprecated
     */
    public static ClassInfo forName(String name) {
	return defaultClasspath.getClassInfo(name);
    }

    ClassInfo(String name, ClassPath classpath) {
        this.name = name.intern();
	this.classpath = classpath;
    }

    /**
     * Returns the classpath in which this class was created.
     */
    public ClassPath getClassPath() {
	return classpath;
    }

    /****** READING CLASS FILES ***************************************/

    private static int javaModifiersToBytecode(int javaModifiers) {
	int modifiers = javaModifiers & (Modifier.FINAL
					 | 0x20 /*ACC_SUPER*/
					 | Modifier.INTERFACE
					 | Modifier.ABSTRACT);

	if ((javaModifiers & (Modifier.PUBLIC | Modifier.PROTECTED)) != 0)
	    return Modifier.PUBLIC | modifiers;
	else
	    return modifiers;
    }
    
    private void mergeModifiers(int newModifiers) {
	if (modifiers == -1) {
	    modifiers = newModifiers;
	    return;
	}
	if (((modifiers ^ newModifiers) & ~0x20) == 0) {
	    modifiers |= newModifiers;
	    return;
	}
	
	int oldSimple = javaModifiersToBytecode(modifiers);
	if (((oldSimple ^ newModifiers) & ~0x20) == 0) {
	    modifiers |= newModifiers & 0x20;
	    return;
	}

	int newSimple = javaModifiersToBytecode(newModifiers);
	if (((newSimple ^ modifiers) & ~0x20) == 0) {
	    modifiers = newModifiers | (modifiers & 0x20);
	    return;
	}

	throw new ClassFormatError
	    ("modifiers in InnerClass info doesn't match: "
             + modifiers + "<->" + newModifiers);
    }

    private void mergeOuterInfo(String className, ClassInfo outer, 
				int realModifiers, boolean ms) 
	throws ClassFormatException 
    {
	if (status >= OUTERCLASS) {
	    if ((className == null 
		 ? this.className != null : !className.equals(this.className))
		|| this.outerClass != outer)
		throw new ClassFormatException("Outer information mismatch "
					       +name+": "+className+","+outer+","+ms+"<->"+this.className +","+this.outerClass+","+this.methodScoped);
	    mergeModifiers(realModifiers);
	} else {
	    if (realModifiers != -1)
		mergeModifiers(realModifiers);
	    this.className = className;
	    this.outerClass = outer;
	    this.methodScoped = ms;
	    this.status = OUTERCLASS;
	}
    }

    private void readInnerClassesAttribute(int length, ConstantPool cp,
					   DataInputStream input)
	throws IOException
    {
	/* The InnerClasses attribute is transformed in a special way
	 * so we want to taker a closer look.  According to the inner
	 * class specification,
	 *
	 * http://java.sun.com/products/jdk/1.1/docs/guide/innerclasses/spec/innerclasses.doc10.html#18814
	 *
	 * there are several InnerClass records in a class.  We differ 
	 * three different types:
	 *
	 * The InnerClass records for our own inner classes:  They can
	 * easily be recognized, since this class must be mentioned in
	 * the outer_class_info_index field.
	 * 
	 * The InnerClass records for the outer class and its outer
	 * classes, and so on: According to the spec, these must
	 * always be present if this is a class scoped class.  And they
	 * must be in reversed order, i.e. the outer most class comes
	 * first.
	 *
	 * Some other InnerClass records, the extra classes.  This is
	 * optional, but we don't want to loose this information if we
	 * just transform classes, so we memorize for which classes we
	 * have to keep the inforamtion anyway.
	 *
	 * Currently we don't use all informations, since we don't
	 * update the information for inner/outer/extra classes or
	 * check it for consistency.  This might be bad, since this 
	 * means that 
	 * <pre>
	 * load(ALL); write(new File())
	 * </pre> 
	 * doesn't work.
	 */
	    
	int count = input.readUnsignedShort();
	if (length != 2 + 8 * count)
	    throw new ClassFormatException
		("InnerClasses attribute has wrong length");

	int innerCount = 0, outerCount = 0, extraCount = 0;
	/**
	 * The first part will contain the inner classes, the last
	 * part the extra classes.
	 */
	ClassInfo[] innerExtra = new ClassInfo[count];

	for (int i=0; i < count; i++) {
	    int innerIndex = input.readUnsignedShort();
	    int outerIndex = input.readUnsignedShort();
	    int nameIndex = input.readUnsignedShort();
	    String inner = cp.getClassName(innerIndex);
	    String outer = outerIndex != 0
		? cp.getClassName(outerIndex) : null;
	    String innername = nameIndex != 0 ? cp.getUTF8(nameIndex) : null;
	    int access = input.readUnsignedShort();
	    if (innername != null && innername.length() == 0)
		innername = null;

	    ClassInfo innerCI = classpath.getClassInfo(inner);
	    ClassInfo outerCI = null;
	    if (outer != null) {
		outerCI = classpath.getClassInfo(outer);
		/* If we didn't find an InnerClasses info for outerCI, yet,
		 * this means that it doesn't have an outer class.  So we
		 * know its (empty) outer class status now.
		 */
		if (outerCI.status < OUTERCLASS)
		    outerCI.mergeOuterInfo(null, null, -1, false);
	    }

	    if (innername == null)
		/* anonymous class */
		outerCI = null;

	    innerCI.mergeOuterInfo(innername, outerCI, 
				   access, outerCI == null);
	    if (outerCI == this)
		innerExtra[innerCount++] = innerCI;
	    else {
		/* Remove outerCI from the extra part of innerExtra
		 * since it is implicit now.
		 */
		for (int j = count - 1; j >= count - extraCount; j--) {
		    if (innerExtra[j] == outerCI) {
			System.arraycopy(innerExtra, count - extraCount,
					 innerExtra, count - extraCount + 1,
					 j - (count - extraCount));
			extraCount--;
			break;
		    }
		}

		/* Add innerCI to the extra classes, except if it is
		 * this class.
		 */
		if (innerCI != this)
		    innerExtra[count - (++extraCount)] = innerCI;
	    }
	}

	/* Now inner classes are at the front of the array in correct
	 * order.  The extra classes are in reverse order at the end
	 * of the array.
	 */
	if (innerCount > 0) {
	    innerClasses = new ClassInfo[innerCount];
	    for (int i=0; i < innerCount; i++)
		innerClasses[i] = innerExtra[i];
	} else
	    innerClasses = null;
	
	if (extraCount > 0) {
	    extraClasses = new ClassInfo[extraCount];
	    for (int i = 0; i < extraCount; i++)
		extraClasses[i] = innerExtra[count - i - 1];
	} else
	    extraClasses = null;
    }

    void readAttribute(String name, int length,
		       ConstantPool cp,
		       DataInputStream input, 
		       int howMuch) throws IOException {
	if (howMuch >= ClassInfo.ALMOSTALL && name.equals("SourceFile")) {
	    if (length != 2)
		throw new ClassFormatException("SourceFile attribute"
					       + " has wrong length");
	    sourceFile = cp.getUTF8(input.readUnsignedShort());
	} else if (howMuch >= ClassInfo.OUTERCLASS
		   && name.equals("InnerClasses")) {
	    readInnerClassesAttribute(length, cp, input);
	} else
	    super.readAttribute(name, length, cp, input, howMuch);
    }

    void loadFromReflection(Class clazz, int howMuch) 
	throws SecurityException, ClassFormatException {
	if (howMuch >= OUTERCLASS) {
	    Class declarer = clazz.getDeclaringClass();
	    if (declarer != null) {
		/* We have to guess the className, since reflection doesn't
		 * tell it :-(
		 */
		int dollar = name.lastIndexOf('$');
		className = name.substring(dollar+1);
		outerClass = classpath.getClassInfo(declarer.getName());
		/* As mentioned above OUTERCLASS is recursive */
		outerClass.loadFromReflection(declarer, OUTERCLASS);
	    } else {
		/* Check if class name ends with $[numeric]$name or
		 * $[numeric], in which case it is a method scoped
		 * resp.  anonymous class.  
		 */
		int dollar = name.lastIndexOf('$');
		if (dollar >= 0 && Character.isDigit(name.charAt(dollar+1))) {
		    /* anonymous class */
		    className = null;
		    outerClass = null;
		    methodScoped = true;
		} else {
		    int dollar2 = name.lastIndexOf('$', dollar);
		    if (dollar2 >= 0
			&& Character.isDigit(name.charAt(dollar2+1))) {
			className = name.substring(dollar+1);
			outerClass = null;
			methodScoped = true;
		    }
		}
	    }
		
	}
	if (howMuch >= HIERARCHY) {
	    modifiers = clazz.getModifiers();
	    if (clazz.getSuperclass() == null)
		superclass = clazz == Object.class
		    ? null : classpath.getClassInfo("java.lang.Object");
	    else
		superclass = classpath.getClassInfo
		    (clazz.getSuperclass().getName());
	    Class[] ifaces = clazz.getInterfaces();
	    interfaces = new ClassInfo[ifaces.length];
	    for (int i=0; i<ifaces.length; i++)
		interfaces[i] = classpath.getClassInfo(ifaces[i].getName());
	    status |= HIERARCHY;
	}
	if (howMuch >= PUBLICDECLARATIONS) {
	    Field[] fs;
	    Method[] ms;
	    Constructor[] cs;
	    Class[] is;
	    if (howMuch == PUBLICDECLARATIONS) {
		fs = clazz.getFields();
		ms = clazz.getMethods();
		cs = clazz.getConstructors();
		is = clazz.getClasses();
	    } else {
		fs = clazz.getDeclaredFields();
		ms = clazz.getDeclaredMethods();
		cs = clazz.getDeclaredConstructors();
		is = clazz.getDeclaredClasses();
	    }

	    int len = 0;
	    for (int i = fs.length; --i >= 0; ) {
		if (fs[i].getDeclaringClass() == clazz)
		    len++;
	    }
	    int fieldPtr = len;
	    fields = new FieldInfo[len];
	    for (int i = fs.length; --i >= 0; ) {
		if (fs[i].getDeclaringClass() == clazz) {
		    String type = TypeSignature.getSignature(fs[i].getType());
		    fields[--fieldPtr] = new FieldInfo
			(fs[i].getName(), type, fs[i].getModifiers());
		}
	    }

	    len = cs.length;
	    for (int i = ms.length; --i >= 0; ) {
		if (ms[i].getDeclaringClass() == clazz)
		    len++;
	    }
	    methods = new MethodInfo[len];
	    int methodPtr = len;
	    for (int i = ms.length; --i >= 0; ) {
		if (ms[i].getDeclaringClass() == clazz) {
		    String type = TypeSignature.getSignature
			(ms[i].getParameterTypes(), ms[i].getReturnType());
		    methods[--methodPtr] = new MethodInfo
			(ms[i].getName(), type, ms[i].getModifiers());
		}
	    }
	    for (int i = cs.length; --i >= 0; ) {
		String type = TypeSignature.getSignature
		    (cs[i].getParameterTypes(), void.class);
		methods[--methodPtr] = new MethodInfo
		    ("<init>", type, cs[i].getModifiers());
	    }
	    if (is.length > 0) {
		innerClasses = new ClassInfo[is.length];
		for (int i = is.length; --i >= 0; ) {
		    innerClasses[i] = classpath.getClassInfo(is[i].getName());
		    /* As mentioned above OUTERCLASS is loaded recursive */
		    innerClasses[i].loadFromReflection(is[i], OUTERCLASS);
		}
	    } else
		innerClasses = null;
	}
	status = howMuch;
    }
    
    /**
     * Reads a class file from a data input stream.  You should really
     * <code>load</code> a class from its classpath instead.  This may
     * be useful for special kinds of input streams, that ClassPath 
     * doesn't handle though.
     *
     * @param input The input stream, containing the class in standard
     *              bytecode format.
     * @param howMuch The amount of information that should be read in, one
     *                of HIERARCHY, PUBLICDECLARATIONS, DECLARATIONS or ALL.
     * @exception ClassFormatException if the file doesn't denote a valid
     * class.  
     * @exception IOException if input throws an exception.
     * @exception IllegalStateException if this ClassInfo was modified.
     * @see #load
     */
    public void read(DataInputStream input, int howMuch) 
	throws IOException 
    {
	if (modified)
	    throw new IllegalStateException(name);
	if (status >= howMuch)
	    return;

	/* Since we have to read the whole class anyway, we load all
	 * info, that we may need later and that does not take much memory. 
	 */
	if (howMuch <= DECLARATIONS)
	    howMuch = DECLARATIONS;

	/* header */
	if (input.readInt() != 0xcafebabe)
	    throw new ClassFormatException("Wrong magic");
	int version = input.readUnsignedShort();
	version |= input.readUnsignedShort() << 16;
	if (version < (45 << 16 | 0)
	    || version > (46 << 16 | 0))
	  throw new ClassFormatException("Wrong class version");

	/* constant pool */
        ConstantPool cpool = new ConstantPool();
        cpool.read(input);

	/* always read modifiers, name, super, ifaces */
	{
	    modifiers = input.readUnsignedShort();
	    String className = cpool.getClassName(input.readUnsignedShort());
	    if (!name.equals(className))
		throw new ClassFormatException("wrong name " + className);
	    String superName = cpool.getClassName(input.readUnsignedShort());
	    superclass = superName != null ? classpath.getClassInfo(superName) : null;
	    int count = input.readUnsignedShort();
	    interfaces = new ClassInfo[count];
	    for (int i=0; i< count; i++) {
		interfaces[i] = classpath.getClassInfo
		    (cpool.getClassName(input.readUnsignedShort()));
	    }
	}	    

	/* fields */
        if (howMuch >= PUBLICDECLARATIONS) {
            int count = input.readUnsignedShort();
	    fields = new FieldInfo[count];
            for (int i=0; i< count; i++) {
		fields[i] = new FieldInfo(); 
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
        if (howMuch >= PUBLICDECLARATIONS) {
            int count = input.readUnsignedShort();
	    methods = new MethodInfo[count];
            for (int i=0; i< count; i++) {
		methods[i] = new MethodInfo(); 
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
	status = howMuch;
    }

    private void reserveSmallConstants(GrowableConstantPool gcp) {
	for (int i=0; i < fields.length; i++)
	    fields[i].reserveSmallConstants(gcp);

	for (int i=0; i < methods.length; i++)
	    methods[i].reserveSmallConstants(gcp);
    }

    /****** WRITING CLASS FILES ***************************************/

    private void prepareWriting(GrowableConstantPool gcp) {
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
	if (outerClass != null || methodScoped
	    || innerClasses != null || extraClasses != null) {
	    gcp.putUTF8("InnerClasses");
	    
	    ClassInfo outer = this;
	    while (outer.outerClass != null || outer.methodScoped) {
		if (outer.status <= OUTERCLASS)
		    throw new IllegalStateException
			(outer.name + "'s state is " + outer.status);
		if (outer.className != null)
		    gcp.putClassName(outer.className);
		if (outer.outerClass == null)
		    break;
		gcp.putClassName(outer.outerClass.name);
		outer = outer.outerClass;
	    }
	    int innerCount = innerClasses != null ? innerClasses.length : 0;
	    for (int i = innerCount; i-- > 0; i++) {
		if (innerClasses[i].status <= OUTERCLASS)
		    throw new IllegalStateException
			(innerClasses[i].name + "'s state is "
			 + innerClasses[i].status);
		if (innerClasses[i].outerClass != this)
		    throw new IllegalStateException
			(innerClasses[i].name + "'s outer is " + 
			 innerClasses[i].outerClass.name);

		gcp.putClassName(innerClasses[i].name);
		if (innerClasses[i].className != null)
		    gcp.putUTF8(innerClasses[i].className);
	    }
	    int extraCount = extraClasses != null ? extraClasses.length : 0;
	    for (int i=extraCount; i-- >= 0; ) {
		if (extraClasses[i].status <= OUTERCLASS)
		    throw new IllegalStateException
			(extraClasses[i].name + "'s state is "
			 + extraClasses[i].status);
		gcp.putClassName(extraClasses[i].name);
		if (extraClasses[i].outerClass != null)
		    gcp.putClassName(extraClasses[i].outerClass.name);
		if (extraClasses[i].className != null)
		    gcp.putUTF8(extraClasses[i].className);
	    }
	}
        prepareAttributes(gcp);
    }

    int getKnownAttributeCount() {
	int count = 0;
	if (sourceFile != null)
	    count++;
	if (outerClass != null || methodScoped
	    || innerClasses != null || extraClasses != null)
	    count++;
	return count;
    }

    void writeKnownAttributes(GrowableConstantPool gcp,
			      DataOutputStream output) 
	throws IOException {
	if (sourceFile != null) {
	    output.writeShort(gcp.putUTF8("SourceFile"));
	    output.writeInt(2);
	    output.writeShort(gcp.putUTF8(sourceFile));
	}
	if (outerClass != null || methodScoped
	    || innerClasses != null || extraClasses != null) {
	    // XXX TODO: Closeness of extra outer information.
	    gcp.putUTF8("InnerClasses");

	    ClassInfo outer;
	    LinkedList outerExtraClasses = new LinkedList();

	    outer = this;
	    while (outer.outerClass != null || outer.methodScoped) {
		/* Outers must be written in backward order, so we
		 * add them to the beginning of the list.
		 */
		outerExtraClasses.add(0, outer);
		if (outer.outerClass == null)
		    break;
		outer = outer.outerClass;
	    }
	    if (extraClasses != null) {
		int extraCount =  extraClasses.length;
		for (int i = 0; i < extraCount; i++) {
		    outer = extraClasses[i];
		    ListIterator insertIter
			= outerExtraClasses.listIterator
			(outerExtraClasses.size());
		    int insertPos = outerExtraClasses.size();
		    while (outer.outerClass != null || outer.methodScoped) {
			if (outerExtraClasses.contains(outer))
			    break;
			/* We have to add outers in reverse order to the
			 * end of the list.  We use the insertIter to do
			 * this trick.
			 */
			insertIter.add(outer);
			insertIter.previous();
			if (outer.outerClass == null)
			    break;
			outer = outer.outerClass;
		    }
		}
	    }
	    
	    int innerCount = (innerClasses != null) ? innerClasses.length : 0;
	    int count = outerExtraClasses.size() + innerCount;
	    output.writeInt(2 + count * 8);
	    output.writeShort(count);

	    for (Iterator i = outerExtraClasses.iterator(); i.hasNext(); ) {
		outer = (ClassInfo) i.next();

		output.writeShort(gcp.putClassName(outer.name));
		output.writeShort(outer.outerClass == null ? 0 : 
				  gcp.putClassName(outer.outerClass.name));
		output.writeShort(outer.className == null ? 0 :
				  gcp.putUTF8(outer.className));
		output.writeShort(outer.modifiers);
	    }
	    for (int i = innerCount; i-- > 0; i++) {
		output.writeShort(gcp.putClassName(innerClasses[i].name));
		output.writeShort(innerClasses[i].outerClass != null ? 
				  gcp.putClassName(innerClasses[i]
						   .outerClass.name) : 0);
		output.writeShort(innerClasses[i].className != null ?
				  gcp.putUTF8(innerClasses[i].className) : 0);
		output.writeShort(innerClasses[i].modifiers);
	    }
	}
    }


    /**
     * Writes a class to the given DataOutputStream.  Of course this only
     * works if ALL information for this class is loaded/set.  If this
     * class has an outer class, inner classes or extra classes, their 
     * status must contain at least the OUTERCLASS information.
     * @param out the output stream.
     * @exception IOException if out throws io exception.
     * @exception IllegalStateException if not enough information is set.
     */
    public void write(DataOutputStream out) throws IOException {
	if (status <= ALL)
	    throw new IllegalStateException("state is "+status);

	GrowableConstantPool gcp = new GrowableConstantPool();
	reserveSmallConstants(gcp);
	prepareWriting(gcp);

	out.writeInt(0xcafebabe);
	out.writeShort(3);
	out.writeShort(45);
	gcp.write(out);

	out.writeShort(javaModifiersToBytecode(modifiers));
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

    /**
     * Loads the contents of a class from the classpath.
     * @param howMuch The amount of information that should be read
     *                in, one of <code>HIERARCHY</code>,
     *                <code>PUBLICDECLARATIONS</code>,
     *                <code>DECLARATIONS</code>, <code>ALMOSTALL</code>
     *                or <code>ALL</code>.
     * @exception ClassFormatException if the file doesn't denote a
     *            valid class.
     * @exception FileNotFoundException if class wasn't found in classpath.
     * @exception IOException if an io exception occured.
     * @exception IllegalStateException if this ClassInfo was modified.
     * @see #HIERARCHY
     * @see #PUBLICDECLARATIONS
     * @see #DECLARATIONS
     * @see #ALMOSTALL
     * @see #ALL 
     */
    public void load(int howMuch) 
	throws IOException
    {
	if (modified)
	    throw new IllegalStateException(name);
	if (status >= howMuch)
	    return;
	if (classpath.loadClass(this, howMuch))
	    return;
	throw new FileNotFoundException(name);
    }

    /**
     * Guess the contents of a class.  It
     * @param howMuch The amount of information that should be read, e.g.
     *                <code>HIERARCHY</code>.
     * @exception ClassFormatException if the file doesn't denote a
     *            valid class.
     * @exception FileNotFoundException if class wasn't found in classpath.
     * @exception IOException if an io exception occured.
     * @exception IllegalStateException if this ClassInfo was modified.
     * @see #OUTERCLASS
     * @see #HIERARCHY
     * @see #PUBLICDECLARATIONS
     * @see #DECLARATIONS
     * @see #ALMOSTALL
     * @see #ALL 
     */
    public void guess(int howMuch) 
    {
	if (howMuch >= OUTERCLASS && status < OUTERCLASS) {
	    int dollar = name.lastIndexOf('$');
	    if (dollar == -1) {
		/* normal class */
	    } else if (Character.isDigit(name.charAt(dollar+1))) {
		/* anonymous class */
		modifiers = Modifier.PUBLIC | 0x20;
		methodScoped = true;
	    } else {
		modifiers = Modifier.PUBLIC | 0x20;
		className = name.substring(dollar+1);
		int prevDollar = name.lastIndexOf('$', dollar);
		if (prevDollar >= 0
		    && Character.isDigit(name.charAt(prevDollar))) {
		    /* probably method scoped class, (or inner class
                     * of anoymous class) */
		    methodScoped = true;
		    outerClass = classpath.getClassInfo
			(name.substring(0, prevDollar));
		} else {
		    /* inner class */
		    modifiers = Modifier.PUBLIC | 0x20;
		    outerClass = classpath.getClassInfo
			(name.substring(0, dollar));
		}
	    }
	}
	if (howMuch >= HIERARCHY && status < HIERARCHY) {
	    modifiers = Modifier.PUBLIC | 0x20;
	    if (name.equals("java.lang.Object"))
		superclass = null;
	    else
		superclass = classpath.getClassInfo("java.lang.Object");
	    interfaces = new ClassInfo[0];
	}
	if (howMuch >= PUBLICDECLARATIONS && status < PUBLICDECLARATIONS) {
	    methods = new MethodInfo[0];
	    fields = new FieldInfo[0];
	    innerClasses = new ClassInfo[0];
	}
	status = howMuch;
    }

    /**  
     * This is the counter part to load.  It will drop all
     * informations up keep and clean up the memory.
     * @param keep tells how much info we should keep, can be
     *    <code>NONE</code> or anything that <code>load</code> accepts.
     * @see #load
     */
    public void drop(int keep) {
	if (status <= keep)
	    return;
	if (modified) {
	    System.err.println("Dropping info between " + keep + " and "
			       + status + " in modified class" + this + ".");
	    Thread.dumpStack();
	    return;
	}
	if (keep < HIERARCHY) {
	    superclass = null;
	    interfaces = null;
	}

	if (keep < OUTERCLASS) {
	    methodScoped = false;
	    outerClass = null;
	    innerClasses = null;
	    extraClasses = null;
	}

	if (keep < PUBLICDECLARATIONS) {
	    fields = null;
	    methods = null;
	    status = keep;
	} else {
	    if (keep < ALMOSTALL && status >= ALMOSTALL) {
		for (int i=0; i < fields.length; i++)
		    fields[i].dropBody();
		for (int i=0; i < methods.length; i++)
		    methods[i].dropBody();
	    }
	    if (status >= DECLARATIONS)
		/* We don't drop non-public declarations, since this
		 * is not worth it.  
		 */
		keep = DECLARATIONS;
	}

	if (keep < ALMOSTALL && status >= ALMOSTALL) {
	    sourceFile = null;
	    super.dropAttributes();
	}
	status = keep;
    }

    /**
     * Returns the full qualified name of this class.
     * @return the full qualified name of this class, an interned string.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the java class name of a class, without package or
     * outer classes.  This is null for an anonymous class.  For other
     * classes it is the name that occured after the
     * <code>class</code> keyword (provided it was compiled from
     * java).
     * This need OUTERCLASS information loaded to work properly.
     *
     * @return the short name of this class.  Returns null for
     * anonymous classes.
     *
     * @exception IllegalStateException if OUTERCLASS information wasn't
     * loaded yet.  */
    public String getClassName() {
	if (status < OUTERCLASS)
            throw new IllegalStateException("status is "+status);
	if (className != null || isMethodScoped())
	    return className;

	int dot = name.lastIndexOf('.');
	return name.substring(dot+1);
    }

    /**
     * Returns the ClassInfo object for the super class.
     * @return the short name of this class.
     * @exception IllegalStateException if HIERARCHY information wasn't
     * loaded yet.
     */
    public ClassInfo getSuperclass() {
	if (status < HIERARCHY)
            throw new IllegalStateException("status is "+status);
        return superclass;
    }
    
    /**
     * Returns the ClassInfo object for the super class.
     * @return the short name of this class.
     * @exception IllegalStateException if HIERARCHY information wasn't
     * loaded yet.
     */
    public ClassInfo[] getInterfaces() {
        if (status < HIERARCHY)
            throw new IllegalStateException("status is "+status);
        return interfaces;
    }

    public int getModifiers() {
        if (modifiers == -1)
            throw new IllegalStateException("status is "+status);
        return modifiers;
    }

    public boolean isInterface() {
        return Modifier.isInterface(getModifiers());
    }

    public FieldInfo findField(String name, String typeSig) {
        if (status < PUBLICDECLARATIONS)
            throw new IllegalStateException("status is "+status);
        for (int i=0; i< fields.length; i++)
            if (fields[i].getName().equals(name)
                && fields[i].getType().equals(typeSig))
                return fields[i];
        return null;
    }

    public MethodInfo findMethod(String name, String typeSig) {
        if (status < PUBLICDECLARATIONS)
            throw new IllegalStateException("status is "+status);
        for (int i=0; i< methods.length; i++)
            if (methods[i].getName().equals(name)
                && methods[i].getType().equals(typeSig))
                return methods[i];
        return null;
    }

    public MethodInfo[] getMethods() {
        if (status < PUBLICDECLARATIONS)
            throw new IllegalStateException("status is "+status);
        return methods;
    }

    public FieldInfo[] getFields() {
        if (status < PUBLICDECLARATIONS)
            throw new IllegalStateException("status is "+status);
        return fields;
    }

    /**
     * Returns the outer class of this class if it is an inner class.
     * This needs the OUTERCLASS information loaded. 
     * @return The class that declared this class, null if the class
     * isn't declared in a class scope
     *
     * @exception IllegalStateException if OUTERCLASS information
     * wasn't loaded yet.  
     */
    public ClassInfo getOuterClass() {
        if (status < OUTERCLASS)
            throw new IllegalStateException("status is "+status);
        return outerClass;
    }

    /**
     * Returns true if the class was declared inside a method.
     * This needs the OUTERCLASS information loaded. 
     * @return true if this is a method scoped or an anonymous class,
     * false otherwise.
     *
     * @exception IllegalStateException if OUTERCLASS information
     * wasn't loaded yet.  
     */
    public boolean isMethodScoped() {
        if (status < OUTERCLASS)
            throw new IllegalStateException("status is "+status);
        return methodScoped;
    }

    public ClassInfo[] getClasses() {
        if (status < PUBLICDECLARATIONS)
            throw new IllegalStateException("status is "+status);
        return innerClasses;
    }

    public ClassInfo[] getExtraClasses() {
        if (status < OUTERCLASS)
            throw new IllegalStateException("status is "+status);
        return extraClasses;
    }

    public String getSourceFile() {
	return sourceFile;
    }

    public void setName(String newName) {
	name = newName.intern();
	modified = true;
    }

    public void setSuperclass(ClassInfo newSuper) {
	superclass = newSuper;
	modified = true;
    }
    
    public void setInterfaces(ClassInfo[] newIfaces) {
        interfaces = newIfaces;
	modified = true;
    }

    public void setModifiers(int newModifiers) {
        modifiers = newModifiers;
	modified = true;
    }

    public void setMethods(MethodInfo[] mi) {
        methods = mi;
	modified = true;
    }

    public void setFields(FieldInfo[] fi) {
        fields = fi;
	modified = true;
    }

    public void setOuterClass(ClassInfo oc) {
        outerClass = oc;
	modified = true;
    }

    public void setMethodScoped(boolean ms) {
        methodScoped = ms;
	modified = true;
    }

    public void setClasses(ClassInfo[] ic) {
        innerClasses = ic;
	modified = true;
    }

    public void setExtraClasses(ClassInfo[] ec) {
        extraClasses = ec;
	modified = true;
    }

    public void setSourceFile(String newSource) {
	sourceFile = newSource;
	modified = true;
    }

    /** 
     * Gets the serial version UID of this class.  If a final static
     * long serialVersionUID  field is present, its constant value
     * is returned.  Otherwise the UID is calculated with the algorithm
     * in the serial version spec.
     * @return the serial version UID of this class.
     * @exception IllegalStateException if DECLARATIONS aren't loaded.
     * @exception NoSuchAlgorithmException if SHA-1 message digest is not
     * supported (needed for calculation of UID.
     */
    public long getSerialVersionUID() throws NoSuchAlgorithmException {
        if (status < DECLARATIONS)
            throw new IllegalStateException("status is "+status);
	FieldInfo fi = findField("serialVersionUID", "J");
	if (fi != null
	    && ((fi.getModifiers() & (Modifier.STATIC | Modifier.FINAL))
		== (Modifier.STATIC | Modifier.FINAL))
	    && fi.getConstant() != null)
	    return ((Long) fi.getConstant()).longValue();
	
	final MessageDigest md = MessageDigest.getInstance("SHA");
	OutputStream digest = new OutputStream() {

	    public void write(int b) {
		md.update((byte) b);
	    }

	    public void write(byte[] data, int offset, int length) {
		md.update(data, offset, length);
	    }
	};
	DataOutputStream out = new DataOutputStream(digest);
	try {
	    out.writeUTF(this.name);

	    // just look at interesting bits of modifiers
	    int modifs = javaModifiersToBytecode(this.modifiers) 
		& (Modifier.ABSTRACT | Modifier.FINAL
		   | Modifier.INTERFACE | Modifier.PUBLIC);
	    out.writeInt(modifs);
	    
	    ClassInfo[] interfaces = (ClassInfo[]) this.interfaces.clone();
	    Arrays.sort(interfaces);
	    for (int i=0; i < interfaces.length; i++)
		out.writeUTF(interfaces[i].name);

	    FieldInfo[] fields  = (FieldInfo[]) this.fields.clone();
	    Arrays.sort(fields);
	    for (int i=0; i < fields.length; i++) {
		modifs = fields[i].getModifiers();
		if ((modifs & Modifier.PRIVATE) != 0
		    && (modifs & (Modifier.STATIC 
				     | Modifier.TRANSIENT)) != 0)
		    continue;
		
		out.writeUTF(fields[i].getName());
		out.writeInt(modifs);
		out.writeUTF(fields[i].getType());
	    }

	    MethodInfo[] methods = (MethodInfo[]) this.methods.clone();
	    Arrays.sort(methods);
	    
	    for (int i=0; i < methods.length; i++) {
		modifs = methods[i].getModifiers();
		/* The modifiers of <clinit> should be just static,
		 * but jikes also marks it final.  
		 */
		if (methods[i].getName().equals("<clinit>"))
		    modifs = Modifier.STATIC;
		if ((modifs & Modifier.PRIVATE) != 0)
		    continue;
		
		out.writeUTF(methods[i].getName());
		out.writeInt(modifs);
		
		// the replacement of '/' with '.' was needed to make
		// computed SUID's agree with those computed by JDK.
		out.writeUTF(methods[i].getType().replace('/', '.'));
	    }
	    
	    out.close();

	    byte[] sha = md.digest();
	    long result = 0;
	    for (int i=0; i < 8; i++) {
		result += (long)(sha[i] & 0xFF) << (8 * i);
	    }
	    return result;
	} catch (IOException ex) {
	    /* Can't happen, since our OutputStream can't throw an
	     * IOException.
	     */
	    throw new InternalError();
	}
    }

    /**
     * Compares two ClassInfo objects for name order.
     * @return a positive number if this name lexicographically
     * follows than other's name, a negative number if it preceeds the
     * other, 0 if they are equal.  
     * @exception ClassCastException if other is not a ClassInfo.
     */
    public int compareTo(Object other) {
	return name.compareTo(((ClassInfo) other).name);
    }

    /**
     * Checks if this class is a super class of child.  This loads the
     * complete hierarchy of child on demand and can throw an IOException
     * if some classes are not found or broken.
     * @param child the class that should be a child class of us.
     * @return true if this is as super class of child, false otherwise
     * @exception IOException if hierarchy of child could not be loaded.
     */
    public boolean superClassOf(ClassInfo child) throws IOException {
        while (child != this && child != null) {
	    if (child.status < HIERARCHY)
		child.load(HIERARCHY);
            child = child.getSuperclass();
        }
        return child == this;
    }

    /**
     * Checks if this interface is implemented by clazz.  This loads the
     * complete hierarchy of clazz on demand and can throw an IOException
     * if some classes are not found or broken.  If this class is not an
     * interface it returns false, but you should check it yourself for 
     * better performance.
     * @param clazz the class to be checked.
     * @return true if this is a interface and is implemented by clazz,
     * false otherwise
     * @exception IOException if hierarchy of clazz could not be loaded.
     */
    public boolean implementedBy(ClassInfo clazz) throws IOException {
        while (clazz != this && clazz != null) {
	    if (clazz.status < HIERARCHY)
		clazz.load(HIERARCHY);
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
