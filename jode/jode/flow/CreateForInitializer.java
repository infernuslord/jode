/* 
 * CreateForInitializer (c) 1998 Jochen Hoenicke
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
import jode.StoreInstruction;

public class CreateForInitializer {

    /**
     * This combines an variable initializer into a for statement
     * @param forBlock the for block
     * @param last  the lastModified of the flow block.
     */
    public static boolean transform(LoopBlock forBlock, StructuredBlock last) {

        if (!(last.outer instanceof SequentialBlock))
            return false;

        SequentialBlock sequBlock = (SequentialBlock) last.outer;

        if (!(sequBlock.subBlocks[0] instanceof InstructionBlock))
            return false;

        Expression initializer = 
            ((InstructionBlock) sequBlock.subBlocks[0]).getInstruction();
            
        if (!(initializer.getOperator() instanceof StoreInstruction)
            || !initializer.getOperator().isVoid())
            return false;

        if (jode.Decompiler.isVerbose)
            System.err.print('f');

        forBlock.init = initializer;
        forBlock.moveDefinitions(last.outer, null);
        last.replace(last.outer);
        return true;
    }
}
