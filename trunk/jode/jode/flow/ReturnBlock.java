/* 
 * ReturnBlock  (c) 1998 Jochen Hoenicke
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
import jode.decompiler.TabbedPrintWriter;
import jode.expr.Expression;

/**
 * This is the structured block for a Return block.
 */
public class ReturnBlock extends InstructionContainer {
    /**
     * The loads that are on the stack before instr is executed.
     */
    VariableStack stack;

    public ReturnBlock() {
	super(null);
    }

    public ReturnBlock(Expression instr) {
        super(instr, new Jump(FlowBlock.END_OF_METHOD));
    }

    /**
     * This does take the instr into account and modifies stack
     * accordingly.  It then calls super.mapStackToLocal.
     * @param stack the stack before the instruction is called
     * @return stack the stack afterwards.
     */
    public VariableStack mapStackToLocal(VariableStack stack) {
	VariableStack newStack = stack;
	if (instr != null) {
	    int params = instr.getOperandCount();
	    if (params > 0) {
		this.stack = stack.peek(params);
		newStack = stack.pop(params);
	    }
	}
	return null;
    }

    public void removePush() {
	if (stack != null)
	    instr = stack.mergeIntoExpression(instr, used);
    }

    /**
     * Tells if this block needs braces when used in a if or while block.
     * @return true if this block should be sorrounded by braces.
     */
    public boolean needsBraces() {
        return declare != null && !declare.isEmpty();
    }

    public void dumpInstruction(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
        writer.println("return" + 
                       (instr == null ? "" : " " + instr.simplify()) + ";");
    }
}
