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
public class ConditionalBlock extends InstructionContainer {

    StructuredBlock trueBlock;
    
    /**
     * Creates a new if then else block.  The method setThenBlock must
     * be called shortly after the creation.
     */
    public ConditionalBlock(Instruction cond, Jump condJump, Jump elseJump) {
        super(cond, elseJump);
        /* cond is a CompareBinary or CompareUnary operator, so no
         * check for LocalVarOperator (for condJump) is needed here.  
         */
        trueBlock = new EmptyBlock(condJump);
        trueBlock.outer = this;
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
        writer.println("IF ("+instr.simplify().toString()+")");
        writer.tab();
        trueBlock.dumpSource(writer);
        writer.untab();
    }
}
