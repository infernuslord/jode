/* CodeAnalyzer Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.Decompiler;
import jode.GlobalOptions;
import jode.type.Type;
import jode.util.SimpleDictionary;
import jode.bytecode.*;
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

import java.util.BitSet;
import java.util.Stack;
import java.util.Vector;
import java.util.Dictionary;
import java.util.Enumeration;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class CodeAnalyzer implements Analyzer, Scope, ClassDeclarer {
    
    FlowBlock methodHeader;
    BytecodeInfo code;
    MethodAnalyzer method;
    ImportHandler imports;
    boolean analyzedAnonymous = false;
    StructuredBlock insertBlock = null;
    
    Vector allLocals = new Vector();
    /**
     * This dictionary maps an anonymous ClassInfo to the
     * ConstructorOperator that creates this class.  
     */
    Dictionary anonClasses = new SimpleDictionary();
    Vector anonAnalyzers = new Vector();
    Vector innerAnalyzers = new Vector();

    LocalInfo[] param;
    LocalVariableTable lvt;
    
    public CodeAnalyzer(MethodAnalyzer ma, MethodInfo minfo,
			ImportHandler i)
    {
        method = ma;
	imports = i;
	code = minfo.getBytecode();

	if ((Decompiler.options & Decompiler.OPTION_VERIFY) != 0) {
	    CodeVerifier verifier = new CodeVerifier(getClazz(), minfo, code);
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

    public void initParams() {
        Type[] paramTypes = method.getType().getParameterTypes();
	int paramCount = (method.isStatic() ? 0 : 1) + paramTypes.length;
	param = new LocalInfo[paramCount];
	int offset = 0;
	int slot = 0;
	if (!method.isStatic())
	    param[offset++] = getLocalInfo(0, slot++);
	for (int i=0; i < paramTypes.length; i++) {
	    param[offset++] = getLocalInfo(0, slot);
	    slot += paramTypes[i].stackSize();
	}
	
    }

    public BytecodeInfo getBytecodeInfo() {
	return code;
    }

    public FlowBlock getMethodHeader() {
        return methodHeader;
    }

    public void insertStructuredBlock(StructuredBlock superBlock) {
	if (insertBlock != null)
	    throw new jode.AssertError();
	insertBlock = superBlock;
    }

    void readCode() {
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
    } 

    public void analyze()
    {
	if (code == null)
	    return;
        readCode();
	if ((Decompiler.options & Decompiler.OPTION_PUSH) == 0
	    && methodHeader.mapStackToLocal())
	    methodHeader.removePush();
	if ((Decompiler.options & Decompiler.OPTION_ONETIME) != 0)
	    methodHeader.removeOnetimeLocals();

	methodHeader.mergeParams(param);
    }

    public void dumpSource(TabbedPrintWriter writer) 
         throws java.io.IOException
    {
	writer.pushScope(this);
	if (methodHeader != null)
	    methodHeader.dumpSource(writer);
	else
	    writer.println("COULDN'T DECOMPILE METHOD!");
	writer.popScope();
    }

    public LocalInfo getLocalInfo(int addr, int slot) {
        LocalInfo li = new LocalInfo(slot);
	if (lvt != null) {
	    LocalVarEntry entry = lvt.getLocal(slot, addr);
	    if (entry != null)
		li.addHint(entry);
	}
	allLocals.addElement(li);
        return li;
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
        Enumeration enum = anonAnalyzers.elements();
        while (enum.hasMoreElements()) {
            ClassAnalyzer classAna = (ClassAnalyzer) enum.nextElement();
            if (classAna.getName() != null
		&& classAna.getName().equals(name))
                return classAna;
        }
        return null;
    }

    public void addAnonymousConstructor(ConstructorOperator cop) {
	ClassInfo cinfo = cop.getClassInfo();

	ConstructorOperator[] cops = 
	    (ConstructorOperator[]) anonClasses.get(cinfo);
	ConstructorOperator[] newCops;
	if (cops == null) {
	    newCops = new ConstructorOperator[] { cop };
	} else {
	    newCops = new ConstructorOperator[cops.length + 1];
	    System.arraycopy(cops, 0, newCops, 0, cops.length);
	    newCops[cops.length] = cop;
	}
	anonClasses.put(cinfo, newCops);
    }

    public void createAnonymousClasses() {
	int serialnr = 0;
	Enumeration keys = anonClasses.keys();
        Enumeration elts = anonClasses.elements();
        while (keys.hasMoreElements()) {
            ClassInfo clazz = (ClassInfo) keys.nextElement();
	    ConstructorOperator[] cops =
		(ConstructorOperator[]) elts.nextElement();
	    ClassAnalyzer anonAnalyzer = getParent().getClassAnalyzer(clazz);
	    int copsNr = 0;

	    Expression[] outerValues;
	    int maxOuter;
	    if (anonAnalyzer != null) {
		outerValues = anonAnalyzer.getOuterValues();
		maxOuter = outerValues.length;
	    } else {
		System.err.println("aac: expr0 = "+cops[0]);
		outerValues = new Expression[maxOuter];
		Expression[] subExprs1 = cops[copsNr++].getSubExpressions();
		maxOuter = subExprs1.length;
		for (int j=0; j < maxOuter; j++) {
		    Expression expr = subExprs1[j].simplify();
		    if (expr instanceof CheckNullOperator)
			expr = ((CheckNullOperator) expr).getSubExpressions()[0];
		    if (expr instanceof ThisOperator) {
			outerValues[j] = 
			    new ThisOperator(((ThisOperator)expr).getClassInfo());
			continue;
		    }
		    if (expr instanceof LocalLoadOperator) {
			LocalLoadOperator llop = (LocalLoadOperator) expr;
			LocalInfo li = llop.getLocalInfo();
			for (int i=1; i < cops.length; i++) {
			    Expression expr2 = 
				cops[i].getSubExpressions()[j].simplify();
			    if (expr2 instanceof CheckNullOperator)
				expr2 = ((CheckNullOperator) expr2)
				    .getSubExpressions()[0];
			    LocalInfo li2 = 
				((LocalLoadOperator) expr2).getLocalInfo();
			    li2.combineWith(li);
			}
			if (li.markFinal()) {
			    outerValues[j] = new OuterLocalOperator(li);
			    continue;
			}
		    }
		    maxOuter = j;
		    System.err.println("new maxOuter: "+maxOuter+" ("+expr);
		}
	    }
	    for (int i=copsNr; i < cops.length; i++) {
		Expression[] subExprs = cops[i].getSubExpressions();
		for (j=0; j < maxOuter; j++) {
		    Expression expr = subExprs[j].simplify();
		    if (expr instanceof CheckNullOperator)
			expr = ((CheckNullOperator) expr).getSubExpressions()[0];
		    if (expr instanceof ThisOperator
			&& expr.equals(outerValues[j]))
			continue;
		    if (expr instanceof LocalLoadOperator) {
			// more thorough checks for constructors!
			// combine locals!
			// what else? 
			XXX
			LocalLoadOperator llop = (LocalLoadOperator) expr;
			LocalInfo li = llop.getLocalInfo();
			for (int i=1; i < cops.length; i++) {
			    Expression expr2 = 
				cops[i].getSubExpressions()[j].simplify();
			    if (expr2 instanceof CheckNullOperator)
				expr2 = ((CheckNullOperator) expr2)
				    .getSubExpressions()[0];
			    LocalInfo li2 = 
				((LocalLoadOperator) expr2).getLocalInfo();
			    li2.combineWith(li);
			}
			if (li.markFinal()) {
			    outerValues[j] = new OuterLocalOperator(li);
			    continue;
			}
		    }
		    maxOuter = j;
		    System.err.println("new maxOuter: "+maxOuter+" ("+expr);
		}
	    }
	    if (maxOuter > outerValues.length) {
		Expression[] newOuter = new Expression[j];
		System.arraycopy(outerValues, 0, newOuter, 0, j);
		outerValues = newOuter;
		break;
	    }

	    if (anonAnalyzer == null)
		anonAnalyzer = new ClassAnalyzer(this, clazz, imports,
						 outerValues);
	    else
		anonAnalyzer.setOuterValues(outerValues);
	    anonAnalyzer.analyze();
	    anonAnalyzers.addElement(anonAnalyzer);
	}
	analyzedAnonymous = true;
    }

    public void analyzeAnonymousClasses() {
	createAnonymousClasses();
    }

    public void makeDeclaration() {
        for (Enumeration enum = allLocals.elements();
	     enum.hasMoreElements(); ) {
            LocalInfo li = (LocalInfo)enum.nextElement();
            if (!li.isShadow())
                imports.useType(li.getType());
        }
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
        for (Enumeration enum = anonAnalyzers.elements();
	     enum.hasMoreElements(); ) {
	    ClassAnalyzer classAna = (ClassAnalyzer) enum.nextElement();
	    classAna.makeDeclaration();
	    addClassAnalyzer(classAna);
	}
    }

    public boolean hasAnalyzedAnonymous() {
	return analyzedAnonymous;
    }

    public LocalInfo getParamInfo(int nr) {
	return param[nr];
    }

    public void addClassAnalyzer(ClassAnalyzer clazzAna) {
	if (innerAnalyzers == null)
	    innerAnalyzers = new Vector();
	innerAnalyzers.addElement(clazzAna);
	getParent().addClassAnalyzer(clazzAna);
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
		    if (classAna.getParent() != this) {
			classAna.setParent(this);
		    }
		    return classAna;
		}
	    }
	}
	return getParent().getClassAnalyzer(cinfo);
    }

    public ClassAnalyzer getClassAnalyzer() {
	return method.classAnalyzer;
    }

    public ClassInfo getClazz() {
        return method.classAnalyzer.clazz;
    }

    /**
     * Get the method.
     * @return The method to which this code belongs.
     */
    public MethodAnalyzer getMethod() {return method;}

    public ImportHandler getImportHandler() {
	return imports;
    }
    
    public void useType(Type type) {
	imports.useType(type);
    }


    public boolean isScopeOf(Object obj, int scopeType) {
	if (scopeType == METHODSCOPE)
	    return anonClasses.get(obj) != null;
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
}
