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
 * This class will transform the constructors.  This involves several
 * steps:
 *
 * <ul><li>remove implicit super() call</li>
 * <li>move constant field initializations that occur in all constructors
 * (except those that start with a this() call) to the fields.</li>
 * <li>For inner classes check if the this$0 field(s) is/are
 * initialized corectly, remove the initializer and mark that
 * field. </li>
 * <li>For method scope classes also check the val$xx fields.</li>
 * <li>For jikes class check for a constructor$xx call, and mark that
 * as the real constructor, moving the super call of the original
 * constructor</li>
 * <li>For anonymous classes check that the constructor only contains
 * a super call and mark it as default</li></ul>
 *
 * It will make use of the <code>outerValues</code> expression, that
 * tell which parameters (this and final method variables) are always
 * given to the constructor.
 *
 * You can debug this class with the <code>--debug=constructors</code>
 * switch.
 * 
 * @author Jochen Hoenicke 
 * @see jode.decompiler.FieldAnalyzer#setInitializer
 * @see jode.decompiler.ClassAnalyzer#getOuterValues */
public class TransformConstructors {
    /* What is sometimes confusing is the distinction between slot and
     * parameter.  Most times parameter nr = slot nr, but double and
     * long parameters take two slots, so the remaining parameters
     * will shift.
     */

    ClassAnalyzer clazzAnalyzer;
    boolean isStatic;
    MethodAnalyzer[] cons;

    Expression[] outerValues;
    /**
     * The minimal first slot number after the outerValues.  This is because
     * the outerValues array may shrink to the desired size.  */
    int ovMinSlots;
    /**
     * The maximal first slot number after the outerValues.  
     */
    int ovMaxSlots;

    boolean jikesAnonInner = false;
    
    public TransformConstructors(ClassAnalyzer clazzAnalyzer,
				 boolean isStatic, MethodAnalyzer[] cons) {
	this.clazzAnalyzer = clazzAnalyzer;
	this.isStatic = isStatic;
	this.cons = cons;
	this.outerValues = clazzAnalyzer.getOuterValues();
	this.ovMinSlots = 1;
	ovMaxSlots = 1;
	if ((GlobalOptions.debuggingFlags
	     & GlobalOptions.DEBUG_CONSTRS) != 0)
	    GlobalOptions.err.print("OuterValues: ");
	if (outerValues != null) {
	    for (int i=0; i< outerValues.length; i++) {
		ovMaxSlots += outerValues[i].getType().stackSize();
		if ((GlobalOptions.debuggingFlags
		     & GlobalOptions.DEBUG_CONSTRS) != 0)
		    GlobalOptions.err.print(outerValues[i]+", ");
	    }
	}
	if ((GlobalOptions.debuggingFlags
	     & GlobalOptions.DEBUG_CONSTRS) != 0)
	    GlobalOptions.err.println(" ["+ovMinSlots+","+ovMaxSlots+"]");
    }
    
    public static boolean isThis(Expression thisExpr, ClassInfo clazz) {
	return ((thisExpr instanceof ThisOperator)
		&& (((ThisOperator)thisExpr).getClassInfo() == clazz));
    }

    /**
     * Translate a slot into an index into the outerValues array.
     * @return index into outerValues array or -1, if not matched.
     */
    public int getOuterValueIndex(int slot) {
	int ovSlot = 1; // slot of first outerValue
	for (int i=0; i< outerValues.length; i++) {
	    if (ovSlot == slot)
		return i;
	    ovSlot += outerValues[i].getType().stackSize();
	}
	return -1;
    }

    public boolean checkAnonymousConstructor(MethodAnalyzer constr,
					     InstructionBlock superBlock) {

	if (clazzAnalyzer.getName() != null)
	    return false;

	/**
	 * Situation:
	 * constructor(outerValues, params) {
	 *   super(params);
	 * }
	 *
	 * Mark constructor as anonymous constructor.
	 */

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

	int ovLength = constrParams.length - (subExpr.length - 1);
	int ovSlots = 1;
	for (int i=0; i< ovLength; i++) {
	    ovSlots += outerValues[i].getType().stackSize();
	}

	if ((GlobalOptions.debuggingFlags
	     & GlobalOptions.DEBUG_CONSTRS) != 0)
	    GlobalOptions.err.println("anonymousConstructor:  slots expected: "
				      +ovSlots+" possible: ["
				      +ovMinSlots+","+ovMaxSlots+"]");
	if (ovSlots < ovMinSlots || ovSlots > ovMaxSlots)
	    return false;
	int slot = ovSlots;
	int start = jikesAnonInner ? 2 : 1;
	for (int i = start; i < subExpr.length; i++) {
	    if (!(subExpr[i] instanceof LocalLoadOperator))
		return false;
	    LocalLoadOperator llop = (LocalLoadOperator) subExpr[i];
	    
	    if (llop.getLocalInfo().getSlot() != slot)
		return false;
	    slot += subExpr[i].getType().stackSize();
	}
	if (jikesAnonInner) {
	    if (!(subExpr[1] instanceof LocalLoadOperator))
		return false;
	    LocalLoadOperator llop = (LocalLoadOperator) subExpr[1];
	    
	    if (llop.getLocalInfo().getSlot() != slot)
		return false;
	}
	if ((GlobalOptions.debuggingFlags
	     & GlobalOptions.DEBUG_CONSTRS) != 0)
	    GlobalOptions.err.println("anonymousConstructors succeeded");
	ovMinSlots = ovMaxSlots = ovSlots;
	constr.setAnonymousConstructor(true);
	return true;
    }

