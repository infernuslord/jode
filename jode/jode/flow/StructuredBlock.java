/* 
 * StructuredBlock  (c) 1998 Jochen Hoenicke
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
import jode.TabbedPrintWriter;

/**
 * A structured block is the building block of the source programm.
 * For every program construct like if, while, try, or blocks there is
 * a corresponding structured block.
 *
 * Some of these Block are only intermediate representation, that get
 * converted to another block later.
 *
 * Every block has to handle the local variables that it contains.
 * This is done by the in/out vectors and the local variable structure
 * themself.  Every local variable used in this structured block is
 * either in or out.
 *
 * There are following types of structured blocks: 
 * <ul>
 * <li>if-then-(else)-block  (IfThenElseBlock)
 * <li>(do)-while/for-block  (LoopBlock)
 * <li>switch-block          (SwitchBlock)
 * <li>try-catch-block       (CatchBlock)
 * <li>try-finally-block     (FinallyBlock)
 * <li>synchronized-block    (SynchronizedBlock)
 * <li>one-instruction       (InstructionBlock)
 * <li>empty-block           (EmptyBlock)
 * <li>multi-blocks-block    (SequentialBlock)
 * </ul>
 */

public abstract class StructuredBlock {
    /* Invariants:
     * in.intersection(out) = empty
     * outer != null => flowBlock = outer.flowBlock
     * outer == null => flowBlock.block = this
     * jump  == null => outer != null
     * either getNextBlock() != null 
     *     or getNextFlowBlock() != null or outer == null
     * either outer.getNextBlock(this) != null 
     *     or outer.getNextFlowBlock(this) != null
     */

    /**
     * The in locals.  This are the locals, which are used in this
     * block and whose values may be the result of a assignment
     * outside of the whole flow block.  That means, that there is a
     * path from the start of the current flow block, on which the
     * local variable is never assigned
     */
    VariableSet in = new VariableSet(); 

    /**
     * The out locals.  This are the locals, which must be overwritten
     * until the end of this block.  That means, that all paths form
     * the start of the current flow block to the end of this
     * structured block contain a (unconditional) assignment to this
     * local
     */
    VariableSet out = new VariableSet();

    /**
     * The variable set containing all variables that must be defined
     * in this block (or maybe an outer block, this changes as the
     * blocks are put together).
     */
    VariableSet defineHere = new VariableSet();

    /**
     * The surrounding structured block.  If this is the outermost
     * block in a flow block, outer is null.  */
    StructuredBlock outer;

    /**
     * The flow block in which this structured block lies.
     */
    FlowBlock flowBlock;
    
    /** 
     * The jump that follows on this block, or null if there is no
     * jump, but the control flows normal (only allowed if
     * getNextBlock != null).  
     */
    Jump jump;

    /**
     * Returns the block where the control will normally flow to, when
     * this block is finished (not ignoring the jump after this block).
     */
    public StructuredBlock getNextBlock() {
        if (jump != null)
            return null;
        if (outer != null)
            return outer.getNextBlock(this);
        return null;
    }

    public void setJump(Jump jump) {
        this.jump = jump;
        jump.prev = this;
    }

    /**
     * Returns the flow block where the control will normally flow to,
     * when this block is finished (not ignoring the jump after this
     * block).  
     * @return null, if the control flows into a non empty structured
     * block or if this is the outermost block.
     */
    public FlowBlock getNextFlowBlock() {
        if (jump != null)
            return jump.destination;
        if (outer != null)
            return outer.getNextFlowBlock(this);
        return null;
    }

    /**
     * Checks if the jump to the outside has correct monitorexit and
     * jsr instructions attached.
     * @return null, if everything is okay, and a diagnostic message that
     * should be put in a comment otherwise.
     */
    public String checkJump(Jump jump) {
        if (outer != null)
            return outer.checkJump(jump);
        if (jump.hasAttachments())
            return "Unknown attachments: "+jump.describeAttachments();
        return null;
    }

    /**
     * Returns the block where the control will normally flow to, when
     * the given sub block is finished (<em>not</em> ignoring the jump
     * after this block). (This is overwritten by SequentialBlock and
     * SwitchBlock).  If this isn't called with a direct sub block,
     * the behaviour is undefined, so take care.  
     * @return null, if the control flows to another FlowBlock.  */
    public StructuredBlock getNextBlock(StructuredBlock subBlock) {
        return getNextBlock();
    }

    public FlowBlock getNextFlowBlock(StructuredBlock subBlock) {
        return getNextFlowBlock();
    }

