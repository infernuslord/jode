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
        Operator op;
        Expression exprs[];
        int params;
        StructuredBlock sequBlock;

//         try {
//             System.err.println("Transformation on: "+flow.getLabel());
//             flow.checkConsistent();
//         } catch (RuntimeException ex) {
//             try {
//                 jode.TabbedPrintWriter writer = 
//                     new jode.TabbedPrintWriter(System.err, "    ");
//                 writer.tab();
//                 flow.block.dumpSource(writer);
//             } catch (java.io.IOException ioex) {
//             }
//         }

        try {
            op = (Operator) 
                ((InstructionContainer)flow.lastModified).getInstruction();
            params = op.getOperandCount();
            if (params == 0)
                return false;
            exprs = new Expression[params];

            sequBlock = flow.lastModified.outer;
            if (sequBlock.getSubBlocks()[1] != flow.lastModified)
                return false;
            for (int i = params-1; i>=0; i--) {
                InstructionBlock block = 
                    (InstructionBlock) sequBlock.getSubBlocks()[0];
                if (block.jump != null)
                    return false;
                exprs[i] = (Expression) block.getInstruction();

                if (i > 0 && exprs[i].getOperandCount() > 0)
                    /* This is a not fully resolved expression in the
                     * middle, we must not touch it.  */
                    return false;

                if (exprs[i].isVoid()) {
		    if (i == params-1)
			return false;
		    Expression e = exprs[i+1].tryToCombine(exprs[i]);
		    if (e == null)
			return false;

		    i++;
                    SequentialBlock subExprBlock = 
                        (SequentialBlock) sequBlock.getSubBlocks()[1];
                    subExprBlock.replace(sequBlock, subExprBlock);
                    sequBlock = subExprBlock;
                    ((InstructionContainer)subExprBlock.getSubBlocks()[0]).
                        setInstruction(e);
		    exprs[i] = e;
		}
                if (i > 0)
                    sequBlock = (SequentialBlock)sequBlock.outer;
            }
        } catch (NullPointerException ex) {
            return false;
        } catch (ClassCastException ex) {
            return false;
        }
        if(jode.Decompiler.isVerbose)
            System.err.print('x');

        ((InstructionContainer) flow.lastModified).setInstruction
            (new ComplexExpression(op, exprs));
        flow.lastModified.replace(sequBlock, flow.lastModified);
        return true;
    }
}

