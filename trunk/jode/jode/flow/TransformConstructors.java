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
import jode.ClassAnalyzer;
import jode.Expression;
import jode.ComplexExpression;
import jode.InvokeOperator;
import jode.PutFieldOperator;

/**
 * 
 * @author Jochen Hoenicke
 */
public class TransformConstructors {
    
    public static void transform(ClassAnalyzer clazzAnalyzer,
                                 boolean isStatic, 
                                 jode.MethodAnalyzer[] cons) {
        if (cons.length == 0)
            return;

        InstructionBlock[] superCall = new InstructionBlock[cons.length];
        StructuredBlock[] start = new StructuredBlock[cons.length];
        StructuredBlock[] sb = new StructuredBlock[cons.length];
        for (int i=0; i< sb.length; i++) {
            sb[i] = cons[i].getMethodHeader().block;
            if (sb[i] == null)
                return;
            if (!isStatic &&
                sb[i] instanceof SequentialBlock
                && sb[i].getSubBlocks()[0] instanceof InstructionBlock) {
                superCall[i] = (InstructionBlock) sb[i].getSubBlocks()[0];
                
                if (!(superCall[i].getInstruction().getOperator()
                      instanceof InvokeOperator)
                    || !((InvokeOperator)superCall[i].getInstruction()
                         .getOperator()).isConstructor())
                    superCall[i] = null;
                else
                    /* skip super call */
                    sb[i] = sb[i].getSubBlocks()[1];
            }
        }
        for (int i=0; i< sb.length; i++)
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
//                 System.err.println("not constant: "+expr);
                break big_loop;
            }

//             System.err.println("field "+pfo.getFieldName()+ " = "+expr);

            if (!isStatic
                && !(((ComplexExpression)instr).getSubExpressions()[0]
                     .toString().equals("this"))) {
//                 System.err.println("not this: "+instr);
                break big_loop;
            }

            for (int i=1; i< sb.length; i++) {
                ib = (sb[0] instanceof SequentialBlock) 
                    ? sb[0].getSubBlocks()[0]
                    : sb[0];
                if (!(ib instanceof InstructionBlock)
                    || !((InstructionBlock)ib).getInstruction().equals(instr)) {
//                     System.err.println("constr "+i+" differs: "+ib);
                    break big_loop;
                }
            }


            if (!clazzAnalyzer.setFieldInitializer(pfo.getFieldName(), expr)) {
//                 System.err.println("setField failed");
                break big_loop;
            }
                                                   
            
            for (int i=0; i< sb.length; i++) {
                if (sb[i] instanceof SequentialBlock)
                    sb[i] = sb[i].getSubBlocks()[1];
                else
                    sb[i] = null; 
            }
            for (int i=0; i< sb.length; i++)
                if (sb[i] == null) {
//                     System.err.println("constr "+i+" is over");
                    break big_loop;
                }
        }
        for (int i=0; i< superCall.length; i++) {
            if (sb[i] == null)
                start[i].removeBlock();
            else
                sb[i].replace(start[i]);
            if (superCall[i] != null) {
                InvokeOperator op = 
                    (InvokeOperator) superCall[i].getInstruction()
                    .getOperator();
                /* super() is implicit */
                if (op.getMethodType().getParameterTypes().length == 0)
                    superCall[i].removeBlock();
            }
        }
    }
}
