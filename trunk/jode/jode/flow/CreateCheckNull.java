/* 
 * CreateCheckNull (c) 1998 Jochen Hoenicke
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
import jode.decompiler.*;
import jode.Type;

public class CreateCheckNull {

    /* Situation:
     * 
     * javac: 
     *  DUP
     *  stack_0.getClass();
     *
     * jikes:
     *  DUP
     *  if (!stack_0 != null)
     *    throw null;
     */

    public static boolean transformJavac(InstructionContainer ic,
					 StructuredBlock last) {
        if (!(last.outer instanceof SequentialBlock)
	    || !(ic.getInstruction() instanceof ComplexExpression)
	    || !(last.outer.getSubBlocks()[0] instanceof SpecialBlock))
            return false;

	SpecialBlock dup = (SpecialBlock) last.outer.getSubBlocks()[0];
	if (dup.type != SpecialBlock.DUP
	    || dup.count != 1 || dup.depth != 0)
	    return false;
	   
	ComplexExpression ce = (ComplexExpression) ic.getInstruction();

	if (!(ce.getOperator() instanceof PopOperator)
	    || !(ce.getSubExpressions()[0] instanceof InvokeOperator))
	    return false;

        InvokeOperator getClassCall
	    = (InvokeOperator) ce.getSubExpressions()[0];
	if (!getClassCall.getMethodName().equals("getClass")
	    || !(getClassCall.getMethodType().toString()
		 .equals("()Ljava/lang/Class;")))
	    return false;

	ic.setInstruction(new CheckNullOperator(Type.tUObject));
	last.replace(last.outer);
        return true;
    }

    public static boolean transformJikes(IfThenElseBlock ifBlock,
					 StructuredBlock last) {
        if (!(last.outer instanceof SequentialBlock)
	    || !(last.outer.getSubBlocks()[0] instanceof SpecialBlock)
	    || ifBlock.elseBlock != null
	    || !(ifBlock.thenBlock instanceof ThrowBlock))
            return false;

	SpecialBlock dup = (SpecialBlock) last.outer.getSubBlocks()[0];
	if (dup.type != SpecialBlock.DUP
	    || dup.count != 1 || dup.depth != 0)
	    return false;
	   
	/* negate the instruction back to its original state */
	Expression expr = ifBlock.cond.negate();
	if (!(expr instanceof CompareUnaryOperator)
	    || expr.getOperator().getOperatorIndex() != Operator.NOTEQUALS_OP
	    || !(expr.getOperator().getOperandType(0).isOfType(Type.tUObject)))
	    return false;

	InstructionContainer ic = 
	    new InstructionBlock(new CheckNullOperator(Type.tUObject));
	ifBlock.flowBlock.removeSuccessor(ifBlock.thenBlock.jump);
	ic.moveJump(ifBlock.jump);
	if (last == ifBlock) {
	    ic.replace(last.outer);
	    last = ic;
	} else {
	    ic.replace(ifBlock);
	    last.replace(last.outer);
	}
        return true;
    }
}
