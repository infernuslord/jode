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

        ComplexExpression parent = null;
        Expression inner = ic.getInstruction();
        while (inner instanceof ComplexExpression) {
            parent = (ComplexExpression)inner;
            inner = parent.getSubExpressions()[0];
        }

        if (!(inner instanceof Operator))
            return false;

        Operator op = (Operator)inner;

        if (!(last.outer instanceof SequentialBlock))
            return false;
        SequentialBlock sequBlock = (SequentialBlock)last.outer;

        /* First check if Expression can be created, but do nothing yet.
         */
        Expression lastExpression = null;
        for (int i = params;;) {
	    
	    if (i >= 2 && sequBlock.subBlocks[0] instanceof SpecialBlock) {
		/* Transform (this is for string += under jikes)
		 *   PUSH arg2
		 *   PUSH arg1
		 *   SWAP
		 */
		SpecialBlock swap = (SpecialBlock) sequBlock.subBlocks[0];
		if (swap.type != SpecialBlock.SWAP)
		    return false;
		/* XXX check if swapping is possible */
	    } else {
		if (!(sequBlock.subBlocks[0] instanceof InstructionBlock))
		    return false;
		
		Expression expr =
		    ((InstructionBlock) sequBlock.subBlocks[0]).getInstruction();
		
		if (!expr.isVoid()) {
		    if (--i == 0)
			break;
		} else if (lastExpression == null
			   || !(expr.getOperator() 
				instanceof CombineableOperator)
			   || lastExpression.canCombine(expr) <= 0)
		    return false;
		
		if (expr.getOperandCount() > 0)
		    /* This is a not fully resolved expression in the
		     * middle, we must not touch it.  */
		    return false;

		lastExpression =  expr;
	    }
	    if (!(sequBlock.outer instanceof SequentialBlock))
		return false;
            sequBlock = (SequentialBlock) sequBlock.outer;
        }

        /* Now, do the combination. Everything must succeed now.
         */
        Expression[] exprs = new Expression[params];
        sequBlock = (SequentialBlock) last.outer;
	int swapping = 0;
        for (int i=params; ;) {

	    if (sequBlock.subBlocks[0] instanceof SpecialBlock) {
		// This must be a swap (see above).
		swapping = 1;
		
	    } else {
		Expression expr =
		    ((InstructionBlock) sequBlock.subBlocks[0]).getInstruction();
		
		if (!expr.isVoid()) {
		    if (swapping == 1) {
			// We start swapping:
			i--;
			swapping = 2;
		    } else if (swapping == 2) {
			// We continue swapping:
			i += 2;
			swapping = 3;
		    } else if (swapping == 3) {
			// Now we end swapping:
			i--;
			swapping = 0;
		    }
		    exprs[--i] = expr;
		    if (i == 0 && swapping == 0
			|| i == 1 && swapping == 3)
			break;
		} else
		    exprs[i] = exprs[i].combine(expr);
	    }
            
            sequBlock = (SequentialBlock)sequBlock.outer;
        }

        if(Decompiler.isVerbose)
            Decompiler.err.print('x');

        Expression newExpr;
        if (params == 1 && op instanceof NopOperator) {
            exprs[0].setType(op.getType());
            newExpr = exprs[0];
        } else
            newExpr = new ComplexExpression(op, exprs);

        if (parent != null)
            parent.setSubExpressions(0, newExpr);
        else
            ic.setInstruction(newExpr);

        ic.moveDefinitions(sequBlock, last);
        last.replace(sequBlock);
        return true;
    }
}
