/* jode.flow.RawTryCatchBlock  (c) 1998 Jochen Hoenicke 
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
 * A RawTryCatchBlock is created for each exception in the
 * ExceptionHandlers-Attribute. <p>
 *
 * For each catch block (there may be more than one catch block
 * appending a single try block) and for each finally and each
 * synchronized block such a RawTryCatchBlock is created.  The
 * finally/synchronized-blocks have a null exception type so that they
 * are easily distinguishable from the catch blocks. <p>
 *
 * A RawTryCatchBlock is an intermediate representation that gets
 * converted later to a CatchBlock, a FinallyBlock or a
 * SynchronizedBlock (after the body is parsed).
 *
 * @date 1998/09/16
 * @author Jochen Hoenicke
 * @see CatchBlock
 * @see FinallyBlock
 * @see SynchronizedBlock
 */

public class RawTryCatchBlock extends StructuredBlock {

    int endAddr;

    public RawTryCatchBlock(jode.Type type, 
                            Jump endDest, Jump catchDest) {
        this.type = type;
        
        endAddr = endDest.destAddr;
        catchBlock = new EmptyBlock(catchDest);
        catchBlock.outer = this;
    }

    public FlowBlock chainTo(FlowBlock flow) {
        FlowBlock first = flow;
        RawTryCatchBlock parent = null;
        while (flow.getBlock() instanceof RawTryCatchBlock
               && (((RawTryCatchBlock) flow.getBlock()).getCatchAddr() 
                   > this.getCatchAddr())) {

            parent = (RawTryCatchBlock) flow.getBlock();
            flow = parent.getTryBlock().jump.destination;
        }

        tryBlock = new EmptyBlock(new Jump(flow));
        tryBlock.outer = this;
        new FlowBlock(flow.code, flow.addr, 0, this);

        if (parent == null) {
            /* We are the outermost try block */
            return flowBlock;
        } else {
            /* Chain into existing try block list */
            parent.getTryBlock().jump.destination = flowBlock;
            return first;
        }
    }

    /**
     * The try block.
     */
    StructuredBlock tryBlock;

    /**
     * The catch block.
     */
    StructuredBlock catchBlock;

    /** 
     * The type of the exception that is catched. This is null for a
     * synchronized/finally block 
     */
    jode.Type type;

    /**
     * Replaces the given sub block with a new block.
     * @param oldBlock the old sub block.
     * @param newBlock the new sub block.
     * @return false, if oldBlock wasn't a direct sub block.
     */
    public boolean replaceSubBlock(StructuredBlock oldBlock, 
                                   StructuredBlock newBlock) {
        if (tryBlock == oldBlock)
            tryBlock = newBlock;
        else if (catchBlock == oldBlock)
            catchBlock = newBlock;
        else
            return false;
        return true;
    }

    /**
     * Returns all sub block of this structured block.
     */
    public StructuredBlock[] getSubBlocks() {
        return new StructuredBlock[] { tryBlock, catchBlock };
    }

    public void dumpInstruction(TabbedPrintWriter writer) 
        throws java.io.IOException {
        writer.println("TRY "+(type != null ? type.toString() : "ALL"));
        writer.tab();
        tryBlock.dumpSource(writer);
        writer.untab();
        writer.println("CATCH TO");
        writer.tab();
        catchBlock.dumpSource(writer);
        writer.untab();
    }

    public StructuredBlock getTryBlock() {
        return tryBlock;
    }

    public int getCatchAddr() {
        return catchBlock.jump.destination.addr;
    }
}
