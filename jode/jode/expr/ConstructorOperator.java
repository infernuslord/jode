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

import java.lang.reflect.Modifier;
///#ifdef JDK12
///import java.util.Set;
///#else
import jode.util.SimpleSet;
///#endif

public class ConstructorOperator extends Operator 
    implements MatchableOperator {
    MethodType methodType;
    Type classType;
    CodeAnalyzer codeAnalyzer;
    boolean removedCheckNull = false;

    public ConstructorOperator(Type classType, MethodType methodType, 
			       CodeAnalyzer codeAna, boolean isVoid) {
        super(isVoid ? Type.tVoid : classType, 0);
        this.classType = classType;
        this.methodType = methodType;
	this.codeAnalyzer = codeAna;
	initOperands(methodType.getParameterTypes().length);
	checkAnonymousClasses();
    }

    public ConstructorOperator(Reference ref, CodeAnalyzer codeAna,
			       boolean isVoid) {
	this (Type.tType(ref.getClazz()), Type.tMethod(ref.getType()),
	      codeAna, isVoid);
    }

    public ConstructorOperator(InvokeOperator invoke, boolean isVoid) {
	this (invoke.getClassType(), invoke.getMethodType(),
	      invoke.codeAnalyzer, isVoid);
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
	if (outer != null && outer.outer != null && outer.name != null
	    && !Modifier.isStatic(outer.modifiers)
	    && (Decompiler.options & Decompiler.OPTION_INNER) != 0) {
	    if (subExpressions.length == 0) {
		System.err.println("outer: "+outer.outer+","+outer.inner+","+outer.name);
	    }

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
	if (outer != null && (outer.outer == null || outer.name == null)) {
	    ClassInfo clazz = getClassInfo();
	    codeAnalyzer.addAnonymousConstructor(this);

	    if (outer.name == null) {
		if (clazz.getInterfaces().length > 0)
		    type = Type.tClass(clazz.getInterfaces()[0]);
		else
		    type = Type.tClass(clazz.getSuperclass());
	    }
	}
    }

///#ifdef JDK12
///    public void fillDeclarables(Set used) {
///#else
    public void fillDeclarables(SimpleSet used) {
///#endif
	if ((Decompiler.options & Decompiler.OPTION_ANON) == 0)
	    return;
	InnerClassInfo outer = getOuterClassInfo();
	if (outer != null && outer.outer == null && outer.name != null) {
	    ClassAnalyzer anonymousClass 
		= codeAnalyzer.getAnonymousClass(getClassInfo());
	    used.add(anonymousClass);
	}
    }

    public void dumpExpression(TabbedPrintWriter writer)
	throws java.io.IOException {

	int arg = 0;
	Type object = classType;
	InnerClassInfo outer = getOuterClassInfo();
	if (outer != null && outer.outer != null && outer.name != null
	    && !Modifier.isStatic(outer.modifiers)
	    && (Decompiler.options & Decompiler.OPTION_INNER) != 0) {
	    if (subExpressions.length == 0) {
		System.err.println("outer: "+outer.outer+","+outer.inner+","+outer.name);
	    }

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
		    writer.printType(Type.tClass
				     (ClassInfo.forName(outer.outer)));
		    writer.print(") ");
		    outExpr.dumpExpression(writer, 700);
		    writer.print(")");
		} else 
		    outExpr.dumpExpression(writer, 950);
		writer.print(".");
	    }
	}

	ClassAnalyzer anonymousClass = null;
	boolean dumpBlock = false;
	if ((Decompiler.options & Decompiler.OPTION_ANON) != 0
	    && codeAnalyzer.hasAnalyzedAnonymous()
	    && outer != null && (outer.outer == null || outer.name == null)) {
	    anonymousClass = codeAnalyzer.getAnonymousClass(getClassInfo());
	    if (anonymousClass.getName() == null) {
		/* This is an anonymous class */
		ClassInfo clazz = anonymousClass.getClazz();
		ClassInfo superClazz = clazz.getSuperclass();
		ClassInfo[] interfaces = clazz.getInterfaces();
		if (interfaces.length == 1
		    && (superClazz == null
			|| superClazz == ClassInfo.javaLangObject)) {
		    object = Type.tClass(interfaces[0]);
		} else {
		    if (interfaces.length > 0) {
			writer.print("too many supers in ANONYMOUS ");
		    }
		    object = (superClazz != null 
			      ? Type.tClass(superClazz) : Type.tObject);
		}
		dumpBlock = true;
	    }
	    arg += anonymousClass.getOuterValues().length;
	}
	writer.print("new ");
	writer.printType(object);
	writer.print("(");
        for (int i = arg; i < methodType.getParameterTypes().length; i++) {
            if (i>arg)
		writer.print(", ");
            subExpressions[i].dumpExpression(writer, 0);
        }
        writer.print(")");
	if (dumpBlock) {
	    writer.openBrace();
	    writer.tab();
	    anonymousClass.dumpBlock(writer);
	    writer.untab();
	    writer.closeBraceNoSpace();
	}
    }
}

