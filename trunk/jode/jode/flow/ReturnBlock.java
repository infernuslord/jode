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
import jode.TabbedPrintWriter;
import jode.Instruction;

/**
 * This is the structured block for an Return block.
 */
public class ReturnBlock extends InstructionContainer {

    public ReturnBlock() {
        super(null);
    }

    public ReturnBlock(Instruction instr) {
        super(instr);
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
        writer.println("return" + (instr == null ? "" : " " + instr) + ";");
    }
}
