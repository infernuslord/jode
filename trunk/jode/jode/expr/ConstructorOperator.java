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
import jode.decompiler.MethodAnalyzer;
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
    MethodAnalyzer methodAnalyzer;

    public ConstructorOperator(Type classType, MethodType methodType, 
			       MethodAnalyzer methodAna, boolean isVoid) {
        super(isVoid ? Type.tVoid : classType, 0);
        this.classType = classType;
        this.methodType = methodType;
	this.methodAnalyzer = methodAna;
	initOperands(methodType.getParameterTypes().length);
	checkAnonymousClasses();
    }

    public ConstructorOperator(Reference ref, MethodAnalyzer methodAna,
			       boolean isVoid) {
	this (Type.tType(ref.getClazz()), Type.tMethod(ref.getType()),
	      methodAna, isVoid);
    }

    public ConstructorOperator(InvokeOperator invoke, boolean isVoid) {
	this (invoke.getClassType(), invoke.getMethodType(),
	      invoke.methodAnalyzer, isVoid);
    }

    public MethodType getMethodType() {
        return methodType;
    }

    public MethodAnalyzer getMethodAnalyzer() {
	return methodAnalyzer;
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
	return super.simplify();
    }

    public ClassInfo getClassInfo() {
	if (classType instanceof ClassInterfacesType) {
	    return ((ClassInterfacesType) classType).getClassInfo();
	}
	return null;
    }

    public InnerClassInfo getOuterClassInfo(ClassInfo ci) {
	if (ci != null) {
	    InnerClassInfo[] outers = ci.getOuterClasses();
	    if (outers != null)
		return outers[0];
	}
	return null;
    }

    public void checkAnonymousClasses() {
	if ((Decompiler.options & Decompiler.OPTION_ANON) == 0)
	    return;
	ClassInfo clazz = getClassInfo();
	InnerClassInfo outer = getOuterClassInfo(clazz);
	if (outer != null && (outer.outer == null || outer.name == null)) {
	    methodAnalyzer.addAnonymousConstructor(this);

	    if (outer.name == null) {
		if (clazz.getInterfaces().length > 0)
		    type = Type.tClass(clazz.getInterfaces()[0]);
		else
		    type = Type.tClass(clazz.getSuperclass());
	    }
	}
    }

    /**
     * We add the named method scoped classes to the declarables, and
     * only fillDeclarables on the parameters we will print.
     */
