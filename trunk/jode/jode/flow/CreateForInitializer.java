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

public class CreateForInitializer implements Transformation {

    /**
     * This combines an variable initializer into a for statement
     * @param flow The FlowBlock that is transformed 
     */
    public boolean transform(FlowBlock flow) {

        if (!(flow.lastModified instanceof LoopBlock)
            || !(flow.lastModified.outer instanceof SequentialBlock))
            return false;

        LoopBlock forBlock = (LoopBlock) flow.lastModified;
        if (forBlock.type != forBlock.FOR || forBlock.init != null)
            return false;
 
        SequentialBlock sequBlock = (SequentialBlock) forBlock.outer;

        if (!(sequBlock.subBlocks[0] instanceof InstructionBlock))
            return false;

        Expression initializer = 
            ((InstructionBlock) sequBlock.subBlocks[0]).getInstruction();
            
        if (!(initializer.getOperator() instanceof StoreInstruction))
            return false;

        if (jode.Decompiler.isVerbose)
            System.err.print('f');

        forBlock.init = initializer;
        forBlock.replace(forBlock.outer, forBlock);
        return true;
    }
}
