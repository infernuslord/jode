/* TransformConstructors Copyright (C) 1998-1999 Jochen Hoenicke.
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

package jode.flow;
import java.lang.reflect.Modifier;
import jode.GlobalOptions;
import jode.Decompiler;
import jode.decompiler.Analyzer;
import jode.decompiler.ClassAnalyzer;
import jode.decompiler.CodeAnalyzer;
import jode.decompiler.MethodAnalyzer;
import jode.expr.*;
import jode.type.MethodType;
import jode.type.Type;
import jode.bytecode.ClassInfo;
import jode.bytecode.InnerClassInfo;

import java.util.Vector;
import java.util.Enumeration;

/**
 * 
 * @author Jochen Hoenicke
 */
public class TransformConstructors {
    
    public static boolean isThis(Expression thisExpr, ClassInfo clazz) {
	return ((thisExpr instanceof ThisOperator)
		&& (((ThisOperator)thisExpr).getClassInfo() == clazz));
    }

    public static boolean checkImplicitAnonymousConstructor
	(ClassAnalyzer clazzAnalyzer, MethodAnalyzer constr) {

	if (clazzAnalyzer.getName() != null)
	    return false;

	int outerValuesLength = clazzAnalyzer.getOuterValues().length;
	MethodType origType = constr.getType();
	/**
	 * Situation:
	 * constructor(outerParams, params) {
	 *   super(params);
	 * }
	 *
	 * Mark constructor as anonymous constructor.
	 */

	StructuredBlock sb = constr.getMethodHeader().block;
	if (!(sb instanceof InstructionBlock))
	    return false;

	InstructionBlock superBlock = (InstructionBlock) sb;
	if (!(superBlock.getInstruction() instanceof InvokeOperator))
	    return false;
	
	InvokeOperator superCall 
	    = (InvokeOperator) superBlock.getInstruction().simplify();
	superBlock.setInstruction(superCall);
	
	if (superCall.getFreeOperandCount() != 0
	    || !superCall.isConstructor() || !superCall.isSuperOrThis())
	    return false;
	    
	Expression[] subExpr = superCall.getSubExpressions();
	Expression thisExpr = subExpr[0];
	if (!isThis(thisExpr, clazzAnalyzer.getClazz()))
	    return false;

	Type[] constrParams = constr.getType().getParameterTypes();
	if (subExpr.length - 1 !=  constrParams.length - outerValuesLength)
	    return false;
	for (int i = 1; i < subExpr.length; i++) {
	    if (!(subExpr[i] instanceof LocalLoadOperator))
		return false;
	    LocalLoadOperator llop = (LocalLoadOperator) subExpr[i];
	    if (llop.getLocalInfo().getSlot() != i + outerValuesLength)
		return false;
	}
	constr.setAnonymousConstructor(true);
	return true;

    }

    public static boolean checkJikesSuperAndFillLoads(Expression expr,
						      int outerValuesLength,
						      Vector localLoads) {
	if (expr instanceof LocalStoreOperator
	    || expr instanceof IIncOperator)
	    return false;

	if (expr instanceof LocalLoadOperator) {
	    LocalLoadOperator llop = (LocalLoadOperator) expr;
	    int slot = llop.getLocalInfo().getSlot();
	    if (slot < outerValuesLength)
		return false;
	    localLoads.addElement(llop);
	}
	if (expr instanceof Operator) {
	    Expression subExpr[] = ((Operator)expr).getSubExpressions();
	    for (int i=0; i< subExpr.length; i++) {
		if (!checkJikesSuperAndFillLoads(subExpr[i], outerValuesLength,
						 localLoads))
		    return false;
	    }
	}
	return true;
    }

