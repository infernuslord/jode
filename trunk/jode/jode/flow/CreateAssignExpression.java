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
        SequentialBlock rhsBlock;
        SequentialBlock sequBlock;
        try {
            InstructionBlock ib;
            lastBlock = (InstructionContainer) flow.lastModified;
            store = (StoreInstruction) lastBlock.getInstruction();

            sequBlock = (SequentialBlock) lastBlock.outer;
            if (sequBlock.subBlocks[1] != lastBlock)
                return false;
            ib = (InstructionBlock) sequBlock.subBlocks[0];

            binop = (BinaryOperator) ib.getInstruction();
            if (binop.getOperator() <  binop.ADD_OP ||
                binop.getOperator() >= binop.ASSIGN_OP)
                return false;

            sequBlock = (SequentialBlock) sequBlock.outer;
            ib = (InstructionBlock) sequBlock.subBlocks[0];

            rightHandSide = (Expression) ib.getInstruction();
            if (rightHandSide.isVoid())
                return false; /* XXX */

            rhsBlock = (SequentialBlock) sequBlock.outer;
            ib = (InstructionBlock) rhsBlock.subBlocks[0];

            Operator load = (Operator) ib.getInstruction();
            if (!store.matches(load))
                return false;

            sequBlock = (SequentialBlock) rhsBlock.outer;
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
        ((InstructionBlock)rhsBlock.subBlocks[0])
            .setInstruction(rightHandSide);
        rhsBlock.replace(sequBlock, rhsBlock);

        store.setOperator(store.OPASSIGN_OP+binop.getOperator());
        store.setLValueType(MyType.intersection(binop.getType(), 
                                                store.getLValueType()));
        lastBlock.setInstruction(store);
        lastBlock.replace(rhsBlock.subBlocks[1], lastBlock);
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
            (new AssignOperator(Operator.ASSIGN_OP, store));
        lastBlock.replace(sequBlock, lastBlock);
        return true;
                          
    }
}
