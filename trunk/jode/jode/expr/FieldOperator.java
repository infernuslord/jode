/* FieldOperator Copyright (C) 1998-1999 Jochen Hoenicke.
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

package jode.expr;
import jode.GlobalOptions;
import jode.type.Type;
import jode.type.NullType;
import jode.type.ClassInterfacesType;
import jode.bytecode.FieldInfo;
import jode.bytecode.ClassInfo;
import jode.bytecode.Reference;
import jode.decompiler.MethodAnalyzer;
import jode.decompiler.ClassAnalyzer;
import jode.decompiler.MethodAnalyzer;
import jode.decompiler.FieldAnalyzer;
import jode.decompiler.Options;
import jode.decompiler.TabbedPrintWriter;
import jode.decompiler.Scope;

import java.io.IOException;
///#def COLLECTIONS java.util
import java.util.Collection;
///#enddef

/**
 * This class contains everything shared between PutFieldOperator and
 * GetFieldOperator
 */
public abstract class FieldOperator extends Operator {
    MethodAnalyzer methodAnalyzer;
    boolean staticFlag;
    Reference ref;
    Type classType;
    ClassInfo classInfo;
    String callerPackage;

    public FieldOperator(MethodAnalyzer methodAnalyzer, boolean staticFlag,
			 Reference ref) {
        super(Type.tType(ref.getType()));
        this.methodAnalyzer = methodAnalyzer;
        this.staticFlag = staticFlag;
        this.classType = Type.tType(ref.getClazz());
	this.ref = ref;
        if (staticFlag)
            methodAnalyzer.useType(classType);
	initOperands(staticFlag ? 0 : 1);

	callerPackage = methodAnalyzer.getClassAnalyzer().getClass().getName();
	int dot = callerPackage.lastIndexOf('.');
	callerPackage = callerPackage.substring(0, dot);
	if (classType instanceof ClassInterfacesType) {
	    classInfo = ((ClassInterfacesType) classType).getClassInfo();
	    if ((Options.options & Options.OPTION_ANON) != 0
		|| (Options.options & Options.OPTION_INNER) != 0) {
		try {
		    classInfo.load(ClassInfo.OUTERCLASS);
		} catch (IOException ex) {
		    classInfo.guess(ClassInfo.OUTERCLASS);
		}
	    }
	}
    }

    public int getPriority() {
        return 950;
    }

    public void updateSubTypes() {
	if (!staticFlag)
	    subExpressions[0].setType(Type.tSubType(classType));
    }

    public void updateType() {
	updateParentType(getFieldType());
    }

    public boolean isStatic() {
        return staticFlag;
    }

    public ClassInfo getClassInfo() {
	return classInfo;
    }

    /**
     * Returns the field analyzer for the field, if the field is
     * declared in the same class or some outer class as the method
     * containing this instruction.  Otherwise it returns null.
     * @return see above.  
     */
    public FieldAnalyzer getField() {
	ClassInfo clazz = classInfo;
	if (clazz != null) {
	    ClassAnalyzer ana = methodAnalyzer.getClassAnalyzer();
	    while (true) {
		if (clazz == ana.getClazz()) {
		    int field = ana.getFieldIndex
			(ref.getName(), Type.tType(ref.getType()));
		    if (field >= 0)
			return ana.getField(field);
		    return null;
		}
		if (ana.getParent() == null)
		    return null;
		if (ana.getParent() instanceof MethodAnalyzer)
		    ana = ((MethodAnalyzer) ana.getParent())
			.getClassAnalyzer();
		else if (ana.getParent() instanceof ClassAnalyzer)
		    ana = (ClassAnalyzer) ana.getParent();
		else 
		    throw new jode.AssertError("Unknown parent");
	    }
	}
	return null;
    }

    public String getFieldName() {
        return ref.getName();
    }

    public Type getFieldType() {
        return Type.tType(ref.getType());
    }

    private FieldInfo[] loadFields(ClassInfo clazz) {
	int howMuch = (clazz.getName().startsWith(callerPackage)
		       && (clazz.getName().lastIndexOf('.')
			   < callerPackage.length()))
	    ? ClassInfo.DECLARATIONS : ClassInfo.PUBLICDECLARATIONS;
	try {
	    clazz.load(howMuch);
	} catch (IOException ex) {
	    GlobalOptions.err.println("Warning: Can't find fields of "
				      +clazz+" to detect hiding conflicts");
	    clazz.guess(howMuch);
	}
	return clazz.getFields();
    }

