/* 
 * CreatePrePostIncExpression (c) 1998 Jochen Hoenicke
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

public class CreatePrePostIncExpression implements Transformation {

    public boolean transform(FlowBlock flow)
    {
        return (createLocalPrePostInc(flow) || createPostInc(flow));
    }
    
    public boolean createLocalPrePostInc(FlowBlock flow) {
        IIncOperator iinc;
        boolean isPost;
        int op;
        InstructionContainer lastBlock;
        Type type;
        try {
            lastBlock = (InstructionContainer) flow.lastModified;

            Expression instr2 = lastBlock.getInstruction();
            SequentialBlock sequBlock = (SequentialBlock)lastBlock.outer;
            if (sequBlock.subBlocks[1] != lastBlock)
                return false;
            InstructionBlock ib = (InstructionBlock) sequBlock.subBlocks[0];
            Expression instr1 = ib.getInstruction();

            LocalLoadOperator load;
            if (instr1 instanceof IIncOperator 
                && instr2 instanceof LocalLoadOperator) {
                iinc = (IIncOperator) instr1;
                load = (LocalLoadOperator) instr2;
                isPost = false;
            } else if (instr1 instanceof LocalLoadOperator
                       && instr2 instanceof IIncOperator) {
                load = (LocalLoadOperator) instr1;
                iinc = (IIncOperator) instr2;
                isPost = true;
            } else
                return false;

	    if (iinc.getOperatorIndex() == iinc.ADD_OP + iinc.OPASSIGN_OP)
                op = Operator.INC_OP;
            else if (iinc.getOperatorIndex() == iinc.NEG_OP + iinc.OPASSIGN_OP)
                op = Operator.DEC_OP;
            else
                return false;

            if (iinc.getValue().equals("-1"))
		op ^= 1;
            else if (!iinc.getValue().equals("1"))
                return false;

	    if (!iinc.matches(load))
		return false;

	    type = load.getType().intersection(Type.tUInt);
        } catch (NullPointerException ex) {
            return false;
        } catch (ClassCastException ex) {
            return false;
        }
	Operator ppop = new LocalPrePostFixOperator(type, op, iinc, isPost);
        lastBlock.setInstruction(ppop);
        lastBlock.replace(lastBlock.outer, lastBlock);
	return true;
    }

    public boolean createPostInc(FlowBlock flow) {
	StoreInstruction store;
	int op;
	Type type;
        InstructionBlock lastBlock;
        SequentialBlock sequBlock;
        try {
            lastBlock = (InstructionBlock) flow.lastModified;

            Expression storeExpr = lastBlock.getInstruction();
	    store = (StoreInstruction) storeExpr.getOperator();

            sequBlock = (SequentialBlock) lastBlock.outer;
            if (sequBlock.subBlocks[1] != lastBlock)
                return false;

            BinaryOperator binOp;
            InstructionBlock ib;
            if (store.getLValueOperandCount() > 0) {
                ib = (InstructionBlock) sequBlock.subBlocks[0];
                binOp = (BinaryOperator) ib.getInstruction();
                sequBlock = (SequentialBlock) sequBlock.outer;
            } else
                binOp = (BinaryOperator) 
                    ((ComplexExpression) storeExpr).getSubExpressions()[0];

            if (binOp.getOperatorIndex() == store.ADD_OP)
                op = Operator.INC_OP;
            else if (store.getOperatorIndex() == store.NEG_OP)
                op = Operator.DEC_OP;
            else
                return false;
                
            ib = (InstructionBlock) sequBlock.subBlocks[0];

            ConstOperator constOp = (ConstOperator) ib.getInstruction();
            if (!constOp.getValue().equals("1") &&
		!constOp.getValue().equals("-1"))
                return false;
            if (constOp.getValue().equals("-1"))
		op ^= 1;

            sequBlock = (SequentialBlock) sequBlock.outer;
            SpecialBlock dup = (SpecialBlock) sequBlock.subBlocks[0];
            if (dup.type != SpecialBlock.DUP
                || dup.count != store.getLValueType().stackSize()
                || dup.depth != store.getLValueOperandCount())
                return false;

            sequBlock = (SequentialBlock) sequBlock.outer;
            ib = (InstructionBlock) sequBlock.subBlocks[0];
            Operator load = (Operator) ib.getInstruction();
	    if (!store.matches(load))
		return false;

            if (store.getLValueOperandCount() > 0) {
                sequBlock = (SequentialBlock) sequBlock.outer;
                SpecialBlock dup2 = (SpecialBlock) sequBlock.subBlocks[0];
                if (dup2.type != SpecialBlock.DUP
                    || dup2.count != store.getLValueOperandCount() 
                    || dup2.depth != 0)
                    return false;
            }
	    type = load.getType().intersection(store.getLValueType());
        } catch (NullPointerException ex) {
            return false;
        } catch (ClassCastException ex) {
            return false;
        }
	Operator postop = new PrePostFixOperator(type, op, store, true);
        lastBlock.setInstruction(postop);
        lastBlock.replace(sequBlock, lastBlock);
	return true;
    }
}
