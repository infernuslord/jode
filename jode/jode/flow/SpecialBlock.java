/* SpecialBlock (c) 1998 Jochen Hoenicke
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
import jode.TabbedPrintWriter;
import jode.Decompiler;
import jode.expr.*;

/**
 * This is the structured block for atomic instructions.
 */
public class SpecialBlock extends StructuredBlock {

    public static int DUP  = 0;
    public static int SWAP = 1;
    public static int POP  = 2;
    private static String[] output = { "DUP", "SWAP", "POP" };

    /**
     * The type, one of DUP or SWAP
     */
    int type;
    /**
     * The count of stack entries that are transformed. 
     * This is 1 for swap, and 1 or 2 for dup.
     */
    int count;
    /**
     * The depth that the dupped element should be put to (0,1 or 2).
     * For swap this is zero.
     */
    int depth;

    public SpecialBlock(int type, int count, int depth, Jump jump) {
        this.type = type;
        this.count = count;
        this.depth = depth;
        setJump(jump);
    }

    public void dumpInstruction(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
        writer.println(output[type] 
                       + ((count == 1) ? "" : "2")
                       + ((depth == 0) ? "" : "_X"+depth));
    }

    public boolean doTransformations() {
        return (type == SWAP && removeSwap(flowBlock.lastModified))
	    || (type == POP && removePop(flowBlock.lastModified));
    }

    public boolean removeSwap(StructuredBlock last) {

        /* Remove non needed swaps; convert:
         *
         *   PUSH expr1
         *   PUSH expr2
         *   SWAP
         *
         * to:
         *
         *   PUSH expr2
         *   PUSH expr1
         */
        if (last.outer instanceof SequentialBlock
            && last.outer.outer instanceof SequentialBlock
            && last.outer.getSubBlocks()[0] instanceof InstructionBlock
            && last.outer.outer.getSubBlocks()[0] 
            instanceof InstructionBlock) {

            InstructionBlock block1 
                = (InstructionBlock) last.outer.outer.getSubBlocks()[0];
            InstructionBlock block2
                = (InstructionBlock) last.outer.getSubBlocks()[0];

	    Expression expr1 = block1.getInstruction();
	    Expression expr2 = block2.getInstruction();

            if (expr1.isVoid() || expr2.isVoid()
		|| expr1.getOperandCount() != 0
		|| expr2.getOperandCount() != 0
		|| expr1.hasSideEffects(expr2) 
		|| expr2.hasSideEffects(expr1))
                return false;

            /* PUSH expr1 == block1
             * PUSH expr2
             * SWAP
             * ...
             */
            last.outer.replace(block1.outer);
            /* PUSH expr2
             * SWAP
             * ...
             */
            block1.replace(this);
            block1.moveJump(jump);
            /* PUSH expr2
             * PUSH expr1
             */
            block1.flowBlock.lastModified = block1;
            return true;
        }
        return false;
    }

    public boolean removePop(StructuredBlock last) {

	/* There are three possibilities:
         *
         *   PUSH method_invocation()
         *   POP[sizeof PUSH]
         * to:
         *   method_invocation()
	 *
	 *   PUSH arg1
	 *   PUSH arg2
	 *   POP2
	 * to:
	 *   if (arg1 == arg2)
	 *     empty
	 *
	 *   PUSH arg1
	 *   POP
	 * to:
	 *   if (arg1 != 0)
	 *     empty 
         */

        if (last.outer instanceof SequentialBlock
	    && last.outer.getSubBlocks()[0] instanceof InstructionBlock) {

	    if (jump != null && jump.destination == null)
		return false;

            InstructionBlock prev
                = (InstructionBlock) last.outer.getSubBlocks()[0];
	    Expression instr = prev.getInstruction();

	    if (instr.getType().stackSize() == count) {
		StructuredBlock newBlock;
		if (instr.getOperator() instanceof InvokeOperator
		    || instr.getOperator() instanceof ConstructorOperator) {
		    ComplexExpression newExpr = new ComplexExpression
			(new PopOperator(instr.getType()),
			 new Expression[] { instr });
		    prev.setInstruction(newExpr);
		    newBlock = prev;
		} else {
		    ComplexExpression newCond = new ComplexExpression
			(new CompareUnaryOperator(instr.getType(), 
						  Operator.NOTEQUALS_OP), 
			 new Expression[] { instr });
		    IfThenElseBlock newIfThen = new IfThenElseBlock(newCond);
		    newIfThen.setThenBlock(new EmptyBlock());
		    newBlock = newIfThen;
		}
		newBlock.moveDefinitions(last.outer, newBlock);
		newBlock.moveJump(jump);
		if (this == last) {
		    newBlock.replace(last.outer);
		    flowBlock.lastModified = newBlock;
		} else {
		    newBlock.replace(this);
		    last.replace(last.outer);
		}
		return true;
	    } else if (last.outer.outer instanceof SequentialBlock
		       && (last.outer.outer.getSubBlocks()[0] 
			   instanceof InstructionBlock)) {
		InstructionBlock prevprev
		    = (InstructionBlock) last.outer.outer.getSubBlocks()[0];
		Expression previnstr = prevprev.getInstruction();
		if (previnstr.getType().stackSize() == 1 
		    && instr.getType().stackSize() == 1
		    && count == 2) {
		    /* compare two objects */

		    ComplexExpression newCond = new ComplexExpression
			(new CompareBinaryOperator(instr.getType(), 
						   Operator.EQUALS_OP), 
			 new Expression[] { previnstr, instr });
		    IfThenElseBlock newIfThen = new IfThenElseBlock(newCond);
		    newIfThen.setThenBlock(new EmptyBlock());
		    newIfThen.moveJump(jump);
		    if (this == last) {
			newIfThen.replace(last.outer.outer);
			flowBlock.lastModified = newIfThen;
		    } else {
			newIfThen.replace(this);
			last.replace(last.outer.outer);
		    }
		    return true;
		}
	    }
        }
        return false;
    }
}




