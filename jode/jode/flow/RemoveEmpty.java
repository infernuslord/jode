/* 
 * RemoveEmpty (c) 1998 Jochen Hoenicke
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
import jode.Decompiler;
import jode.expr.*;

public class RemoveEmpty {
    
    public static boolean removeSwap(SpecialBlock swapBlock,
                                     StructuredBlock last) {

        /* Remove non needed swaps; convert:
         *
         *   PUSH expr1
         *   PUSH expr2
         *   SWAP
         *
         * to:
         *
         *   PUSH expr2
         *   PUSH expr1
         */
        if (last.outer instanceof SequentialBlock
            && last.outer.outer instanceof SequentialBlock
            && last.outer.getSubBlocks()[0] instanceof InstructionBlock
            && last.outer.outer.getSubBlocks()[0] 
            instanceof InstructionBlock) {

            InstructionBlock block1 
                = (InstructionBlock) last.outer.outer.getSubBlocks()[0];
            InstructionBlock block2
                = (InstructionBlock) last.outer.getSubBlocks()[0];

            /* XXX check if blocks may be swapped 
             * (there mustn't be side effects in one of them).
             */
            Decompiler.err.println("WARNING: this program contains a SWAP "
			  +"opcode and may not be translated correctly.");

            if (block1.getInstruction().isVoid()
                || block2.getInstruction().isVoid())
                return false;

            /* PUSH expr1 == block1
             * PUSH expr2
             * SWAP
             * ...
             */
            last.outer.replace(block1.outer);
            /* PUSH expr2
             * SWAP
             * ...
             */
            block1.replace(swapBlock);
            block1.moveJump(swapBlock.jump);
            /* PUSH expr2
             * PUSH expr1
             */
            block1.flowBlock.lastModified = block1;
            return true;
        }
        return false;
    }

    public boolean removeEmpty(FlowBlock flow) {
        StructuredBlock lastBlock = flow.lastModified;
        if (lastBlock instanceof EmptyBlock &&
            lastBlock.outer instanceof SequentialBlock &&
            lastBlock.outer.getSubBlocks()[1] == lastBlock) {
            
            StructuredBlock block = lastBlock.outer.getSubBlocks()[0];
            block.replace(block.outer);
            if (lastBlock.jump != null)
                block.moveJump(lastBlock.jump);
            flow.lastModified = block;
            return true;
        }
        if (lastBlock.outer instanceof SequentialBlock &&
            lastBlock.outer.getSubBlocks()[0] instanceof EmptyBlock &&
            lastBlock.outer.getSubBlocks()[0].jump == null) {
            
            lastBlock.replace(lastBlock.outer);
            flow.lastModified = lastBlock;
            return true;
        }
        return false;
    }
}