///#ifdef JDK12
///    public void fillDeclarables(Set used) {
///#else
    public void fillDeclarables(SimpleSet used) {
///#endif
	ClassInfo clazz = getClassInfo();
	InnerClassInfo outer = getOuterClassInfo(clazz);
	ClassAnalyzer clazzAna = methodAnalyzer.getClassAnalyzer(clazz);
	int arg = 0;
	int length = subExpressions.length;
	boolean jikesAnonymousInner = false;

	if ((Decompiler.options & Decompiler.OPTION_ANON) != 0
	    && clazzAna != null
	    && outer != null && (outer.outer == null || outer.name == null)) {
	    arg += clazzAna.getOuterValues().length;
	    jikesAnonymousInner = clazzAna.isJikesAnonymousInner();

	    if (outer.name != null) {
		if (clazzAna.getParent() == methodAnalyzer)
		    /* This is a named method scope class, declare it */
		    used.add(clazzAna);
	    } else {
		/* This is an anonymous class */
		ClassInfo superClazz = clazz.getSuperclass();
		ClassInfo[] interfaces = clazz.getInterfaces();
		if (interfaces.length == 1
		    && (superClazz == null
			|| superClazz == ClassInfo.javaLangObject)) {
		    clazz = interfaces[0];
		} else {
		    clazz = (superClazz != null
			 ? superClazz : ClassInfo.javaLangObject);
		}
		outer = getOuterClassInfo(clazz);
	    }
	}
	if ((Decompiler.options & Decompiler.OPTION_INNER) != 0
	    && outer != null && outer.outer != null && outer.name != null
	    && !Modifier.isStatic(outer.modifiers)) {

	    Expression outerExpr = jikesAnonymousInner 
			   ? subExpressions[--length]
			   : subExpressions[arg++];
	    if (outerExpr instanceof CheckNullOperator) {
		CheckNullOperator cno = (CheckNullOperator) outerExpr;
		outerExpr = cno.subExpressions[0];
	    }
	    outerExpr.fillDeclarables(used);
	}
	for (int i=arg; i < length; i++)
	    subExpressions[i].fillDeclarables(used);
    }

    public void dumpExpression(TabbedPrintWriter writer)
	throws java.io.IOException {

	int arg = 0;
	int length = subExpressions.length;
	boolean jikesAnonymousInner = false;
	ClassInfo clazz = getClassInfo();
	InnerClassInfo outer = getOuterClassInfo(clazz);
	ClassAnalyzer clazzAna = methodAnalyzer.getClassAnalyzer(clazz);

	boolean dumpBlock = false;
	if ((Decompiler.options & 
	     (Decompiler.OPTION_ANON | Decompiler.OPTION_CONTRAFO)) != 0
	    && clazzAna != null
	    && outer != null && (outer.outer == null || outer.name == null)) {
	    arg += clazzAna.getOuterValues().length;
	    jikesAnonymousInner = clazzAna.isJikesAnonymousInner();

	    if (outer.name == null) {
		/* This is an anonymous class */
		ClassInfo superClazz = clazz.getSuperclass();
		ClassInfo[] interfaces = clazz.getInterfaces();
		if (interfaces.length == 1
		    && (superClazz == null
			|| superClazz == ClassInfo.javaLangObject)) {
		    clazz = interfaces[0];
		} else {
		    if (interfaces.length > 0) {
			writer.print("too many supers in ANONYMOUS ");
		    }
		    clazz = (superClazz != null
			     ? superClazz : ClassInfo.javaLangObject);
		}
		outer = getOuterClassInfo(clazz);
		dumpBlock = true;
		if (jikesAnonymousInner
		    && outer.outer == null && outer.name != null) {
		    Expression thisExpr = subExpressions[--length];
		    if (thisExpr instanceof CheckNullOperator) {
			CheckNullOperator cno = (CheckNullOperator) thisExpr;
			thisExpr = cno.subExpressions[0];
		    }
		    if (!(thisExpr instanceof ThisOperator)
			|| (((ThisOperator) thisExpr).getClassInfo() 
			    != methodAnalyzer.getClazz()))
			writer.print("ILLEGAL ANON CONSTR");
		}
	    }
	}

	if (outer != null && outer.outer != null && outer.name != null
	    && !Modifier.isStatic(outer.modifiers)
	    && (Decompiler.options & 
		(Decompiler.OPTION_INNER | Decompiler.OPTION_CONTRAFO)) != 0) {

	    Expression outerExpr = jikesAnonymousInner 
			   ? subExpressions[--length]
			   : subExpressions[arg++];
	    if (outerExpr instanceof CheckNullOperator) {
		CheckNullOperator cno = (CheckNullOperator) outerExpr;
		outerExpr = cno.subExpressions[0];
	    } else if (!(outerExpr instanceof ThisOperator)) {
		// Bug in jikes: it doesn't do a check null.
		// We don't complain here.
		if (!jikesAnonymousInner)
		    writer.print("MISSING CHECKNULL ");
	    }

	    if (outerExpr instanceof ThisOperator) {
		Scope scope = writer.getScope
		    (((ThisOperator) outerExpr).getClassInfo(), 
		     Scope.CLASSSCOPE);
		if (writer.conflicts(outer.name, scope, Scope.CLASSNAME)) {
		    outerExpr.dumpExpression(writer, 950);
		    writer.print(".");
		}
	    } else {
		if (outerExpr.getType() instanceof NullType) {
		    writer.print("((");
		    writer.printType(Type.tClass
				     (ClassInfo.forName(outer.outer)));
		    writer.print(") ");
		    outerExpr.dumpExpression(writer, 700);
		    writer.print(")");
		} else 
		    outerExpr.dumpExpression(writer, 950);
		writer.print(".");
	    }
	}

	writer.print("new ");
	writer.printType(Type.tClass(clazz));
	writer.print("(");
        for (int i = arg; i < length; i++) {
            if (i>arg)
		writer.print(", ");
            subExpressions[i].dumpExpression(writer, 0);
        }
        writer.print(")");
	if (dumpBlock) {
	    writer.openBrace();
	    writer.tab();
	    clazzAna.dumpBlock(writer);
	    writer.untab();
	    writer.closeBraceNoSpace();
	}
    }
}
