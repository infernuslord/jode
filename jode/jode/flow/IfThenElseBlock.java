/* IfThenElseBlock (c) 1998 Jochen Hoenicke
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
 * An IfThenElseBlock is the structured block representing an if
 * instruction.  The else part may be null.
 */
public class IfThenElseBlock extends StructuredBlock {

    /**
     * The condition.  Must be of boolean type.
     */
    Instruction cond;

    /**
     * The then part.  This is always a valid block and not null 
     */
    StructuredBlock thenBlock;

    /**
     * The else part, may be null, and mustn't be the then part.
     */
    StructuredBlock elseBlock;

    /**
     * Creates a new if then else block.  The method setThenBlock must
     * be called shortly after the creation.
     */
    public IfThenElseBlock(Instruction cond) {
        this.cond = cond;
    }

    /** 
     * Sets the then block.
     * @param thenBlock the then block, must be non null.
     */
    public void setThenBlock(StructuredBlock thenBlock) {
        this.thenBlock = thenBlock;
        thenBlock.outer = this;
    }

    /** 
     * Sets the else block.
     * @param elseBlock the else block
     */
    public void setElseBlock(StructuredBlock elseBlock) {
        this.elseBlock = elseBlock;
        elseBlock.outer = this;
    }
    
    /* The implementation of getNext[Flow]Block is the standard
     * implementation */

    /**
     * Replaces the given sub block with a new block.
     * @param oldBlock the old sub block.
     * @param newBlock the new sub block.
     * @return false, if oldBlock wasn't a direct sub block.
     */
    boolean replaceSubBlock(StructuredBlock oldBlock, 
                            StructuredBlock newBlock) {
        if (thenBlock == oldBlock)
            thenBlock = newBlock;
        else if (elseBlock == oldBlock)
            elseBlock = newBlock;
        else
            return false;
        return true;
    }

    /**
     * Print the source code for this structured block.  This may be
     * called only once, because it remembers which local variables
     * were declared.
     */
    public void dumpSource(TabbedPrintWriter writer)
        throws java.io.IOException
    {
        boolean needBrace = ! (thenBlock instanceof InstructionBlock);
        writer.println("if ("+cond.toString()+")"+needBrace?" {":"");
        writer.tab();
        thenBlock.dumpSource(writer);
        writer.untab();
        if (elseBlock != null) {
            writer.print(needBrace?"} ":"");
            needBrace = ! (thenBlock instanceof InstructionBlock);
            writer.println("else"+needBrace?" {":"");
            writer.tab();
            elseBlock.dumpSource(writer);
            writer.untab();
        }
        if (needBrace)
            writer.println("}");
    }

    /**
     * Returns all sub block of this structured block.
     */
    StructuredBlock[] getSubBlocks() {
        if (elseBlock == null) {
            StructuredBlock result = { thenBlock };
            return result;
        } else {
            StructuredBlock result = { thenBlock, elseBlock };
            return result;
        }
    }

    /**
     * Determines if there is a sub block, that flows through to the end
     * of this block.  If this returns true, you know that jump is null.
     * @return true, if the jump may be safely changed.
     */
    public boolean jumpMayBeChanged() {
        if (thenBlock.jump == null && !thenBlock.jumpMayBeChanged())
            return false;

        if (elseBlock != null && elseBlock.jump == null &&
            !elseBlock.jumpMayBeChanged)
            return false;
        return true;
    }
}
