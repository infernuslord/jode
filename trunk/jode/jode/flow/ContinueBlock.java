/* ContinueBlock (c) 1998 Jochen Hoenicke
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
 * 
 */
public class ContinueBlock extends StructuredBlock {
    StructuredBlock continuesBlock;
    String continueLabel;

    public ContinueBlock(LoopBlock continuesBlock, boolean needsLabel) {
        this.continuesBlock = continuesBlock;
        if (needsLabel) 
            continueLabel = continuesBlock.getLabel();
        else
            continueLabel = null;
    }

    public void checkConsistent() {
        super.checkConsistent();
        StructuredBlock sb = outer;
        while (sb != continuesBlock) {
            if (sb == null) 
                throw new RuntimeException("Inconsistency");
            sb = sb.outer;
        }
    }

    /**
     * Tells if this block is empty and only changes control flow.
     */
    public boolean isEmpty() {
        return true;
    }

    /**
     * Returns the block where the control will normally flow to, when
     * this block is finished (not ignoring the jump after this block).
     */
    public StructuredBlock getNextBlock() {
        /* This continues to continuesBlock. */
        return continuesBlock;
    }

    /**
     * Returns the flow block where the control will normally flow to,
     * when this block is finished (not ignoring the jump after this
     * block).  
     * @return null, if the control flows into a non empty structured
     * block or if this is the outermost block.
     */
    public FlowBlock getNextFlowBlock() {
        return null;
    }

    public void dumpInstruction(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
        writer.println("continue"+
                       (continueLabel == null ? "" : " "+continueLabel) + ";");
    }
}
