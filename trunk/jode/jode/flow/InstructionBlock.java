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
import jode.*;

/**
 * This is the structured block for atomic instructions.
 */
public class InstructionBlock extends StructuredBlock 
    implements InstructionContainer {
    Instruction instr;

    public InstructionBlock(Instruction instr) {
        this.instr = instr;
        if (instr instanceof LocalVarOperator) {
            LocalVarOperator varOp = (LocalVarOperator) instr;
            if (varOp.isRead()) {
                in.addElement(varOp.getLocalInfo());
            }
            out.addElement(varOp.getLocalInfo());
        }
    }

    public InstructionBlock(Instruction instr, Jump jump) {
        this(instr);
        setJump(jump);
    }

    /**
     * Get the underlying instruction.
     * @return the underlying instruction.
     */
    public Instruction getInstruction() {
        return instr;
    }

    /**
     * Change the underlying instruction.
     * @param instr the new underlying instruction.
     */
    public void setInstruction(Instruction instr) {
        this.instr = instr;
    }

    public void dumpInstruction(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
        if (instr.getType() != MyType.tVoid)
            writer.print("push ");
        writer.println(instr.toString()+";");
    }
}
