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
import jode.Decompiler;
import jode.expr.*;

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
public class CreateExpression {

    /**
     * This does the transformation.
     * @param FlowBlock the flow block to transform.
     * @return true if flow block was simplified.
     */
    public static boolean transform(InstructionContainer ic,
                                    StructuredBlock last) {
        int params = ic.getInstruction().getOperandCount();
        if (params == 0)
            return false;

        if (!(last.outer instanceof SequentialBlock))
            return false;
        SequentialBlock sequBlock = (SequentialBlock)last.outer;

        /* First check if Expression can be created, but do nothing yet.
         */
        Expression lastExpression = ic.getInstruction();
	while (true) {
	    if (!(sequBlock.subBlocks[0] instanceof InstructionBlock))
		return false;
	    
	    Expression expr =
		((InstructionBlock) sequBlock.subBlocks[0]).getInstruction();

	    if (!expr.isVoid())
		break;

	    if (expr.getOperandCount() > 0
		|| !(expr.getOperator() instanceof CombineableOperator)
		|| lastExpression.canCombine(expr) <= 0)
		return false;

	    lastExpression =  expr;
	    if (!(sequBlock.outer instanceof SequentialBlock))
		return false;
            sequBlock = (SequentialBlock) sequBlock.outer;
	}
	    
        /* Now, do the combination. Everything must succeed now.
         */
        sequBlock = (SequentialBlock) last.outer;
	lastExpression = ic.getInstruction();
	while (true) {

	    Expression expr =
		((InstructionBlock) sequBlock.subBlocks[0]).getInstruction();
	    
	    if (!expr.isVoid()) {
		lastExpression = lastExpression.addOperand(expr);
		break;
	    }

	    lastExpression = lastExpression.combine(expr);
            sequBlock = (SequentialBlock)sequBlock.outer;
        }

        if(Decompiler.isVerbose && lastExpression.getOperandCount() == 0)
            Decompiler.err.print('x');

	ic.setInstruction(lastExpression);
        ic.moveDefinitions(sequBlock, last);
        last.replace(sequBlock);
        return true;
    }
}
