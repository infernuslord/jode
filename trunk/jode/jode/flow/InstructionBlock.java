/* InstructionBlock (c) 1998 Jochen Hoenicke
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
import jode.Type;
import jode.decompiler.TabbedPrintWriter;
import jode.decompiler.LocalInfo;
import jode.expr.ComplexExpression;
import jode.expr.Expression;
import jode.expr.LocalStoreOperator;

/**
 * This is the structured block for atomic instructions.
 */
public class InstructionBlock extends InstructionContainer {
    /**
     * The loads that are on the stack before cond is executed.
     */
    VariableStack stack;
    /**
     * The local to which we push to, if the instruction is non void
     */
    LocalInfo pushedLocal = null;

    public InstructionBlock(Expression instr) {
        super(instr);
    }

    public InstructionBlock(Expression instr, Jump jump) {
        super(instr, jump);
    }

    /**
     * This does take the instr into account and modifies stack
     * accordingly.  It then calls super.mapStackToLocal.
     * @param stack the stack before the instruction is called
     * @return stack the stack afterwards.
     */
    public VariableStack mapStackToLocal(VariableStack stack) {
	VariableStack newStack;
	int params = instr.getOperandCount();
	if (params > 0)
	    this.stack = stack.peek(params);

	if (instr.getType() != Type.tVoid) {
	    pushedLocal = new LocalInfo();
	    newStack = stack.poppush(params, pushedLocal);
	} else if (params > 0) {
	    newStack = stack.pop(params);
	} else
	    newStack = stack;
	return super.mapStackToLocal(newStack);
    }

    public void removePush() {
	if (stack != null)
	    instr = stack.mergeIntoExpression(instr, used);
	if (pushedLocal != null) {
	    LocalStoreOperator store = new LocalStoreOperator
		(pushedLocal.getType(), pushedLocal, 
		 LocalStoreOperator.ASSIGN_OP);
	    instr = new ComplexExpression(store, new Expression[] { instr });
	    used.addElement(pushedLocal);
	}
	super.removePush();
    }

    /**
     * Tells if this block needs braces when used in a if or while block.
     * @return true if this block should be sorrounded by braces.
     */
    public boolean needsBraces() {
        return declare != null && !declare.isEmpty();
    }

    /**
     * True if this is a declaration.
     */
    private boolean isDeclaration = false;

    public void dumpDeclaration(TabbedPrintWriter writer, LocalInfo local)
	throws java.io.IOException
    {
        if (instr instanceof ComplexExpression
            && instr.getOperator() instanceof LocalStoreOperator
            && ((LocalStoreOperator) instr.getOperator()).getLocalInfo() 
            == local.getLocalInfo()) {
            isDeclaration = true;
        } else
            super.dumpDeclaration(writer, local);
    }

    public void dumpSource(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
        isDeclaration = false;
        super.dumpSource(writer);
    }

    public void dumpInstruction(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
        if (isDeclaration) {
            LocalInfo local = ((LocalStoreOperator) instr.getOperator())
                .getLocalInfo();
            Expression expr = 
                ((ComplexExpression) instr).getSubExpressions()[0];
            expr.makeInitializer();
            writer.println(local.getType() + " " + local.getName() + " = "
                           + expr.simplify().toString() + ";");
        } else {
            if (instr.getType() != jode.Type.tVoid)
                writer.print("PUSH ");
            writer.println(instr.simplify().toString()+";");
        }
    }
}