    public static boolean checkJikesContinuation
	(ClassAnalyzer clazzAnalyzer, MethodAnalyzer constr) {

	Expression[] oVs = clazzAnalyzer.getOuterValues();
	int outerValuesLength = oVs == null ? 0 : oVs.length;
	MethodType origType = constr.getType();

	/**
	 * Situation:
	 * constructor(outerParams, params) {
	 *   [optional: super(this, params, exprs)]
	 *   constructor$?(Outer.this, params);
	 * }
	 *
	 * Move optional super to method constructor$?
	 * (renaming local variables) and mark constructor and
	 * constructor$? as Jikes constructor.
	 */

	StructuredBlock sb = constr.getMethodHeader().block;
	
	Vector localLoads = null;
	InstructionBlock superBlock = null;
	if (sb instanceof SequentialBlock) {

	    if (!(sb.getSubBlocks()[0] instanceof InstructionBlock)
		|| !(sb.getSubBlocks()[1] instanceof InstructionBlock))
		return false;
	    superBlock = (InstructionBlock) sb.getSubBlocks()[0];
	    sb = sb.getSubBlocks()[1];

	    if (!(superBlock.getInstruction() instanceof InvokeOperator))
		return false;

	    InvokeOperator superCall 
		= (InvokeOperator) superBlock.getInstruction().simplify();
	    superBlock.setInstruction(superCall);
	    
	    if (superCall.getFreeOperandCount() != 0
		|| !superCall.isConstructor() || !superCall.isSuperOrThis())
		return false;
	    
	    Expression[] subExpr = superCall.getSubExpressions();
	    Expression thisExpr = subExpr[0];
	    if (!isThis(thisExpr, clazzAnalyzer.getClazz()))
		return false;

	    localLoads = new Vector();
	    for (int i=1; i< subExpr.length; i++) {
		if (!checkJikesSuperAndFillLoads(subExpr[i], outerValuesLength,
						 localLoads))
		    return false;
	    }

	} else if (!(sb instanceof InstructionBlock))
	    return false;

	/* Now check the constructor$? invocation */
	Expression lastExpr = ((InstructionBlock)sb).getInstruction();
	if (!(lastExpr instanceof InvokeOperator))
	    return false;

	InvokeOperator invoke = (InvokeOperator) lastExpr;
	if (!invoke.isThis()
	    || invoke.getFreeOperandCount() != 0)
	    return false;
	MethodAnalyzer methodAna = invoke.getMethodAnalyzer();
	if (methodAna == null)
	    return false;
	CodeAnalyzer codeAna = methodAna.getCode();
	if (codeAna == null)
	    return false;
	MethodType type = methodAna.getType();
	
	if (!methodAna.getName().startsWith("constructor$")
	    || (type.getParameterTypes().length - 1 
		!= origType.getParameterTypes().length - outerValuesLength)
	    || type.getReturnType() != Type.tVoid)
	    return false;
	
	if (!isThis(invoke.getSubExpressions()[0], 
		    clazzAnalyzer.getClazz()))
	    return false;

	ClassInfo parent;
	if (clazzAnalyzer.getParent() instanceof ClassAnalyzer)
	    parent = ((ClassAnalyzer) clazzAnalyzer.getParent())
		.getClazz();
	else if (clazzAnalyzer.getParent() instanceof CodeAnalyzer)
	    parent = ((CodeAnalyzer) clazzAnalyzer.getParent())
		.getClazz();
	else
	    return false;
	if (!isThis(invoke.getSubExpressions()[1], parent))
	    return false;
	for (int j = 1; j < type.getParameterTypes().length; j++) {
	    if (!(invoke.getSubExpressions()[j+1]
		  instanceof LocalLoadOperator))
		return false;
	    LocalLoadOperator llop
		= (LocalLoadOperator) invoke.getSubExpressions()[j+1];
	    if (llop.getLocalInfo().getSlot() != j + outerValuesLength)
		return false;
	}

	constr.getMethodHeader().block.removeBlock();

	/* Now move the constructor call.
	 */
	if (superBlock != null) {
	    SequentialBlock sequBlock = new SequentialBlock();
	    Enumeration enum = localLoads.elements();
	    while (enum.hasMoreElements()) {
		LocalLoadOperator llop 
		    = (LocalLoadOperator) enum.nextElement();
		int newSlot = llop.getLocalInfo().getSlot()
		    - outerValuesLength + 1;
		llop.setCodeAnalyzer(codeAna);
		llop.setLocalInfo(codeAna.getLocalInfo(0, newSlot));
	    }
	    codeAna.insertStructuredBlock(superBlock);
	}
	constr.setJikesConstructor(true);
	methodAna.setJikesConstructor(true);
	methodAna.analyze();
	checkImplicitAnonymousConstructor(clazzAnalyzer, methodAna);
	return true;
    }

