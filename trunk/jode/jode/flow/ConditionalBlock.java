/* ConditionalBlock (c) 1998 Jochen Hoenicke
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
import jode.LocalVarOperator;
import jode.TabbedPrintWriter;

/**
 * An ConditionalBlock is the structured block representing an if
 * instruction.  The else part may be null.
 */
public class ConditionalBlock extends StructuredBlock 
    implements InstructionContainer {

    /**
     * The condition.  Must be of boolean type.
     */
    Instruction cond;

    StructuredBlock trueBlock;
    
    /**
     * Creates a new if then else block.  The method setThenBlock must
     * be called shortly after the creation.
     */
    public ConditionalBlock(Instruction cond, Jump condJump, Jump elseJump) {
        this.cond = cond;
        if (cond instanceof LocalVarOperator) {
            LocalVarOperator varOp = (LocalVarOperator) cond;
            if (varOp.isRead()) {
                in.addElement(varOp.getLocalInfo());
            }
            out.addElement(varOp.getLocalInfo());
        }
        this.jump = elseJump;
        elseJump.prev = this;
        trueBlock = new EmptyBlock(condJump);
        trueBlock.outer = this;
    }

    public Instruction getInstruction() {
        return cond;
    }

    /**
     * Change the underlying instruction.
     * @param instr the new underlying instruction.
     */
    public void setInstruction(Instruction instr) {
        this.cond = instr;
    }

    public Instruction getCondition(Jump forJump) {
        /*XXX*/
        return cond;
    }

    /* The implementation of getNext[Flow]Block is the standard
     * implementation 
     */

    /**
     * Returns all sub block of this structured block.
     */
    public StructuredBlock[] getSubBlocks() {
        StructuredBlock[] result = { trueBlock };
        return result;
    }

    /**
     * Replaces the given sub block with a new block.
     * @param oldBlock the old sub block.
     * @param newBlock the new sub block.
     * @return false, if oldBlock wasn't a direct sub block.
     */
    public boolean replaceSubBlock(StructuredBlock oldBlock, 
                            StructuredBlock newBlock) {
        if (trueBlock == oldBlock)
            trueBlock = newBlock;
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
        writer.println("IF ("+cond.toString()+")");
        writer.tab();
        trueBlock.dumpSource(writer);
        writer.untab();
    }
}
