/* SyntheticAnalyzer Copyright (C) 1999 Jochen Hoenicke.
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

package jode.decompiler;
import jode.Decompiler;
import jode.flow.*;
import jode.expr.*;
import jode.type.Type;
import jode.type.MethodType;

public class SyntheticAnalyzer {
    public final static int UNKNOWN = 0;
    public final static int GETCLASS = 1;
    public final static int GETFIELD = 2;
    public final static int PUTFIELD = 3;
    
    int type = UNKNOWN;
    MethodAnalyzer method;

    public SyntheticAnalyzer(MethodAnalyzer method) {
	this.method = method;
	if (method.getName().equals("class$"))
	    if (!checkGetClass() && Decompiler.isVerbose)
		Decompiler.err.println("class$ seems to be wrong");
    }

    boolean checkGetClass() {
	MethodType type = method.getType();
	if (!method.isStatic()
	    || !type.getReturnType().isOfType(Type.tJavaLangClass)
	    || type.getParameterTypes().length != 1
	    || !type.getParameterTypes()[0].isOfType(Type.tString))
	    return false;
	
	FlowBlock flow = method.getMethodHeader();
	if (!flow.hasNoJumps())
	    return false;
	StructuredBlock tryblock = flow.getBlock();
	if (!(tryblock instanceof TryBlock))
	    return false;
	StructuredBlock[] subBlocks = tryblock.getSubBlocks();
	if (subBlocks.length != 2
	    || !(subBlocks[0] instanceof ReturnBlock)
	    || !(subBlocks[1] instanceof CatchBlock))
	    return false;

	// Now check the return Block, it should be
	// return Class.forName(local_0);
	ReturnBlock ret = (ReturnBlock) subBlocks[0];
	Expression retExpr = ret.getInstruction();
	if (!(retExpr instanceof ComplexExpression)
	    || !(retExpr.getOperator() instanceof InvokeOperator))
	    return false;
	InvokeOperator invoke = (InvokeOperator) retExpr.getOperator();
	if (!invoke.isStatic()
	    || !invoke.getClassType().equals(Type.tJavaLangClass)
	    || !(invoke.getMethodType().getReturnType()
		 .equals(Type.tJavaLangClass))
	    || invoke.getMethodType().getParameterTypes().length != 1
	    || !(invoke.getMethodType().getParameterTypes()[0]
		 .equals(Type.tString))
	    || !invoke.getMethodName().equals("forName"))
	    return false;

	Expression[] subExpr = 
	    ((ComplexExpression) retExpr).getSubExpressions();
	if (!(subExpr[0] instanceof LocalLoadOperator)
	    || ((LocalLoadOperator) subExpr[0]).getLocalInfo().getSlot() != 0)
	    return false;

	// Now check the CatchBlock it should contain (we don't check all):
	// throw new NoClassDefFoundError(exception.getMessage());
	CatchBlock catchBlock = (CatchBlock) subBlocks[1];
	StructuredBlock subBlock = catchBlock.getSubBlocks()[0];
	if (!(subBlock instanceof ThrowBlock)
	    || !(catchBlock.getExceptionType().equals
		 (Type.tClass("java.lang.ClassNotFoundException"))))
	    return false;
	this.type = GETCLASS;
	return true;
    }
}
