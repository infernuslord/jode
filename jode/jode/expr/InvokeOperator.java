/* InvokeOperator Copyright (C) 1998-1999 Jochen Hoenicke.
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

import jode.Decompiler;
import jode.decompiler.CodeAnalyzer;
import jode.decompiler.MethodAnalyzer;
import jode.decompiler.ClassAnalyzer;
import jode.decompiler.TabbedPrintWriter;
import jode.GlobalOptions;
import jode.bytecode.*;
import jode.jvm.*;
import jode.type.*;
import java.lang.reflect.InvocationTargetException;
import jode.decompiler.Scope;

public final class InvokeOperator extends Operator 
    implements MatchableOperator {
    CodeAnalyzer codeAnalyzer;
    boolean staticFlag;
    boolean specialFlag;
    MethodType methodType;
    String methodName;
    Type classType;

    public InvokeOperator(CodeAnalyzer codeAnalyzer,
			  boolean staticFlag, boolean specialFlag, 
			  Reference reference) {
        super(Type.tUnknown, 0);
        this.methodType = Type.tMethod(reference.getType());
        this.methodName = reference.getName();
        this.classType = Type.tType(reference.getClazz());
        this.type = methodType.getReturnType();
        this.codeAnalyzer  = codeAnalyzer;
	this.staticFlag = staticFlag;
        this.specialFlag = specialFlag;
        if (staticFlag)
            codeAnalyzer.useType(classType);
	initOperands((staticFlag ? 0 : 1) 
		     + methodType.getParameterTypes().length);
    }

    public final boolean isStatic() {
        return staticFlag;
    }

    public MethodType getMethodType() {
        return methodType;
    }

    public String getMethodName() {
        return methodName;
    }

    public Type getClassType() {
        return classType;
    }

    public int getPriority() {
        return 950;
    }

    public void updateSubTypes() {
	int offset = 0;
        if (!isStatic()) {
	    subExpressions[offset++].setType(Type.tSubType(getClassType()));
        }
	Type[] paramTypes = methodType.getParameterTypes();
	for (int i=0; i < paramTypes.length; i++)
	    subExpressions[offset++].setType(Type.tSubType(paramTypes[i]));
    }
    public void updateType() {
    }

    public boolean isConstructor() {
        return methodName.equals("<init>");
    }

    public ClassInfo getClassInfo() {
	if (classType instanceof ClassInterfacesType)
	    return ((ClassInterfacesType) classType).getClassInfo();
	return null;
    }

    /**
     * Checks, whether this is a call of a method from this class.
     * @XXX check, if this class implements the method and if not
     * allow super class
     */
    public boolean isThis() {
	return getClassInfo() == codeAnalyzer.getClazz();
    }

    public InnerClassInfo getOuterClassInfo() {
	ClassInfo clazz = getClassInfo();
	if (clazz != null) {
	    InnerClassInfo[] outers = clazz.getOuterClasses();
	    if (outers != null)
		return outers[0];
	}
	return null;
    }

    /**
     * Checks, whether this is a call of a method from this class or an
     * outer instance.
     */
    public boolean isOuter() {
	if (classType instanceof ClassInterfacesType) {
	    ClassInfo clazz = ((ClassInterfacesType) classType).getClassInfo();
	    ClassAnalyzer ana = codeAnalyzer.getClassAnalyzer();
	    while (true) {
		if (clazz == ana.getClazz())
		    return true;
		if (ana.getParent() == null)
		    break;
		if (ana.getParent() instanceof CodeAnalyzer
		    && (Decompiler.options & Decompiler.OPTION_ANON) != 0)
		    ana = ((CodeAnalyzer) ana.getParent())
			.getClassAnalyzer();
		else if (ana.getParent() instanceof ClassAnalyzer
			 && (Decompiler.options 
			     & Decompiler.OPTION_INNER) != 0)
		    ana = (ClassAnalyzer) ana.getParent();
		else 
		    throw new jode.AssertError
			("Unknown parent: "+ana+": "+ana.getParent());
	    }
	}
	return false;
    }

    public MethodAnalyzer getMethodAnalyzer() {
	if (classType instanceof ClassInterfacesType) {
	    ClassAnalyzer ana = codeAnalyzer.getClassAnalyzer();
	    while (true) {
		if (((ClassInterfacesType) classType).getClassInfo() 
		    == ana.getClazz()) {
		    return ana.getMethod(methodName, methodType);
		}
		if (ana.getParent() == null)
		    return null;
		if (ana.getParent() instanceof CodeAnalyzer)
		    ana = ((CodeAnalyzer) ana.getParent())
			.getClassAnalyzer();
		else if (ana.getParent() instanceof ClassAnalyzer)
		    ana = (ClassAnalyzer) ana.getParent();
		else 
		    throw new jode.AssertError("Unknown parent");
	    }
	}
	return null;
    }

    /**
     * Checks, whether this is a call of a method from the super class.
     * @XXX check, if its the first super class that implements the method.
     */
    public boolean isSuperOrThis() {
	if (classType instanceof ClassInterfacesType) {
	    return ((ClassInterfacesType) classType).getClassInfo()
		.superClassOf(codeAnalyzer.getClazz());
	}
	return false;
    }

    /**
     * Checks if the value of the operator can be changed by this expression.
     */
    public boolean matches(Operator loadop) {
        return (loadop instanceof InvokeOperator
		|| loadop instanceof ConstructorOperator
		|| loadop instanceof GetFieldOperator);
    }

    /**
     * Checks if the method is the magic class$ method.
     * @return true if this is the magic class$ method, false otherwise.
     */
    public boolean isGetClass() {
	if (isThis()) {
	    SyntheticAnalyzer synth = getMethodAnalyzer().getSynthetic();
	    if (synth != null && synth.getKind() == SyntheticAnalyzer.GETCLASS)
		return true;
	}
	return false;
    }

    class Environment extends SimpleRuntimeEnvironment {

	public Object invokeMethod(Reference ref, boolean isVirtual, 
				   Object cls, Object[] params) 
	    throws InterpreterException, InvocationTargetException {
	    if (ref.getClazz().equals
		("L"+codeAnalyzer.getClazz().getName().replace('.','/')+";")) {
		MethodType mt = (MethodType) Type.tType(ref.getType());
		BytecodeInfo info = codeAnalyzer.getClassAnalyzer()
		    .getMethod(ref.getName(), mt).getCode().getBytecodeInfo();
		Value[] locals = new Value[info.getMaxLocals()];
		for (int i=0; i< locals.length; i++)
		    locals[i] = new Value();
		int param = params.length;
		int slot = 0;
		if (cls != null)
		    locals[slot++].setObject(cls);
		for (int i = 0; i < param; i++) {
		    locals[slot].setObject(params[i]);
		    slot += mt.getParameterTypes()[i].stackSize();
		}
		return Interpreter.interpretMethod(this, info, locals); 
	    } else
		return super.invokeMethod(ref, isVirtual, cls, params);
	}
    }

    public ConstOperator deobfuscateString(ConstOperator op) {
	ClassAnalyzer clazz = codeAnalyzer.getClassAnalyzer();
	CodeAnalyzer ca = clazz.getMethod(methodName, methodType).getCode();
	if (ca == null)
	    return null;
	Environment env = new Environment();
	BytecodeInfo info = ca.getBytecodeInfo();
	Value[] locals = new Value[info.getMaxLocals()];
	for (int i=0; i< locals.length; i++)
	    locals[i] = new Value();
	locals[0].setObject(op.getValue());
	String result;
	try {
	    result = (String) Interpreter.interpretMethod(env, info, locals);
	} catch (InterpreterException ex) {
	    GlobalOptions.err.println("Warning: Can't interpret method "
				   +methodName);
	    ex.printStackTrace(GlobalOptions.err);
	    return null;
	} catch (InvocationTargetException ex) {
	    GlobalOptions.err.println("Warning: Interpreted method throws"
				   +" an uncaught exception: ");
	    ex.getTargetException().printStackTrace(GlobalOptions.err);
	    return null;
	}
	return new ConstOperator(result);
    }

    public Expression simplifyStringBuffer() {
        if (getClassType().equals(Type.tStringBuffer)
            && !isStatic() 
            && getMethodName().equals("append")
            && getMethodType().getParameterTypes().length == 1) {

            Expression e = subExpressions[0].simplifyStringBuffer();
            if (e == null)
                return null;
            
	    subExpressions[1] = subExpressions[1].simplifyString();

            if (e == EMPTYSTRING
		&& subExpressions[1].getType().isOfType(Type.tString))
                return subExpressions[1];
	    
	    if (e instanceof StringAddOperator
		&& ((Operator)e).getSubExpressions()[0] == EMPTYSTRING)
		e = ((Operator)e).getSubExpressions()[1];

	    Operator result = new StringAddOperator();
	    result.addOperand(subExpressions[1]);
	    result.addOperand(e);
	    return result;
        }
        return null;
    }

    public Expression simplifyString() {
	if (getMethodName().equals("toString")
	    && !isStatic()
	    && getClassType().equals(Type.tStringBuffer)
	    && subExpressions.length == 1) {
	    Expression simple = subExpressions[0].simplifyStringBuffer();
	    if (simple != null)
		return simple;
	}
	else if (getMethodName().equals("valueOf")
		 && isStatic() 
		 && getClassType().equals(Type.tString)
		 && subExpressions.length == 1) {
	    
	    if (subExpressions[0].getType().isOfType(Type.tString))
		return subExpressions[0];
	    
	    Operator op = new StringAddOperator();
	    op.addOperand(subExpressions[0]);
	    op.addOperand(EMPTYSTRING);
	}
	/* The pizza way (pizza is the compiler of kaffe) */
	else if (getMethodName().equals("concat")
		 && !isStatic()
		 && getClassType().equals(Type.tString)) {
	    
	    Expression result = new StringAddOperator();
	    Expression right = subExpressions[1].simplify();
	    if (right instanceof StringAddOperator) {
		Operator op = (Operator) right;
		if (op.subExpressions != null
		    && op.subExpressions[0] == EMPTYSTRING)
		    right = op.subExpressions[1];
	    }
	    result.addOperand(right);
	    result.addOperand(subExpressions[0].simplify());
	} 
	else if ((Decompiler.options & Decompiler.OPTION_DECRYPT) != 0
		 && isThis() && isStatic()
		 && methodType.getParameterTypes().length == 1
		 && methodType.getParameterTypes()[0].equals(Type.tString)
		 && methodType.getReturnType().equals(Type.tString)) {

	    Expression expr = subExpressions[0].simplifyString();
	    if (expr instanceof ConstOperator) {
		expr = deobfuscateString((ConstOperator)expr);
		if (expr != null)
		    return expr;
	    }
	}
        return this;
    }

    public Expression simplifyAccess() {
	if (getMethodAnalyzer() != null) {
	    SyntheticAnalyzer synth = getMethodAnalyzer().getSynthetic();
	    if (synth != null) {
		Operator op = null;
		switch (synth.getKind()) {
		case SyntheticAnalyzer.ACCESSGETFIELD:
		    op = new GetFieldOperator(codeAnalyzer, false,
					      synth.getReference());
		    break;
		case SyntheticAnalyzer.ACCESSGETSTATIC:
		    op = new GetFieldOperator(codeAnalyzer, true,
					      synth.getReference());
		    break;
		case SyntheticAnalyzer.ACCESSPUTFIELD:
		    op = new StoreInstruction
			(new PutFieldOperator(codeAnalyzer, false,
					      synth.getReference()));
		    break;
		case SyntheticAnalyzer.ACCESSPUTSTATIC:
		    op = new StoreInstruction
			(new PutFieldOperator(codeAnalyzer, true,
					      synth.getReference()));
		    break;
		case SyntheticAnalyzer.ACCESSMETHOD:
		    op = new InvokeOperator(codeAnalyzer, false, 
					    false, synth.getReference());
		    break;
		case SyntheticAnalyzer.ACCESSSTATICMETHOD:
		    op = new InvokeOperator(codeAnalyzer, true, 
					    false, synth.getReference());
		    break;
		}

		if (op != null) {
		    if (subExpressions != null) {
			for (int i=subExpressions.length; i-- > 0; )
			    op.addOperand(subExpressions[i]);
		    }
		    return op;
		}
	    }
	}
	return null;
    }

    public Expression simplify() {
	Expression expr = simplifyAccess();
	if (expr != null)
	    return expr.simplify();
	expr = simplifyString();
	if (expr != this)
	    return expr.simplify();
	return super.simplify();
    }


    /* Invokes never equals: they may return different values even if
     * they have the same parameters.
     */

    public void dumpExpression(TabbedPrintWriter writer)
	throws java.io.IOException {
	boolean opIsThis = !staticFlag
	    && subExpressions[0] instanceof ThisOperator;
        int arg = 1;

	if (isConstructor()) {
	    InnerClassInfo outer = getOuterClassInfo();
	    if (outer != null && outer.outer != null && outer.name != null
		&& !Modifier.isStatic(outer.modifiers)
		&& (Decompiler.options & Decompiler.OPTION_INNER) != 0) {
		Expression outerInstance = subExpressions[arg++];
		if (!(outerInstance instanceof ThisOperator)) {
		    outerInstance.dumpExpression(writer, 0);
		    writer.print(".");
		}
	    }
	}
	if (specialFlag) {
	    if (opIsThis
		&& (((ThisOperator)subExpressions[0]).getClassInfo()
		    == codeAnalyzer.getClazz())) {
		if (isThis()) {
		    /* XXX check if this is a private or final method. */
		} else {
		    /* XXX check that this is the first defined
		     * super method. */
		    writer.print("super");
		    opIsThis = false;
		}
	    } else {
		/* XXX check if this is a private or final method. */
		int minPriority = 950; /* field access */
		if (!isThis()) {
		    writer.print("(NON VIRTUAL ");
		    writer.printType(classType);
		    writer.print(")");
		    minPriority = 700;
		}
		subExpressions[0].dumpExpression(writer, minPriority);
	    }
	} else if (staticFlag) {
	    arg = 0;
	    Scope scope = writer.getScope(getClassInfo(),
					  Scope.CLASSSCOPE);
	    if (scope != null
		&& !writer.conflicts(methodName, scope, Scope.METHODNAME))
		opIsThis = true;
	    else
		writer.printType(classType);
	} else {
	    if (opIsThis) {
		ThisOperator thisOp = (ThisOperator) subExpressions[0];
		Scope scope = writer.getScope(thisOp.getClassInfo(),
					      Scope.CLASSSCOPE);
		if (writer.conflicts(methodName, scope, Scope.METHODNAME)) {
		    thisOp.dumpExpression(writer, 950);
		    writer.print(".");
		} else if (/* This is a inherited field conflicting
			    * with a field name in some outer class.
			    */
			   getMethodAnalyzer() == null 
			   && writer.conflicts(methodName, null,
					       Scope.METHODNAME)) {
		    writer.print("this.");
		}
	    } else {
		if (subExpressions[0].getType() instanceof NullType) {
		    writer.print("((");
		    writer.printType(classType);
		    writer.print(") ");
		    subExpressions[0].dumpExpression(writer, 700);
		    writer.print(")");
		} else 
		    subExpressions[0].dumpExpression(writer, 950);
	    }
	}

	if (isConstructor()) {
	    if (opIsThis)
		writer.print("this");
	} else {
	    if (!opIsThis)
		writer.print(".");
	    writer.print(methodName);
	}
	writer.print("(");
	boolean first = true;
	while (arg < subExpressions.length) {
            if (!first)
		writer.print(", ");
	    else
		first = false;
            subExpressions[arg++].dumpExpression(writer, 0);
        }
        writer.print(")");
    }
}