    public boolean needsCast(Type type) {
	if (type instanceof NullType)
	    return true;
	if (!(type instanceof ClassInterfacesType
	      && classType instanceof ClassInterfacesType))
	    return false;
	
	ClassInfo clazz = ((ClassInterfacesType) classType).getClassInfo();
	ClassInfo parClazz = ((ClassInterfacesType) type).getClassInfo();
	while (clazz != parClazz && clazz != null) {
	    FieldInfo[] fields = parClazz.getFields();
	    for (int i = 0; i < fields.length; i++) {
		if (fields[i].getName().equals(ref.getName()))
		    return true;
	    }
	    parClazz = parClazz.getSuperclass();
	}
	return false;
    }

    /**
     * We add the named method scoped classes to the declarables.
     */
    public void fillDeclarables(Collection used) {
	ClassInfo clazz = getClassInfo();
	ClassAnalyzer clazzAna = methodAnalyzer.getClassAnalyzer(clazz);

	if ((Options.options & Options.OPTION_ANON) != 0
	    && clazz != null
	    && clazz.isMethodScoped() && clazz.getClassName() != null
	    && clazzAna != null
	    && clazzAna.getParent() == methodAnalyzer) {

	    /* This is a named method scope class, declare it.
	     * But first declare all method scoped classes,
	     * that are used inside; order does matter.
	     */
	    clazzAna.fillDeclarables(used);
	    used.add(clazzAna);
	}
	super.fillDeclarables(used);
    }

    public void dumpExpression(TabbedPrintWriter writer)
	throws java.io.IOException {
	boolean opIsThis = !staticFlag
	    && subExpressions[0] instanceof ThisOperator;
	String fieldName = ref.getName();
	if (staticFlag) {
	    if (!classType.equals(Type.tClass(methodAnalyzer.getClazz()))
		|| methodAnalyzer.findLocal(fieldName) != null) {
		writer.printType(classType);
		writer.breakOp();
		writer.print(".");
	    }
	    writer.print(fieldName);
	} else if (needsCast(subExpressions[0].getType().getCanonic())) {
	    writer.print("(");
	    writer.startOp(writer.EXPL_PAREN, 1);
	    writer.print("(");
	    writer.printType(classType);
	    writer.print(") ");
	    writer.breakOp();
	    subExpressions[0].dumpExpression(writer, 700);
	    writer.endOp();
	    writer.print(")");
	    writer.breakOp();
	    writer.print(".");
	    writer.print(fieldName);
	} else {
	    if (opIsThis) {
		ThisOperator thisOp = (ThisOperator) subExpressions[0];
		Scope scope = writer.getScope(thisOp.getClassInfo(),
					      Scope.CLASSSCOPE);

		if (scope == null || writer.conflicts(fieldName, scope, 
						      Scope.FIELDNAME)) {
		    thisOp.dumpExpression(writer, 950);
		    writer.breakOp();
		    writer.print(".");
		} else if (writer.conflicts(fieldName, scope, 
					    Scope.AMBIGUOUSNAME)
			   || (/* This is a inherited field conflicting
				* with a field name in some outer class.
				*/
			       getField() == null 
			       && writer.conflicts(fieldName, null,
						   Scope.NOSUPERFIELDNAME))) {

		    ClassAnalyzer ana = methodAnalyzer.getClassAnalyzer();
		    while (ana.getParent() instanceof ClassAnalyzer
			   && ana != scope)
			ana = (ClassAnalyzer) ana.getParent();
		    if (ana == scope)
			// For a simple outer class we can say this
			writer.print("this");
		    else {
			// For a class that owns a method that owns
			// us, we have to give the full class name
			thisOp.dumpExpression(writer, 950);
		    }
		    writer.breakOp();
		    writer.print(".");
		}
	    } else {
		subExpressions[0].dumpExpression(writer, 950);
		writer.breakOp();
		writer.print(".");
	    }
	    writer.print(fieldName);
	}
    }
}
