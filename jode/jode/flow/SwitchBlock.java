/* 
 * SwitchBlock  (c) 1998 Jochen Hoenicke
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
 * This is the structured block for an empty block.
 */
public class SwitchBlock extends BreakableBlock {
    int[] cases;
    StructuredBlock[] caseBlocks;

    public SwitchBlock(int[] cases, int[] dests) {
        this.cases = cases;
        this.caseBlocks = new StructuredBlock[dests.length];
        for (int i=0; i<dests.length; i++) {
            /* XXX sort the destinations, multi-cases? */
            caseBlocks[i] = new EmptyBlock(new Jump(dests[i]));
        }
        this.jump = null;
        mayChangeJump = true;
    }

    public void dumpInstruction(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
        writer.println("switch (/* XXX implement */)");
    }


    /**
     * Returns the block where the control will normally flow to, when
     * the given sub block is finished (<em>not</em> ignoring the jump
     * after this block). (This is overwritten by SequentialBlock and
     * SwitchBlock).  If this isn't called with a direct sub block,
     * the behaviour is undefined, so take care.  
     * @return null, if the control flows to another FlowBlock.  */
    public StructuredBlock getNextBlock(StructuredBlock subBlock) {
        /*XXX*/
        return getNextBlock();
    }

    public FlowBlock getNextFlowBlock(StructuredBlock subBlock) {
        /*XXX*/
        return getNextFlowBlock();
    }

    /**
     * Returns all sub block of this structured block.
     */
    public StructuredBlock[] getSubBlocks() {
        return caseBlocks;
    }
}

