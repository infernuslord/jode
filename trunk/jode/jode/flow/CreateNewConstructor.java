/* CreateNewConstructor Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.expr.*;

public class CreateNewConstructor {

    public static boolean transform(InstructionContainer ic,
                                    StructuredBlock last) {
        /* Situation:
         *
         *  new <object>
         *  (optional DUP)
         *  PUSH load_ops
         *  optionally:  <= used for "string1 += string2"
         *      DUP_X2/1  <= 2 if above DUP is present
         *  stack_n.<init>((optional: stack_n), resolved expressions)
         *
         * transform it to
         *
         *  PUSH load_ops
         *  optionally:
         *      DUP       <= remove the depth
         *  (optional PUSH) new <object>((optional: stack_n), 
	 *                               resolved expressions)
         */

        if (!(last.outer instanceof SequentialBlock))
            return false;
        if (!(ic.getInstruction().getOperator() instanceof InvokeOperator))
            return false;
	Expression constrExpr = ic.getInstruction();
        InvokeOperator constrCall = (InvokeOperator) constrExpr.getOperator();
        if (!constrCall.isConstructor())
            return false;

        /* The rest should probably succeed */

        SpecialBlock optDupX2 = null;
        SequentialBlock sequBlock = (SequentialBlock) last.outer;

	if (constrExpr instanceof ComplexExpression) {
	    Expression[] subs = 
		((ComplexExpression) constrExpr).getSubExpressions();
	    if (!(subs[0] instanceof NopOperator))
		return false;
	    if (constrExpr.getOperandCount() > 1) {
		if (!(sequBlock.outer instanceof SequentialBlock)
		    || !(sequBlock.subBlocks[0] instanceof SpecialBlock))
		    return false;
		optDupX2 = (SpecialBlock) sequBlock.subBlocks[0];
		sequBlock = (SequentialBlock) sequBlock.outer;
		if (optDupX2.type != SpecialBlock.DUP 
		    || optDupX2.depth == 0)
		    return false;
		int count = optDupX2.count;
		int opcount = constrExpr.getOperandCount() - 1;
		do {
		    if (!(sequBlock.outer instanceof SequentialBlock)
			|| !(sequBlock.subBlocks[0] 
			     instanceof InstructionBlock))
			return false;
		    Expression expr = 
			((InstructionBlock) 
			 sequBlock.subBlocks[0]).getInstruction();
		    sequBlock = (SequentialBlock) sequBlock.outer;

		    if (expr.isVoid())
			continue;
		    count -= expr.getType().stackSize();
		    opcount--;
		} while (count > 0 && opcount > 0);
		if (opcount != 0 || count != 0)
		    return false;
	    } else if (constrExpr.getOperandCount() != 1)
		return false;
	} else if (constrExpr.getOperandCount() != 1)
	    return false;
	
        while (sequBlock.subBlocks[0] instanceof InstructionBlock
               && ((InstructionBlock)sequBlock.subBlocks[0])
               .getInstruction().isVoid()
               && sequBlock.outer instanceof SequentialBlock)
            sequBlock = (SequentialBlock) sequBlock.outer;
               
        SpecialBlock dup = null;
        if (sequBlock.outer instanceof SequentialBlock
            && sequBlock.subBlocks[0] instanceof SpecialBlock) {

            dup = (SpecialBlock) sequBlock.subBlocks[0];
            if (dup.type != SpecialBlock.DUP 
                || dup.count != 1 || dup.depth != 0)
                return false;
            sequBlock = (SequentialBlock)sequBlock.outer;
	    if (optDupX2 != null && optDupX2.depth != 2)
		return false;
        } else if (optDupX2 != null && optDupX2.depth != 1)
	    return false;

        if (!(sequBlock.subBlocks[0] instanceof InstructionBlock))
            return false;
        InstructionBlock block = (InstructionBlock) sequBlock.subBlocks[0];
        if (!(block.getInstruction() instanceof NewOperator))
            return false;
        
        NewOperator op = (NewOperator) block.getInstruction();
        if (constrCall.getClassType() != op.getType())
            return false;
        
        block.removeBlock();
        if (dup != null)
            dup.removeBlock();
        if (optDupX2 != null)
            optDupX2.depth = 0;

	Expression newExpr = new ConstructorOperator
	    (constrCall.getClassType(), constrCall.getMethodType(), 
	     dup == null);

	if (constrExpr instanceof ComplexExpression) {
	    Expression[] subs = 
		((ComplexExpression)constrExpr).getSubExpressions();
	    for (int i=subs.length - 1; i>=1; i--)
		if (!(subs[i] instanceof NopOperator))
		    newExpr = newExpr.addOperand(subs[i]);
	}
	ic.setInstruction(newExpr);
        return true;
    }
}

