/* BreakBlock (c) 1998 Jochen Hoenicke
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
public class BreakBlock extends StructuredBlock {
    StructuredBlock breaksBlock;
    String label;

    public BreakBlock(BreakableBlock breaksBlock, boolean needsLabel) {
        this.breaksBlock = (StructuredBlock) breaksBlock;
        breaksBlock.setBreaked();
        if (needsLabel) 
            label = breaksBlock.getLabel();
        else
            label = null;
    }

    public void checkConsistent() {
        super.checkConsistent();
        StructuredBlock sb = outer;
        while (sb != breaksBlock) {
            if (sb == null) 
                throw new RuntimeException("Inconsistency");
            sb = sb.outer;
        }
    }

    /**
     * Returns the block where the control will normally flow to, when
     * this block is finished.
     */
    public StructuredBlock getNextBlock() {
        return breaksBlock.getNextBlock();
    }

    /**
     * Returns the flow block where the control will normally flow to,
     * when this block is finished (not ignoring the jump after this
     * block).  
     * @return null, if the control flows into a non empty structured
     * block or if this is the outermost block.
     */
    public FlowBlock getNextFlowBlock() {
        return breaksBlock.getNextFlowBlock();
    }

    public void dumpInstruction(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
        writer.println("break"+(label == null ? "" : " "+label) + ";");
    }
}