    /**
     * Replaces the given sub block with a new block.
     * @param oldBlock the old sub block.
     * @param newBlock the new sub block.
     * @return false, if oldBlock wasn't a direct sub block.
     */
    public boolean replaceSubBlock(StructuredBlock oldBlock, 
                                   StructuredBlock newBlock) {
        return false;
    }

    /**
     * Returns all sub block of this structured block.
     */
    public StructuredBlock[] getSubBlocks() {
        return new StructuredBlock[0];
    }

    /**
     * Returns if this block contains the given block.
     * @param child the block which should be contained by this block.
     * @return false, if child is null, or is not contained in this block.
     */
    public boolean contains(StructuredBlock child) {
        while (child != this && child != null)
            child = child.outer;
        return (child == this);
    }

    /**
     * Removes the given jump if it is our jump.  This does also
     * update the successors vector of the flow block.  
     *
     * @param jump  The jump that should be removed.  
     */
    public void removeJump() {
        if (jump != null) {
            jump.prev = null;
            flowBlock.removeSuccessor(jump);
            jump = null;
        }
    }

    /**
     * This function replaces sb with this block.  It copies outer and
     * from sb, and updates the outer block, so it knows that sb was
     * replaced.  You have to replace sb.outer or mustn't use sb
     * anymore.
     * @param sb The structured block that should be replaced.  */
    public void replace(StructuredBlock sb) {
        in = sb.in;
        out = sb.out;
        defineHere = sb.defineHere;
        outer = sb.outer;
        flowBlock = sb.flowBlock;

        if (outer != null) {
            outer.replaceSubBlock(sb, this);
        } else {
            flowBlock.block = this;
        }
    }

    /**
     * This function moves the jumps from sb to this block.  
     * The jump field of sb is removed afterwards.  
     * @param sb  The structured block whose jump is copied.
     */
    public void moveJump(StructuredBlock sb) {
        jump = sb.jump;
        if (jump != null) {
            jump.prev = this;
            sb.jump = null;
        }
    }

    /**
     * Determines if there is a sub block, that flows through to the end
     * of this block.  If this returns true, you know that jump is null.
     * @return true, if the jump may be safely changed.
     */
    public boolean jumpMayBeChanged() {
        return false;
    }

    public void checkConsistent() {
        StructuredBlock[] subs = getSubBlocks();
        for (int i=0; i<subs.length; i++) {
            if (subs[i].outer != this ||
                subs[i].flowBlock != flowBlock) {
                throw new RuntimeException("Inconsistency");
            }
            subs[i].checkConsistent();
        }
        if (jump != null && 
            (jump.prev != this || 
             !flowBlock.successors.contains(jump) ||
             !jump.destination.predecessors.contains(flowBlock))) {
                throw new RuntimeException("Inconsistency");
        }
    }

    /**
     * Set the flow block of this block and all sub blocks.
     * @param flowBlock the new flow block
     */
    public void setFlowBlock(FlowBlock flowBlock) {
        if (this.flowBlock != flowBlock) {
            this.flowBlock = flowBlock;
            StructuredBlock[] subs = getSubBlocks();
            for (int i=0; i<subs.length; i++) {
                subs[i].setFlowBlock(flowBlock);
            }
        }
    }

    /**
     * Put all the successors of this block and all subblocks into
     * the given vector.
     * @param succs The vector, the successors should be stored to.
     */
    public void fillSuccessors(java.util.Vector succs) {
        succs.addElement(jump);
        StructuredBlock[] subs = getSubBlocks();
        for (int i=0; i<subs.length; i++) {
            subs[i].fillSuccessors(succs);
        }
    }
    
    /**
     * Print the source code for this structured block.  This handles
     * everything that is unique for all structured blocks and calls
     * dumpInstruction afterwards.
     * @param writer The tabbed print writer, where we print to.
     */
    public void dumpSource(TabbedPrintWriter writer)
        throws java.io.IOException
    {
        /* XXX declare variables needed in this block */
        dumpInstruction(writer);
        if (jump != null) {
            if (jump.destination == null)
                writer.println ("GOTO null-ptr!!!!!");
            else {
                writer.println("GOTO "+jump.destination.getLabel());
                if (jump.prev != this)
                    writer.println("GOTO has wrong prev");
            }
        }
    }


    /**
     * Print the instruction expressing this structured block.
     * @param writer The tabbed print writer, where we print to.
     */
    public abstract void dumpInstruction(TabbedPrintWriter writer)
        throws java.io.IOException;
}

