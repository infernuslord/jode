/* ConstructorOperator Copyright (C) 1998-1999 Jochen Hoenicke.
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
import java.lang.reflect.Modifier;

import jode.type.Type;
import jode.type.NullType;
import jode.type.ClassInterfacesType;
import jode.type.MethodType;
import jode.bytecode.ClassInfo;
import jode.bytecode.InnerClassInfo;
import jode.bytecode.Reference;
import jode.Decompiler;
import jode.decompiler.CodeAnalyzer;
import jode.decompiler.ClassAnalyzer;
import jode.decompiler.TabbedPrintWriter;
import jode.decompiler.Scope;

public class ConstructorOperator extends Operator 
    implements MatchableOperator {
    MethodType methodType;
    Type classType;
    CodeAnalyzer codeAnalyzer;
    ClassAnalyzer anonymousClass = null;
    boolean removedCheckNull = false;

    public ConstructorOperator(Reference ref, CodeAnalyzer codeAna,
			       boolean isVoid) {
        super(isVoid ? Type.tVoid : Type.tType(ref.getClazz()), 0);
        this.classType = Type.tType(ref.getClazz());
        this.methodType = Type.tMethod(ref.getType());
	this.codeAnalyzer = codeAna;
	initOperands(methodType.getParameterTypes().length);
	checkAnonymousClasses();
    }

    public ConstructorOperator(InvokeOperator invoke, boolean isVoid) {
        super(isVoid ? Type.tVoid : invoke.getClassType(), 0);
        this.classType  = invoke.getClassType();
        this.methodType = invoke.getMethodType();
	this.codeAnalyzer = invoke.codeAnalyzer;
	initOperands(methodType.getParameterTypes().length);
	checkAnonymousClasses();
    }

    public MethodType getMethodType() {
        return methodType;
    }

    public CodeAnalyzer getCodeAnalyzer() {
	return codeAnalyzer;
    }

    /**
     * Checks if the value of the operator can be changed by this expression.
     */
    public boolean matches(Operator loadop) {
        return (loadop instanceof InvokeOperator
		|| loadop instanceof ConstructorOperator
		|| loadop instanceof GetFieldOperator);
    }

    public int getPriority() {
        return 950;
    }

    public Type getClassType() {
        return classType;
    }

    public void updateSubTypes() {
	Type[] paramTypes = methodType.getParameterTypes();
	for (int i=0; i < paramTypes.length; i++)
	    subExpressions[i].setType(Type.tSubType(paramTypes[i]));
    }

    public void updateType() {
    }

    public Expression simplifyStringBuffer() {
	if (getClassType() == Type.tStringBuffer) {
	    if (methodType.getParameterTypes().length == 0)
		return EMPTYSTRING;
	    if (methodType.getParameterTypes().length == 1
		&& methodType.getParameterTypes()[0].equals(Type.tString))
                return subExpressions[0].simplifyString();
        }
        return (getClassType() == Type.tStringBuffer)
            ? EMPTYSTRING : null;
    }

    public Expression simplify() {
	InnerClassInfo outer = getOuterClassInfo();
	if (outer != null && outer.outer != null
	    && !Modifier.isStatic(outer.modifiers)
	    && (Decompiler.options & Decompiler.OPTION_INNER) != 0) {
	    if (subExpressions[0] instanceof CheckNullOperator) {
		CheckNullOperator cno = (CheckNullOperator) subExpressions[0];
		cno.removeLocal();
		subExpressions[0] = cno.subExpressions[0];
		removedCheckNull = true;
	    }
	}
	return super.simplify();
    }

    public ClassInfo getClassInfo() {
	if (classType instanceof ClassInterfacesType) {
	    return ((ClassInterfacesType) classType).getClassInfo();
	}
	return null;
    }

    public InnerClassInfo getOuterClassInfo() {
	ClassInfo ci = getClassInfo();
	if (ci != null && ci.getName().indexOf('$') >= 0) {
	    InnerClassInfo[] outers = ci.getOuterClasses();
	    if (outers != null)
		return outers[0];
	}
	return null;
    }

    public void checkAnonymousClasses() {
	if ((Decompiler.options & Decompiler.OPTION_ANON) == 0)
	    return;
	InnerClassInfo outer = getOuterClassInfo();
	if (outer != null && outer.outer == null) {
	    ClassInfo clazz = getClassInfo();
	    anonymousClass = codeAnalyzer.addAnonymousClass(clazz);
	    
	    if (anonymousClass.getName() == null) {
		if (clazz.getInterfaces().length > 0)
		    type = Type.tClass(clazz.getInterfaces()[0]);
		else
		    type = Type.tClass(clazz.getSuperclass());
	    }
	}
    }

    public void dumpExpression(TabbedPrintWriter writer)
	throws java.io.IOException {

	int arg = 0;
	if (anonymousClass != null) {
	    writer.print("new ");
	    anonymousClass.dumpSource(writer);
	    return;
	}
	InnerClassInfo outer = getOuterClassInfo();
	if (outer != null && outer.outer != null
	    && !Modifier.isStatic(outer.modifiers)
	    && (Decompiler.options & Decompiler.OPTION_INNER) != 0) {
	    Expression outExpr = subExpressions[arg++];
	    if (!removedCheckNull && !(outExpr instanceof ThisOperator))
		writer.print("MISSING CHECKNULL");
	    if (outExpr instanceof ThisOperator) {
		Scope scope = writer.getScope
		    (((ThisOperator) outExpr).getClassInfo(), 
		     Scope.CLASSSCOPE);
		if (writer.conflicts(outer.name, scope, Scope.CLASSNAME)) {
		    outExpr.dumpExpression(writer, 950);
		    writer.print(".");
		}
	    } else {
		int minPriority = 950; /* field access */
		if (outExpr.getType() instanceof NullType) {
		    writer.print("((");
		    writer.printType(classType);
		    writer.print(") ");
		    outExpr.dumpExpression(writer, 700);
		    writer.print(")");
		} else 
		    outExpr.dumpExpression(writer, 950);
		writer.print(".");
	    }
	}
	writer.print("new ");
	if (outer != null && outer.name != null)
	    writer.print(outer.name);
	else
	    writer.printType(classType);
	writer.print("(");
        for (int i = arg; i < methodType.getParameterTypes().length; i++) {
            if (i>arg)
		writer.print(", ");
            subExpressions[i].dumpExpression(writer, 0);
        }
        writer.print(")");
    }
}
