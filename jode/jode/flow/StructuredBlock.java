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
     * outer != null => flowblock = outer.flowblock
     * outer == null => flowblock.block = this
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
    Vector in; 

    /**
     * The out locals.  This are the locals, which must be overwritten
     * until the end of this block.  That means, that all paths form
     * the start of the current flow block to the end of this
     * structured block contain a (unconditional) assignment to this
     * local
     */
    Vector out;

    /**
     * The surrounding structured block.  If this is the outermost
     * block in a flow block, outer is null.  
     */
    StructuredBlock outer;

    /**
     * The flow block in which this structured block lies.
     */
    FlowBlock flowblock;
    
    /** 
     * The jump that follows on this block, or null if there is no
     * jump, but the control flows normal (only allowed if
     * getNextBlock != null).  
     */
    Jump jump;

    /**
     * Returns the block where the control will normally flow to, when
     * this block is finished (ignoring the jump after this block).
     */
    StructuredBlock getNextBlock() {
        if (outer != null)
            outer.getNextBlock(this);
        else 
            return null;
    }

    /**
     * Returns the flow block where the control will normally flow to,
     * when this block is finished (ignoring the jump after this
     * block).  
     * @return null, if the control flows into a non empty structured
     * block or if this is the outermost block.
     */
    FlowBlock getNextFlowBlock() {
        if (outer != null)
            outer.getNextFlowBlock(this);
        else 
            return null;
    }

    /**
     * Checks if the jump to the outside has correct monitorexit and
     * jsr instructions attached.
     * @return null, if everything is okay, and a diagnostic message that
     * should be put in a comment otherwise.
     */
    String checkJump(Jump jump) {
        if (outer != null)
            return outer.checkJump(jump);
        else if (jump.hasAttachments())
            return "Unknown attachments: "+jump.describeAttachments()
        else
            return null;
    }

    /**
     * Returns the block where the control will normally flow to, when
     * the given sub block is finished (<em>not</em> ignoring the jump
     * after this block). (This is overwritten by SequentialBlock and
     * SwitchBlock).  If this isn't called with a direct sub block,
     * the behaviour is undefined, so take care.  
     * @return null, if the control flows to another FlowBlock.  */
    StructuredBlock getNextBlock(StructuredBlock subBlock) {
        if (jump != null)
            return null;
        return getNextBlock();
    }

    FlowBlock getNextFlowBlock(StructuredBlock subBlock) {
        if (jump != null)
            return jump;
        return getNextFlowBlock();
    }

    /**
     * Replaces the given sub block with a new block.
     * @param oldBlock the old sub block.
     * @param newBlock the new sub block.
     * @return false, if oldBlock wasn't a direct sub block.
     */
    boolean replaceSubBlock(StructuredBlock oldBlock, 
                            StructuredBlock newBlock) {
        return false;
    }

    /**
     * Print the source code for this structured block.  This may be
     * called only once, because it remembers which local variables
     * were declared.
     */
    public abstract void dumpSource(TabbedPrintWriter writer)
        throws java.io.IOException;
}
