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
        return (createAssignOp(flow) || createAssignExpression(flow));
    }

    public boolean createAssignOp(FlowBlock flow) {
        Expression rightHandSide;
        StoreInstruction store;
        BinaryOperator binop;
        InstructionContainer lastBlock;
        SequentialBlock opBlock;
        SequentialBlock sequBlock;
        boolean isExpression = false;
        try {
            InstructionBlock ib;
            lastBlock = (InstructionContainer) flow.lastModified;
            store = (StoreInstruction) lastBlock.getInstruction();

            opBlock = (SequentialBlock) lastBlock.outer;
            if (opBlock.subBlocks[1] != lastBlock)
                return false;
            ib = (InstructionBlock) opBlock.subBlocks[0];

            if (ib.getInstruction() instanceof DupOperator) {
                DupOperator dup = (DupOperator) ib.getInstruction();
                if (dup.getDepth() != store.getLValueOperandCount() && 
                    dup.getCount() != store.getLValueType().stackSize())
                    return false;
                opBlock = (SequentialBlock) lastBlock.outer;
                ib = (InstructionBlock) opBlock.subBlocks[0];
                isExpression = true;
            }

            ComplexExpression binopExpr = 
                (ComplexExpression) ib.getInstruction();
            binop = (BinaryOperator) binopExpr.getOperator();
            if (binop.getOperatorIndex() <  binop.ADD_OP ||
                binop.getOperatorIndex() >= binop.ASSIGN_OP)
                return false;

            rightHandSide = binopExpr.getSubExpressions()[1];
            if (rightHandSide.isVoid())
                return false; /* XXX */

            Operator load = (Operator) binopExpr.getSubExpressions()[0];
            if (!store.matches(load))
                return false;

            sequBlock = (SequentialBlock) opBlock.outer;
            ib = (InstructionBlock) sequBlock.subBlocks[0];

            DupOperator dup = (DupOperator) ib.getInstruction();
            if (dup.getDepth() != 0 && 
                dup.getCount() != store.getLValueOperandCount())
                return false;
        } catch (ClassCastException ex) {
            return false;
        } catch (NullPointerException ex) {
            return false;
        }
        ((InstructionBlock)opBlock.subBlocks[0])
            .setInstruction(rightHandSide);
        opBlock.replace(sequBlock, opBlock);

        store.setOperatorIndex(store.OPASSIGN_OP+binop.getOperatorIndex());
        store.setLValueType(binop.getType()
                            .intersection(store.getLValueType()));

        if (isExpression)
            lastBlock.setInstruction
                (new AssignOperator(store.getOperatorIndex(), store));
        else
            lastBlock.setInstruction(store);
        lastBlock.replace(opBlock.subBlocks[1], lastBlock);
        return true;
    }

    public boolean createAssignExpression(FlowBlock flow) {
        StoreInstruction store;
        InstructionContainer lastBlock;
        SequentialBlock sequBlock;
        try {
            lastBlock = (InstructionContainer) flow.lastModified;
            store = (StoreInstruction) lastBlock.getInstruction();

            sequBlock = (SequentialBlock) lastBlock.outer;
            InstructionBlock ib = (InstructionBlock) sequBlock.subBlocks[0];

            DupOperator dup = (DupOperator) ib.getInstruction();
            if (dup.getDepth() != store.getLValueOperandCount() && 
                dup.getCount() != store.getLValueType().stackSize())
                return false;
        } catch (NullPointerException ex) {
            return false;
        } catch (ClassCastException ex) {
            return false;
        }
        lastBlock.setInstruction
            (new AssignOperator(store.getOperatorIndex(), store));
        lastBlock.replace(sequBlock, lastBlock);
        return true;
                          
    }
}
