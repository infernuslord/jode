/* TransformConstructors Copyright (C) 1997-1998 Jochen Hoenicke.
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
import jode.decompiler.ClassAnalyzer;
import jode.decompiler.MethodAnalyzer;
import jode.expr.*;

/**
 * 
 * @author Jochen Hoenicke
 */
public class TransformConstructors {
    
    public static void transform(ClassAnalyzer clazzAnalyzer,
                                 boolean isStatic, 
                                 MethodAnalyzer[] cons) {
        if (cons.length == 0)
            return;

        int constrCount = cons.length;
        StructuredBlock[] sb = new StructuredBlock[constrCount];
        for (int i=0; i< constrCount; ) {
	    FlowBlock header = cons[i].getMethodHeader();
	    if (!header.hasNoJumps())
		return;
            sb[i] = cons[i].getMethodHeader().block;
//             Decompiler.err.println("constr "+i+": "+sb[i]);
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
                
                if (!(instr instanceof ComplexExpression)
                    || !(instr.getOperator() instanceof InvokeOperator)
                    || !(((ComplexExpression)instr)
                         .getSubExpressions()[0].toString().equals("this")))
                    return;
                    
                InvokeOperator invoke = (InvokeOperator) instr.getOperator();
                if (!invoke.isConstructor() || !invoke.isSuperOrThis())
                    return;

                if (invoke.isThis()) {
                    /* This constructor calls another constructor, so we
                     * can skip it.
                     */
//                     Decompiler.err.println("skipping this()");
                    cons[i] = cons[--constrCount];
                    continue;
                }
                /* This constructor begins with a super call, as 
                 * expected. If the super() has no parameters, we
                 * can remove it as it is implicit.
                 */
                if (invoke.getMethodType().getParameterTypes().length == 0)
                    ib.removeBlock();
                if (sb[i] instanceof SequentialBlock)
                    sb[i] = sb[i].getSubBlocks()[1];
                else
                    sb[i] = null;
//                 Decompiler.err.println("normal constructor");
            }
            i++;
        }
        StructuredBlock[] start = new StructuredBlock[constrCount];
        for (int i=0; i< constrCount; i++)
            start[i] = sb[i];
    big_loop:
        for (;;) {
            StructuredBlock ib = 
                (sb[0] instanceof SequentialBlock) 
                ? sb[0].getSubBlocks()[0]
                : sb[0];
            if (!(ib instanceof InstructionBlock))
                break big_loop;

            Expression instr = ((InstructionBlock) ib).getInstruction();
            if (!(instr instanceof ComplexExpression)
                || !(instr.getOperator() instanceof PutFieldOperator)
                || (((PutFieldOperator)instr.getOperator()).isStatic() 
                    != isStatic))
                break big_loop;

            PutFieldOperator pfo = (PutFieldOperator) instr.getOperator();
            Expression expr =  ((ComplexExpression)instr)
                .getSubExpressions()[isStatic ? 0 : 1];


            if (!expr.isConstant()) {
//                 Decompiler.err.println("not constant: "+expr);
                break big_loop;
            }

//             Decompiler.err.println("field "+pfo.getFieldName()+ " = "+expr);

            if (!isStatic
                && !(((ComplexExpression)instr).getSubExpressions()[0]
                     .toString().equals("this"))) {
//                 Decompiler.err.println("not this: "+instr);
                break big_loop;
            }

            for (int i=1; i< constrCount; i++) {
                ib = (sb[i] instanceof SequentialBlock) 
                    ? sb[i].getSubBlocks()[0]
                    : sb[i];
                if (!(ib instanceof InstructionBlock)
                    || !((InstructionBlock)ib).getInstruction().equals(instr)) {
//                     Decompiler.err.println("constr "+i+" differs: "+ib);
                    break big_loop;
                }
            }


            if (!(clazzAnalyzer
		  .getField(pfo.getFieldName(), pfo.getFieldType())
		  .setInitializer(expr))) {
//                 Decompiler.err.println("setField failed");
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
//                     Decompiler.err.println("constr "+i+" is over");
                    break big_loop;
                }
        }
        for (int i=0; i< constrCount; i++) {
            if (start[i] == null)
                continue;
            if (sb[i] == null)
                start[i].removeBlock();
            else
                sb[i].replace(start[i]);
        }
    }
}
