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
import jode.expr.*;
import jode.bytecode.ClassInfo;
import jode.bytecode.InnerClassInfo;

/**
 * 
 * @author Jochen Hoenicke
 */
public class TransformConstructors {
    
    public static boolean isThis(Expression thisExpr, ClassInfo clazz) {
	return ((thisExpr instanceof ThisOperator)
		&& (((ThisOperator)thisExpr).getClassInfo() == clazz));
    }

    public static void transform(ClassAnalyzer clazzAnalyzer,
                                 boolean isStatic, boolean isMember,
				 boolean isAnonymous, MethodAnalyzer[] cons) {
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
		if (!invoke.isConstructor() || !invoke.isSuperOrThis()
		    || !isThis(invoke.getSubExpressions()[0], 
			       clazzAnalyzer.getClazz()))
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
                 * expected. If the super() has no parameters, we
                 * can remove it as it is implicit.
                 */
		InnerClassInfo outer = invoke.getOuterClassInfo();
		if (outer != null && outer.outer != null
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

	if (isMember || isAnonymous) {
	this_loop:
	    for (;;) {
		StructuredBlock ib = 
		    (sb[0] instanceof SequentialBlock) 
		    ? sb[0].getSubBlocks()[0]
		    : sb[0];

		if ((GlobalOptions.debuggingFlags
		     & GlobalOptions.DEBUG_CONSTRS) != 0)
		    GlobalOptions.err.println("isMember: "+ib);

		if (!(ib instanceof InstructionBlock))
		    break this_loop;
		
		Expression instr
		    = ((InstructionBlock) ib).getInstruction().simplify();
		if (!(instr instanceof StoreInstruction)
		    || instr.getFreeOperandCount() != 0)
		    break this_loop;

		StoreInstruction store = (StoreInstruction) instr;
		if (!(store.getLValue() instanceof PutFieldOperator))
		    break this_loop;

		PutFieldOperator pfo = (PutFieldOperator) store.getLValue();
		if (pfo.isStatic() || !pfo.isThis())
		    break this_loop;

		Expression expr = store.getSubExpressions()[1];

		if (isMember) {
		    if (!(expr instanceof ThisOperator))
			break this_loop;
		} else {
		    if (!(expr instanceof LocalLoadOperator))
			break this_loop;
		}

		if ((GlobalOptions.debuggingFlags
		     & GlobalOptions.DEBUG_CONSTRS) != 0)
		    GlobalOptions.err.println("field "+pfo.getFieldName()
					      + " = " + expr);
                if (!isThis(pfo.getSubExpressions()[0], 
			    clazzAnalyzer.getClazz())) {
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_CONSTRS) != 0)
			GlobalOptions.err.println("not this: "+instr);
		    break this_loop;
		}
		
		for (int i=1; i< constrCount; i++) {
		    ib = (sb[i] instanceof SequentialBlock) 
			? sb[i].getSubBlocks()[0]
			: sb[i];
		    if (!(ib instanceof InstructionBlock)
			|| !(((InstructionBlock)ib).getInstruction().simplify()
			     .equals(instr))) {
			if ((GlobalOptions.debuggingFlags
			     & GlobalOptions.DEBUG_CONSTRS) != 0)
			    GlobalOptions.err.println("constr "+i+" differs: "
						      +ib);
			break this_loop;
		    }
		}
		
		if (!(clazzAnalyzer
		      .getField(pfo.getFieldName(), pfo.getFieldType())
		      .setSpecial(expr))) {
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_CONSTRS) != 0)
			GlobalOptions.err.println("setField failed");
		    break this_loop;
		}
		
            
		for (int i=0; i< constrCount; i++) {
		    if (sb[i] instanceof SequentialBlock)
			sb[i] = sb[i].getSubBlocks()[1];
                else
                    sb[i] = null; 
		}
		for (int i=0; i< constrCount; i++) {
		    if (sb[i] == null) {
			if ((GlobalOptions.debuggingFlags
			     & GlobalOptions.DEBUG_CONSTRS) != 0)
			    GlobalOptions.err.println("constr "+i+" is over");
			break this_loop;
		    }
		}
	    }
	}
	    
	
    big_loop:
        for (;;) {
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
            for (int i=0; i< constrCount; i++)
                if (sb[i] == null) {
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_CONSTRS) != 0)
			GlobalOptions.err.println("constr "+i+" is over");
                    break big_loop;
                }
        }
	
        for (int i=0; i< constrCount; i++) {
            if (start[i] == null)
                continue;
            if (sb[i] == null)
                start[i].removeBlock();
            else {
                sb[i].replace(start[i]);
		sb[i].simplify();
	    }
        }
    }
}
