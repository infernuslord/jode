/* SequentialBlock Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.decompiler.TabbedPrintWriter;
import jode.decompiler.LocalInfo;
import jode.expr.LocalStoreOperator;

/**
 * A sequential block combines exactly two structured blocks to a new
 * one. The first sub block mustn't be another sequential block,
 * instead the second sub block should be used for this.  This
 * condition is temporarily violated, while the t1 transformation is
 * done.
 */
public class SequentialBlock extends StructuredBlock {
    StructuredBlock[] subBlocks;

    public SequentialBlock() {
        subBlocks = new StructuredBlock[2];
    }

    public void setFirst(StructuredBlock sb) {
        subBlocks[0] = sb;
        sb.outer = this;
        sb.setFlowBlock(flowBlock);
    }

    public void setSecond(StructuredBlock sb) {
        subBlocks[1] = sb;
        sb.outer = this;
        sb.setFlowBlock(flowBlock);
    }

    public void checkConsistent() {
        super.checkConsistent();
        if (subBlocks[0].jump != null
            || subBlocks[0] instanceof SequentialBlock
            || jump != null)
            throw new jode.AssertError("Inconsistency");
    }

    /**
     * This does take the instr into account and modifies stack
     * accordingly.  It then calls super.mapStackToLocal.
     * @param stack the stack before the instruction is called
     * @return stack the stack afterwards.
     */
    public VariableStack mapStackToLocal(VariableStack stack) {
	if (stack == null)
	    jode.Decompiler.err.println("map stack to local called with null: " + this+ " in "+this.flowBlock);
	VariableStack middle = subBlocks[0].mapStackToLocal(stack);
	if (middle != null)
	    // Otherwise the second block is at least "logical" dead code
	    return subBlocks[1].mapStackToLocal(middle);
	jode.Decompiler.err.println("Dead code after Block " + subBlocks[0]);
	return null;
    }

    /** 
     * This method should remove local variables that are only written
     * and read one time directly after another.  <br>
     *
     * This is especially important for stack locals, that are created
     * when there are unusual swap or dup instructions, but also makes
     * inlined functions more pretty (but not that close to the
     * bytecode).  
     */
    public void removeOnetimeLocals() {
	StructuredBlock secondBlock = subBlocks[1];
	if (secondBlock instanceof SequentialBlock)
	    secondBlock = ((SequentialBlock)secondBlock).subBlocks[0];
	if (subBlocks[0] instanceof InstructionBlock
	    && secondBlock instanceof InstructionContainer) {
	    InstructionBlock first = (InstructionBlock) subBlocks[0];
	    InstructionContainer second = (InstructionContainer) secondBlock;
	    /* check if subBlocks[0] writes to a local, second reads
	     * that local, the local is only used by this two blocks,
	     * and there are no side effects.  In that case replace
	     * the LoadLocal with the righthandside of subBlocks[0]
	     * and replace subBlocks[1] with this block.  Call
	     * removeOnetimelLocals on subBlocks[1] afterwards and
	     * return.  
	     */

	    if (first.getInstruction().getOperator() 
		instanceof LocalStoreOperator) {
		LocalStoreOperator store = (LocalStoreOperator) 
		    first.getInstruction().getOperator();
		if (store.getLocalInfo().getUseCount() == 2
		    && (second.getInstruction().canCombine
			(first.getInstruction()) > 0)) {
		    System.err.println("before: "+first+second);

		    second.setInstruction(second.getInstruction()
					  .combine(first.getInstruction()));
		    System.err.println("after: "+second);
		    StructuredBlock sb = subBlocks[1];
		    sb.moveDefinitions(this, sb);
		    sb.replace(this);
		    sb.removeOnetimeLocals();
		    return;
		}
	    }	    
	}
	super.removeOnetimeLocals();
    }

    /**
     * Returns the block where the control will normally flow to, when
     * the given sub block is finished (<em>not</em> ignoring the jump
     * after this block). (This is overwritten by SequentialBlock and
     * SwitchBlock).  If this isn't called with a direct sub block,
     * the behaviour is undefined, so take care.  
     * @return null, if the control flows to another FlowBlock.  */
    public StructuredBlock getNextBlock(StructuredBlock subBlock) {
        if (subBlock == subBlocks[0]) {
            if (subBlocks[1].isEmpty())
                return subBlocks[1].getNextBlock();
            else
                return subBlocks[1];
        }
        return getNextBlock();
    }

    public FlowBlock getNextFlowBlock(StructuredBlock subBlock) {
        if (subBlock == subBlocks[0]) {
            if (subBlocks[1].isEmpty())
                return subBlocks[1].getNextFlowBlock();
            else
                return null;
        }
        return getNextFlowBlock();
    }

    /**
     * Tells if the sub block is the single exit point of the current block.
     * @param subBlock the sub block.
     * @return true, if the sub block is the single exit point of the
     * current block.  
     */
    public boolean isSingleExit(StructuredBlock subBlock) {
	return (subBlock == subBlocks[1]);
    }
    
    /**
     * Make the declarations, i.e. initialize the declare variable
     * to correct values.  This will declare every variable that
     * is marked as used, but not done.
     * @param done The set of the already declare variables.
     */
    public void makeDeclaration(VariableSet done) {
	if (subBlocks[0] instanceof InstructionBlock) {
	    /* Special case: If the first block is an InstructionBlock,
	     * it can declare the variable it writes to in a special way
	     * and that declaration will last for the second sub block.
	     */
	    LocalInfo local = 
		((InstructionBlock) subBlocks[0]).checkDeclaration(done);
	    if (local != null) {
		done.addElement(local);
		super.makeDeclaration(done);
		done.removeElement(local);
		return;
	    }
	}
	super.makeDeclaration(done);
    }

    public void dumpInstruction(TabbedPrintWriter writer)
        throws java.io.IOException
    {
        subBlocks[0].dumpSource(writer);
        subBlocks[1].dumpSource(writer);
    }

    /**
     * Replaces the given sub block with a new block.
     * @param oldBlock the old sub block.
     * @param newBlock the new sub block.
     * @return false, if oldBlock wasn't a direct sub block.
     */
    public boolean replaceSubBlock(StructuredBlock oldBlock, 
                            StructuredBlock newBlock) {
        for (int i=0; i<2; i++) {
            if (subBlocks[i] == oldBlock) {
                subBlocks[i] = newBlock;
                return true;
            }
        }
        return false;
    }

    /**
     * Returns all sub block of this structured block.
     */
    public StructuredBlock[] getSubBlocks() {
        return subBlocks;
    }

    /**
     * Determines if there is a sub block, that flows through to the end
     * of this block.  If this returns true, you know that jump is null.
     * @return true, if the jump may be safely changed.
     */
    public boolean jumpMayBeChanged() {
        return (subBlocks[1].jump != null || subBlocks[1].jumpMayBeChanged());
    }
}
