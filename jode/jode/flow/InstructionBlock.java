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
import jode.TabbedPrintWriter;
import jode.LocalInfo;
import jode.decompiler.ComplexExpression;
import jode.decompiler.Expression;
import jode.decompiler.LocalStoreOperator;

/**
 * This is the structured block for atomic instructions.
 */
public class InstructionBlock extends InstructionContainer {

    public InstructionBlock(Expression instr) {
        super(instr);
    }

    public InstructionBlock(Expression instr, Jump jump) {
        super(instr, jump);
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
