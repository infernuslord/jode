/* 
 * CreateClassField (c) 1998 Jochen Hoenicke
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
import jode.decompiler.LocalInfo;

public class CreateClassField {


    public static boolean transform(IfThenElseBlock ifBlock,
				    StructuredBlock last) {
	// convert
	//   if (class$classname == null)
	//       class$classname = class$("java.lang.Object");
	// to
	//   if (classname.class == null) {
	//   }
        if (!(ifBlock.cond instanceof ComplexExpression)
	    || !(ifBlock.cond.getOperator() instanceof CompareUnaryOperator)
	    || !(ifBlock.cond.getOperator().getOperatorIndex()
		 == Operator.EQUALS_OP)
	    || !(ifBlock.thenBlock instanceof InstructionBlock)
	    || ifBlock.elseBlock != null)
            return false;

	if (ifBlock.thenBlock.jump != null 
	    && (ifBlock.jump == null
		|| (ifBlock.jump.destination 
		    != ifBlock.thenBlock.jump.destination)))
	    return false;

	ComplexExpression cmp = (ComplexExpression) ifBlock.cond;
	Expression instr = 
	    ((InstructionBlock)ifBlock.thenBlock).getInstruction();
	if (!(cmp.getSubExpressions()[0] instanceof GetFieldOperator)
	    || !(instr instanceof ComplexExpression)
	    || !(instr.getOperator() instanceof PutFieldOperator))
	    return false;

	ComplexExpression ass = (ComplexExpression) instr;
	PutFieldOperator put = (PutFieldOperator) ass.getOperator();
	if (!put.getField().isSynthetic()
	    || !put.matches((GetFieldOperator)cmp.getSubExpressions()[0])
	    || !(ass.getSubExpressions()[0] instanceof ComplexExpression)
	    || !(ass.getSubExpressions()[0].getOperator() 
		 instanceof InvokeOperator))
	    return false;

	InvokeOperator invoke = (InvokeOperator) 
	    ass.getSubExpressions()[0].getOperator();
	Expression param = 
	    ((ComplexExpression)ass.getSubExpressions()[0])
	    .getSubExpressions()[0];
	if (invoke.isGetClass()
	    && param instanceof ConstOperator
	    && param.getType().equals(Type.tString)) {
	    String clazz = ((ConstOperator)param).getValue();
	    if (put.getFieldName()
		.equals("class$" + clazz.replace('.', '$'))
		|| put.getFieldName()
		.equals("class$L" + clazz.replace('.', '$'))) {
		cmp.setSubExpressions
		    (0, new ClassFieldOperator(Type.tClass(clazz)));
		put.getField().analyzedSynthetic();
		EmptyBlock empty = new EmptyBlock();
		empty.moveJump(ifBlock.thenBlock.jump);
		ifBlock.setThenBlock(empty);
		return true;
	    }
	}
	return false;
    }
}
