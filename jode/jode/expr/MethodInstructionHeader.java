/* 
 * MethodInstructionHeader (c) 1998 Jochen Hoenicke
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
import java.util.Vector;
import sun.tools.java.BinaryExceptionHandler;
import sun.tools.java.Type;

/**
 * This class is the end point of the InstructionHeader list.
 * @author Jochen Hoenicke
 */
public class MethodInstructionHeader extends InstructionHeader {

    /**
     * Create a new InstructionHeader.
     * @param addr   The address of this Instruction.
     * @param length The length of this Instruction.
     * @param instr  The underlying Instruction.
     */
    public MethodInstructionHeader(JodeEnvironment env,
                                   InstructionHeader[] instr,
                                   BinaryExceptionHandler[] handlers) {
        super(METHOD, 0, instr.length, new InstructionHeader[1], null);
        successors[0] = instr[0];
        instr[0].predecessors.addElement(this);
        nextInstruction  = new InstructionHeader(EMPTY, instr.length, null);
        nextInstruction.prevInstruction = this;

	for (int addr = 0; addr < instr.length; addr = instr[addr].nextAddr) {

            instr[addr].outer = this;
	    instr[addr].resolveSuccessors(instr);

	    if (instr[addr].flowType == RETURN) {
                InstructionHeader[] retSuccs = { nextInstruction };
                instr[addr].successors = retSuccs;
		nextInstruction.predecessors.addElement(instr[addr]);
                if (instr[addr].instr == null) {
                    /* if this is a void return, replace it by a
                     * goto to the nextInstruction.
                     */
                    instr[addr].flowType = GOTO;
                }
            }
	}
        for (int i=0; i<handlers.length; i++) {
            InstructionHeader tryIH   = instr[handlers[i].startPC];
            if (tryIH.flowType != TRY)
                instr[handlers[i].startPC] = tryIH = 
                    new TryInstructionHeader(tryIH, this);

            Type type = 
                (handlers[i].exceptionClass != null)? 
                handlers[i].exceptionClass.getType() : MyType.tVoid;
            instr[handlers[i].handlerPC] = 
                new CatchInstructionHeader
                (type, env.getTypeString(type),
                 instr[handlers[i].handlerPC], this);

            InstructionHeader endIH   = instr[handlers[i].endPC];
            InstructionHeader catchIH = instr[handlers[i].handlerPC];
            ((TryInstructionHeader)tryIH).addHandler(endIH, catchIH);
        }
    }

    public InstructionHeader getFirst() {
        return successors[0];
    }

    public Vector getReturns() {
        return nextInstruction.predecessors;
    }

    public void dumpSource(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
	for (InstructionHeader ih = successors[0]; ih != null; 
             ih = ih.nextInstruction)
	    ih.dumpSource(writer);

        // dump the last label if it was used.
        nextInstruction.dumpSource(writer);
    }

    public InstructionHeader doTransformations(Transformation[] trafo) {
        InstructionHeader next;
        for (InstructionHeader ih = successors[0]; ih != null; ih = next) {
            if ((next = ih.doTransformations(trafo)) == null)
                next = ih.getNextInstruction();
        }
        return null;
    }
}
