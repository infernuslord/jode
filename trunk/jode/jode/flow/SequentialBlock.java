/* SequentialBlock (c) 1998 Jochen Hoenicke
 *
 * You may distribute under the terms of the GNU General Public License.
 *
 * IN NO EVENT SHALL JOCHEN HOENICKE BE LIABLE TO ANY PARTY FOR DIRECT,
 * INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT OF
 * THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF JOCHEN HOENICKE 
 * HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * JOCHEN HOENICKE SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS"
 * BASIS, AND JOCHEN HOENICKE HAS NO OBLIGATION TO PROVIDE MAINTENANCE,
 * SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * $Id$
 */
package jode.flow;
import jode.decompiler.TabbedPrintWriter;

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
	     * it can declare the variable it uses for us.
	     *
	     * Now add the variables used in the first block to the done
	     * set of the second block, since the first sub block has
	     * declared them.  
	     */
	    declare = new VariableSet();

	    subBlocks[0].makeDeclaration(done);
	    done.unionExact(subBlocks[0].declare);
	    subBlocks[1].makeDeclaration(done);
	    done.subtractExact(subBlocks[0].declare);
	} else
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
