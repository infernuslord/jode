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
import jode.TabbedPrintWriter;
import jode.expr.Expression;

/**
 * An IfThenElseBlock is the structured block representing an if
 * instruction.  The else part may be null.
 */
public class IfThenElseBlock extends StructuredBlock {

    /**
     * The condition.  Must be of boolean type.
     */
    Expression cond;

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
    public IfThenElseBlock(Expression cond) {
        this.cond = cond;
    }

    /** 
     * Sets the then block.
     * @param thenBlock the then block, must be non null.
     */
    public void setThenBlock(StructuredBlock thenBlock) {
        this.thenBlock = thenBlock;
        thenBlock.outer = this;
        thenBlock.setFlowBlock(flowBlock);
    }

    /** 
     * Sets the else block.
     * @param elseBlock the else block
     */
    public void setElseBlock(StructuredBlock elseBlock) {
        this.elseBlock = elseBlock;
        elseBlock.outer = this;
        elseBlock.setFlowBlock(flowBlock);
    }
    
    /* The implementation of getNext[Flow]Block is the standard
     * implementation */

    /**
     * Replaces the given sub block with a new block.
     * @param oldBlock the old sub block.
     * @param newBlock the new sub block.
     * @return false, if oldBlock wasn't a direct sub block.
     */
    public boolean replaceSubBlock(StructuredBlock oldBlock, 
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
    public void dumpInstruction(TabbedPrintWriter writer)
        throws java.io.IOException
    {
        boolean needBrace = thenBlock.needsBraces();
        writer.print("if ("+cond.simplify().toString()+")");
	if (needBrace)
	    writer.openBrace();
	else
	    writer.println();
        writer.tab();
        thenBlock.dumpSource(writer);
        writer.untab();
        if (elseBlock != null) {
	    if (needBrace)
		writer.closeBraceContinue();

            if (elseBlock instanceof IfThenElseBlock
                && (elseBlock.declare == null 
                    || elseBlock.declare.isEmpty())) {
                needBrace = false;
                writer.print("else ");
                elseBlock.dumpSource(writer);
            } else {
                needBrace = elseBlock.needsBraces();
                writer.print("else");
		if (needBrace)
		    writer.openBrace();
		else
		    writer.println();
                writer.tab();
                elseBlock.dumpSource(writer);
                writer.untab();
            }
        }
        if (needBrace)
	    writer.closeBrace();
    }

    /**
     * Returns all sub block of this structured block.
     */
    public StructuredBlock[] getSubBlocks() {
        return (elseBlock == null)
            ? new StructuredBlock[] { thenBlock }
            : new StructuredBlock[] { thenBlock, elseBlock };
    }

    /**
     * Determines if there is a sub block, that flows through to the end
     * of this block.  If this returns true, you know that jump is null.
     * @return true, if the jump may be safely changed.
     */
    public boolean jumpMayBeChanged() {
        return (thenBlock.jump != null || thenBlock.jumpMayBeChanged())
            && elseBlock != null
            && (elseBlock.jump != null || elseBlock.jumpMayBeChanged());
    }

    public boolean doTransformations() {
        StructuredBlock last = flowBlock.lastModified;
        return CreateCheckNull.transformJikes(this, last);
    }
}