    public boolean checkJikesSuperAndFillLoads(Expression expr,
					       Vector localLoads) {
	if (expr instanceof LocalStoreOperator
	    || expr instanceof IIncOperator)
	    return false;

	if (expr instanceof LocalLoadOperator) {
	    LocalLoadOperator llop = (LocalLoadOperator) expr;
	    int slot = llop.getLocalInfo().getSlot();
	    if (slot != 1) {
		/* slot 1 is outerValues[0], which is okay */
		if (slot < ovMinSlots)
		    return false;
		if (slot < ovMaxSlots)
		    ovMaxSlots = slot;
	    }
	    localLoads.addElement(llop);
	}
	if (expr instanceof Operator) {
	    Expression subExpr[] = ((Operator)expr).getSubExpressions();
	    for (int i=0; i< subExpr.length; i++) {
		if (!checkJikesSuperAndFillLoads(subExpr[i], localLoads))
		    return false;
	    }
	}
	return true;
    }

    public boolean checkJikesContinuation(MethodAnalyzer constr) {

	MethodType constrType = constr.getType();

	/*
	 * Situation:
	 * constructor(outerValues, params) {
	 *   [optional: super(expressions builded of (outerValues[0], params))]
	 *   constructor$?(outerValues[0], params);
	 * }
	 *
	 * For anonymous classes that extends class/method scope classes
	 * the situation is more unusal:
	 * constructor(outerValues, params, outerClass) {
	 *   outerClass.super(params);
	 *   constructor$?(outerValues[0], params);
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

	    ClassInfo superClazz = superCall.getClassInfo();
	    if ((Decompiler.options & 
		 (Decompiler.OPTION_ANON | Decompiler.OPTION_INNER)) != 0
		&& clazzAnalyzer.getName() == null
		&& superClazz.getOuterClasses() != null
		&& subExpr[1] instanceof LocalLoadOperator) {
		Type[] paramTypes = constrType.getParameterTypes();
		int expectedSlot = 1;
		for (int i=0; i< paramTypes.length-1; i++) {
		    expectedSlot += paramTypes[i].stackSize();
		}
		if (((LocalLoadOperator) 
		     subExpr[1]).getLocalInfo().getSlot() == expectedSlot)
		    jikesAnonInner = true;
	    }

	    localLoads = new Vector();
	    for (int i=2; i< subExpr.length; i++) {
		if (!checkJikesSuperAndFillLoads(subExpr[i], localLoads))
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
	MethodType methodType = methodAna.getType();

	int ovLength = constrType.getParameterTypes().length
	    - (methodType.getParameterTypes().length - 1);
	if (jikesAnonInner)
	    ovLength--;
	if (ovLength > outerValues.length)
	    return false;
	int ovSlots = 1;
	for (int i=0; i< ovLength; i++) {
	    ovSlots += outerValues[i].getType().stackSize();
	}
	if ((GlobalOptions.debuggingFlags
	     & GlobalOptions.DEBUG_CONSTRS) != 0)
	    GlobalOptions.err.println("jikesConstrCont:  slots expected: "
				      +ovSlots+" possible: ["
				      +ovMinSlots+","+ovMaxSlots+"]");

	if (!methodAna.getName().startsWith("constructor$")
	    || ovSlots < ovMinSlots || ovSlots > ovMaxSlots
	    || methodType.getReturnType() != Type.tVoid)
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
	
	Expression[] constrParams = invoke.getSubExpressions();
	if (!isThis(outerValues[0], parent)
	    || !(constrParams[1] instanceof LocalLoadOperator)
	    || ((LocalLoadOperator) 
		constrParams[1]).getLocalInfo().getSlot() != 1)
	    return false;
	{
	    int slot = ovSlots;
	    int start = 2;
	    for (int j = start; j < constrParams.length; j++) {
		if (!(constrParams[j] instanceof LocalLoadOperator))
		    return false;
		LocalLoadOperator llop
		    = (LocalLoadOperator) constrParams[j];
		if (llop.getLocalInfo().getSlot() != slot)
		    return false;
		slot += constrParams[j].getType().stackSize(); 
	    }
	}
	if ((GlobalOptions.debuggingFlags
	     & GlobalOptions.DEBUG_CONSTRS) != 0)
	    GlobalOptions.err.println("jikesConstrCont succeded.");

	ovMinSlots = ovMaxSlots = ovSlots;
	if (superBlock != null
	    && checkAnonymousConstructor(constr, superBlock)) {
	    superBlock = null;
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
		int slot = llop.getLocalInfo().getSlot();
		    
		int newSlot = (slot == 1 
			       ? 1 /* outerValues[0] */
			       : slot - ovSlots + 2);
		llop.setCodeAnalyzer(codeAna);
		llop.setLocalInfo(codeAna.getLocalInfo(0, newSlot));
	    }
	    codeAna.insertStructuredBlock(superBlock);
	}
	clazzAnalyzer.setJikesAnonymousInner(jikesAnonInner);
	constr.setJikesConstructor(true);
	methodAna.setJikesConstructor(true);
	codeAna.getParamInfo(1).setExpression(outerValues[0]);
	methodAna.analyze();
	if (constr.isAnonymousConstructor()
	    && methodAna.getMethodHeader().block instanceof EmptyBlock)
	    methodAna.setAnonymousConstructor(true);
	return true;
    }

    public void transform() {
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
		    MethodAnalyzer temp = cons[i];
                    cons[i] = cons[--constrCount];
		    cons[constrCount] = temp;
                    continue;
                }
                /* This constructor begins with a super call, as 
                 * expected. 
		 */
		InnerClassInfo outer = invoke.getOuterClassInfo();
		/* If the super() has no parameters, we can remove it
		 */
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
	    if (expr instanceof LocalLoadOperator
		&& outerValues != null
		&& (Decompiler.options & Decompiler.OPTION_CONTRAFO) != 0) {
		int slot = ((LocalLoadOperator)expr).getLocalInfo().getSlot();
		int pos = getOuterValueIndex(slot);
		if (pos < 0 || slot >= ovMaxSlots) {
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_CONSTRS) != 0)
			GlobalOptions.err.println("not outerValue: "+expr
						  +" ["+ovMinSlots
						  +","+ovMaxSlots+"]");
		    break big_loop;
		}
		expr = outerValues[pos];
		if (slot >= ovMinSlots)
		    ovMinSlots = slot + expr.getType().stackSize();
            } else if (!expr.isConstant()) {
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
			GlobalOptions.err.println("constr "+i+" differs: "+ib
						  +((InstructionBlock)ib).getInstruction().simplify()+" <!=> "+instr);
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
		if ((Decompiler.options & Decompiler.OPTION_CONTRAFO) != 0)
		    // Check for jikes continuation
		    checkJikesContinuation(cons[i]);
	    }
	    if ((Decompiler.options & Decompiler.OPTION_CONTRAFO) != 0
		&& (cons[i].getMethodHeader().block
		    instanceof InstructionBlock)) {
		checkAnonymousConstructor(cons[i],
					  (InstructionBlock)
					  cons[i].getMethodHeader().block);
	    }
	}
	ovMaxSlots = ovMinSlots;
	int ovLength = 0;
	if (outerValues != null) {
	    for (int slot=1; slot < ovMinSlots; ) {
		slot += outerValues[ovLength++].getType().stackSize();
	    }
	    if ((GlobalOptions.debuggingFlags
		 & GlobalOptions.DEBUG_CONSTRS) != 0)
		GlobalOptions.err.println("shrinking outerValues from "
					  + outerValues.length
					  + " to " + ovLength);

	    if (ovLength < outerValues.length) {
		Expression[] newOuterValues = new Expression[ovLength];
		System.arraycopy(outerValues, 0, newOuterValues, 0, ovLength);
		clazzAnalyzer.setOuterValues(newOuterValues);
	    }
	}

	/* Now tell _all_ constructors the value of outerValues-parameters
	 * and simplify them again.
	 */
	for (int i=0; i< cons.length; i++) {
	    CodeAnalyzer codeAna = cons[i].getCode();
	    if (codeAna == null)
		continue;
	    for (int j=0; j< ovLength; j++)
		codeAna.getParamInfo(j+1).setExpression(outerValues[j]);
	    codeAna.getMethodHeader().simplify();
	}	
    }
}
