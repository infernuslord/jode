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
import jode.expr.*;
import jode.Type;

public class CreatePrePostIncExpression {

    public static boolean transform(InstructionContainer ic, 
                                    StructuredBlock last)
    {
        return (createLocalPrePostInc(ic, last) || createPostInc(ic, last));
    }
    
    public static boolean createLocalPrePostInc(InstructionContainer ic, 
                                                StructuredBlock last) {

        if (last.outer instanceof SequentialBlock 
            && last.outer.getSubBlocks()[0] instanceof InstructionBlock) {

            Expression instr1 = ((InstructionBlock)
                                 last.outer.getSubBlocks()[0])
                .getInstruction();
            Expression instr2 = ic.getInstruction();

            IIncOperator iinc;
            LocalLoadOperator load;
            boolean isPost;
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

            int op;
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

	    Type type = load.getType().intersection(Type.tUInt);
            Operator ppop = 
                new LocalPrePostFixOperator(type, op, iinc, isPost);

            ic.setInstruction(ppop);
            ic.moveDefinitions(last.outer, last);
            last.replace(last.outer);
            return true;
        }
	return false;
    }

    public static boolean createPostInc(InstructionContainer ic, 
                                        StructuredBlock last) {

        /* Situation:
         *
         *   PUSH load/storeOps              PUSH load/storeOps
         *   DUP  load/storeOps              PUSH store++/--
         *   load  (unresolved)
         *   DUP                       ->
         *   PUSH +/-1
         *   IADD/SUB
         *   store (unresolved)
         *
         *   load (no params)          ->    PUSH store++/--
         *   DUP
         *   PUSH +/-1
         *   store IADD/SUB
         */

        if (!(ic.getInstruction().getOperator() instanceof StoreInstruction)
            || !(ic.getInstruction().isVoid()))
            return false;
        
        StoreInstruction store = 
            (StoreInstruction) ic.getInstruction().getOperator();


        if (!(last.outer instanceof SequentialBlock))
            return false;
        SequentialBlock sb = (SequentialBlock)last.outer;

        Expression binOp;
        if (store.getLValueOperandCount() == 0) {
            if (!(ic.getInstruction() instanceof ComplexExpression))
                return false;
            binOp = ((ComplexExpression) ic.getInstruction())
                .getSubExpressions()[0];
        } else {
            if (!(sb.subBlocks[0] instanceof InstructionBlock)
                || !(sb.outer instanceof SequentialBlock))
                return false;
            binOp = ((InstructionBlock) sb.subBlocks[0])
                .getInstruction();
            sb = (SequentialBlock) sb.outer;
        }
        if (!(binOp instanceof BinaryOperator))
            return false;
 
        int op;
        if (binOp.getOperator().getOperatorIndex() == store.ADD_OP)
            op = Operator.INC_OP;
        else if (binOp.getOperator().getOperatorIndex() == store.NEG_OP)
            op = Operator.DEC_OP;
        else
            return false;
                
        if (!(sb.subBlocks[0] instanceof InstructionBlock))
            return false;
        InstructionBlock ib = (InstructionBlock) sb.subBlocks[0];
        if (!(ib.getInstruction() instanceof ConstOperator))
            return false;
        ConstOperator constOp = (ConstOperator) ib.getInstruction();
        if (constOp.getValue().equals("-1"))
            op ^= 1;
        else if (!constOp.getValue().equals("1"))
            return false;

        if (!(sb.outer instanceof SequentialBlock))
            return false;
        sb = (SequentialBlock) sb.outer;
        if (!(sb.subBlocks[0] instanceof SpecialBlock))
            return false;
            
        SpecialBlock dup = (SpecialBlock) sb.subBlocks[0];
        if (dup.type != SpecialBlock.DUP
            || dup.count != store.getLValueType().stackSize()
            || dup.depth != store.getLValueOperandCount())
            return false;

        if (!(sb.outer instanceof SequentialBlock))
            return false;
        sb = (SequentialBlock) sb.outer;
        if (!(sb.subBlocks[0] instanceof InstructionBlock))
            return false;
        ib = (InstructionBlock) sb.subBlocks[0];

        if (!(ib.getInstruction() instanceof Operator)
            || !store.matches((Operator) ib.getInstruction()))
            return false;

        if (store.getLValueOperandCount() > 0) {
            if (!(sb.outer instanceof SequentialBlock))
                return false;
            sb = (SequentialBlock) sb.outer;
            if (!(sb.subBlocks[0] instanceof SpecialBlock))
                return false;
            SpecialBlock dup2 = (SpecialBlock) sb.subBlocks[0];
            if (dup2.type != SpecialBlock.DUP
                || dup2.count != store.getLValueOperandCount() 
                || dup2.depth != 0)
                return false;
        }
        ic.setInstruction
            (new PrePostFixOperator(store.getLValueType(), op, store, true));
        ic.moveDefinitions(sb, last);
        last.replace(sb);
	return true;
    }
}
