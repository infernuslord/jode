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
import jode.decompiler.MethodAnalyzer;
import jode.decompiler.FieldAnalyzer;
import jode.decompiler.MethodAnalyzer;
import jode.decompiler.OuterValueListener;
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
 * @see jode.decompiler.ClassAnalyzer#getOuterValues 
 */
public class TransformConstructors implements OuterValueListener {
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
	this.ovMinSlots = 1;
	this.outerValues = clazzAnalyzer.getOuterValues();
	if (outerValues != null) {
	    clazzAnalyzer.addOuterValueListener(this);
	    ovMaxSlots = Integer.MAX_VALUE;
	    updateOuterValues(outerValues.length);
	} else
	    ovMaxSlots = 1;
    }

    public String dumpOuterValues() {
	if (outerValues == null)
	    return "null";
	StringBuffer sb = new StringBuffer();
	int slot = 1;
	for (int i=0; i < outerValues.length; i++) {
	    if (i>0)
		sb.append(", ");
	    if (slot == ovMinSlots)
		sb.append("[");
	    sb.append(outerValues[i]);
	    if (slot == ovMaxSlots)
		sb.append("]");
	    slot += outerValues[i].getType().stackSize();
	}
	sb.append(" ["+ovMinSlots+","+ovMaxSlots+"]");
	return sb.toString();
    }

    public void updateOuterValues(int count) {
	int outerSlots = 1;
	outerValues = clazzAnalyzer.getOuterValues();
	for (int i=0; i< count; i++)
	    outerSlots += outerValues[i].getType().stackSize();
	if (outerSlots < ovMaxSlots)
	    ovMaxSlots = outerSlots;

	if ((GlobalOptions.debuggingFlags
	     & GlobalOptions.DEBUG_CONSTRS) != 0)
	    GlobalOptions.err.println("OuterValues: " + dumpOuterValues());

	if (ovMaxSlots < ovMinSlots) {
	    GlobalOptions.err.println
		("WARNING: something got wrong with scoped class "
		 +clazzAnalyzer.getClazz()+": "+ovMinSlots+","+ovMaxSlots);
	    GlobalOptions.err.println
		("CAN'T REPAIR.  PRODUCED CODE IS PROBABLY WRONG.");
	    Thread.dumpStack();
	    ovMinSlots = outerSlots;
	}
    }
    
    public void shrinkingOuterValues(ClassAnalyzer ca, int newCount) {
	updateOuterValues(newCount);
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
	Expression expr = superBlock.getInstruction().simplify();

	if (!(expr instanceof InvokeOperator))
	    return false;
	
	InvokeOperator superCall = (InvokeOperator) expr;
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
	 * The outerValues[0] parameter is the normal this of the
	 * surrounding method (but what is the surrounding method?
	 * That can't be determined in some cases).  If the
	 * surrounding method is static, the outerValues[0] parameter
	 * disappears!
	 *
	 * Move optional super to method constructor$?
	 * (renaming local variables) and mark constructor and
	 * constructor$? as Jikes constructor.  */

	StructuredBlock sb = constr.getMethodHeader().block;
	
	Vector localLoads = null;
	InstructionBlock superBlock = null;
	boolean mayBeAnonInner = false;
	int anonInnerSlot = 1;
	if (sb instanceof SequentialBlock) {

	    if (!(sb.getSubBlocks()[0] instanceof InstructionBlock)
		|| !(sb.getSubBlocks()[1] instanceof InstructionBlock))
		return false;
	    superBlock = (InstructionBlock) sb.getSubBlocks()[0];
	    sb = sb.getSubBlocks()[1];

	    Expression superExpr = superBlock.getInstruction().simplify();
	    if (!(superExpr instanceof InvokeOperator))
		return false;

	    InvokeOperator superInvoke = (InvokeOperator) superExpr;
	    superBlock.setInstruction(superInvoke);
	    
	    if (superInvoke.getFreeOperandCount() != 0
		|| !superInvoke.isConstructor() 
		|| !superInvoke.isSuperOrThis())
		return false;
	    
	    Expression[] subExpr = superInvoke.getSubExpressions();
	    Expression thisExpr = subExpr[0];
	    if (!isThis(thisExpr, clazzAnalyzer.getClazz()))
		return false;

	    ClassInfo superClazz = superInvoke.getClassInfo();
	    if ((Decompiler.options & 
		 (Decompiler.OPTION_ANON | Decompiler.OPTION_INNER)) != 0
		&& clazzAnalyzer.getName() == null
		&& superClazz.getOuterClasses() != null
		&& subExpr[1] instanceof LocalLoadOperator) {
		Type[] paramTypes = constrType.getParameterTypes();
		anonInnerSlot = 1;
		for (int i=0; i< paramTypes.length-1; i++) {
		    anonInnerSlot += paramTypes[i].stackSize();
		}
		if (((LocalLoadOperator) 
		     subExpr[1]).getLocalInfo().getSlot() == anonInnerSlot)
		    mayBeAnonInner = true;
	    }

	    localLoads = new Vector();
	    for (int i=2; i< subExpr.length; i++) {
		if (!checkJikesSuperAndFillLoads(subExpr[i], localLoads))
		    return false;
	    }

	} else if (!(sb instanceof InstructionBlock))
	    return false;

	/* Now check the constructor$? invocation */
	Expression lastExpr
	    = ((InstructionBlock)sb).getInstruction().simplify();
	if (!(lastExpr instanceof InvokeOperator))
	    return false;

	InvokeOperator invoke = (InvokeOperator) lastExpr;
	if (!invoke.isThis()
	    || invoke.getFreeOperandCount() != 0)
	    return false;
	MethodAnalyzer methodAna = invoke.getMethodAnalyzer();
	if (methodAna == null)
	    return false;
	MethodType methodType = methodAna.getType();
	Expression[] constrParams = invoke.getSubExpressions();

	if (!methodAna.getName().startsWith("constructor$")
	    || methodType.getReturnType() != Type.tVoid)
	    return false;

	if (!isThis(invoke.getSubExpressions()[0], 
		    clazzAnalyzer.getClazz()))
	    return false;
	
	/* Now check if this must be of the anonymous extends inner
         * class form */
	if (mayBeAnonInner
	    && constrParams.length > 0
	    && !(constrParams[constrParams.length-1] 
		 instanceof LocalLoadOperator
		 && (((LocalLoadOperator)
		      constrParams[constrParams.length-1]).getLocalInfo()
		     .getSlot() == anonInnerSlot)))
	    jikesAnonInner = true;


	int ovLength = constrType.getParameterTypes().length
	    - methodType.getParameterTypes().length;

	if (jikesAnonInner)
	    ovLength--;

	int firstArg = 1;
	boolean unsureOuter = false;
	boolean canHaveOuter = false;
	if (outerValues != null && outerValues.length > 0
	    && outerValues[0] instanceof ThisOperator
	    && firstArg < constrParams.length
	    && constrParams[firstArg] instanceof LocalLoadOperator
	    && ((LocalLoadOperator) 
		constrParams[firstArg]).getLocalInfo().getSlot() == 1) {
	    if (ovLength == 0 && ovMinSlots == 1)
		/* It is not sure, if outerValues[0] is present or not.
		 */
		unsureOuter = true;
	    
	    /* Assume outerValues[0] is there */
	    ovLength++;
	    firstArg++;
	    canHaveOuter = true;
	}

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

	if (ovSlots < ovMinSlots || ovSlots > ovMaxSlots)
	    return false;

	{
	    int slot = ovSlots;
	    for (int j = firstArg; j < constrParams.length; j++) {
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

	if (unsureOuter) {
	    ovMaxSlots = 2;
	} else {
	    ovMinSlots = ovMaxSlots = ovSlots;
	}
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
		llop.setMethodAnalyzer(methodAna);
		llop.setLocalInfo(methodAna.getLocalInfo(0, newSlot));
	    }
	    methodAna.insertStructuredBlock(superBlock);
	}
	clazzAnalyzer.setJikesAnonymousInner(jikesAnonInner);
	constr.setJikesConstructor(true);
	methodAna.setJikesConstructor(true);
	methodAna.setHasOuterValue(canHaveOuter);
	if (constr.isAnonymousConstructor()
	    && methodAna.getMethodHeader().block instanceof EmptyBlock)
	    methodAna.setAnonymousConstructor(true);
	return true;
    }

    /**
     * This methods checks if expr is a valid field initializer.  It
     * will also merge outerValues, that occur in expr.
     * @param expr the initializer to check
     * @return the transformed initializer or null if expr is not valid.
     */
    public Expression transformFieldInitializer(Expression expr) {
	if (expr instanceof LocalVarOperator) {
	    if (!(expr instanceof LocalLoadOperator)) {
		if ((GlobalOptions.debuggingFlags
		     & GlobalOptions.DEBUG_CONSTRS) != 0)
		    GlobalOptions.err.println("illegal local op: "+expr);
		return null;
	    }
	    if (outerValues != null
		&& (Decompiler.options & Decompiler.OPTION_CONTRAFO) != 0) {
		int slot = ((LocalLoadOperator)expr).getLocalInfo().getSlot();
		int pos = getOuterValueIndex(slot);
		if (pos >= 0 && slot < ovMaxSlots) {
		    expr = outerValues[pos];
		    if (slot >= ovMinSlots)
			ovMinSlots = slot + expr.getType().stackSize();
		    return expr;
		}
            }
	    if ((GlobalOptions.debuggingFlags
		 & GlobalOptions.DEBUG_CONSTRS) != 0)
		GlobalOptions.err.println("not outerValue: "+expr
					  +" ["+ovMinSlots
					  +","+ovMaxSlots+"]");
	    return null;
	}
	if (expr instanceof Operator) {
	    Operator op = (Operator) expr;
	    Expression[] subExpr = op.getSubExpressions();
	    for (int i=0; i< subExpr.length; i++) {
		Expression transformed = transformFieldInitializer(subExpr[i]);
		if (transformed == null)
		    return null;
		if (transformed != subExpr[i])
		    op.setSubExpressions(i, transformed);
	    }
	}
	return expr;
    }

    public void initSyntheticFields() {
	if (isStatic)
	    return;

        if (cons.length == 0)
            return;

        int constrCount = cons.length;
        StructuredBlock[] sb = new StructuredBlock[constrCount];
        for (int i=0; i< constrCount; ) {
	    FlowBlock header = cons[i].getMethodHeader();
	    /* Check that code block is fully analyzed */
	    if (header == null || !header.hasNoJumps())
		return;

	    /* sb[i] will iterate the instructions of the constructor. */
            sb[i] = cons[i].getMethodHeader().block;
	    if ((GlobalOptions.debuggingFlags
		 & GlobalOptions.DEBUG_CONSTRS) != 0)
		GlobalOptions.err.println("constr "+i+": "+sb[i]);

	    /* A non static constructor must begin with a call to
	     * another constructor.  Either to a constructor of the
	     * same class or to the super class
	     */
	    InstructionBlock ib;
	    if (sb[i] instanceof InstructionBlock)
		ib = (InstructionBlock)sb[i];
	    else if (sb[i] instanceof SequentialBlock
		     && (sb[i].getSubBlocks()[0] 
			 instanceof InstructionBlock))
		ib = (InstructionBlock) sb[i].getSubBlocks()[0];
	    else
		return;
	    
	    Expression superExpr = ib.getInstruction().simplify();
	    if (!(superExpr instanceof InvokeOperator)
		|| superExpr.getFreeOperandCount() != 0)
		return;
	    InvokeOperator superInvoke = (InvokeOperator) superExpr;
	    if (!superInvoke.isConstructor()
		|| !superInvoke.isSuperOrThis())
		return;
	    Expression thisExpr = superInvoke.getSubExpressions()[0];
	    if (!isThis(thisExpr, clazzAnalyzer.getClazz()))
		return;
	    
	    if (superInvoke.isThis()) {
		/* This constructor calls another constructor of this
		 * class, which will do the initialization.  We can skip
		 * this constructor.
		 */
		/* Move constructor to the end of cons array, and
		 * decrease constrCount.  It will not be transformed
		 * any further.
		 */
		MethodAnalyzer temp = cons[i];
		cons[i] = cons[--constrCount];
		cons[constrCount] = temp;
		continue;
	    }
	    
	    /* This constructor begins with a super call, as 
	     * expected. 
	     */
	    
	    if (sb[i] instanceof SequentialBlock)
		sb[i] = sb[i].getSubBlocks()[1];
	    else
		sb[i] = null;
	    if ((GlobalOptions.debuggingFlags
		 & GlobalOptions.DEBUG_CONSTRS) != 0)
		GlobalOptions.err.println("normal constructor");
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

	    if (!isThis(pfo.getSubExpressions()[0], 
			clazzAnalyzer.getClazz()))
		break big_loop;

	    FieldAnalyzer field = clazzAnalyzer.getField(pfo.getFieldName(), 
							 pfo.getFieldType());

	    /* Don't check for final.  Jikes sometimes omits this attribute.
	     */
	    if (!field.isSynthetic())
		break big_loop;

            Expression expr =  store.getSubExpressions()[1];
	    expr = transformFieldInitializer(expr);
	    if (expr == null)
		break big_loop;

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


            if (!(field.setInitializer(expr))) {
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
	    }
	}
    }


    public void transformBlockInitializer(StructuredBlock block) {
	StructuredBlock start = block;
	while (block != null) {
	init_loop:
	    do {
		StructuredBlock ib = 
		    (block instanceof SequentialBlock) 
		    ? block.getSubBlocks()[0]
		    : block;
		
		if ((GlobalOptions.debuggingFlags
		     & GlobalOptions.DEBUG_CONSTRS) != 0)
		    GlobalOptions.err.println("fieldInit: "+ib);
		
		if (!(ib instanceof InstructionBlock))
		    break init_loop;
		
		Expression instr
		    = ((InstructionBlock) ib).getInstruction().simplify();
		
		if (!(instr instanceof StoreInstruction)
		    || instr.getFreeOperandCount() != 0)
		    break init_loop;
		
		StoreInstruction store = (StoreInstruction) instr;
		if (!(store.getLValue() instanceof PutFieldOperator))
		    break init_loop;
		
		PutFieldOperator pfo = (PutFieldOperator) store.getLValue();
		if (pfo.isStatic() != isStatic || !pfo.isThis())
		    break init_loop;
		
		if (!isStatic) {
		    if (!isThis(pfo.getSubExpressions()[0], 
				clazzAnalyzer.getClazz())) {
			if ((GlobalOptions.debuggingFlags
			     & GlobalOptions.DEBUG_CONSTRS) != 0)
			    GlobalOptions.err.println("not this: "+instr);
			break init_loop;
		    }
		}
		
		Expression expr =  store.getSubExpressions()[1];
		expr = transformFieldInitializer(expr);
		if (expr == null)
		    break init_loop;
		
		if ((GlobalOptions.debuggingFlags
		     & GlobalOptions.DEBUG_CONSTRS) != 0)
		    GlobalOptions.err.println("field " + pfo.getFieldName()
					      + " = " + expr);
				
		FieldAnalyzer field = clazzAnalyzer
		    .getField(pfo.getFieldName(), pfo.getFieldType());

		if (!(field.setInitializer(expr))) {
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_CONSTRS) != 0)
			GlobalOptions.err.println("setField failed");
		    break init_loop;
		}

		block.removeBlock();
		if (start != block) {
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_CONSTRS) != 0)
			GlobalOptions.err.println("adding block initializer");
		    
		    clazzAnalyzer.addBlockInitializer(field, start);
		}

		if (block instanceof SequentialBlock)
		    start = block.getSubBlocks()[1];
		else
		    start = null;
	    } while (false);
	    
	    if (block instanceof SequentialBlock)
		block = block.getSubBlocks()[1];
	    else
		block = null; 
	}

	if (start != null) {
	    if ((GlobalOptions.debuggingFlags
		 & GlobalOptions.DEBUG_CONSTRS) != 0)
		GlobalOptions.err.println("adding block initializer");
	    
	    clazzAnalyzer.addBlockInitializer(null, start);
	}
    }

    public boolean checkBlockInitializer(InvokeOperator invoke) {
	if (!invoke.isThis()
	    || invoke.getFreeOperandCount() != 0)
	    return false;
	MethodAnalyzer methodAna = invoke.getMethodAnalyzer();
	if (methodAna == null)
	    return false;
	FlowBlock flow = methodAna.getMethodHeader();
	MethodType methodType = methodAna.getType();
	if (!methodAna.getName().startsWith("block$")
	    || methodType.getParameterTypes().length != 0
	    || methodType.getReturnType() != Type.tVoid)
	    return false;
	if (flow == null || !flow.hasNoJumps())
	    return false;

	if (!isThis(invoke.getSubExpressions()[0], 
		    clazzAnalyzer.getClazz()))
	    return false;
	
	methodAna.setJikesBlockInitializer(true);
	transformBlockInitializer(flow.block);
	return true;
    }

    /**
     * This does the transformations.  It will set the field initializers
     * and removing the initializers from all constructors.
     */
    public void transform() {
        if (cons.length == 0)
            return;

        int constrCount = cons.length;
        StructuredBlock[] sb = new StructuredBlock[constrCount];
        for (int i=0; i< constrCount; ) {
	    FlowBlock header = cons[i].getMethodHeader();
	    /* Check that code block is fully analyzed */
	    if (header == null || !header.hasNoJumps())
		return;

	    /* sb[i] will iterate the instructions of the constructor. */
            sb[i] = cons[i].getMethodHeader().block;
	    if ((GlobalOptions.debuggingFlags
		 & GlobalOptions.DEBUG_CONSTRS) != 0)
		GlobalOptions.err.println("constr "+i+": "+sb[i]);

            if (!isStatic) {
		/* A non static constructor must begin with a call to
		 * another constructor.  Either to a constructor of the
		 * same class or to the super class
		 */
                InstructionBlock ib;
                if (sb[i] instanceof InstructionBlock)
                    ib = (InstructionBlock)sb[i];
                else if (sb[i] instanceof SequentialBlock
                         && (sb[i].getSubBlocks()[0] 
                             instanceof InstructionBlock))
                    ib = (InstructionBlock) sb[i].getSubBlocks()[0];
                else
                    return;

                Expression superExpr = ib.getInstruction().simplify();
                if (!(superExpr instanceof InvokeOperator)
		    || superExpr.getFreeOperandCount() != 0)
		    return;
                InvokeOperator superInvoke = (InvokeOperator) superExpr;
		if (!superInvoke.isConstructor()
		    || !superInvoke.isSuperOrThis())
                    return;
		Expression thisExpr = superInvoke.getSubExpressions()[0];
		if (!isThis(thisExpr, clazzAnalyzer.getClazz()))
		    return;

                if (superInvoke.isThis()) {
                    /* This constructor calls another constructor of this
		     * class, which will do the initialization.  We can skip
		     * this constructor.
                     */
		    /* But first check that outerValues are correctly promoted
		     * XXX: Note that I couldn't check this code, yet,
		     * since I couldn't find a compiler that can handle
		     * this() calls in method scoped classes :-(
		     */
		    for (int slot = 1, j=0; slot < ovMaxSlots; j++) {
			Expression param
			    = superInvoke.getSubExpressions()[j+1];
			if (!(param instanceof LocalLoadOperator)
			    || ((LocalLoadOperator) 
				param).getLocalInfo().getSlot() != slot) {
			    ovMaxSlots = slot;
			    break;
			}
			slot += param.getType().stackSize();
		    }

		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_CONSTRS) != 0)
			GlobalOptions.err.println("skipping this()");
		    /* Move constructor to the end of cons array, and
		     * decrease constrCount.  It will not be transformed
		     * any further.
		     */
		    MethodAnalyzer temp = cons[i];
                    cons[i] = cons[--constrCount];
		    cons[constrCount] = temp;
                    continue;
                }

                /* This constructor begins with a super call, as 
                 * expected. 
		 */
		ClassInfo superClazz = superInvoke.getClassInfo();
		InnerClassInfo[] outers = superClazz.getOuterClasses();
		/* If the super() has no parameters (or only default
		 * outerValue parameter for inner/anonymous classes), we
		 * can remove it 
		 */
		if ((Decompiler.options & Decompiler.OPTION_ANON) != 0
		    && outers != null
		    && (outers[0].outer == null || outers[0].name == null)) {
		    ClassAnalyzer superAnalyzer = null;
		    Object parent = clazzAnalyzer.getParent();
		    while (parent != null) {
			if (parent instanceof MethodAnalyzer) {
			    MethodAnalyzer methodAna = (MethodAnalyzer)parent;
			    parent = methodAna.getClassAnalyzer().getParent();
			} else
			    parent = ((ClassAnalyzer) parent).getParent();
		    }
		    if (superAnalyzer != null) {
			/* XXX check outer values.
			 */
		    }
		} else if ((Decompiler.options & Decompiler.OPTION_INNER) != 0
			   && outers != null
			   && outers[0].outer != null
			   && outers[0].name != null) {
		    if (!Modifier.isStatic(outers[0].modifiers)
			&& (superInvoke.getMethodType().getParameterTypes()
			    .length == 1)
			&& (superInvoke.getSubExpressions()[1]
			    instanceof ThisOperator))
			ib.removeBlock();
		} else if (superInvoke.getMethodType().getParameterTypes()
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

	    if (instr instanceof InvokeOperator
		&& checkBlockInitializer((InvokeOperator) instr)) {
		for (int i=0; i< constrCount; i++) {
		    if (sb[i] instanceof SequentialBlock)
			sb[i] = sb[i].getSubBlocks()[1];
		    else
			sb[i] = null; 
		}
		break big_loop;
	    }
		
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
	    expr = transformFieldInitializer(expr);
	    if (expr == null)
		break big_loop;

	    if ((GlobalOptions.debuggingFlags
		 & GlobalOptions.DEBUG_CONSTRS) != 0)
		GlobalOptions.err.println("field " + pfo.getFieldName()
					  + " = " + expr);

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
	ovMinSlots = ovMaxSlots;
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
		clazzAnalyzer.shrinkOuterValues(ovLength);
	    }
	}

	/* Now tell _all_ constructors the value of outerValues-parameters
	 * and simplify them again.
	 */
	for (int i=0; i< cons.length; i++) {
	    for (int j=0; j< ovLength; j++)
		cons[i].getParamInfo(j+1).setExpression(outerValues[j]);
	    cons[i].getMethodHeader().simplify();
	}	
    }
}
