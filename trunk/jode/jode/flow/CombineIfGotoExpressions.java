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
        Expression[] e;
        ConditionalBlock cb;
        Jump prevJump;
        int operator;
        try {
            cb = (ConditionalBlock) flow.lastModified;
            SequentialBlock sequBlock = (SequentialBlock) cb.outer;
            if (sequBlock.subBlocks[1] != cb)
                return false;

            // jode.Assert.assert(sequBlock.jump == null)

            e = new Expression[2];
            e[1] = (Expression)cb.getInstruction();
            while (sequBlock.subBlocks[0] instanceof InstructionBlock) {
                InstructionBlock ib = 
                    (InstructionBlock) sequBlock.subBlocks[0];
                Expression expr = (Expression) ib.getInstruction();
                if (!expr.isVoid())
                    return false;

                Expression combined = e[1].tryToCombine(expr);
                if (combined == null)
                    return false;
                
                cb.replace(sequBlock, cb);
                cb.setInstruction(combined);
                e[1] = combined;
                sequBlock = (SequentialBlock) cb.outer;
            }

            ConditionalBlock cbprev = 
                (ConditionalBlock) sequBlock.subBlocks[0];

            if (cbprev.jump != null)
                return false;
            
            prevJump = ((EmptyBlock) cbprev.trueBlock).jump;

            if (prevJump.destination == cb.jump.destination) {
                operator = BinaryOperator.LOG_AND_OP;
                e[0] = ((Expression)cbprev.getInstruction()).negate();
            } else if (prevJump.destination
                       == ((EmptyBlock) cb.trueBlock).jump.destination) {
                operator = BinaryOperator.LOG_OR_OP;
                e[0] = (Expression)cbprev.getInstruction();
            } else
                return false;
        } catch (ClassCastException ex) {
            return false;
        } catch (NullPointerException ex) {
            return false;
        }
        flow.removeSuccessor(prevJump);
        prevJump.prev.removeJump();
        Expression cond = 
            new ComplexExpression
            (new BinaryOperator(Type.tBoolean, operator), e);
        cb.setInstruction(cond);
        cb.replace(cb.outer, cb);
        return true;
    }
}
