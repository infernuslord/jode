/* 
 * RetInstructionHeader (c) 1998 Jochen Hoenicke
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

package jode;

/**
 * This is an InstructionHeader for an RET (return from JSR) opcode.
 * @author Jochen Hoenicke
 */
public class RetInstructionHeader extends InstructionHeader {
    int dest;
    boolean conditional;

    InstructionHeader destination;
    InstructionHeader[] successors;/*XXX*/

    /**
     * Create an InstructionHeader for a conditional or unconditional
     * Ret.  
     * @param addr   The address of this instruction.
     * @param length The length of this instruction.
     * @param instr  The underlying Instruction of int type (ret addr).
     */
    public RetInstructionHeader(int addr, int length, Instruction instr) {
	super(RET, addr, addr+length, instr, new int[0]);
    }

    /**
     * Get the successors of this instructions.  This function mustn't
     * be called before resolveSuccessors is executed for this
     * InstructionHeaders.  
     * @return Array of successors.  
     */
    public InstructionHeader[] getSuccessors() {
	/* XXX */
	return successors;
    }

    /**
     * Resolve the successors and predecessors and build a doubly
     * linked list.  
     * @param instHeaders an array of the InstructionHeaders, indexed
     * by addresses.
     */
    public void resolveSuccessors(InstructionHeader[] instHeaders) {
	nextInstruction = instHeaders[nextAddr];
    }
}
