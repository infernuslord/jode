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
import jode.type.*;
import jode.bytecode.ClassInfo;
import jode.bytecode.FieldInfo;
import jode.bytecode.MethodInfo;
import jode.bytecode.ConstantPool;
import jode.expr.Expression;
import jode.flow.TransformConstructors;
import java.util.NoSuchElementException;
import java.lang.reflect.Modifier;

public class ClassAnalyzer implements Analyzer {
    ImportHandler imports;
    FieldAnalyzer[] fields;
    MethodAnalyzer[] methods;
    MethodAnalyzer staticConstructor;
    MethodAnalyzer[] constructors;

    ClassInfo clazz;
    ClassAnalyzer parent;
    
    public ClassAnalyzer(ClassAnalyzer parent, ClassInfo clazz, 
                         ImportHandler imports)
    {
        clazz.loadInfo(clazz.FULLINFO);
        this.parent = parent;
        this.clazz = clazz;
        this.imports = imports;
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
    
    public ClassInfo getClazz() {
        return clazz;
    }

    public void analyze() {
        int numFields = 0;
        int i = 0;
        
        FieldInfo[] finfos = clazz.getFields();
        MethodInfo[] minfos = clazz.getMethods();
        if (finfos == null) {
            /* This means that the class could not be loaded.
             * give up.
             */
            return;
        }

	fields = new FieldAnalyzer[finfos.length];
	methods = new MethodAnalyzer[minfos.length];
        for (int j=0; j < finfos.length; j++) {
            fields[j] = new FieldAnalyzer(this, finfos[j], imports);
            fields[j].analyze();
        }

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
	    // First analyze only synthetic methods.
	    if (methods[j].isSynthetic())
		methods[j].analyze();
        }
        for (int j=0; j < methods.length; j++) {
	    // Now analyze the remaining methods
	    if (!methods[j].isSynthetic())
		methods[j].analyze();
	}
        constructors = new MethodAnalyzer[constrVector.size()];
        if (constructors.length > 0) {
            constrVector.copyInto(constructors);
            TransformConstructors.transform(this, false, constructors);
        }
        if (staticConstructor != null)
            TransformConstructors.transform(this, true, new MethodAnalyzer[] 
                                            { staticConstructor });

	imports.useClass(clazz);
        if (clazz.getSuperclass() != null)
            imports.useClass(clazz.getSuperclass());
        ClassInfo[] interfaces = clazz.getInterfaces();
        for (int j=0; j< interfaces.length; j++)
            imports.useClass(interfaces[j]);
    }

    public void dumpSource(TabbedPrintWriter writer) throws java.io.IOException
    {
        if (fields == null) {
            /* This means that the class could not be loaded.
             * give up.
             */
            return;
        }
        String modif = Modifier.toString(clazz.getModifiers() 
                                         & ~Modifier.SYNCHRONIZED);
        if (modif.length() > 0)
            writer.print(modif + " ");
        writer.print(clazz.isInterface() 
                     ? ""/*interface is in modif*/ : "class ");
	writer.println(imports.getClassString(clazz));
	writer.tab();
        ClassInfo superClazz = clazz.getSuperclass();
	if (superClazz != null && 
            superClazz != ClassInfo.javaLangObject) {
	    writer.println("extends "+imports.getClassString(superClazz));
        }
        ClassInfo[] interfaces = clazz.getInterfaces();
	if (interfaces.length > 0) {
	    writer.print(clazz.isInterface() ? "extends " : "implements ");
	    for (int i=0; i < interfaces.length; i++) {
		if (i > 0)
		    writer.print(", ");
		writer.print(imports.getClassString(interfaces[i]));
	    }
            writer.println("");
	}
	writer.untab();
	writer.openBrace();
	writer.tab();

	for (int i=0; i< fields.length; i++)
	    fields[i].dumpSource(writer);
	for (int i=0; i< methods.length; i++)
	    methods[i].dumpSource(writer);
	writer.untab();
	writer.closeBrace();
    }

    public String getTypeString(Type type) {
        return type.toString();
    }

    public String getTypeString(Type type, String name) {
        return type.toString() + " " + name;
    }
}
