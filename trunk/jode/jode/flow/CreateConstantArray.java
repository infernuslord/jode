/* CreateConstantArray Copyright (C) 1997-1998 Jochen Hoenicke.
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
import jode.Expression;
import jode.DupOperator;
import jode.ArrayStoreOperator;
import jode.NewArrayOperator;
import jode.ConstantArrayOperator;
import jode.ConstOperator;
import jode.MyType;
import sun.tools.java.Type;

public class CreateConstantArray implements Transformation {

    public boolean transform(FlowBlock flow) {
	InstructionBlock lastBlock;
	SequentialBlock sequBlock;
        Expression[] consts = null;
	int count = 0;
        Type type;
        try {
	    InstructionBlock ib = (InstructionBlock) flow.lastModified;
            if (ib.getInstruction() instanceof DupOperator)
                /* this is not the end of the array assign */
                return false;
	    
	    sequBlock = (SequentialBlock) ib.outer;
	    ib = (InstructionBlock) sequBlock.subBlocks[0];
	    lastBlock = ib;

            int lastindex = -1;
            while (ib.getInstruction() instanceof ArrayStoreOperator) {
                ArrayStoreOperator store = 
                    (ArrayStoreOperator) ib.getInstruction();

		sequBlock = (SequentialBlock) sequBlock.outer;
		ib = (InstructionBlock) sequBlock.subBlocks[0];
		Expression lastconst = (Expression) ib.getInstruction();

		sequBlock = (SequentialBlock) sequBlock.outer;
		ib = (InstructionBlock) sequBlock.subBlocks[0];
                Expression indexexpr = (Expression) ib.getInstruction();
                ConstOperator indexop = 
                    (ConstOperator) indexexpr.getOperator();
                if (!MyType.isOfType(indexop.getType(), MyType.tUInt))
                    return false;
                int index = Integer.parseInt(indexop.getValue());
                if (index >= 0 && consts == null) {
                    lastindex = index;
                    consts = new Expression[lastindex+1];
                } else if (index < 0 || index > lastindex)
                    return false;
		else { 
                    while (index < lastindex) {
                        consts[lastindex--] = new Expression
                            (new ConstOperator(MyType.tUnknown, "0"), 
                             new Expression[0]);
                    }
                }
                consts[lastindex--] = lastconst;
		sequBlock = (SequentialBlock) sequBlock.outer;
		ib = (InstructionBlock) sequBlock.subBlocks[0];
                DupOperator dup = (DupOperator) ib.getInstruction();
                if (dup.getDepth() != 0 || 
                    dup.getCount() != store.getLValueType().stackSize())
                    return false;
		count++;
		sequBlock = (SequentialBlock) sequBlock.outer;
		ib = (InstructionBlock) sequBlock.subBlocks[0];
            }
            if (count == 0)
                return false;
            while (lastindex >= 0) {
                consts[lastindex--] = new Expression
                    (new ConstOperator(MyType.tUnknown, "0"), 
                     new Expression[0]);
            }
            Expression newArrayExpr = (Expression) ib.getInstruction();
            NewArrayOperator newArrayOp = 
                (NewArrayOperator) newArrayExpr.getOperator();
            type = newArrayOp.getType();
            if (newArrayOp.getOperandCount() != 1)
                return false;
            Expression countexpr = 
                (Expression) newArrayExpr.getSubExpressions()[0];
            ConstOperator countop = 
                (ConstOperator) countexpr.getOperator();
            if (!MyType.isOfType(countop.getType(), MyType.tUInt))
                return false;
            int arraylength = Integer.parseInt(countop.getValue());
            if (arraylength != consts.length) {
                if (arraylength < consts.length)
                    return false;
                Expression[] newConsts = new Expression[arraylength];
                System.arraycopy(consts, 0, newConsts, 0, consts.length);
                for (int i=consts.length; i<arraylength; i++)
                    newConsts[i] = new Expression
                        (new ConstOperator(MyType.tUnknown, "0"), 
                         new Expression[0]);
                consts = newConsts;
            }
        } catch (NullPointerException ex) {
            return false;
        } catch (ClassCastException ex) {
            return false;
        }
        if (jode.Decompiler.isVerbose)
            System.err.print("a");

	lastBlock.setInstruction
	    (new Expression(new ConstantArrayOperator(type, consts.length), 
			    consts));
	lastBlock.replace(sequBlock.subBlocks[0], lastBlock);
	flow.lastModified.replace(sequBlock.subBlocks[1], flow.lastModified);
        return true;
    }
}
