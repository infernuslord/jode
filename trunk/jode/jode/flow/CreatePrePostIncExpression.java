/* CreatePrePostIncExpression Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.type.Type;

public class CreatePrePostIncExpression {

    public static boolean transform(InstructionContainer ic, 
                                    StructuredBlock last)
    {
        return (createLocalPrePostInc(ic, last) || createPostInc(ic, last));
    }
    
    public static boolean createLocalPrePostInc(InstructionContainer ic, 
                                                StructuredBlock last) {
        /* Situations:
         *
         *   PUSH local_x        ->  PUSH local_x++
	 *   IINC local_x, +/-1
	 *
	 *   IINC local_x, +/-1
         *   PUSH local_x        ->  PUSH ++local_x
         */

        if (!(last.outer instanceof SequentialBlock) 
            || !(last.outer.getSubBlocks()[0] instanceof InstructionBlock))
	    return false;
	    
	Expression instr1 = ((InstructionBlock)
			     last.outer.getSubBlocks()[0]).getInstruction();
	Expression instr2 = ic.getInstruction();
	
	IIncOperator iinc;
	LocalLoadOperator load;
	boolean isPost;
	if (instr1 instanceof IIncOperator 
	    && instr2 instanceof LocalLoadOperator) {
	    iinc = (IIncOperator) instr1;
	    load = (LocalLoadOperator) instr2;
	    isPost = false;
	} else if (instr1 instanceof LocalLoadOperator
		   && instr2 instanceof IIncOperator) {
	    load = (LocalLoadOperator) instr1;
	    iinc = (IIncOperator) instr2;
	    isPost = true;
	} else
	    return false;
	
	int op;
	if (iinc.getOperatorIndex() == iinc.ADD_OP + iinc.OPASSIGN_OP)
	    op = Operator.INC_OP;
	else if (iinc.getOperatorIndex() == iinc.SUB_OP + iinc.OPASSIGN_OP)
	    op = Operator.DEC_OP;
	else
	    return false;
	
	if (iinc.getValue().equals("-1"))
	    op ^= 1;
	else if (!iinc.getValue().equals("1"))
	    return false;
	
	if (!iinc.matches(load))
	    return false;
	
	Type type = load.getType().intersection(Type.tUInt);
	Operator ppop = 
	    new LocalPrePostFixOperator(type, op, iinc, isPost);
	
	ic.setInstruction(ppop);
	ic.moveDefinitions(last.outer, last);
	last.replace(last.outer);
	return true;
    }
    
    public static boolean createPostInc(InstructionContainer ic, 
                                        StructuredBlock last) {

        /* Situation:
         *
         *   PUSH load/storeOps (optional/
	 *                       not checked)  PUSH load/storeOps
         *   DUP  load/storeOps (optional)     PUSH store++/--
         *   PUSH load(stack)
         *   DUP_X(storeOps count)          ->
         *   store(stack) = stack_0 +/- 1
         */

        if (!(ic.getInstruction() instanceof ComplexExpression)
	    || !(ic.getInstruction().getOperator() instanceof StoreInstruction)
            || !(ic.getInstruction().isVoid()))
            return false;
        
	ComplexExpression storeExpr = (ComplexExpression) ic.getInstruction();
        StoreInstruction store = 
            (StoreInstruction) ic.getInstruction().getOperator();

	/* Make sure that the lvalue part of the store is
	 * not yet resolved (and not that the rvalue part 
	 * should also have 1 remaining operand)
	 */
	int storeParams = store.getLValueOperandCount();
	if (storeExpr.getOperandCount() != storeParams + 1
	    || !(storeExpr.getSubExpressions()[storeParams] 
		 instanceof ComplexExpression))
	    return false;
	if (storeParams > 0) {
	    for (int i=0; i< storeParams; i++) {
		if (!(storeExpr.getSubExpressions()[i] instanceof NopOperator))
		    return false;
	    }
	}

        ComplexExpression binExpr = (ComplexExpression)
	    storeExpr.getSubExpressions()[storeParams];
        if (!(binExpr instanceof ComplexExpression)
	    || !(binExpr.getOperator() instanceof BinaryOperator)
	    || !(binExpr.getSubExpressions()[0] instanceof NopOperator)
	    || !(binExpr.getSubExpressions()[1] instanceof ConstOperator))
            return false;

        BinaryOperator binOp = (BinaryOperator) binExpr.getOperator();
        ConstOperator constOp = (ConstOperator) binExpr.getSubExpressions()[1];
        int op;
        if (binOp.getOperatorIndex() == store.ADD_OP)
            op = Operator.INC_OP;
        else if (binOp.getOperatorIndex() == store.SUB_OP)
            op = Operator.DEC_OP;
        else
            return false;
          
        if (constOp.getValue().equals("-1")
	    || constOp.getValue().equals("-1.0"))
            op ^= 1;
        else if (!constOp.getValue().equals("1")
		 && !constOp.getValue().equals("-1.0"))
            return false;

        if (!(last.outer instanceof SequentialBlock))
            return false;
        SequentialBlock sb = (SequentialBlock)last.outer;
        if (!(sb.subBlocks[0] instanceof SpecialBlock))
            return false;
            
        SpecialBlock dup = (SpecialBlock) sb.subBlocks[0];
        if (dup.type != SpecialBlock.DUP
            || dup.count != store.getLValueType().stackSize()
            || dup.depth != store.getLValueOperandCount())
            return false;

        if (!(sb.outer instanceof SequentialBlock))
            return false;
        sb = (SequentialBlock) sb.outer;
        if (!(sb.subBlocks[0] instanceof InstructionBlock))
            return false;
        InstructionBlock ib = (InstructionBlock) sb.subBlocks[0];

        if (!(ib.getInstruction() instanceof Operator)
            || !store.matches((Operator) ib.getInstruction()))
            return false;

        if (store.getLValueOperandCount() > 0) {
            if (!(sb.outer instanceof SequentialBlock))
                return false;
            sb = (SequentialBlock) sb.outer;
            if (!(sb.subBlocks[0] instanceof SpecialBlock))
                return false;
            SpecialBlock dup2 = (SpecialBlock) sb.subBlocks[0];
            if (dup2.type != SpecialBlock.DUP
                || dup2.count != store.getLValueOperandCount() 
                || dup2.depth != 0)
                return false;
        }
        ic.setInstruction
            (new PrePostFixOperator(store.getLValueType(), op, store, true));
        ic.moveDefinitions(sb, last);
        last.replace(sb);
	return true;
    }
}
