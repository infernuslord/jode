/* 
 * CombineIfGotoExpressions (c) 1998 Jochen Hoenicke
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
import java.util.Vector;
import jode.Expression;
import jode.ComplexExpression;
import jode.Type;
import jode.BinaryOperator;

public class CombineIfGotoExpressions implements Transformation{

    public boolean transform(FlowBlock flow) {
        if (!(flow.lastModified instanceof ConditionalBlock)
            || !(flow.lastModified.outer instanceof SequentialBlock))
            return false;
        
        ConditionalBlock cb = (ConditionalBlock) flow.lastModified;
        SequentialBlock sequBlock = (SequentialBlock) cb.outer;
        Expression[] e = new Expression[2];

        e[1] = cb.getInstruction();
        Expression lastCombined = e[1];
            
        while (sequBlock.subBlocks[0] instanceof InstructionBlock) {
            InstructionBlock ib = 
                (InstructionBlock) sequBlock.subBlocks[0];

            if (!(sequBlock.outer instanceof SequentialBlock))
                return false;

            Expression expr = ib.getInstruction();
            if (lastCombined.canCombine(expr) + e[1].canCombine(expr) <= 0)
                /* Tricky, the above is true, iff one of the two
                 * Expressions conflict, or both fail.  */
                return false;

            lastCombined = expr;

            sequBlock = (SequentialBlock) sequBlock.outer;
        }
        
        if (sequBlock.subBlocks[0] instanceof ConditionalBlock) {

            ConditionalBlock cbprev = 
                (ConditionalBlock) sequBlock.subBlocks[0];

            Jump prevJump = cbprev.trueBlock.jump;

            int operator;
            if (prevJump.destination == cb.jump.destination) {
                operator = BinaryOperator.LOG_AND_OP;
                e[0] = cbprev.getInstruction().negate();
            } else if (prevJump.destination == cb.trueBlock.jump.destination) {
                operator = BinaryOperator.LOG_OR_OP;
                e[0] = cbprev.getInstruction();
            } else
                return false;

            sequBlock = (SequentialBlock) cb.outer;
            while (sequBlock.subBlocks[0] instanceof InstructionBlock) {
                /* Now combine the expression.  Everything should
                 * succeed, because we have checked above.  */
                InstructionBlock ib = 
                     (InstructionBlock) sequBlock.subBlocks[0];

                Expression expr = ib.getInstruction();

                e[1] = e[1].combine(expr);
                sequBlock = (SequentialBlock) sequBlock.outer;
            }

            flow.removeSuccessor(prevJump);
            prevJump.prev.removeJump();
            Expression cond = 
                new ComplexExpression
                (new BinaryOperator(Type.tBoolean, operator), e);
            cb.setInstruction(cond);
            cb.moveDefinitions(sequBlock, cb);
            cb.replace(sequBlock);
            return true;
        }
        return false;    
    }
}
