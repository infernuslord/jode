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
import jode.Expression;
import jode.NopOperator;

public class RemoveEmpty implements Transformation {
    
    public boolean transform (FlowBlock fb) {
        return (removeNop(fb) || removeSwap(fb) || removeEmpty(fb));
    }

    public boolean removeNop(FlowBlock flow) {
        StructuredBlock block = flow.lastModified;
        if (block instanceof InstructionContainer
            && block.outer instanceof SequentialBlock
            && block.outer.getSubBlocks()[0] instanceof InstructionBlock) {
            
            InstructionContainer ic = (InstructionContainer) block;

            Expression nopInstr = ic.getInstruction();
            if (!(nopInstr instanceof NopOperator)
                || nopInstr.getType() == jode.Type.tVoid)
                return false;
            
            InstructionBlock prev = 
                (InstructionBlock) ic.outer.getSubBlocks()[0];

            Expression instr = prev.getInstruction();
            if (instr.getType() == jode.Type.tVoid)
                return false;

            instr.setType(nopInstr.getType());
            ic.setInstruction(instr);
            ic.replace(ic.outer);
            return true;
        }
        return false;
    }

    public boolean removeSwap(FlowBlock flow) {
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
        StructuredBlock block = flow.lastModified;
        if (block instanceof SpecialBlock
            && ((SpecialBlock)block).type == SpecialBlock.SWAP
            && block.outer instanceof SequentialBlock
            && block.outer.outer instanceof SequentialBlock
            && block.outer.getSubBlocks()[0] instanceof InstructionBlock
            && block.outer.outer.getSubBlocks()[0] 
            instanceof InstructionBlock) {

            InstructionBlock block1 
                = (InstructionBlock) block.outer.outer.getSubBlocks()[0];
            InstructionBlock block2
                = (InstructionBlock) block.outer.getSubBlocks()[0];

            if (block1.getInstruction().isVoid()
                || block2.getInstruction().isVoid())
                return false;
            /* PUSH expr1 == block1
             * PUSH expr2
             * SWAP
             */
            block.outer.replace(block1.outer);
            /* PUSH expr2
             * SWAP
             */
            block1.replace(block);
            block1.moveJump(block.jump);
            /* PUSH expr2
             * PUSH expr1
             */
            flow.lastModified = block1;
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
