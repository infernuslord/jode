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
import jode.decompiler.MethodAnalyzer;
import jode.decompiler.MethodAnalyzer;
import jode.decompiler.ClassAnalyzer;
import jode.decompiler.TabbedPrintWriter;
import jode.GlobalOptions;
import jode.bytecode.*;
import jode.jvm.*;
import jode.type.*;
import jode.decompiler.Scope;
import jode.util.SimpleMap;

import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
///#ifdef JDK12
///import java.util.Collections;
///import java.util.Collection;
///import java.util.Map;
///import java.util.Iterator;
///#else
import jode.util.Collections;
import jode.util.Collection;
import jode.util.Map;
import jode.util.Iterator;
///#endif

public final class InvokeOperator extends Operator 
    implements MatchableOperator {
    MethodAnalyzer methodAnalyzer;
    boolean staticFlag;
    boolean specialFlag;
    MethodType methodType;
    String methodName;
    Type classType;
    Type[] hints;

    /**
     * This hashtable contains hints for every library method.  Some
     * library method take or return an int, but it should be a char
     * instead.  We will remember that here to give them the right
     * hint.
     *
     * The key is the string: methodName + "." + methodType, the value
     * is a map: It maps base class types for which this hint applies,
     * to an array of hint types corresponding to the parameters: The
     * first element is the hint type of the return value, the
     * remaining entries are the hint types of the parameters.  All
     * hint types may be null, if that parameter shouldn't be hinted.  
     */
    private final static Hashtable hintTypes = new Hashtable();

    static {
	/* Fill the hint type hashtable.  For example, the first
	 * parameter of String.indexOf should be hinted as char, even
	 * though the formal parameter is an int.
	 * First hint is hint of return value (even if void)
	 * other hints are that of the parameters in order
	 *
	 * You only have to hint the base class.  Other classes will
	 * inherit the hints.
	 *
	 * We reuse a lot of objects, since they are all unchangeable
	 * this is no problem.  We only hint for chars; it doesn't
	 * make much sense to hint for byte, since its constant
	 * representation is more difficult than an int
	 * representation.  If you have more hints to suggest, please
	 * write contact me. (see GlobalOptions.EMAIL)
	 */
	Type tCharHint = new IntegerType(IntegerType.IT_I, IntegerType.IT_C);
	Type[] hintC   = new Type[] { tCharHint };
	Type[] hint0C  = new Type[] { null, tCharHint };
	Type[] hint0C0 = new Type[] { null, tCharHint, null };

	Map hintString0CMap = new SimpleMap
	    (Collections.singleton
	     (new SimpleMap.SimpleEntry(Type.tString, hint0C)));
	Map hintString0C0Map = new SimpleMap
	    (Collections.singleton
	     (new SimpleMap.SimpleEntry(Type.tString, hint0C0)));
	hintTypes.put("indexOf.(I)I", hintString0CMap);
	hintTypes.put("lastIndexOf.(I)I", hintString0CMap);
	hintTypes.put("indexOf.(II)I", hintString0C0Map);
	hintTypes.put("lastIndexOf.(II)I", hintString0C0Map);
	hintTypes.put("write.(I)V", new SimpleMap
		      (Collections.singleton
		       (new SimpleMap.SimpleEntry
			(Type.tClass("java.io.Writer"), hint0C))));
	hintTypes.put("read.()I", new SimpleMap
		      (Collections.singleton
		       (new SimpleMap.SimpleEntry
			(Type.tClass("java.io.Reader"), hintC))));
	hintTypes.put("unread.(I)V", new SimpleMap
		      (Collections.singleton
		       (new SimpleMap.SimpleEntry
			(Type.tClass("java.io.PushbackReader"), hint0C))));
    }


    public InvokeOperator(MethodAnalyzer methodAnalyzer,
			  boolean staticFlag, boolean specialFlag, 
			  Reference reference) {
        super(Type.tUnknown, 0);
        this.methodType = Type.tMethod(reference.getType());
        this.methodName = reference.getName();
        this.classType = Type.tType(reference.getClazz());
	this.hints = null;
	Map allHints = (Map) hintTypes.get(methodName+"."+methodType);
	if (allHints != null) {
	    for (Iterator i = allHints.entrySet().iterator(); i.hasNext();) {
		Map.Entry e = (Map.Entry) i.next();
		if (classType.isOfType(((Type)e.getKey()).getSubType())) {
		    this.hints = (Type[]) e.getValue();
		    break;
		}
	    }
	}
	if (hints != null && hints[0] != null)
	    this.type = hints[0];
	else
	    this.type = methodType.getReturnType();
        this.methodAnalyzer  = methodAnalyzer;
	this.staticFlag = staticFlag;
        this.specialFlag = specialFlag;
        if (staticFlag)
            methodAnalyzer.useType(classType);
	initOperands((staticFlag ? 0 : 1) 
		     + methodType.getParameterTypes().length);
	checkAnonymousClasses();
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

    public void checkAnonymousClasses() {
	if ((Decompiler.options & Decompiler.OPTION_ANON) == 0)
	    return;
	if (!isConstructor())
	    return;
	ClassInfo clazz = getClassInfo();
	InnerClassInfo outer = getOuterClassInfo(clazz);
	if (outer != null && (outer.outer == null || outer.name == null)) {
	    methodAnalyzer.addAnonymousConstructor(this);
	System.err.println("addAnonymousConstructor: "+this);
	}
    }

    public void updateSubTypes() {
	int offset = 0;
        if (!isStatic()) {
	    subExpressions[offset++].setType(Type.tSubType(getClassType()));
        }
	Type[] paramTypes = methodType.getParameterTypes();
	for (int i=0; i < paramTypes.length; i++) {
	    Type pType = (hints != null && hints[i+1] != null) 
		? hints[i+1] : paramTypes[i];
	    subExpressions[offset++].setType(Type.tSubType(pType));
	}
    }

    public void updateType() {
    }

    /**
     * Makes a non void expression out of this store instruction.
     */
    public void makeNonVoid() {
        if (type != Type.tVoid)
            throw new jode.AssertError("already non void");
	ClassInfo clazz = getClassInfo();
	InnerClassInfo outer = getOuterClassInfo(clazz);
	if (outer != null && outer.name == null) {
	    /* This is an anonymous class */
	    if (clazz.getInterfaces().length > 0)
		type = Type.tClass(clazz.getInterfaces()[0]);
	    else
		type = Type.tClass(clazz.getSuperclass());
	} else
	    type = subExpressions[0].getType();
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
	return getClassInfo() == methodAnalyzer.getClazz();
    }

    public InnerClassInfo getOuterClassInfo(ClassInfo ci) {
	if (ci != null) {
	    InnerClassInfo[] outers = ci.getOuterClasses();
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
	    ClassAnalyzer ana = methodAnalyzer.getClassAnalyzer();
	    while (true) {
		if (clazz == ana.getClazz())
		    return true;
		if (ana.getParent() == null)
		    break;
		if (ana.getParent() instanceof MethodAnalyzer
		    && (Decompiler.options & Decompiler.OPTION_ANON) != 0)
		    ana = ((MethodAnalyzer) ana.getParent())
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
	ClassInfo clazz = getClassInfo();
	if (clazz != null) {
	    ClassAnalyzer ana = methodAnalyzer.getClassAnalyzer();
	    while (true) {
		if (clazz == ana.getClazz()) {
		    return ana.getMethod(methodName, methodType);
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

    /**
     * Checks, whether this is a call of a method from the super class.
     * @XXX check, if its the first super class that implements the method.
     */
    public boolean isSuperOrThis() {
	ClassInfo clazz = getClassInfo();
	if (clazz != null) {
	    return clazz.superClassOf(methodAnalyzer.getClazz());
	}
	return false;
    }

    public boolean isConstant() {
	if ((Decompiler.options & Decompiler.OPTION_ANON) == 0)
	    return super.isConstant();

	ClassInfo clazz = getClassInfo();
	InnerClassInfo outer = getOuterClassInfo(clazz);
	ClassAnalyzer clazzAna = methodAnalyzer.getClassAnalyzer(clazz);
	if (clazzAna != null
	    && outer != null && outer.outer == null && outer.name != null
	    && clazzAna.getParent() == methodAnalyzer) {
	    /* This is a named method scope class, it needs
	     * declaration.  And therefore can't be moved into
	     * a field initializer. */
	    return false;
	}
	return super.isConstant();
    }

    /**
     * Checks if the value of the operator can be changed by this expression.
     */
    public boolean matches(Operator loadop) {
        return (loadop instanceof InvokeOperator
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
		("L"+methodAnalyzer.getClazz().getName().replace('.','/')+";")) {
		MethodType mt = (MethodType) Type.tType(ref.getType());
		BytecodeInfo info = methodAnalyzer.getClassAnalyzer()
		    .getMethod(ref.getName(), mt).getBytecodeInfo();
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
	ClassAnalyzer clazz = methodAnalyzer.getClassAnalyzer();
	MethodAnalyzer ma = clazz.getMethod(methodName, methodType);
	if (ma == null)
	    return null;
	Environment env = new Environment();
	BytecodeInfo info = ma.getBytecodeInfo();
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
	if (getClassType().equals(Type.tStringBuffer)) {
	    if (isConstructor() 
		&& subExpressions[0] instanceof NewOperator) {
		if (methodType.getParameterTypes().length == 0)
		    return EMPTYSTRING;
		if (methodType.getParameterTypes().length == 1
		    && methodType.getParameterTypes()[0].equals(Type.tString))
		    return subExpressions[1].simplifyString();
	    }

	    if (!isStatic() 
		&& getMethodName().equals("append")
		&& getMethodType().getParameterTypes().length == 1) {
		
		Expression firstOp = subExpressions[0].simplifyStringBuffer();
		if (firstOp == null)
		    return null;
		
		subExpressions[1] = subExpressions[1].simplifyString();
		
		if (firstOp == EMPTYSTRING
		    && subExpressions[1].getType().isOfType(Type.tString))
		    return subExpressions[1];
		
		if (firstOp instanceof StringAddOperator
		    && (((Operator)firstOp).getSubExpressions()[0]
			== EMPTYSTRING))
		    firstOp = ((Operator)firstOp).getSubExpressions()[1];
		
		Expression secondOp = subExpressions[1];
		Type[] paramTypes = new Type[] {
		    Type.tStringBuffer, secondOp.getType().getCanonic()
		};
		if (needsCast(1, paramTypes)) {
		    Type castType = methodType.getParameterTypes()[0];
		    Operator castOp = new ConvertOperator(castType, castType);
		    castOp.addOperand(secondOp);
		    secondOp = castOp;
		}
		Operator result = new StringAddOperator();
		result.addOperand(secondOp);
		result.addOperand(firstOp);
		return result;
	    }
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
		Expression op = null;
		switch (synth.getKind()) {
		case SyntheticAnalyzer.ACCESSGETFIELD:
		    op = new GetFieldOperator(methodAnalyzer, false,
					      synth.getReference());
		    break;
		case SyntheticAnalyzer.ACCESSGETSTATIC:
		    op = new GetFieldOperator(methodAnalyzer, true,
					      synth.getReference());
		    break;
		case SyntheticAnalyzer.ACCESSPUTFIELD:
		    op = new StoreInstruction
			(new PutFieldOperator(methodAnalyzer, false,
					      synth.getReference()));
		    break;
		case SyntheticAnalyzer.ACCESSPUTSTATIC:
		    op = new StoreInstruction
			(new PutFieldOperator(methodAnalyzer, true,
					      synth.getReference()));
		    break;
		case SyntheticAnalyzer.ACCESSMETHOD:
		    op = new InvokeOperator(methodAnalyzer, false, 
					    false, synth.getReference());
		    break;
		case SyntheticAnalyzer.ACCESSSTATICMETHOD:
		    op = new InvokeOperator(methodAnalyzer, true, 
					    false, synth.getReference());
		    break;
		}

		if (op != null) {
		    if (subExpressions != null) {
			for (int i=subExpressions.length; i-- > 0; ) {
			    op = op.addOperand(subExpressions[i]);
			    if (subExpressions[i].getFreeOperandCount() > 0)
				break;
			}
		    }
		    return op;
		}
	    }
	}
	return null;
    }

    public boolean needsCast(int param, Type[] paramTypes) {
	Type realClassType;
	if (staticFlag) 
	    realClassType = classType;
	else {
	    if (param == 0)
		return paramTypes[0] instanceof NullType;
	    realClassType = paramTypes[0];
	}

	if (!(realClassType instanceof ClassInterfacesType)) {
	    /* Arrays don't have overloaded methods, all okay */
	    return false;
	}
	ClassInfo clazz = ((ClassInterfacesType) realClassType).getClassInfo();
	int offset = staticFlag ? 0 : 1;
	
	Type[] myParamTypes = methodType.getParameterTypes();
	if (myParamTypes[param-offset].equals(paramTypes[param])) {
	    /* Type at param is okay. */
	    return false;
	}
	/* Now check if there is a conflicting method in this class or
	 * a superclass.  */
	while (clazz != null) {
	    MethodInfo[] methods = clazz.getMethods();
	next_method:
	    for (int i=0; i< methods.length; i++) {
		if (!methods[i].getName().equals(methodName))
		    /* method name doesn't match*/
		    continue next_method;

		Type[] otherParamTypes
		    = Type.tMethod(methods[i].getType()).getParameterTypes();
		if (otherParamTypes.length != myParamTypes.length) {
		    /* parameter count doesn't match*/
		    continue next_method;
		}

		if (myParamTypes[param-offset].isOfType
		    (Type.tSubType(otherParamTypes[param-offset]))) {
		    /* cast to myParamTypes cannot resolve any conflicts. */
		    continue next_method;
		}
		for (int p = offset; p < paramTypes.length; p++) {
		    if (!paramTypes[p]
			.isOfType(Type.tSubType(otherParamTypes[p-offset])))
			/* No conflict here */
			continue next_method;
		}
		/* There is a conflict that can be resolved by a cast. */
		return true;
	    }
	    clazz = clazz.getSuperclass();
	}	    
	return false;
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


    /**
     * We add the named method scoped classes to the declarables, and
     * only fillDeclarables on the parameters we will print.
     */
    public void fillDeclarables(Collection used) {
	if (!isConstructor()) {
	    super.fillDeclarables(used);
	    return;
	}

	ClassInfo clazz = getClassInfo();
	InnerClassInfo outer = getOuterClassInfo(clazz);
	ClassAnalyzer clazzAna = methodAnalyzer.getClassAnalyzer(clazz);
	int arg = 1;
	int length = subExpressions.length;
	boolean jikesAnonymousInner = false;

	if ((Decompiler.options & Decompiler.OPTION_ANON) != 0
	    && clazzAna != null
	    && outer != null && (outer.outer == null || outer.name == null)) {
	    arg += clazzAna.getOuterValues().length;
	    for (int i=1; i< arg; i++) {
		Expression expr = subExpressions[i];
		if (expr instanceof CheckNullOperator) {
		    CheckNullOperator cno = (CheckNullOperator) expr;
		    expr = cno.subExpressions[0];
		}
		expr.fillDeclarables(used);
	    }
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

    /* Invokes never equals: they may return different values even if
     * they have the same parameters.
     */
    public void dumpExpression(TabbedPrintWriter writer)
	throws java.io.IOException {
	boolean opIsThis = !staticFlag
	    && subExpressions[0] instanceof ThisOperator;
        int arg = 1;
	int length = subExpressions.length;
	/* true, if this is the constructor of an anonymous class and we
	 * must therefore dump the class.
	 */
	boolean dumpBlock = false;
	ClassInfo clazz = getClassInfo();
	ClassAnalyzer clazzAna = null;

	Type[] paramTypes = new Type[subExpressions.length];
	for (int i=0; i< subExpressions.length; i++)
	    paramTypes[i] = subExpressions[i].getType().getCanonic();

	if (isConstructor()) {
	    boolean jikesAnonymousInner = false;
	    InnerClassInfo outer = getOuterClassInfo(clazz);
	    clazzAna = methodAnalyzer.getClassAnalyzer(clazz);

	    if ((Decompiler.options & 
		 (Decompiler.OPTION_ANON | Decompiler.OPTION_CONTRAFO)) != 0
		&& clazzAna != null
		&& outer != null 
		&& (outer.outer == null || outer.name == null)) {

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
			    CheckNullOperator cno
				= (CheckNullOperator) thisExpr;
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
		    (Decompiler.OPTION_INNER
		     | Decompiler.OPTION_CONTRAFO)) != 0) {
		Expression outerExpr = jikesAnonymousInner 
		    ? subExpressions[--length]
		    : subExpressions[arg++];
		if (outerExpr instanceof CheckNullOperator) {
		    CheckNullOperator cno = (CheckNullOperator) outerExpr;
		    outerExpr = cno.subExpressions[0];
		} else if (!(outerExpr instanceof ThisOperator)) {
		    if (!jikesAnonymousInner)
			// Bug in jikes: it doesn't do a check null.
			// We don't complain here.
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
	}

	if (specialFlag) {
	    if (opIsThis
		&& (((ThisOperator)subExpressions[0]).getClassInfo()
		    == methodAnalyzer.getClazz())) {
		if (isThis()) {
		    /* XXX check if this is a private or final method. */
		} else {
		    /* XXX check that this is the first defined
		     * super method. */
		    writer.print("super");
		    ClassInfo superClazz = getClassInfo().getSuperclass();
		    paramTypes[0] = superClazz == null
			? Type.tObject : Type.tClass(superClazz);
		    opIsThis = false;
		}
	    } else if (isConstructor() 
		       && subExpressions[0] instanceof NewOperator) {

		writer.print("new ");
		writer.printType(Type.tClass(clazz));

	    } else {
		/* XXX check if this is a private or final method. */
		int minPriority = 950; /* field access */
		if (!isThis()) {
		    writer.print("(NON VIRTUAL ");
		    writer.printType(classType);
		    writer.print(")");
		    paramTypes[0] = classType;
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
					       Scope.NOSUPERMETHODNAME)) {
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
		if (needsCast(0, paramTypes)){
		    writer.print("((");
		    writer.printType(classType);
		    writer.print(") ");
		    subExpressions[0].dumpExpression(writer, 700);
		    writer.print(")");
		    paramTypes[0] = classType;
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
	int offset = staticFlag ? 0 : 1;
	while (arg < length) {
            if (!first)
		writer.print(", ");
	    else
		first = false;
	    int priority = 0;
	    if (needsCast(arg, paramTypes)) {
		Type castType = methodType.getParameterTypes()[arg-offset];
		writer.print("(");
		writer.printType(castType);
		writer.print(") ");
		paramTypes[arg] = castType;
		priority = 700;
	    }
            subExpressions[arg++].dumpExpression(writer, priority);
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
