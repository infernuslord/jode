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

/**
 * A sequential block combines exactly two structured blocks to a new
 * one. The first sub block mustn't be another sequential block,
 * instead the second sub block should be used for this.  This
 * condition is temporarily violated, while the t1 transformation is
 * done.
 */
public class SequentialBlock {
    StructuredBlock[] subBlocks;

    public SequentialBlock() {
        subBlocks = new StructuredBlock[2];
    }

    public setFirst(StructuredBlock sb) {
        subBlocks[0] = sb;
        sb.outer = this;
    }

    public setSecond(StructuredBlock sb) {
        subBlocks[1] = sb;
        sb.outer = this;
    }

    /**
     * Returns the block where the control will normally flow to, when
     * the given sub block is finished (<em>not</em> ignoring the jump
     * after this block). (This is overwritten by SequentialBlock and
     * SwitchBlock).  If this isn't called with a direct sub block,
     * the behaviour is undefined, so take care.  
     * @return null, if the control flows to another FlowBlock.  */
    StructuredBlock getNextBlock(StructuredBlock subBlock) {
        if (subBlock == subBlocks[0])
            return subBlocks[1];
        return getNextBlock();
    }

    FlowBlock getNextFlowBlock(StructuredBlock subBlock) {
        if (subBlock == subBlocks[0])
            return null;
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
    StructuredBlock[] getSubBlocks() {
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




