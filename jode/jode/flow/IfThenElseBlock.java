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
     * Creates a new if then else block.
     * @param thenBlock the then block, must be non null.
     * @param elseBlock the else block, may be null.
     */
    public IfThenElseBlock(Instruction cond,
                           StructuredBlock thenBlock, 
                           StructuredBlock elseBlock) {
        this.cond = cond;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
        thenBlock.outer = this;
        if (elseBlock != null)
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
}
