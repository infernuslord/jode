/* 
 * SyntheticAnalyzer (c) 1998 Jochen Hoenicke
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

package jode.decompiler;
import jode.Decompiler;
import jode.flow.*;
import jode.expr.*;
import jode.Type;
import jode.MethodType;

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
	if (!type.isStatic()
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