    public static void transform(ClassAnalyzer clazzAnalyzer,
                                 boolean isStatic, MethodAnalyzer[] cons) {
        if (cons.length == 0)
            return;

        int constrCount = cons.length;
        StructuredBlock[] sb = new StructuredBlock[constrCount];
        for (int i=0; i< constrCount; ) {
	    FlowBlock header = cons[i].getMethodHeader();
	    if (header == null || !header.hasNoJumps())
		return;
            sb[i] = cons[i].getMethodHeader().block;
	    if ((GlobalOptions.debuggingFlags
		 & GlobalOptions.DEBUG_CONSTRS) != 0)
		GlobalOptions.err.println("constr "+i+": "+sb[i]);
            if (!isStatic) {
                InstructionBlock ib;
                if (sb[i] instanceof InstructionBlock)
                    ib = (InstructionBlock)sb[i];
                else if (sb[i] instanceof SequentialBlock
                         && (sb[i].getSubBlocks()[0] 
                             instanceof InstructionBlock))
                    ib = (InstructionBlock) sb[i].getSubBlocks()[0];
                else
                    return;

                Expression instr = ib.getInstruction();
                
                if (!(instr instanceof InvokeOperator)
		    || instr.getFreeOperandCount() != 0)
		    return;

                InvokeOperator invoke = (InvokeOperator) instr;
		if (!invoke.isConstructor() || !invoke.isSuperOrThis())
                    return;

		Expression thisExpr = invoke.getSubExpressions()[0];
		if (!isThis(thisExpr, clazzAnalyzer.getClazz()))
		    return;

                if (invoke.isThis()) {
                    /* This constructor calls another constructor, so we
                     * can skip it.
                     */
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_CONSTRS) != 0)
			GlobalOptions.err.println("skipping this()");
                    cons[i] = cons[--constrCount];
                    continue;
                }
                /* This constructor begins with a super call, as 
                 * expected. 
		 */
		InnerClassInfo outer = invoke.getOuterClassInfo();
		/* If the super() has no parameters, we can remove it
		 */
//                  /* If this is an anonymous class, and the super() gets
//  		 * the same parameters as this class, remove it, too
//                   */
//  		if (clazzAnalyzer.getName() == null
//  		    && (Decompiler.options & Decompiler.OPTION_ANON) != 0
//  		    && cons.length == 1) {
//  		    int skipParams = 1;
//  		    if (outer != null && outer.outer != null
//  			&& outer.name != null
//  			&& !Modifier.isStatic(outer.modifiers)
//  			&& (Decompiler.options
//  			    & Decompiler.OPTION_INNER) != 0
//  			&& (invoke.getSubExpressions()[1]
//  			    instanceof ThisOperator))
//  			skipParams++;
//  		    int length = invoke.getSubExpressions().length 
//  			- skipParams;
//  		    Type[] consType = cons[i].getMethodType()
//  			.getParameterTypes();
//  		    int outerValuesLength = 
//  			clazzAnalyzer.getOuterValues().length;
//  		    if (length == consType.length - outerValuesLength) {
//  			for (int j=0; j< length; j++) {
//  			    Expression expr = 
//  				invoke.getSubExpressions()[skipParams+j];
//  			    /*XXX*/
//  			}
//  		    }
//  		    clazzAnalyzer.setSuperParams(params);
//  		    ib.removeBlock();
//  		}
//  		else
		if (outer != null && outer.outer != null
			 && outer.name != null
			 && !Modifier.isStatic(outer.modifiers)) {
		    if ((Decompiler.options & Decompiler.OPTION_INNER) != 0
			&& (invoke.getMethodType().getParameterTypes()
			    .length == 1)
			&& (invoke.getSubExpressions()[1]
			    instanceof ThisOperator))
			ib.removeBlock();
		} else if (invoke.getMethodType().getParameterTypes()
			   .length == 0)
                    ib.removeBlock();

                if (sb[i] instanceof SequentialBlock)
                    sb[i] = sb[i].getSubBlocks()[1];
                else
                    sb[i] = null;
		if ((GlobalOptions.debuggingFlags
		     & GlobalOptions.DEBUG_CONSTRS) != 0)
		    GlobalOptions.err.println("normal constructor");
            }
            i++;
        }
        StructuredBlock[] start = new StructuredBlock[constrCount];
        for (int i=0; i< constrCount; i++)
            start[i] = sb[i];
	
    big_loop:
        for (;;) {
            for (int i=0; i< constrCount; i++) {
                if (sb[i] == null) {
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_CONSTRS) != 0)
			GlobalOptions.err.println("constr "+i+" is over");
                    break big_loop;
                }
	    }

            StructuredBlock ib = 
                (sb[0] instanceof SequentialBlock) 
                ? sb[0].getSubBlocks()[0]
                : sb[0];

	    if ((GlobalOptions.debuggingFlags
		 & GlobalOptions.DEBUG_CONSTRS) != 0)
		GlobalOptions.err.println("fieldInit: "+ib);

            if (!(ib instanceof InstructionBlock))
                break big_loop;

            Expression instr
		= ((InstructionBlock) ib).getInstruction().simplify();
		
	    if (!(instr instanceof StoreInstruction)
		|| instr.getFreeOperandCount() != 0)
		break big_loop;
	    
	    StoreInstruction store = (StoreInstruction) instr;
	    if (!(store.getLValue() instanceof PutFieldOperator))
		break big_loop;
	    
	    PutFieldOperator pfo = (PutFieldOperator) store.getLValue();
	    if (pfo.isStatic() != isStatic || !pfo.isThis())
		break big_loop;

            if (!isStatic) {
                if (!isThis(pfo.getSubExpressions()[0], 
			    clazzAnalyzer.getClazz())) {
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_CONSTRS) != 0)
			GlobalOptions.err.println("not this: "+instr);
		    break big_loop;
		}
            }

            Expression expr =  store.getSubExpressions()[1];
            if (!expr.isConstant()) {
		if ((GlobalOptions.debuggingFlags
		     & GlobalOptions.DEBUG_CONSTRS) != 0)
		    GlobalOptions.err.println("not constant: "+expr);
                break big_loop;
            }

	    if ((GlobalOptions.debuggingFlags
		 & GlobalOptions.DEBUG_CONSTRS) != 0)
		GlobalOptions.err.println("field " + pfo.getFieldName()
					  + " = " + expr);


            for (int i=1; i< constrCount; i++) {
                ib = (sb[i] instanceof SequentialBlock) 
                    ? sb[i].getSubBlocks()[0]
                    : sb[i];
                if (!(ib instanceof InstructionBlock)
                    || !(((InstructionBlock)ib).getInstruction().simplify()
			 .equals(instr))) {
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_CONSTRS) != 0)
			GlobalOptions.err.println("constr "+i+" differs: "+ib);
                    break big_loop;
                }
            }


            if (!(clazzAnalyzer
		  .getField(pfo.getFieldName(), pfo.getFieldType())
		  .setInitializer(expr))) {
		if ((GlobalOptions.debuggingFlags
		     & GlobalOptions.DEBUG_CONSTRS) != 0)
		    GlobalOptions.err.println("setField failed");
                break big_loop;
            }
                                                   
            
            for (int i=0; i< constrCount; i++) {
                if (sb[i] instanceof SequentialBlock)
                    sb[i] = sb[i].getSubBlocks()[1];
                else
                    sb[i] = null; 
            }
        }

	for (int i=0; i< constrCount; i++) {
            if (start[i] != null) {
		if (sb[i] == null)
		    start[i].removeBlock();
		else {
		    sb[i].replace(start[i]);
		    sb[i].simplify();
		}
		// Check for jikes continuation
		checkJikesContinuation(clazzAnalyzer, cons[i]);
	    }
	    checkImplicitAnonymousConstructor(clazzAnalyzer, cons[i]);
        }
    }
}

