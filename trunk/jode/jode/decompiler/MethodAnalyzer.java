/* MethodAnalyzer Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.AssertError;
import jode.Decompiler;
import jode.GlobalOptions;
import jode.bytecode.*;
import jode.jvm.SyntheticAnalyzer;
import jode.type.*;
import jode.expr.Expression;
import jode.expr.ConstOperator;
import jode.expr.CheckNullOperator;
import jode.expr.ThisOperator;
import jode.expr.LocalLoadOperator;
import jode.expr.OuterLocalOperator;
import jode.expr.ConstructorOperator;
import jode.flow.StructuredBlock;
import jode.flow.FlowBlock;
import jode.flow.TransformExceptionHandlers;
import jode.flow.Jump;
import jode.jvm.CodeVerifier;
import jode.jvm.VerifyException;
import jode.util.SimpleDictionary;

import java.lang.reflect.Modifier;
import java.util.BitSet;
import java.util.Stack;
import java.util.Vector;
import java.util.Dictionary;
import java.util.Enumeration;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class MethodAnalyzer implements Analyzer, Scope, ClassDeclarer {
    ImportHandler imports;
    ClassAnalyzer classAnalyzer;
    MethodInfo minfo;

    String methodName;
    MethodType methodType;
    boolean isConstructor;

    Type[] exceptions;

    SyntheticAnalyzer synth;

    FlowBlock methodHeader;
    BytecodeInfo code;

    Vector allLocals = new Vector();
    LocalInfo[] param;
    LocalVariableTable lvt;

    /**
     * This is a block that will be inserted at the beginning of the
     * method, when the code is analyzed.
     */
    StructuredBlock insertBlock = null;

    boolean isJikesConstructor;
    boolean hasJikesOuterValue;
    boolean isImplicitAnonymousConstructor;

    /**
     * This dictionary maps an anonymous ClassInfo to the
     * ConstructorOperator that creates this class.  
     */
    Vector anonConstructors = new Vector();
    Vector innerAnalyzers;


    public MethodAnalyzer(ClassAnalyzer cla, MethodInfo minfo,
                          ImportHandler imports) {
        this.classAnalyzer = cla;
        this.imports = imports;
	this.minfo = minfo;
        this.methodName = minfo.getName();
        this.methodType = Type.tMethod(minfo.getType());
        this.isConstructor = 
            methodName.equals("<init>") || methodName.equals("<clinit>");
        
	if (minfo.getBytecode() != null) {
	    code = minfo.getBytecode();

	    if ((Decompiler.options & Decompiler.OPTION_VERIFY) != 0) {
		CodeVerifier verifier
		    = new CodeVerifier(getClazz(), minfo, code);
		try {
		    verifier.verify();
		} catch (VerifyException ex) {
		    ex.printStackTrace(GlobalOptions.err);
		    throw new jode.AssertError("Verification error");
		}
	    }
	
	    if ((Decompiler.options & Decompiler.OPTION_LVT) != 0) {
		LocalVariableInfo[] localvars = code.getLocalVariableTable();
		if (localvars != null)
		    lvt = new LocalVariableTable(code.getMaxLocals(), 
						 localvars);
	    }
	    initParams();
	}
        String[] excattr = minfo.getExceptions();
        if (excattr == null) {
            exceptions = new Type[0];
        } else {
	    int excCount = excattr.length;
	    this.exceptions = new Type[excCount];
	    for (int i=0; i< excCount; i++)
		exceptions[i] = Type.tClass(excattr[i]);
        }
	if (minfo.isSynthetic() || methodName.indexOf('$') != -1)
	    synth = new SyntheticAnalyzer(minfo, true);
    }



    public void initParams() {
        Type[] paramTypes = getType().getParameterTypes();
	int paramCount = (isStatic() ? 0 : 1) + paramTypes.length;
	param = new LocalInfo[paramCount];
	int offset = 0;
	int slot = 0;
	if (!isStatic())
	    param[offset++] = getLocalInfo(0, slot++);
	for (int i=0; i < paramTypes.length; i++) {
	    param[offset++] = getLocalInfo(0, slot);
	    slot += paramTypes[i].stackSize();
	}
    }

    public String getName() {
	return methodName;
    }

    public MethodType getType() {
	return methodType;
    }

    public FlowBlock getMethodHeader() {
        return methodHeader;
    }

    public final BytecodeInfo getBytecodeInfo() {
	return code;
    }

    public final ImportHandler getImportHandler() {
	return imports;
    }

    public final void useType(Type type) {
	imports.useType(type);
    }

    public void insertStructuredBlock(StructuredBlock superBlock) {
	if (insertBlock != null)
	    throw new jode.AssertError();
	insertBlock = superBlock;
    }

    public final boolean isConstructor() {
        return isConstructor;
    }

    public final boolean isStatic() {
        return minfo.isStatic();
    }

    public final boolean isSynthetic() {
	return minfo.isSynthetic();
    }

    public final void setJikesConstructor(boolean value) {
	isJikesConstructor = value;
    }

    public final void setHasOuterValue(boolean value) {
	hasJikesOuterValue = value;
    }

    public final void setAnonymousConstructor(boolean value) {
	isImplicitAnonymousConstructor = value;
    }

    public final boolean isAnonymousConstructor() {
	return isImplicitAnonymousConstructor;
    }

    public final SyntheticAnalyzer getSynthetic() {
	return synth;
    }

    public Type getReturnType() {
        return methodType.getReturnType();
    }


    public void analyzeCode()
    {
	if (GlobalOptions.verboseLevel > 0)
	    GlobalOptions.err.print(methodName+": ");
	/* The adjacent analyzation relies on this */
	DeadCodeAnalysis.removeDeadCode(code);
	Handler[] handlers = code.getExceptionHandlers();
	int returnCount;
        TransformExceptionHandlers excHandlers; 
	{
	    /* First create a FlowBlock for every block that has a 
	     * predecessor other than the previous instruction.
	     */
	    for (Instruction instr = code.getFirstInstr();
		 instr != null; instr = instr.nextByAddr) {
		if (instr.prevByAddr == null
		    || instr.prevByAddr.alwaysJumps
		    || instr.preds != null)
		    instr.tmpInfo = new FlowBlock
			(this, instr.addr, instr.length);
	    }

	    for (int i=0; i < handlers.length; i++) {
		Instruction instr = handlers[i].start;
		if (instr.tmpInfo == null)
		    instr.tmpInfo 
			= new FlowBlock(this, instr.addr, instr.length);
		instr = handlers[i].catcher;
		if (instr.tmpInfo == null)
		    instr.tmpInfo 
			= new FlowBlock(this, instr.addr, instr.length);
	    }

            /* While we read the opcodes into FlowBlocks 
             * we try to combine sequential blocks, as soon as we
             * find two sequential instructions in a row, where the
             * second has no predecessors.
             */
            int mark = 1000;
            FlowBlock lastBlock = null;
	    boolean   lastSequential = false;
	    for (Instruction instr = code.getFirstInstr();
		 instr != null; instr = instr.nextByAddr) {

		jode.flow.StructuredBlock block
		    = Opcodes.readOpcode(instr, this);

                if (GlobalOptions.verboseLevel > 0 && instr.addr > mark) {
                    GlobalOptions.err.print('.');
                    mark += 1000;
                }

                if (lastSequential && instr.tmpInfo == null
		    /* Only merge with previous block, if this is sequential, 
		     * too.  
		     * Why?  doSequentialT2 does only handle sequential blocks.
		     */
		    && !instr.alwaysJumps && instr.succs == null) {
		    
                    lastBlock.doSequentialT2(block, instr.length);

                } else {

		    if (instr.tmpInfo == null)
			instr.tmpInfo = new FlowBlock
			    (this, instr.addr, instr.length);
		    FlowBlock flowBlock = (FlowBlock) instr.tmpInfo;
		    flowBlock.setBlock(block);

		    if (lastBlock != null)
			lastBlock.setNextByAddr(flowBlock);

                    instr.tmpInfo = lastBlock = flowBlock;
		    lastSequential = !instr.alwaysJumps && instr.succs == null;
                }
	    }

	    methodHeader = (FlowBlock) code.getFirstInstr().tmpInfo;
	    if (insertBlock != null) {
		insertBlock.setJump(new Jump(methodHeader));
		FlowBlock insertFlowBlock = new FlowBlock(this, 0, 0);
		insertFlowBlock.setBlock(insertBlock);
		insertFlowBlock.setNextByAddr(methodHeader);
		methodHeader = insertFlowBlock;
	    }

            excHandlers = new TransformExceptionHandlers();
            for (int i=0; i<handlers.length; i++) {
                Type type = null;
                FlowBlock start 
		    = (FlowBlock) handlers[i].start.tmpInfo;
                int endAddr = handlers[i].end.nextByAddr.addr;
                FlowBlock handler
		    = (FlowBlock) handlers[i].catcher.tmpInfo;
                if (handlers[i].type != null)
                    type = Type.tClass(handlers[i].type);
                
                excHandlers.addHandler(start, endAddr, handler, type);
            }
        }
	for (Instruction instr = code.getFirstInstr();
	     instr != null; instr = instr.nextByAddr)
	    instr.tmpInfo = null;

        if (GlobalOptions.verboseLevel > 0)
            GlobalOptions.err.print('-');
            
//          try {
//              TabbedPrintWriter writer = new TabbedPrintWriter(System.err);
//  	    methodHeader.dumpSource(writer);
//          } catch (java.io.IOException ex) {
//          }
        excHandlers.analyze();
        methodHeader.analyze();

	if ((Decompiler.options & Decompiler.OPTION_PUSH) == 0
	    && methodHeader.mapStackToLocal())
	    methodHeader.removePush();
	if ((Decompiler.options & Decompiler.OPTION_ONETIME) != 0)
	    methodHeader.removeOnetimeLocals();

	methodHeader.mergeParams(param);

	if (GlobalOptions.verboseLevel > 0)
	    GlobalOptions.err.println("");
    } 

    public void analyze() 
      throws ClassFormatError
    {
	if (code == null)
	    return;

	int offset = 0;
	if (!isStatic()) {
	    ClassInfo classInfo = classAnalyzer.getClazz();
	    LocalInfo thisLocal = getParamInfo(0);
	    thisLocal.setExpression(new ThisOperator(classInfo, true));
	    offset++;
	}

	Type[] paramTypes = methodType.getParameterTypes();
	for (int i=0; i< paramTypes.length; i++) {
	    getParamInfo(offset).setType(paramTypes[i]);
            offset++;
        }

        for (int i= 0; i< exceptions.length; i++)
            imports.useType(exceptions[i]);
    
        if (!isConstructor)
            imports.useType(methodType.getReturnType());

	if ((Decompiler.options & Decompiler.OPTION_IMMEDIATE) == 0)
	    analyzeCode();
    }

    public final LocalInfo getParamInfo(int nr) {
	return param[nr];
    }

    public void analyzeInnerClasses() 
      throws ClassFormatError
    {
	createAnonymousClasses();
    }

    public void makeDeclaration() {
	if (isConstructor() && !isStatic()
	    && classAnalyzer.outerValues != null
	    && (Decompiler.options & Decompiler.OPTION_CONTRAFO) != 0) {
	    Expression[] outerValues = classAnalyzer.outerValues;
	    for (int i=0; i< outerValues.length; i++) {
		LocalInfo local = getParamInfo(1+i);
		local.setExpression(outerValues[i]);
	    }
	}
	if (isJikesConstructor && hasJikesOuterValue
	    && classAnalyzer.outerValues != null
	    && classAnalyzer.outerValues.length > 0)
	    getParamInfo(1).setExpression(classAnalyzer.outerValues[0]);
        
        for (Enumeration enum = allLocals.elements();
	     enum.hasMoreElements(); ) {
            LocalInfo li = (LocalInfo)enum.nextElement();
            if (!li.isShadow())
                imports.useType(li.getType());
        }
	if (code != null) {
	    for (int i=0; i < param.length; i++) {
		param[i].guessName();
		for (int j=0; j < i; j++) {
		    if (param[j].getName().equals(param[i].getName())) {
			/* A name conflict happened. */
			param[i].makeNameUnique();
			break; /* j */
		    }
		}
	    }
	    methodHeader.makeDeclaration(param);
	    methodHeader.simplify();
	}
	    
	if (innerAnalyzers != null) {
	    for (Enumeration enum = innerAnalyzers.elements();
		 enum.hasMoreElements(); ) {
		ClassAnalyzer classAna = (ClassAnalyzer) enum.nextElement();
		if (classAna.getParent() == this) {
		    Expression[] outerValues = classAna.getOuterValues();
		    for (int i=0; i< outerValues.length; i++) {
			if (outerValues[i] instanceof OuterLocalOperator) {
			    LocalInfo li = ((OuterLocalOperator) 
					    outerValues[i]).getLocalInfo();
			    if (li.getMethodAnalyzer() == this)
				li.markFinal();
			}
		    }
		    classAna.makeDeclaration();
		}
	    }
	}
    }

    public boolean skipWriting() {
	if (synth != null) {
	    // We don't need this class anymore (hopefully?)
	    if (synth.getKind() == synth.GETCLASS)
		return true;
	    if (synth.getKind() >= synth.ACCESSGETFIELD
		&& synth.getKind() <= synth.ACCESSSTATICMETHOD
		&& (Decompiler.options & Decompiler.OPTION_INNER) != 0
		&& (Decompiler.options & Decompiler.OPTION_ANON) != 0)
		return true;
	}

	if (isConstructor && isJikesConstructor) {
	    // This is the first empty part of a jikes constructor
	    return true;
	}

	boolean declareAsConstructor = isConstructor;
	int skipParams = 0;
	if (isConstructor() && !isStatic()
	    && classAnalyzer.outerValues != null)
	    skipParams = classAnalyzer.outerValues.length;

	if (isJikesConstructor) {
	    // This is the real part of a jikes constructor
	    declareAsConstructor = true;
	    skipParams = hasJikesOuterValue
		&& classAnalyzer.outerValues.length > 0 ? 1 : 0;
	}

	if (declareAsConstructor
	    && classAnalyzer.constructors.length == 1) {

	    // If this is the only constructor and it is empty and
	    // takes no parameters, this is the default constructor.
	    if (methodType.getParameterTypes().length == skipParams
		&& getMethodHeader() != null
		&& getMethodHeader().getBlock() instanceof jode.flow.EmptyBlock
		&& getMethodHeader().hasNoJumps())
		return true;

	    // if this is an anonymous class and this is the only
	    // constructor and it only does a super call with the given
	    // parameters, this is constructor is implicit.
	    if (isImplicitAnonymousConstructor)
		return true;
	}
	return false;
    }
    
    public void dumpSource(TabbedPrintWriter writer) 
         throws IOException
    {
	boolean declareAsConstructor = isConstructor;
	int skipParams = 0;
	if (isConstructor() && !isStatic()
	    && (Decompiler.options & Decompiler.OPTION_CONTRAFO) != 0
	    && classAnalyzer.outerValues != null)
	    skipParams = classAnalyzer.outerValues.length;

	if (isJikesConstructor) {
	    // This is the real part of a jikes constructor
	    declareAsConstructor = true;
	    skipParams = hasJikesOuterValue
		&& classAnalyzer.outerValues.length > 0 ? 1 : 0;
	}

	if ((Decompiler.options & Decompiler.OPTION_IMMEDIATE) != 0
	    && code != null) {
            // We do the analyzeCode() here, to get 
            // immediate output.

	    analyzeCode();
	}

        if (isConstructor() && isStatic() 
            && getMethodHeader().getBlock() instanceof jode.flow.EmptyBlock)
            return;

	if (minfo.isDeprecated()) {
	    writer.println("/**");
	    writer.println(" * @deprecated");
	    writer.println(" */");
	}

	writer.pushScope(this);

	if (minfo.isSynthetic()
	    && (classAnalyzer.getName() != null
		|| !isConstructor()))
	    writer.print("/*synthetic*/ ");
	int modifiedModifiers = minfo.getModifiers();
	/*
	 * JLS-1.0, section 9.4:
	 *
	 * For compatibility with older versions of Java, it is
	 * permitted but discouraged, as a matter of style, to
	 * redundantly specify the abstract modifier for methods
	 * declared in interfaces.
	 *
	 * Every method declaration in the body of an interface is
	 * implicitly public. It is permitted, but strongly
	 * discouraged as a matter of style, to redundantly specify
	 * the public modifier for interface methods.
	 */
	if (classAnalyzer.getClazz().isInterface())
	    modifiedModifiers &= ~(Modifier.PUBLIC | Modifier.ABSTRACT);
	String modif = Modifier.toString(modifiedModifiers);
	if (modif.length() > 0)
	    writer.print(modif+" ");
        if (isConstructor
	    && (isStatic()
		|| (classAnalyzer.getName() == null
		    && skipParams == methodType.getParameterTypes().length))) {
            /* static block or unnamed constructor */
        } else { 
            if (declareAsConstructor)
		writer.print(classAnalyzer.getName());
            else {
                writer.printType(getReturnType());
		writer.print(" " + methodName);
	    }
            writer.print("(");
            Type[] paramTypes = methodType.getParameterTypes();
            int offset = skipParams + (isStatic() ? 0 : 1);
	    int start = skipParams;

	    LocalInfo[] param = new LocalInfo[paramTypes.length];
            for (int i=start; i<paramTypes.length; i++) {
                if (code == null) {
                    param[i] = new LocalInfo(this, offset);
                    param[i].setType(paramTypes[i]);
		    param[i].guessName();
		    for (int j=0; j < i; j++) {
			if (param[j].getName().equals(param[i].getName())) {
			    /* A name conflict happened. */
			    param[i].makeNameUnique();
			    break; /* j */
			}
		    }
                } else {
                    param[i] = getParamInfo(offset);
		    offset++;
		}
            }

            for (int i=start; i<paramTypes.length; i++) {
                if (i>start)
                    writer.print(", ");
		param[i].dumpDeclaration(writer);
            }
            writer.print(")");
        }
        if (exceptions.length > 0) {
            writer.println("");
            writer.print("throws ");
            for (int i= 0; i< exceptions.length; i++) {
                if (i > 0)
                    writer.print(", ");
                writer.printType(exceptions[i]);
            }
        }
        if (code != null) {
	    writer.openBrace();
            writer.tab();
	    if (methodHeader != null)
		methodHeader.dumpSource(writer);
	    else
		writer.println("COULDN'T DECOMPILE METHOD!");
            writer.untab();
	    writer.closeBrace();
        } else
            writer.println(";");
	writer.popScope();
    }

    public LocalInfo getLocalInfo(int addr, int slot) {
        LocalInfo li = new LocalInfo(this, slot);
	if (lvt != null) {
	    LocalVarEntry entry = lvt.getLocal(slot, addr);
	    if (entry != null)
		li.addHint(entry.getName(), entry.getType());
	}
	allLocals.addElement(li);
        return li;
    }

    public ClassAnalyzer getClassAnalyzer() {
	return classAnalyzer;
    }

    public ClassInfo getClazz() {
        return classAnalyzer.clazz;
    }


    /**
     * Checks if the variable set contains a local with the given name.
     */
    public LocalInfo findLocal(String name) {
        Enumeration enum = allLocals.elements();
        while (enum.hasMoreElements()) {
            LocalInfo li = (LocalInfo) enum.nextElement();
            if (li.getName().equals(name))
                return li;
        }
        return null;
    }

    /**
     * Checks if an anonymous class with the given name exists.
     */
    public ClassAnalyzer findAnonClass(String name) {
	if (innerAnalyzers != null) {
	    Enumeration enum = innerAnalyzers.elements();
	    while (enum.hasMoreElements()) {
		ClassAnalyzer classAna = (ClassAnalyzer) enum.nextElement();
		if (classAna.getParent() == this
		    && classAna.getName() != null
		    && classAna.getName().equals(name)) {
		    return classAna;
		}
	    }
	}
        return null;
    }

    public boolean isScopeOf(Object obj, int scopeType) {
	if (scopeType == METHODSCOPE
	    && obj instanceof ClassInfo) {
	    ClassAnalyzer ana = getClassAnalyzer((ClassInfo)obj);
	    if (ana != null)
		return ana.getParent() == this;
	}
	return false;
    }

    public boolean conflicts(String name, int usageType) {
	if (usageType == AMBIGUOUSNAME || usageType == LOCALNAME)
	    return findLocal(name) != null;
	if (usageType == AMBIGUOUSNAME || usageType == CLASSNAME)
	    return findAnonClass(name) != null;
	return false;
    }

    public ClassDeclarer getParent() {
	return getClassAnalyzer();
    }


    public void addAnonymousConstructor(ConstructorOperator cop) {
	anonConstructors.addElement(cop);
    }

    private boolean unifyOuterValues(Expression ov1, Expression ov2,
				     final ClassAnalyzer clazzAna, 
				     final int shrinkTo) {


	/* Wow, unifying outer values of different constructors in
	 * different methods of different classes can get complicated.
	 * We have not committed the number of OuterValues.  So we
	 * can't say for sure, if the local load matches an outer
	 * local if this is a constructor.  Even worse: The previous
	 * outerValues may be a load of a constructor local, that
	 * should be used as outer value...
	 *
	 * We look if there is a way to merge them and register an
	 * outer value listener to lots of classes.  
	 */

	LocalInfo li1 = null;
	MethodAnalyzer method1 = null;
	if (ov2 instanceof ThisOperator) {
	    if (ov1 instanceof ThisOperator)
		return ov1.equals(ov2);
	    Expression temp = ov2;
	    ov2 = ov1;
	    ov1 = temp;

	} else {

	    if (ov1 instanceof LocalLoadOperator)
		li1 = ((LocalLoadOperator) ov1).getLocalInfo();
	    else if (ov1 instanceof OuterLocalOperator)
		li1 = ((OuterLocalOperator) ov1).getLocalInfo();
	    else if (!(ov1 instanceof ThisOperator))
		return false;
	}

	LocalInfo li2;
	if (ov2 instanceof LocalLoadOperator)
	    li2 = ((LocalLoadOperator) ov2).getLocalInfo();
	else if (ov2 instanceof OuterLocalOperator)
	    li2 = ((OuterLocalOperator) ov2).getLocalInfo();
	else
	    return false;
	MethodAnalyzer method2 = li2.getMethodAnalyzer();


	/* Now: li2 != null, method2 != null
	 *      (li1 == null and method1 == null) iff ov1 is ThisOperator 
	 */

	class ShrinkOnShrink implements OuterValueListener {
	    Dictionary limits = new SimpleDictionary();

	    public void setLimit(ClassAnalyzer other, 
				 int newLimit) {
		limits.put(other, new Integer(newLimit));
		other.addOuterValueListener(this);
	    }
	    
	    public void done() {
		limits = null;
	    }
	    
	    public void shrinkingOuterValues
		(ClassAnalyzer other, int newCount) {
		if (limits != null) {
		    int limit = ((Integer) limits.get(other)
				 ).intValue();
		    if (newCount <= limit) {
			clazzAna.shrinkOuterValues(shrinkTo);
			done();
		    }
		}
	    }
	}
	
	ShrinkOnShrink sos = new ShrinkOnShrink();

	if (li1 != null) {
	    method1 = li1.getMethodAnalyzer();
	    
	    System.err.println("unifyLocalInfos: "+method1+"."+li1
			       +" and "+method2+"."+li2);
	    
	    while (!method2.isParent(method1)) {
		if (!method1.isConstructor() || method1.isStatic()) {
		    sos.done();
		    return false;
		}
		
		ClassAnalyzer ca1 = method1.classAnalyzer;
		int slot = li1.getSlot();
		Expression[] ov = ca1.getOuterValues();
		if (ov == null) {
		    sos.done();
		    return false;
		}
		
		int param = 0;
		while (param < ov.length && slot > 0)
		    slot -= ov[param++].getType().stackSize();
		
		if (slot != 0) {
		    sos.done();
		    return false;
		}
		ov1 = ov[param];
		sos.setLimit(ca1, param);

		if (ov1 instanceof ThisOperator) {
		    li1 = null;
		    method1 = null;
		    break;
		}
		li1 = ((OuterLocalOperator) ov1).getLocalInfo();
		method1 = li1.getMethodAnalyzer();
		System.err.println("unifyLocalInfos: "+method1+"."+li1
				   +" and "+method2+"."+li2);
	    }
	}

	/* Now: ov1 is ThisOperator  and method1 == null
	 *   or (ov1 is LocalExpression, li1 is LocalInfo,
	 *       method1 is parent of method2).
	 */
	    
	System.err.println(method1+" is parent of "+method2);
	while (method1 != method2) {
	    if (!method2.isConstructor() || method2.isStatic()) {
		sos.done();
		return false;
	    }
	    
	    ClassAnalyzer ca2 = method2.classAnalyzer;
	    int slot = li2.getSlot();
	    Expression[] ov = ca2.getOuterValues();
	    if (ov == null) {
		sos.done();
		return false;
	    }

	    slot--;
	    int param = 0;
	    while (param < ov.length && slot > 0)
		slot -= ov[param++].getType().stackSize();
	    
	    if (slot != 0) {
		System.err.println("slot: "+slot+"; param: "+param+"; "+ov[param]);
		sos.done();
		return false;
	    }

	    ov2 = ov[param];
	    sos.setLimit(ca2, param);
	    if (ov2 instanceof ThisOperator) {
		if (ov1.equals(ov2))
		    return true;
		else {
		    sos.done();
		    return false;
		}
	    }

	    li2 = ((OuterLocalOperator) ov2).getLocalInfo();
	    method2 = li2.getMethodAnalyzer();
	    System.err.println("unifyLocalInfos: "+method1+"."+li1
			       +" and "+method2+"."+li2);
	}
	if (!li1.equals(li2)) {
	    sos.done();
	    return false;
	}
	return true;
    }

    public void analyzeConstructorOperator(ConstructorOperator cop) {
	ClassInfo clazz = (ClassInfo) cop.getClassInfo();
	ClassAnalyzer anonAnalyzer = getParent().getClassAnalyzer(clazz);
	
	Expression[] outerValues;
	if (anonAnalyzer == null) {
	    /* Create a new outerValues array corresponding to the
	     * first constructor invocation.
	     */
	    if (GlobalOptions.verboseLevel > 0)
		GlobalOptions.err.println("Analyzing method scope class: "
					  +clazz);
	    Expression[] subExprs = cop.getSubExpressions();
	    outerValues = new Expression[subExprs.length];
	    
	    for (int j=0; j < outerValues.length; j++) {
		Expression expr = subExprs[j].simplify();
		if (expr instanceof CheckNullOperator)
		    expr = ((CheckNullOperator) 
			    expr).getSubExpressions()[0];
		if (expr instanceof ThisOperator) {
		    outerValues[j] = 
			new ThisOperator(((ThisOperator)
					  expr).getClassInfo());
		    continue;
		}
		LocalInfo li = null;
		if (expr instanceof LocalLoadOperator) {
		    li = ((LocalLoadOperator) expr).getLocalInfo();
		    if (!li.isConstant())
			li = null;
		}
		if (expr instanceof OuterLocalOperator)
		    li = ((OuterLocalOperator) expr).getLocalInfo();
		
		if (li != null) {
		    outerValues[j] = new OuterLocalOperator(li);
		    continue;
		}
		
		Expression[] newOuter = new Expression[j];
		System.arraycopy(outerValues, 0, newOuter, 0, j);
		outerValues = newOuter;
		break;
	    }
	    anonAnalyzer = new ClassAnalyzer(this, clazz, imports,
					     outerValues);
	    addClassAnalyzer(anonAnalyzer);
	    anonAnalyzer.analyze();
	    anonAnalyzer.analyzeInnerClasses();
	} else {
	    
	    /*
	     * Get the previously created outerValues array and
	     * its length.  
	     */
	    outerValues = anonAnalyzer.getOuterValues();
	    /*
	     * Merge the other constructor invocation and
	     * possibly shrink outerValues array.  
	     */
	    Expression[] subExprs = cop.getSubExpressions();
	    for (int j=0; j < outerValues.length; j++) {
		if (j < subExprs.length) {
		    Expression expr = subExprs[j].simplify();
		    if (expr instanceof CheckNullOperator)
			expr = ((CheckNullOperator) expr)
			    .getSubExpressions()[0];

		    if (unifyOuterValues(outerValues[j], expr, 
					 anonAnalyzer, j))
			continue;
		    
		    System.err.println("shrinkOuterValues: "
				       +outerValues[j]+" vs. "+expr);
		}
		anonAnalyzer.shrinkOuterValues(j);
		break;
	    }
	}
    }
    
    public void createAnonymousClasses() {
	int serialnr = 0;
        Enumeration elts = anonConstructors.elements();
        while (elts.hasMoreElements()) {
	    ConstructorOperator cop = (ConstructorOperator) elts.nextElement();
	    analyzeConstructorOperator(cop);
	}
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
	if (innerAnalyzers != null) {
	    Enumeration enum = innerAnalyzers.elements();
	    while (enum.hasMoreElements()) {
		ClassAnalyzer classAna = (ClassAnalyzer) enum.nextElement();
		if (classAna.getClazz().equals(cinfo)) {
		    if (!isParent(classAna.getParent())) {
			
			Expression[] outerValues = classAna.getOuterValues();
			for (int i=0; i< outerValues.length; i++) {
			    if (outerValues[i] instanceof OuterLocalOperator) {
				LocalInfo li = ((OuterLocalOperator) 
						outerValues[i]).getLocalInfo();
				classAna.shrinkOuterValues(i-1);
			    }
			}
			classAna.setParent(this);
		    }
		    return classAna;
		}
	    }
	}
	return getParent().getClassAnalyzer(cinfo);
    }

    public void addClassAnalyzer(ClassAnalyzer clazzAna) {
	if (innerAnalyzers == null)
	    innerAnalyzers = new Vector();
	innerAnalyzers.addElement(clazzAna);
	getParent().addClassAnalyzer(clazzAna);
    }

    public boolean isParent(ClassDeclarer declarer) {
	ClassDeclarer ancestor = this;
	while (ancestor != null) {
	    if (ancestor == declarer)
		return true;
	    ancestor = ancestor.getParent();
	}
	return false;
    }

    public String toString() {
	return "MethodAnalyzer["+getClazz()+"."+getName()+"]";
    }
}
