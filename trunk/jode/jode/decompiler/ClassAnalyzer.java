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
import jode.expr.ThisOperator;
import jode.expr.ConstructorOperator;
import jode.flow.TransformConstructors;

import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.Enumeration;
import java.lang.reflect.Modifier;

public class ClassAnalyzer 
    implements Analyzer, Scope, Declarable, ClassDeclarer 
{
    ImportHandler imports;
    ClassInfo clazz;
    ClassDeclarer parent;

    String name;
    FieldAnalyzer[] fields;
    MethodAnalyzer[] methods;
    ClassAnalyzer[] inners;
    int modifiers;

    TransformConstructors constrAna;
    MethodAnalyzer staticConstructor;
    MethodAnalyzer[] constructors;

    Expression[] outerValues;
    boolean jikesAnonymousInner = false;

    Vector ovListeners;

    public ClassAnalyzer(ClassDeclarer parent,
			 ClassInfo clazz, ImportHandler imports,
			 Expression[] outerValues)
    {
        clazz.loadInfo(clazz.FULLINFO);
        this.parent = parent;
        this.clazz = clazz;
        this.imports = imports;
	this.outerValues = outerValues;
	modifiers = clazz.getModifiers();
	name = clazz.getName();

	if (parent != null) {
	    InnerClassInfo[] outerInfos = clazz.getOuterClasses();
	    if (outerInfos[0].outer == null || outerInfos[0].name == null) {
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
	    modifiers = outerInfos[0].modifiers;
	} else {
	    name = clazz.getName();
	    int dot = name.lastIndexOf('.');
	    if (dot >= 0)
		name = name.substring(dot+1);
	}
    }

    public ClassAnalyzer(ClassDeclarer parent,
			 ClassInfo clazz, ImportHandler imports)
    {
	this(parent, clazz, imports, null);
    }

    public ClassAnalyzer(ClassInfo clazz, ImportHandler imports)
    {
	this(null, clazz, imports);
    }

    public final boolean isStatic() {
        return Modifier.isStatic(modifiers);
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
	return null;
    }
    
    public ClassDeclarer getParent() {
        return parent;
    }

    public void setParent(ClassDeclarer newParent) {
	this.parent = newParent;
    }

    public ClassInfo getClazz() {
        return clazz;
    }


    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

    public Expression[] getOuterValues() {
	return outerValues;
    }

    public void addOuterValueListener(OuterValueListener l) {
	if (ovListeners == null)
	    ovListeners = new Vector();
	ovListeners.addElement(l);
    }

    public void shrinkOuterValues(int newCount) {
	if (newCount >= outerValues.length)
	    return;
	Expression[] newOuter = new Expression[newCount];
	System.arraycopy(outerValues, 0, newOuter, 0, newCount);
	outerValues = newOuter;
	if (ovListeners != null) {
	    for (Enumeration enum = ovListeners.elements();
		 enum.hasMoreElements();)
		((OuterValueListener) enum.nextElement()
		 ).shrinkingOuterValues(this, newCount);
	}
    }

    /**
     * Jikes gives the outer class reference in an unusual place (as last
     * parameter) for anonymous classes that extends an inner (or method
     * scope) class.  This method tells if this is such a class.
     */
    public void setJikesAnonymousInner(boolean value) {
	jikesAnonymousInner = value;
    }

    /**
     * Jikes gives the outer class reference in an unusual place (as last
     * parameter) for anonymous classes that extends an inner (or method
     * scope) class.  This method tells if this is such a class.
     */
    public boolean isJikesAnonymousInner() {
	return jikesAnonymousInner;
    }

    public void analyze() {
	imports.useClass(clazz);
        if (clazz.getSuperclass() != null)
            imports.useClass(clazz.getSuperclass());
        ClassInfo[] interfaces = clazz.getInterfaces();
        for (int j=0; j< interfaces.length; j++)
            imports.useClass(interfaces[j]);

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
	    Expression[] outerThis = new Expression[] {
		new ThisOperator(clazz)
	    };

	    int innerCount = innerInfos.length;
	    inners = new ClassAnalyzer[innerCount];
	    for (int i=0; i < innerCount; i++) {
		ClassInfo ci = ClassInfo.forName(innerInfos[i].inner);
		inners[i] = new ClassAnalyzer
		    (this, ci, imports, 
		     Modifier.isStatic(innerInfos[i].modifiers)
		     ? null : outerThis);
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

	// now analyze constructors and synthetic fields:
	constrAna = null;
        constructors = new MethodAnalyzer[constrVector.size()];
	if (constructors.length > 0) {
            constrVector.copyInto(constructors);
	    for (int j=0; j< constructors.length; j++)
		constructors[j].analyze();
	    constrAna = new TransformConstructors(this, false, constructors);
	    constrAna.initSyntheticFields();
        }

	// Now analyze remaining methods.
        for (int j=0; j < methods.length; j++) {
	    if (!methods[j].isConstructor()
		&& !methods[j].isJikesConstructor)
		methods[j].analyze();
	}
    }

    public void analyzeInnerClasses() {
	// Now analyze the inner classes.
	for (int j=0; j < inners.length; j++) {
	    inners[j].analyze();
	    inners[j].analyzeInnerClasses();
	}

	// Now analyze the method scoped classes.
        for (int j=0; j < methods.length; j++)
	    methods[j].analyzeInnerClasses();

    }

    public void makeDeclaration() {
	// Finally anlyze the remaining field initializers.
	if (constrAna != null)
	    constrAna.transform();
        if (staticConstructor != null) {
	    staticConstructor.analyze();
            new TransformConstructors
		(this, true, new MethodAnalyzer[] { staticConstructor })
		.transform();
	}
        for (int j=0; j < fields.length; j++)
	    fields[j].makeDeclaration();
        for (int j=0; j < inners.length; j++)
	    inners[j].makeDeclaration();
        for (int j=0; j < methods.length; j++)
	    methods[j].makeDeclaration();
    }

    public void dumpDeclaration(TabbedPrintWriter writer)
        throws java.io.IOException
    {
	dumpSource(writer);
    }

    public void dumpBlock(TabbedPrintWriter writer) throws java.io.IOException
    {
	boolean needNewLine = false;
	for (int i=0; i< fields.length; i++) {
	    if (fields[i].skipWriting())
		continue;
	    fields[i].dumpSource(writer);
	    needNewLine = true;
	}
	for (int i=0; i< inners.length; i++) {
	    if (needNewLine)
		writer.println("");
	    inners[i].dumpSource(writer);
	    needNewLine = true;
	}
	for (int i=0; i< methods.length; i++) {
	    if (methods[i].skipWriting())
		continue;
	    if (needNewLine)
		writer.println("");
	    methods[i].dumpSource(writer);
	    needNewLine = true;
	}
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

	int modifiedModifiers = modifiers & ~Modifier.SYNCHRONIZED;
	if (clazz.isInterface())
	    modifiedModifiers &= ~Modifier.ABSTRACT;
	if (parent instanceof MethodAnalyzer) {
	    /* method scope classes are implicitly private */
	    modifiedModifiers &= ~Modifier.PRIVATE;
	    /* anonymous classes are implicitly final */
	    if (name == null)
		modifiedModifiers &= ~Modifier.FINAL;
	}
        String modif = Modifier.toString(modifiedModifiers);
        if (modif.length() > 0)
            writer.print(modif + " ");
	/*interface is in modif*/
	if (!clazz.isInterface())
	    writer.print("class ");
	writer.println(name);
	writer.tab();
	ClassInfo superClazz = clazz.getSuperclass();
	if (superClazz != null && 
	    superClazz != ClassInfo.javaLangObject) {
	    writer.println("extends " + (writer.getClassString
					 (superClazz, Scope.CLASSNAME)));
	}
	ClassInfo[] interfaces = clazz.getInterfaces();
	if (interfaces.length > 0) {
	    writer.print(clazz.isInterface() ? "extends " : "implements ");
	    for (int i=0; i < interfaces.length; i++) {
		if (i > 0)
		    writer.print(", ");
		writer.print(writer.getClassString
			     (interfaces[i], Scope.CLASSNAME));
	    }
	    writer.println("");
	}
	writer.untab();

	writer.openBrace();
	writer.tab();
	dumpBlock(writer);
	writer.untab();
	if (parent instanceof MethodAnalyzer) {
	    /* This is a method scope class */
	    writer.closeBraceNoSpace();
	} else
	    writer.closeBrace();
	writer.popScope();
    }

    public void dumpJavaFile(TabbedPrintWriter writer) 
	throws java.io.IOException {
	imports.init(clazz.getName());
	LocalInfo.init();
	analyze();
	analyzeInnerClasses();
	makeDeclaration();
	
	imports.dumpHeader(writer);
	dumpSource(writer);
    }

    public boolean isScopeOf(Object obj, int scopeType) {
	if (clazz.equals(obj) && scopeType == CLASSSCOPE)
	    return true;
	return false;
    }

    static int serialnr = 0;
    public void makeNameUnique() {
	name = name + "_" + serialnr++ + "_";
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

    /**
     * Get the class analyzer for the given class info.  This searches
     * the method scoped/anonymous classes in this method and all
     * outer methods and the outer classes for the class analyzer.
     * @param cinfo the classinfo for which the analyzer is searched.
     * @return the class analyzer, or null if there is not an outer
     * class that equals cinfo, and not a method scope/inner class in
     * an outer method.
     */
    public ClassAnalyzer getClassAnalyzer(ClassInfo cinfo) {
	if (cinfo == getClazz())
	    return this;
	if (parent == null)
	    return null;
	return getParent().getClassAnalyzer(cinfo);
    }

    public void addClassAnalyzer(ClassAnalyzer clazzAna) {
	if (parent != null)
	    parent.addClassAnalyzer(clazzAna);
    }
}
