/* 
 * CreatePostIncExpression (c) 1998 Jochen Hoenicke
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
import sun.tools.java.Type;
import jode.*;

public class CreatePostIncExpression implements Transformation {

    public boolean transform(FlowBlock flow)
    {
        return (createLocalPostInc(flow) || createPostInc(flow));
    }
    
    public boolean createLocalPostInc(FlowBlock flow) {
        IIncOperator iinc;
        int op;
        InstructionContainer lastBlock;
        Type type;
        try {
            lastBlock = (InstructionContainer) flow.lastModified;
	    Expression iincExpr = (Expression) lastBlock.getInstruction();
	    iinc = (IIncOperator) iincExpr.getOperator();
	    if (iinc.getOperator() == iinc.ADD_OP + iinc.OPASSIGN_OP)
                op = Operator.INC_OP;
            else if (iinc.getOperator() == iinc.NEG_OP + iinc.OPASSIGN_OP)
                op = Operator.DEC_OP;
            else
                return false;
            if (!iinc.getValue().equals("1") &&
		!iinc.getValue().equals("-1"))
                return false;
            if (iinc.getValue().equals("-1"))
		op ^= 1;

            SequentialBlock sequBlock = (SequentialBlock)lastBlock.outer;
            if (sequBlock.subBlocks[1] != lastBlock)
                return false;

            InstructionBlock ib = (InstructionBlock) sequBlock.subBlocks[0];
	    Expression loadExpr = (Expression) ib.getInstruction();
	    LocalLoadOperator load = 
		(LocalLoadOperator)loadExpr.getOperator();
	    if (!iinc.matches(load))
		return false;

	    type = MyType.intersection(load.getType(), MyType.tUInt);
        } catch (NullPointerException ex) {
            return false;
        } catch (ClassCastException ex) {
            return false;
        }
	Operator postop = new LocalPostFixOperator(type, op, iinc);
        lastBlock.setInstruction(postop);
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
	    store = (StoreInstruction) lastBlock.getInstruction();

            sequBlock = (SequentialBlock) lastBlock.outer;
            if (sequBlock.subBlocks[1] != lastBlock)
                return false;

            InstructionBlock ib = (InstructionBlock) sequBlock.subBlocks[0];
            BinaryOperator binOp = (BinaryOperator) ib.getInstruction();
            if (binOp.getOperator() == store.ADD_OP)
                op = Operator.INC_OP;
            else if (store.getOperator() == store.NEG_OP)
                op = Operator.DEC_OP;
            else
                return false;

            sequBlock = (SequentialBlock) sequBlock.outer;
            ib = (InstructionBlock) sequBlock.subBlocks[0];

            Expression expr = (Expression) ib.getInstruction();
            ConstOperator constOp = (ConstOperator) expr.getOperator();
            if (!constOp.getValue().equals("1") &&
		!constOp.getValue().equals("-1"))
                return false;
            if (constOp.getValue().equals("-1"))
		op ^= 1;

            sequBlock = (SequentialBlock) sequBlock.outer;
            ib = (InstructionBlock) sequBlock.subBlocks[0];

            DupOperator dup = (DupOperator) ib.getInstruction();
            if (dup.getCount() != store.getLValueType().stackSize() ||
                dup.getDepth() != store.getLValueOperandCount())
                return false;

            sequBlock = (SequentialBlock) sequBlock.outer;
            ib = (InstructionBlock) sequBlock.subBlocks[0];

            Instruction instr = ib.getInstruction();
            if (instr instanceof Expression
                && ((Expression)instr).getSubExpressions().length == 0)
                instr = ((Expression)instr).getOperator();

            Operator load = (Operator) instr;
	    if (!store.matches(load))
		return false;

            if (store.getLValueOperandCount() > 0) {
                sequBlock = (SequentialBlock) sequBlock.outer;
                ib = (InstructionBlock) sequBlock.subBlocks[0];
                
                DupOperator dup2 = (DupOperator) ib.getInstruction();
                if (dup2.getCount() != store.getLValueOperandCount() ||
                    dup2.getDepth() != 0)
                    return false;
            }
	    type = MyType.intersection(load.getType(), store.getLValueType());
        } catch (NullPointerException ex) {
            return false;
        } catch (ClassCastException ex) {
            return false;
        }
	Operator postop = new PostFixOperator(type, op, store);
        lastBlock.setInstruction(postop);
        lastBlock.replace(sequBlock, lastBlock);
	return true;
    }
}
