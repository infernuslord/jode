/* jode.flow.TryBlock  (c) 1998 Jochen Hoenicke 
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
 * A TryBlock is created for each exception in the
 * ExceptionHandlers-Attribute. <p>
 *
 * For each catch block (there may be more than one catch block
 * appending a single try block) and for each finally and each
 * synchronized block such a TryBlock is created.  The
 * finally/synchronized-blocks have a null exception type so that they
 * are easily distinguishable from the catch blocks. <p>
 *
 * A TryBlock is an intermediate representation that gets
 * converted later to a CatchBlock, a FinallyBlock or a
 * SynchronizedBlock (after the body is parsed).
 *
 * @date 1998/09/16
 * @author Jochen Hoenicke
 * @see CatchBlock
 * @see FinallyBlock
 * @see SynchronizedBlock
 */

public class TryBlock extends StructuredBlock {

    VariableSet gen;
    StructuredBlock[] subBlocks = new StructuredBlock[1];

    public TryBlock(FlowBlock tryFlow) {
        this.gen = (VariableSet) tryFlow.gen.clone();
        this.flowBlock = tryFlow;

        StructuredBlock bodyBlock = tryFlow.block;
        replace(bodyBlock);
        this.subBlocks = new StructuredBlock[] { bodyBlock };
        bodyBlock.outer = this;
        tryFlow.lastModified = this;
    }

    public void addCatchBlock(CatchBlock catchBlock) {
        StructuredBlock[] newSubBlocks = 
            new StructuredBlock[subBlocks.length+1];
        System.arraycopy(subBlocks, 0, newSubBlocks, 0, subBlocks.length);
        newSubBlocks[subBlocks.length] = catchBlock;
        subBlocks = newSubBlocks;
        catchBlock.outer = this;
        catchBlock.setFlowBlock(flowBlock);
    }

    /**
     * Replaces the given sub block with a new block.
     * @param oldBlock the old sub block.
     * @param newBlock the new sub block.
     * @return false, if oldBlock wasn't a direct sub block.
     */
    public boolean replaceSubBlock(StructuredBlock oldBlock, 
                                   StructuredBlock newBlock) {
        for (int i=0; i<subBlocks.length; i++) {
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

    public void dumpInstruction(TabbedPrintWriter writer) 
        throws java.io.IOException {
        writer.println("try {");
        writer.tab();
        subBlocks[0].dumpSource(writer);
        writer.untab();
        for (int i=1; i<subBlocks.length;i++)
            subBlocks[i].dumpSource(writer);
        writer.println("}");
    }

    /**
     * Determines if there is a sub block, that flows through to the end
     * of this block.  If this returns true, you know that jump is null.
     * @return true, if the jump may be safely changed.
     */
    public boolean jumpMayBeChanged() {
        for (int i=0; i<subBlocks.length;i++) {
            if (subBlocks[i].jump == null
                && !subBlocks[i].jumpMayBeChanged())
                return false;
        }
        return true;
    }
}
