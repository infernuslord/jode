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
        return (removeNop(fb) || removeEmpty(fb));
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
