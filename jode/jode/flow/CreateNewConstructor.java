/* 
 * CreateNewConstructor (c) 1998 Jochen Hoenicke
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
import jode.InvokeOperator;
import jode.Expression;
import jode.ComplexExpression;
import jode.ConstructorOperator;
import jode.NewOperator;

public class CreateNewConstructor implements Transformation{

    public boolean transform(FlowBlock flow) {
        if (!(flow.lastModified instanceof InstructionBlock)
            || !(flow.lastModified.outer instanceof SequentialBlock))
            return false;
        InstructionBlock block = (InstructionBlock) flow.lastModified;
        if (!(block.getInstruction() instanceof InvokeOperator))
            return false;
        InvokeOperator constrCall = (InvokeOperator) block.getInstruction();
        if (!constrCall.isConstructor())
            return false;

        /* The rest should probably succeed */
        int params = constrCall.getOperandCount();
        Expression[] exprs = new Expression[params];
        SequentialBlock sequBlock = (SequentialBlock) block.outer;
        try {
        for_loop:
            for (int i = params-1; i>0; i--) {

                block = (InstructionBlock) sequBlock.subBlocks[0];
                exprs[i] = block.getInstruction();
                sequBlock = (SequentialBlock) sequBlock.outer;

                while (sequBlock.subBlocks[0] instanceof InstructionBlock) {

                    Expression expr = 
                        ((InstructionBlock) sequBlock.subBlocks[0])
                        .getInstruction();

                    if (!expr.isVoid())
                        continue for_loop;
                    if (exprs[i].canCombine(expr) <= 0)
                        return false;

		    exprs[i] = exprs[i].combine(expr);
                    SequentialBlock subExprBlock = 
                        (SequentialBlock) sequBlock.subBlocks[1];
                    subExprBlock.moveDefinitions(sequBlock, subExprBlock);
                    subExprBlock.replace(sequBlock);
                    sequBlock = subExprBlock;
                    ((InstructionContainer)subExprBlock.subBlocks[0]).
                        setInstruction(exprs[i]);
                    sequBlock = (SequentialBlock)sequBlock.outer;
		}
            }
            SpecialBlock dup = (SpecialBlock) sequBlock.subBlocks[0];
            if (dup.type != SpecialBlock.DUP 
                || dup.count != 1 || dup.depth != 0)
                return false;
            sequBlock = (SequentialBlock)sequBlock.outer;
            block = (InstructionBlock) sequBlock.subBlocks[0];
            exprs[0] = block.getInstruction();
            if (exprs[0].isVoid())
                return false;
            NewOperator op = (NewOperator) exprs[0].getOperator();
            if (constrCall.getClassType() != op.getType())
                return false;
        } catch (ClassCastException ex) {
            return false;
        } catch (NullPointerException ex) {
            return false;
        }
        ((InstructionContainer) flow.lastModified).setInstruction
            (new ComplexExpression
             (new ConstructorOperator(constrCall.getClassType(), 
                                      constrCall.getField()),
              exprs));
             
        flow.lastModified.moveDefinitions(sequBlock, null);
        flow.lastModified.replace(sequBlock);
        return true;
    }
}
