/* GetFieldOperator Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.decompiler.TabbedPrintWriter;
import jode.decompiler.Scope;

public class GetFieldOperator extends Operator {
    boolean staticFlag;
    MethodAnalyzer methodAnalyzer;
    Reference ref;
    Type classType;

    public GetFieldOperator(MethodAnalyzer methodAnalyzer, boolean staticFlag,
			    Reference ref) {
        super(Type.tType(ref.getType()), 0);
        this.methodAnalyzer = methodAnalyzer;
        this.staticFlag = staticFlag;
        this.classType = Type.tType(ref.getClazz());
	this.ref = ref;
        if (staticFlag)
            methodAnalyzer.useType(classType);
	initOperands(staticFlag ? 0 : 1);
    }

    public int getPriority() {
        return 950;
    }

    public void updateSubTypes() {
	if (!staticFlag)
	    subExpressions[0].setType(Type.tSubType(classType));
    }
    public void updateType() {
    }

    /**
     * Checks, whether this is a call of a method from this class or an
     * outer instance.
     */
    public boolean isOuter() {
	if (classType instanceof ClassInterfacesType) {
	    ClassInfo clazz = ((ClassInterfacesType) classType).getClassInfo();
	    ClassAnalyzer ana = methodAnalyzer.getClassAnalyzer();
	    while (true) {
		if (clazz == ana.getClazz())
		    return true;
		if (ana.getParent() instanceof MethodAnalyzer)
		    ana = ((MethodAnalyzer) ana.getParent())
			.getClassAnalyzer();
		else if (ana.getParent() instanceof ClassAnalyzer)
		    ana = (ClassAnalyzer) ana.getParent();
		else
		    return false;
	    }
	}
	return false;
    }

    public ClassInfo getClassInfo() {
	if (classType instanceof ClassInterfacesType)
	    return ((ClassInterfacesType) classType).getClassInfo();
	return null;
    }

    public FieldAnalyzer getField() {
	ClassInfo clazz = getClassInfo();
	if (clazz != null) {
	    ClassAnalyzer ana = methodAnalyzer.getClassAnalyzer();
	    while (true) {
		if (clazz == ana.getClazz()) {
		    return ana.getField(ref.getName(), 
					Type.tType(ref.getType()));
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

    public void dumpExpression(TabbedPrintWriter writer)
	throws java.io.IOException {
	boolean opIsThis = !staticFlag
	    && subExpressions[0] instanceof ThisOperator;
	String fieldName = ref.getName();
	if (staticFlag) {
	    if (!classType.equals(Type.tClass(methodAnalyzer.getClazz()))
		|| methodAnalyzer.findLocal(fieldName) != null) {
		writer.printType(classType);
		writer.print(".");
	    }
	    writer.print(fieldName);
	} else if (needsCast(subExpressions[0].getType().getCanonic())) {
	    writer.print("((");
	    writer.printType(classType);
	    writer.print(") ");
	    subExpressions[0].dumpExpression(writer, 700);
	    writer.print(").");
	    writer.print(fieldName);
	} else {
	    if (opIsThis) {
		ThisOperator thisOp = (ThisOperator) subExpressions[0];
		Scope scope = writer.getScope(thisOp.getClassInfo(),
					      Scope.CLASSSCOPE);

		if (scope == null || writer.conflicts(fieldName, scope, 
						      Scope.FIELDNAME)) {
		    thisOp.dumpExpression(writer, 950);
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
			writer.print("this.");
		    else {
			// For a class that owns a method that owns
			// us, we have to give the full class name
			thisOp.dumpExpression(writer, 950);
			writer.print(".");
		    }
		}
	    } else {
		subExpressions[0].dumpExpression(writer, 950);
		writer.print(".");
	    }
	    writer.print(fieldName);
	}
    }

    public Expression simplify() {
	if (!staticFlag) {
	    subExpressions[0] = subExpressions[0].simplify();
	    subExpressions[0].parent = this;
	    if (subExpressions[0] instanceof ThisOperator) {
		FieldAnalyzer field = getField();
		/* This should check for isFinal(), but sadly,
		 * sometimes jikes doesn't make a val$ field final.  I
		 * don't know when, or why, so I currently ignore
		 * isFinal.  
		 */
		if (field != null && field.isSynthetic()) {
		    Expression constant = field.getConstant();
		    if (constant instanceof ThisOperator
			|| constant instanceof OuterLocalOperator)
			return constant;
		}
	    }
	}
	return this;
    }

    public boolean opEquals(Operator o) {
	return o instanceof GetFieldOperator
	    && ((GetFieldOperator)o).ref.equals(ref);
    }
}
