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
import jode.DupOperator;
import jode.NewOperator;

public class CreateNewConstructor implements Transformation{

    public boolean transform(FlowBlock flow) {
        SequentialBlock sequBlock;
        InvokeOperator constrCall;
        Expression exprs[];
        try {
            InstructionBlock block;
            block = (InstructionBlock) flow.lastModified;
            constrCall = (InvokeOperator) block.getInstruction();
            if (!constrCall.isConstructor())
                return false;
            int params = constrCall.getOperandCount();
            exprs = new Expression[params];

            sequBlock = (SequentialBlock) block.outer;
            if (sequBlock.getSubBlocks()[1] != block)
                return false;

            for (int i = params-1; i>0; i--) {

                block = (InstructionBlock) sequBlock.getSubBlocks()[0];
                if (block.jump != null)
                    return false;

                exprs[i] = (Expression) block.getInstruction();
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
                sequBlock = (SequentialBlock)sequBlock.outer;
            }
            block = (InstructionBlock) sequBlock.getSubBlocks()[0];
            DupOperator dup = (DupOperator) block.getInstruction();
            if (dup.getCount() != 1 && dup.getDepth() != 0)
                return false;
            sequBlock = (SequentialBlock)sequBlock.outer;
            block = (InstructionBlock) sequBlock.getSubBlocks()[0];
            exprs[0] = (Expression) block.getInstruction();
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
             
        flow.lastModified.replace(sequBlock, flow.lastModified);
        return true;
    }
}
