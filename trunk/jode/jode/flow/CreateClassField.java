/* CreateClassField Copyright (C) 1999 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id$
 */

package jode.flow;
import jode.expr.*;
import jode.type.Type;
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
