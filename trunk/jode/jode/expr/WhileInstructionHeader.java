/* 
 * WhileInstructionHeader (c) 1998 Jochen Hoenicke
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
import java.util.Enumeration;


/**
 * This instruction header represents an if instruction.  The
 * linkage of the instructions is as follow:
 * <pre>
 *  A: ....
 * <p>
 *     prev = A, next = H or null, pred = normal, succ = {H,C}
 *  B: while ( instr ) {
 * <p>
 *          prev = null, next = D, pred = {B}, succ = {D}
 *       C: first block-instr
 * <p>
 *          prev = C, next = E, pred = normal succ = normal
 *       D: ...
 * <p>
 *          prev = D, next = null, pred = normal succ = {B}
 *       E: last block-instr
 *     }
 * <p>
 *     prev = B, ..., pred = (normal+{G}) \ {C..F}, succ = normal
 *  H: ...
 * </pre>
 */
public class WhileInstructionHeader extends InstructionHeader {
    /**
     * Creates a new while statement.
     * @param gotoHeader  the goto instruction in front of this while loop.
     * @param ifHeader    the instruction header which contains the 
     *                    if-goto statement.
     */
    public WhileInstructionHeader(InstructionHeader gotoHeader,
                                  InstructionHeader ifHeader) {

        super(WHILESTATEMENT, 
              gotoHeader.addr, ifHeader.nextAddr,
              ifHeader.successors, ifHeader.outer);

        this.instr = ifHeader.instr;

        this.outer = ifHeader.outer;

        this.addPredecessors(ifHeader);
        for (int i=0; i < successors.length; i++) {
            successors[i].predecessors.removeElement(ifHeader);
            successors[i].predecessors.addElement(this);
        }
        if (successors[0].flowType == GOTO) {
            successors[0].predecessors.removeElement(this);
            successors[0] = successors[0].successors[0];
            successors[0].predecessors.addElement(this);
        }


        if (gotoHeader != ifHeader) {
            this.addPredecessors(gotoHeader);
            gotoHeader.successors[0].predecessors.removeElement(gotoHeader);
        }

        this.prevInstruction = gotoHeader.prevInstruction;
        if (prevInstruction != null)
            prevInstruction.nextInstruction = this;

        this.nextInstruction = ifHeader.nextInstruction;
        if (nextInstruction != null)
            nextInstruction.prevInstruction = this;

        if (successors[1] != this) {
            successors[1].prevInstruction = null;
            for (InstructionHeader ih = successors[1]; ih != null;
                 ih = ih.nextInstruction) {
                if (ih.outer == outer)
                    ih.outer = this;
                if (ih.nextInstruction == ifHeader)
                    ih.nextInstruction = null;
            }
        }
    }

    /** 
     * This should be implemented for those blocks, that is headers
     * which are outer of other headers.  This gives the instruction
     * where the control flows after this block.
     * @return the first instruction after this block.  
     */
    InstructionHeader getEndBlock() {
        return this;
    }

    public void dumpSource(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
	if (Decompiler.isDebugging) {
            dumpDebugging(writer);
	    writer.tab();
	}
        
	if (needsLabel()) {
            writer.untab();
	    writer.println(getLabel()+": ");
            writer.tab();
        }

        boolean braces = (successors[1].flowType != NORMAL || 
                          successors[1].nextInstruction != null);
        writer.println("while (" + instr.toString() + ")" + 
                       (braces ? " {": ""));

        writer.tab();     
        if (successors[1] != this) {
            for (InstructionHeader ih = successors[1]; ih != null; 
                 ih = ih.nextInstruction)
                ih.dumpSource(writer);
        } else
            writer.println("/* empty */");
        writer.untab();

        if (braces)
            writer.println("} ");
        
	if (Decompiler.isDebugging)
	    writer.untab();
    }

    /**
     * Returns the InstructionHeader where a break of this instruction
     * would jump to. Does only make sense for do/while/for-loops and
     * switch instructions.
     */
    public InstructionHeader getBreak() {
        return successors[0];
    }

    /**
     * Returns the InstructionHeader where a continue of this instruction
     * would jump to. Does only make sense for do/while/for-loops.
     */
    public InstructionHeader getContinue() {
        return this;
    }

    public InstructionHeader doTransformations(Transformation[] trafo) {
        InstructionHeader next;
        if (successors[1] != this)
            for (InstructionHeader ih = successors[1]; ih != null; ih = next) {
                if ((next = ih.doTransformations(trafo)) == null)
                    next = ih.getNextInstruction();
            }
        return super.doTransformations(trafo);
    }
}

