/* 
 * MarkInlineExpression (c) 1998 Jochen Hoenicke
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
 * This handles inline methods.  When you compile with -O flag javac will
 * inline methods that are static, final or private (i.e. non virtual).  
 * The parameters may be stored in locals, before inlining the body of the
 * method.  
 * 
 * Inlined methods look as follows:
 * 
 * local_1 = expr_1
 * local_2 = expr_2
 * [PUSH ]expr(local_1,local_2,constants...)
 *
 * where local_1 and local_2 aren't used any more.
 *
 * If later one finds a reuse of local_x, the inline transformation
 * must be reverted: If there is no surrounding expression, the locals
 * are moved to initializers around; otherwise if the locals are used
 * in the expression or a super expression, they are combined with the
 * next occurence; otherwise the locals are moved behind the next expression.
 * This doesn't care for side effects though :-(
 *
 * There are many other reasons why a part could look like this; only
 * if a matching method is found, that does exactly this (or if the
 * options createInlines is given), we may call this method.  
 *
 * As long as inlines are not supported, if the local is never used, it
 * is simply removed; if the local is used only one time and there are
 * no side effects to the expression on the right hand side between usage
 * and initialization, the usage is replaced by the right hand side.
 */
public class MarkInlineExpression {

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
        Expression expr = ic.getInstruction();

        if (!(last.outer instanceof SequentialBlock))
            return false;
        SequentialBlock sequBlock = (SequentialBlock)last.outer;

	int localCount = 0;
	while (sequBlock.subBlocks[0] instanceof InstructionBlock) {
	    InstructionBlock assign = 
		(InstructionBlock) sequBlock.subBlocks[0];
	    if (!(assign.getInstruction().getOperator()
		  instanceof LocalStoreOperator)
		|| assign.getInstruction().getOperandCount() == 0)
		break;
	    
	    LocalStoreOperator store = (LocalStoreOperator)
		assign.getInstruction().getOperator();
	    if (!store.getLocal().onlyUsedInside(expr))
		break;

	    localCount++;

	    if (!(sequBlock.outer instanceof SequentialBlock))
		break;
	    sequBlock = (SequentialBlock)sequBlock.outer;
        }
	if (localCount == 0)
	    return false;

	Expression[] stores = new Expression[localCount];
        sequBlock = (SequentialBlock) last.outer;
        for (int i=0; i<stores; i++) {
	    stores[i] = ((InstructionBlock)sequBlock.subBlocks[0])
		.getInstruction();
	    LocalStoreOperator store = 
		(LocalStoreOperator) stores[i].getOperator();
	    if (!store.getLocal().onlyUsedInside(expr))
	    stores[i].get.markInlinedIn(expr);
	    sequBlock = (SequentialBlock)sequBlock.outer;
	}

	expr.markInlineExpression(stores);
        ic.moveDefinitions(sequBlock, last);
        last.replace(sequBlock);
        return true;
    }
}

