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
        this.methodType = (MethodType) Type.tType(reference.getType());
        this.methodName = reference.getName();
        this.classType = Type.tType(reference.getClazz());
        this.type = methodType.getReturnType();
        this.codeAnalyzer  = codeAnalyzer;
	this.staticFlag = staticFlag;
        this.specialFlag = specialFlag;
        if (staticFlag)
            codeAnalyzer.useType(classType);
    }

    /**
     * Checks if the value of the given expression can change, due to
     * side effects in this expression.  If this returns false, the 
     * expression can safely be moved behind the current expresion.
     * @param expr the expression that should not change.
     */
    public boolean hasSideEffects(Expression expr) {
	return expr.containsConflictingLoad(this);
    }

    /**
     * Checks if the value of the operator can be changed by this expression.
     */
    public boolean matches(Operator loadop) {
        return (loadop instanceof InvokeOperator
		|| loadop instanceof ConstructorOperator
		|| loadop instanceof GetFieldOperator);
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

    public int getOperandCount() {
        return (isStatic()?0:1) 
            + methodType.getParameterTypes().length;
    }

    public Type getOperandType(int i) {
        if (!isStatic()) {
            if (i == 0)
                return getClassType();
            i--;
        }
        return methodType.getParameterTypes()[i];
    }

    public void setOperandType(Type types[]) {
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

    public ClassInfo getOuterClass() {
	ClassInfo clazz = getClassInfo();
	if (clazz != null) {
	    InnerClassInfo[] outers = clazz.getOuterClasses();
	    if (outers != null && outers[0].outer != null
		&& (Decompiler.options & Decompiler.OPTION_INNER) != 0)
		return ClassInfo.forName(outers[0].outer);
	}
	return null;
    }

    /**
     * Checks, whether this is a call of a method from this class or an
     * outer instance.
     */
    public boolean isOuter() {
	if (classType instanceof ClassInterfacesType) {
	    ClassAnalyzer ana = codeAnalyzer.getClassAnalyzer();
	    while (true) {
		if (((ClassInterfacesType) classType).getClassInfo() 
		    == ana.getClazz())
		    return true;
		if (ana.getParent() instanceof CodeAnalyzer
		    && (Decompiler.options & Decompiler.OPTION_ANON) != 0)
		    ana = ((CodeAnalyzer) ana.getParent())
			.getClassAnalyzer();
		else if (ana.getParent() instanceof ConstructorOperator
			 && (Decompiler.options & Decompiler.OPTION_ANON) != 0)
		    ana = ((ConstructorOperator) ana.getParent())
			.getCodeAnalyzer().getClassAnalyzer();
		else if (ana.getParent() instanceof ClassAnalyzer
			 && (Decompiler.options 
			     & Decompiler.OPTION_INNER) != 0)
		    ana = (ClassAnalyzer) ana.getParent();
		else
		    return false;
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
		if (ana.getParent() instanceof MethodAnalyzer)
		    ana = ((MethodAnalyzer) ana.getParent())
			.getClassAnalyzer();
		else if (ana.getParent() instanceof ClassAnalyzer)
		    ana = (ClassAnalyzer) ana.getParent();
		else
		    return null;
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

    public void dumpExpression(TabbedPrintWriter writer, 
			       Expression[] operands) 
	throws java.io.IOException {
	boolean opIsThis = !staticFlag && operands[0] instanceof ThisOperator;
        int arg = 1;

	if (isConstructor()) {
	    ClassInfo outer = getOuterClass();
	    if (outer != null) {
		operands[arg++].dumpExpression(writer, 0);
		writer.print(".");
	    }
	}
	if (specialFlag) {
	    if (opIsThis
		&& (((ThisOperator)operands[0]).getClassInfo()
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
		operands[0].dumpExpression(writer, minPriority);
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
		ThisOperator thisOp = (ThisOperator) operands[0];
		Scope scope = writer.getScope(thisOp.getClassInfo(),
					      Scope.CLASSSCOPE);
		if (writer.conflicts(methodName, scope, Scope.METHODNAME)) {
		    thisOp.dumpExpression(writer, 950);
		    writer.print(".");
		}
	    } else {
		if (operands[0].getType() instanceof NullType) {
		    writer.print("((");
		    writer.printType(classType);
		    writer.print(") ");
		    operands[0].dumpExpression(writer, 700);
		    writer.print(")");
		} else 
		    operands[0].dumpExpression(writer, 950);
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
	while (arg < operands.length) {
            if (!first)
		writer.print(", ");
	    else
		first = false;
            operands[arg++].dumpExpression(writer, 0);
        }
        writer.print(")");
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
	if (!isThis() || !isStatic()
	    || methodType.getParameterTypes().length != 1
	    || !methodType.getParameterTypes()[0].equals(Type.tString)
	    || !methodType.getReturnType().equals(Type.tString))
	    return null;
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

    public Expression simplifyAccess(Expression[] subs) {
	if (isOuter()) {
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
		    op = new PutFieldOperator(codeAnalyzer, false,
					      synth.getReference());
		    break;
		case SyntheticAnalyzer.ACCESSPUTSTATIC:
		    op = new PutFieldOperator(codeAnalyzer, true,
					      synth.getReference());
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
		    if (subs == null)
			return op;
		    return new ComplexExpression(op, subs);
		}
	    }
	}
	return null;
    }

    public Expression simplify() {
	Expression expr = simplifyAccess(null);
	if (expr != null)
	    return expr.simplify();
	return super.simplify();
    }


    /* Invokes never equals: they may return different values even if
     * they have the same parameters.
     */
}
