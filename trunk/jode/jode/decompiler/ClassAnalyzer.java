/* ClassAnalyzer Copyright (C) 1998-1999 Jochen Hoenicke.
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

package jode.decompiler;
import jode.Decompiler;
import jode.type.*;
import jode.bytecode.ClassInfo;
import jode.bytecode.FieldInfo;
import jode.bytecode.MethodInfo;
import jode.bytecode.InnerClassInfo;
import jode.bytecode.ConstantPool;
import jode.expr.Expression;
import jode.expr.ConstructorOperator;
import jode.flow.TransformConstructors;
import java.util.NoSuchElementException;
import java.lang.reflect.Modifier;

public class ClassAnalyzer implements Analyzer, Scope {
    ImportHandler imports;
    ClassInfo clazz;
    Object parent;

    String name;
    FieldAnalyzer[] fields;
    MethodAnalyzer[] methods;
    ClassAnalyzer[] inners;

    MethodAnalyzer staticConstructor;
    MethodAnalyzer[] constructors;


    public boolean isScopeOf(Object obj, int scopeType) {
	if (clazz.equals(obj) && scopeType == CLASSSCOPE)
	    return true;
	return false;
    }

    public boolean conflicts(String name, int usageType) {
	ClassInfo info = clazz;
	while (info != null) {
	    if (usageType == METHODNAME) {
		MethodInfo[] minfos = info.getMethods();
		for (int i = 0; i< minfos.length; i++)
		    if (minfos[i].getName().equals(name))
			return true;
	    }
	    if (usageType == FIELDNAME || usageType == AMBIGUOUSNAME) {
		FieldInfo[] finfos = info.getFields();
		for (int i=0; i < finfos.length; i++) {
		    if (finfos[i].getName().equals(name))
			return true;
		}
	    }
	    if (usageType == CLASSNAME || usageType == AMBIGUOUSNAME) {
		InnerClassInfo[] iinfos = info.getInnerClasses();
		if (iinfos != null) {
		    for (int i=0; i < iinfos.length; i++) {
			if (iinfos[i].name.equals(name))
			    return true;
		    }
		}
	    }
	    info = info.getSuperclass();
	}
	return false;
    }
        
    public ClassAnalyzer(Object parent,
			 ClassInfo clazz, ImportHandler imports)
    {
        clazz.loadInfo(clazz.FULLINFO);
        this.parent = parent;
        this.clazz = clazz;
        this.imports = imports;
	name = clazz.getName();

	if (parent != null) {
	    InnerClassInfo[] outerInfos = clazz.getOuterClasses();
	    if (outerInfos[0].outer == null) {
		if (parent instanceof ClassAnalyzer)
		    throw new jode.AssertError
			("ClassInfo Attributes are inconsistent: "
			 + clazz.getName());
	    } else {
		if (!(parent instanceof ClassAnalyzer)
		    || !(((ClassAnalyzer) parent).clazz.getName()
			 .equals(outerInfos[0].outer))
		    || outerInfos[0].name == null)
		    throw new jode.AssertError
			("ClassInfo Attributes are inconsistent: "
			 + clazz.getName());
	    }
	    name = outerInfos[0].name;
	} else {
	    name = clazz.getName();
	    int dot = name.lastIndexOf('.');
	    if (dot >= 0)
		name = name.substring(dot+1);
	}
    }

    public ClassAnalyzer(ClassInfo clazz, ImportHandler imports)
    {
	this(null, clazz, imports);
    }

    public FieldAnalyzer getField(String fieldName, Type fieldType) {
        for (int i=0; i< fields.length; i++) {
	    if (fields[i].getName().equals(fieldName)
		&& fields[i].getType().equals(fieldType))
		return fields[i];
        }
	throw new NoSuchElementException
	    ("Field "+fieldType+" "+clazz.getName()+"."+fieldName);
    }
    
    public MethodAnalyzer getMethod(String methodName, MethodType methodType) {
        for (int i=0; i< methods.length; i++) {
	    if (methods[i].getName().equals(methodName)
		&& methods[i].getType().equals(methodType))
		return methods[i];
        }
	throw new NoSuchElementException
	    ("Method "+methodType+" "+clazz.getName()+"."+methodName);
    }
    
    public Object getParent() {
        return parent;
    }

    public ClassInfo getClazz() {
        return clazz;
    }

    public void analyze() {
        FieldInfo[] finfos = clazz.getFields();
        MethodInfo[] minfos = clazz.getMethods();
	InnerClassInfo[] innerInfos = clazz.getInnerClasses();

        if (finfos == null) {
            /* This means that the class could not be loaded.
             * give up.
             */
            return;
        }

	if ((Decompiler.options & Decompiler.OPTION_INNER) != 0
	    && innerInfos != null) {
	    int innerCount = innerInfos.length;
	    inners = new ClassAnalyzer[innerCount];
	    for (int i=0; i < innerCount; i++) {
		inners[i] = new ClassAnalyzer
		    (this, ClassInfo.forName(innerInfos[i].inner), imports);
	    }
	} else
	    inners = new ClassAnalyzer[0];

	fields = new FieldAnalyzer[finfos.length];
	methods = new MethodAnalyzer[minfos.length];
        for (int j=0; j < finfos.length; j++)
            fields[j] = new FieldAnalyzer(this, finfos[j], imports);

        staticConstructor = null;
        java.util.Vector constrVector = new java.util.Vector();
        for (int j=0; j < methods.length; j++) {
            methods[j] = new MethodAnalyzer(this, minfos[j], imports);

            if (methods[j].isConstructor()) {
                if (methods[j].isStatic())
                    staticConstructor = methods[j];
                else
                    constrVector.addElement(methods[j]);
            }
        }
	// First analyze fields
        for (int j=0; j < fields.length; j++)
	    fields[j].analyze();

	// now analyze constructors:
        constructors = new MethodAnalyzer[constrVector.size()];
        if (constructors.length > 0) {
            constrVector.copyInto(constructors);
	    for (int j=0; j< constructors.length; j++)
		constructors[j].analyze();
            TransformConstructors.transform
		(this, false, parent instanceof ClassAnalyzer, 
		 parent instanceof ConstructorOperator, constructors);
        }
        if (staticConstructor != null) {
	    staticConstructor.analyze();
            TransformConstructors.transform(this, true, false, false,
					    new MethodAnalyzer[]
					    { staticConstructor });
	}

	// Now analyze remaining methods.
        for (int j=0; j < methods.length; j++) {
	    if (!methods[j].isConstructor())
		methods[j].analyze();
	}
	// Now analyze the inner classes.
	for (int j=0; j < inners.length; j++)
	    inners[j].analyze();

	imports.useClass(clazz);
        if (clazz.getSuperclass() != null)
            imports.useClass(clazz.getSuperclass());
        ClassInfo[] interfaces = clazz.getInterfaces();
        for (int j=0; j< interfaces.length; j++)
            imports.useClass(interfaces[j]);
    }

    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

    public void dumpSource(TabbedPrintWriter writer) throws java.io.IOException
    {
        if (fields == null) {
            /* This means that the class could not be loaded.
             * give up.
             */
            return;
        }
	writer.pushScope(this);
	int modifiedModifiers = clazz.getModifiers() & ~Modifier.SYNCHRONIZED;
	if (clazz.isInterface())
	    modifiedModifiers &= ~Modifier.ABSTRACT;
        String modif = Modifier.toString(modifiedModifiers);
        if (modif.length() > 0)
            writer.print(modif + " ");
	/*interface is in modif*/
	if (!clazz.isInterface())
	    writer.print("class ");
	if (parent != null && name == null) {
	    /* This is an anonymous class */
	    ClassInfo superClazz = clazz.getSuperclass();
	    ClassInfo[] interfaces = clazz.getInterfaces();
	    if (interfaces.length == 1
		&& (superClazz == null
		    || superClazz == ClassInfo.javaLangObject)) {
		writer.print(writer.getClassString(interfaces[0]));
	    } else {
		if (interfaces.length > 0) {
		    writer.print("/*too many supers*/ ");
		    for (int i=0; i< interfaces.length; i++)
			writer.print(writer.getClassString(interfaces[i])+",");
		}
		if (superClazz == null)
		    writer.print(writer.getClassString
				 (ClassInfo.javaLangObject));
		else
		    writer.print(writer.getClassString(superClazz));
	    }
	} else {
	    writer.println(name);
	    writer.tab();
	    ClassInfo superClazz = clazz.getSuperclass();
	    if (superClazz != null && 
		superClazz != ClassInfo.javaLangObject) {
		writer.println("extends "+writer.getClassString(superClazz));
	    }
	    ClassInfo[] interfaces = clazz.getInterfaces();
	    if (interfaces.length > 0) {
		writer.print(clazz.isInterface() ? "extends " : "implements ");
		for (int i=0; i < interfaces.length; i++) {
		    if (i > 0)
			writer.print(", ");
		    writer.print(writer.getClassString(interfaces[i]));
		}
		writer.println("");
	    }
	    writer.untab();
	}
	writer.openBrace();
	writer.tab();
	
	for (int i=0; i< fields.length; i++)
	    fields[i].dumpSource(writer);
	for (int i=0; i< inners.length; i++) {
	    writer.println("");
	    inners[i].dumpSource(writer);
	}
	for (int i=0; i< methods.length; i++)
	    methods[i].dumpSource(writer);
	writer.untab();
	if (parent != null && name == null) {
	    /* This is an anonymous class */
	    writer.closeBraceNoSpace();
	} else
	    writer.closeBrace();
	writer.popScope();
    }

    public String getTypeString(Type type) {
        return type.toString();
    }

    public String getTypeString(Type type, String name) {
        return type.toString() + " " + name;
    }
}
