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
import jode.Instruction;
import jode.NopOperator;

public class RemoveEmpty implements Transformation {
    
    public boolean transform (FlowBlock fb) {
        return (removeNop(fb) || removeEmpty(fb));
    }

    public boolean removeNop(FlowBlock flow) {
        StructuredBlock block;
        SequentialBlock sequBlock;
        Instruction instr;
        try {
            block = flow.lastModified;
            Instruction prevInstr = 
                ((InstructionContainer)block).getInstruction();
            if (!(prevInstr instanceof NopOperator))
                return false;
            
            sequBlock = (SequentialBlock)block.outer;
            if (sequBlock.getSubBlocks()[1] != block)
                return false;

            InstructionBlock prev = 
                (InstructionBlock) sequBlock.getSubBlocks()[0];
            if (prev.jump != null)
                return false;
            instr = (Instruction) prev.getInstruction();
            if (instr.getType() == jode.Type.tVoid)
                return false;
            instr.setType(prevInstr.getType());
        } catch (NullPointerException ex) {
            return false;
        } catch (ClassCastException ex) {
            return false;
        }
        ((InstructionContainer)block).setInstruction(instr);
        block.replace(sequBlock, block);
        flow.lastModified = block;
        return true;
    }

    public boolean removeEmpty(FlowBlock flow) {
        StructuredBlock lastBlock = flow.lastModified;
        if (lastBlock instanceof EmptyBlock &&
            lastBlock.outer instanceof SequentialBlock &&
            lastBlock.outer.getSubBlocks()[1] == lastBlock) {
            
            StructuredBlock block = lastBlock.outer.getSubBlocks()[0];
            block.replace(block.outer, block);
            if (lastBlock.jump != null)
                block.moveJump(lastBlock.jump);
            flow.lastModified = block;
            return true;
        }
        if (lastBlock.outer instanceof SequentialBlock &&
            lastBlock.outer.getSubBlocks()[0] instanceof EmptyBlock &&
            lastBlock.outer.getSubBlocks()[0].jump == null) {
            
            lastBlock.replace(lastBlock.outer, lastBlock);
            flow.lastModified = lastBlock;
            return true;
        }
        return false;
    }
}
