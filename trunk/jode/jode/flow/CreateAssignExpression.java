/* 
 * CreateAssignExpression (c) 1998 Jochen Hoenicke
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
import jode.*;

public class CreateAssignExpression implements Transformation{

    public boolean transform(FlowBlock flow) {
        if (!(flow.lastModified instanceof InstructionContainer)
            || !(flow.lastModified.outer instanceof SequentialBlock)
            || !(((InstructionContainer)flow.lastModified).getInstruction()
                 instanceof StoreInstruction)
            || !(((InstructionContainer)flow.lastModified).getInstruction()
                 .isVoid()))
            return false;
        
        return (createAssignOp(flow) || createAssignExpression(flow));
    }

    public boolean createAssignOp(FlowBlock flow) {

        /* Situation:
         *
         *   (push loadstoreOps)  <- not checked
         * sequBlock:
         *   dup
         * opBlock:
         *   load(stack) * rightHandSide
         *   (optional dup_x)
         *   store(stack)
         *
         * We transform it to:
         *   (push loadstoreOps)
         *   rightHandSide
         *   store(stack) *= (stack)
         *
         * If the optional dup is present the store*= becomes non void.
         */
        InstructionContainer lastBlock
            = (InstructionContainer) flow.lastModified;
        SequentialBlock opBlock = (SequentialBlock) lastBlock.outer;
        StoreInstruction store 
            = (StoreInstruction) lastBlock.getInstruction();

        boolean isAssignOp = false;
        if (opBlock.subBlocks[0] instanceof SpecialBlock) {
            SpecialBlock dup = (SpecialBlock) opBlock.subBlocks[0];
            if (dup.type != SpecialBlock.DUP
                || dup.depth != store.getLValueOperandCount()
                || dup.count != store.getLValueType().stackSize()
                || !(opBlock.outer instanceof SequentialBlock))
                return false;
            opBlock = (SequentialBlock) opBlock.outer;
            isAssignOp = true;
        }

        if (!(opBlock.subBlocks[0] instanceof InstructionBlock))
            return false;

        InstructionBlock ib = (InstructionBlock) opBlock.subBlocks[0];
        
        if (!(opBlock.outer instanceof SequentialBlock)
            || !(opBlock.outer.getSubBlocks()[0] instanceof SpecialBlock))
            return false;

        SequentialBlock sequBlock = (SequentialBlock) opBlock.outer;
        SpecialBlock dup = (SpecialBlock) sequBlock.subBlocks[0];
        if (dup.type != SpecialBlock.DUP
            || dup.depth != 0
            || dup.count != store.getLValueOperandCount())
            return false;

        if (!(ib.getInstruction() instanceof ComplexExpression))
            return false;
        ComplexExpression expr = (ComplexExpression) ib.getInstruction();

        int opIndex;
        Expression rightHandSide;

        if (expr.getOperator() instanceof BinaryOperator) {
            BinaryOperator binop = (BinaryOperator) expr.getOperator();
            
            opIndex = binop.getOperatorIndex();
            
            if (opIndex <  binop.ADD_OP || opIndex >= binop.ASSIGN_OP
                || !(expr.getSubExpressions()[0] instanceof Operator)
                || !store.matches((Operator) expr.getSubExpressions()[0]))
                return false;
            
            rightHandSide = expr.getSubExpressions()[1];
        } else {
            Expression simple = expr.simplifyString();
            rightHandSide = simple;
            /* Now search for the leftmost operand ... */
            ComplexExpression last = null;
            while (simple instanceof ComplexExpression
                   && simple.getOperator() instanceof StringAddOperator) {
                last = (ComplexExpression) simple;
                simple = last.getSubExpressions()[0];
            }

            /* ... check it ... */
            if (last == null || !(simple instanceof Operator)
                || !store.matches((Operator) simple))
                return false;
            
            /* ... and remove it. */
            if (last.getParent() != null) {
                ((ComplexExpression)last.getParent()).getSubExpressions()[0] 
                    = last.getSubExpressions()[1];
            } else
                rightHandSide = last.getSubExpressions()[1]; 

            opIndex = Operator.ADD_OP;
        }
        dup.removeBlock();
        ib.setInstruction(rightHandSide);
        
        store.setOperatorIndex(store.OPASSIGN_OP+opIndex);

        if (isAssignOp)
            store.makeNonVoid();
        lastBlock.replace(opBlock.subBlocks[1]);
        return true;
    }

    public boolean createAssignExpression(FlowBlock flow) {
        /* Situation:
         * sequBlock:
         *   dup_X(lvalue_count)
         *   store instruction
         */
        InstructionContainer lastBlock
            = (InstructionContainer) flow.lastModified;
        SequentialBlock sequBlock = (SequentialBlock) lastBlock.outer;
        StoreInstruction store = (StoreInstruction) lastBlock.getInstruction();

        if (sequBlock.subBlocks[0] instanceof SpecialBlock) {

            SpecialBlock dup = (SpecialBlock) sequBlock.subBlocks[0];
            if (dup.type != SpecialBlock.DUP
                || dup.depth != store.getLValueOperandCount()
                || dup.count != store.getLValueType().stackSize())
                return false;
            
            dup.removeBlock();
            store.makeNonVoid();
            return true;
        }
        return false;
    }
}
