/* 
 * CreateExpression (c) 1998 Jochen Hoenicke
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
import jode.Operator;
import jode.Expression;
import jode.ComplexExpression;

/**
 * This transformation creates expressions.  It transforms
 * <pre>
 *  Sequ[expr_1, Sequ[expr_2, ..., Sequ[expr_n, op] ...]] 
 * </pre>
 * to
 * <pre>
 *  expr(op, [ expr_1, ..., expr_n ])
 * </pre>
 */
public class CreateExpression implements Transformation {

    /**
     * This does the transformation.
     * @param FlowBlock the flow block to transform.
     * @return true if flow block was simplified.
     */
    public boolean transform(FlowBlock flow) {
        if (!(flow.lastModified instanceof InstructionContainer)
            || !(flow.lastModified.outer instanceof SequentialBlock))
            return false;

        InstructionContainer ic = (InstructionContainer) flow.lastModified;
        if (ic.getInstruction() instanceof Operator) {

            Operator op = (Operator) ic.getInstruction();
            int params = op.getOperandCount();
            if (params == 0)
                return false;

            Expression[] exprs = new Expression[params];

            SequentialBlock sequBlock = (SequentialBlock) ic.outer;

            Expression lastExpression = null;
            /* First check if Expression can be created, but do nothing yet.
             */
            for (int i=params; ;) {
                if (!(sequBlock.subBlocks[0] instanceof InstructionBlock))
                    return false;

                InstructionBlock block = 
                    (InstructionBlock) sequBlock.subBlocks[0];
                
                Expression expr = block.getInstruction();

                if (!expr.isVoid()) {
                    if (--i == 0)
                        break;
                } else if (lastExpression != null
                           && lastExpression.canCombine(expr) <= 0)
                    return false;

                if (expr.getOperandCount() > 0)
                    /* This is a not fully resolved expression in the
                     * middle, we must not touch it.  */
                    return false;

                lastExpression =  expr;
                if (!(sequBlock.outer instanceof SequentialBlock))
                    return false;
                sequBlock = (SequentialBlock)sequBlock.outer;
            }

            /* Now, do the combination. Everything must succeed now.
             */
            sequBlock = (SequentialBlock) ic.outer;
            for (int i=params; ;) {

                InstructionBlock block = 
                    (InstructionBlock) sequBlock.subBlocks[0];
                
                Expression expr = block.getInstruction();

                if (!expr.isVoid()) {
                    exprs[--i] = expr;
                    if (i == 0)
                        break;
                } else
                    exprs[i] = exprs[i].combine(expr);

                sequBlock = (SequentialBlock)sequBlock.outer;
            }

            if(jode.Decompiler.isVerbose)
                System.err.print('x');
            
            ic.setInstruction(new ComplexExpression(op, exprs));
            ic.moveDefinitions(sequBlock, ic);
            ic.replace(sequBlock);
            return true;
        }
        return false;
    }
}
