/* 
 * CreateIfThenElseOperator (c) 1998 Jochen Hoenicke
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
import jode.Type;
import jode.Expression;
import jode.ConstOperator;
import jode.ComplexExpression;
import jode.IfThenElseOperator;
import jode.CompareUnaryOperator;
import java.util.Enumeration;
import java.util.Vector;

public class CreateIfThenElseOperator {

    /**
     * This handles the body of createFunny. There are three cases:
     *
     * <pre>
     * --------
     *  IF (c2)
     *    GOTO trueDest            ->   PUSH c2
     *  PUSH false
     * --------
     *  PUSH bool                  ->   PUSH bool
     * --------
     *  if (c2)
     *    (handled recursively)    ->   PUSH (c2 ? expr1 : expr2)
     *  else
     *    (handled recursively)
     * --------
     * </pre>
     */
    private static boolean createFunnyHelper(FlowBlock trueDest, 
                                             FlowBlock falseDest,
                                             StructuredBlock block) {

        if (block instanceof InstructionBlock
            && !((InstructionBlock)block).getInstruction().isVoid())
            return true;
        
        if (block instanceof IfThenElseBlock) {
            IfThenElseBlock ifBlock = (IfThenElseBlock) block;
            Expression expr1, expr2;
            if (ifBlock.elseBlock == null)
                return false;

            if (!createFunnyHelper(trueDest, falseDest, ifBlock.thenBlock)
                | !createFunnyHelper(trueDest, falseDest, ifBlock.elseBlock))
                return false;

            if (jode.Decompiler.isVerbose)
                System.err.print('?');

            IfThenElseOperator iteo = new IfThenElseOperator(Type.tBoolean);
            ((InstructionBlock)ifBlock.thenBlock).setInstruction
                (new ComplexExpression
                 (iteo, new Expression[] {
                     ifBlock.cond,
                     ((InstructionBlock) ifBlock.thenBlock).getInstruction(),
                     ((InstructionBlock) ifBlock.elseBlock).getInstruction() 
                  }));

            ifBlock.thenBlock.moveDefinitions(ifBlock, null);
            ifBlock.thenBlock.replace(ifBlock);
            return true;
        }

        if (block instanceof SequentialBlock
            && block.getSubBlocks()[0] instanceof ConditionalBlock
            && block.getSubBlocks()[1] instanceof InstructionBlock) {

            ConditionalBlock condBlock =
                (ConditionalBlock) block.getSubBlocks()[0];
            InstructionBlock pushBlock =
                (InstructionBlock) block.getSubBlocks()[1];
            
            if (!(pushBlock.getInstruction() instanceof ConstOperator))
                return false;

            ConstOperator constOp = 
                (ConstOperator) pushBlock.getInstruction();

            if (condBlock.trueBlock.jump.destination == trueDest
                && constOp.getValue().equals("0")) {

                Expression cond = condBlock.getInstruction();
                condBlock.flowBlock.removeSuccessor(condBlock.trueBlock.jump);
                condBlock.trueBlock.removeJump();

                pushBlock.setInstruction(cond);
                pushBlock.moveDefinitions(block, null);
                pushBlock.replace(block);
                return true;
            }
        }
        return false;
    }

    /**
     * This handles the more complicated form of the ?-:-operator used
     * in a conditional block.  The simplest case is:
     * <pre>
     *   if (cond)
     *       PUSH e1
     *   else {
     *       IF (c2)
     *           GOTO flow_2_
     *       PUSH false
     *   }
     * -&gt;IF (stack_0 == 0)
     *     GOTO flow_1_
     *   GOTO flow_2_
     * </pre>
     * is transformed to
     * <pre>
     *   push cond ? e1 : c2
     * -&gt;IF (stack_0 == 0)
     *     GOTO flow_1_
     *   GOTO flow_2_
     * </pre>
     *
     * The <code>-&gt;</code> points to the lastModified block.  Note
     * that both the if and the then part may contain this
     * condition+push0-block.  There may be even stack if-then-else-blocks
     * for very complicated nested ?-:-Operators. <p>
     *
     * Also note that the produced code is suboptimal:  The push-0 could
     * sometimes be better replaced with a correct jump.
     * @param flow The FlowBlock that is transformed 
     */
    public static boolean createFunny(ConditionalBlock cb, 
                                      StructuredBlock last) {

        if (cb.jump == null
            || !(cb.getInstruction() instanceof CompareUnaryOperator)
            || !(last.outer instanceof SequentialBlock)
            || !(last.outer.getSubBlocks()[0] instanceof IfThenElseBlock))
            return false;
        
        CompareUnaryOperator compare = 
            (CompareUnaryOperator) cb.getInstruction();

        FlowBlock trueDestination;
        FlowBlock falseDestination;
        if (compare.getOperatorIndex() == compare.EQUALS_OP) {
            trueDestination = cb.jump.destination;
            falseDestination = cb.trueBlock.jump.destination;
        } else if (compare.getOperatorIndex() == compare.NOTEQUALS_OP) {
            falseDestination = cb.jump.destination;
            trueDestination = cb.trueBlock.jump.destination;
        } else
            return false;

        Expression[] e = new Expression[3];
        IfThenElseBlock ifBlock;

        SequentialBlock sequBlock = (SequentialBlock) last.outer;
        return createFunnyHelper(trueDestination, falseDestination,
                                 sequBlock.subBlocks[0]);
    }

    /**
     * This handles the normal form of the ?-:-operator:
     * <pre>
     *   if (cond)
     *       push e1
     *       GOTO flow_1_
     * -&gt;push e2
     *   GOTO flow_2
     * </pre>
     * is transformed to
     * <pre>
     * -&gt;push cond ? e1 : e2
     * </pre>
     * The <code>-&gt;</code> points to the lastModified block.
     * @param flow The FlowBlock that is transformed
     */
    public static boolean create(InstructionContainer ic,
                                 StructuredBlock last) {
        Expression e[] = new Expression[3];
        InstructionBlock thenBlock;
        if (ic.jump == null
            || !(last.outer instanceof SequentialBlock))
            return false;
        SequentialBlock sequBlock = (SequentialBlock)last.outer;
        if (!(sequBlock.subBlocks[0] instanceof IfThenElseBlock))
            return false;

        IfThenElseBlock ifBlock = (IfThenElseBlock)sequBlock.subBlocks[0];
        if (!(ifBlock.thenBlock instanceof InstructionBlock)
            || ifBlock.thenBlock.jump == null
            || ifBlock.thenBlock.jump.destination != ic.jump.destination
            || ifBlock.elseBlock != null)
            return false;
        
        thenBlock = (InstructionBlock) ifBlock.thenBlock;
        
        e[1] = thenBlock.getInstruction();
        if (e[1].isVoid())
            return false;
        e[2] = ic.getInstruction();
        if (e[2].isVoid())
            return false;
        e[0] = ifBlock.cond;
        
        if (jode.Decompiler.isVerbose)
            System.err.print('?');

        thenBlock.flowBlock.removeSuccessor(thenBlock.jump);
        thenBlock.removeJump();

        IfThenElseOperator iteo = new IfThenElseOperator
            (Type.tSuperType(e[1].getType())
             .intersection(Type.tSuperType(e[2].getType())));

        ic.setInstruction(new ComplexExpression(iteo, e));
        ic.moveDefinitions(last.outer, last);
        last.replace(last.outer);
        return true;
    }
}
