/* 
 * DoWhileInstructionHeader (c) 1998 Jochen Hoenicke
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
public class DoWhileInstructionHeader extends InstructionHeader {

    InstructionHeader endHeader;

    /**
     * Creates a new while statement.
     * @param head   the first instruction of this do-while loop
     * @param end    the last instruction which contains the 
     *                    if-goto (or goto) statement.
     */
    public DoWhileInstructionHeader(InstructionHeader head,
                                    InstructionHeader end) {

        super(DOWHILESTATEMENT, 
              head.addr, end.nextAddr,
              new InstructionHeader[1], end.outer);

        this.endHeader = end;

        if (end.flowType == GOTO) {
            /* This is a for(;;) loop
             */
            this.instr = null;
            end.successors[0].predecessors.removeElement(end);
        } else {
            this.instr = end.instr;
            end.successors[0].predecessors.removeElement(end);
            end.successors[1].predecessors.removeElement(end);
        }
        this.addPredecessors(head);
        this.successors[0] = head;
        head.predecessors.addElement(this);

        this.prevInstruction = head.prevInstruction;
        if (prevInstruction != null)
            prevInstruction.nextInstruction = this;

        this.nextInstruction = end.nextInstruction;
        if (nextInstruction != null)
            nextInstruction.prevInstruction = this;

        if (successors[0] != this) {
            successors[0].prevInstruction = null;
            for (InstructionHeader ih = successors[0]; ih != null;
                 ih = ih.nextInstruction) {
                if (ih.outer == outer)
                    ih.outer = this;
                if (ih.nextInstruction == end)
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
        return getContinue();
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

        boolean braces = (successors[0].flowType != NORMAL || 
                          successors[0].nextInstruction != null);

        writer.println((instr == null ? "for(;;)" : "do")+
                       (braces ? " {": ""));

        writer.tab();
        if (successors[0] != this) {
            for (InstructionHeader ih = successors[0]; ih != null; 
                 ih = ih.nextInstruction)
                ih.dumpSource(writer);
        } else
            writer.println("/* empty */");
        writer.untab();

        if (instr != null)
            writer.println((braces ? "} " : "") + "while ("+instr+");");
        else if (braces)
            writer.println("}");
        
	if (Decompiler.isDebugging)
	    writer.untab();
    }

    /**
     * Returns the InstructionHeader where a break of this instruction
     * would jump to. Does only make sense for do/while/for-loops and
     * switch instructions.
     */
    public InstructionHeader getBreak() {
        return super.getEndBlock();
    }

    public InstructionHeader getShadow() {
        return successors[0];
    }

    /**
     * Returns the InstructionHeader where a continue of this instruction
     * would jump to. Does only make sense for do/while/for-loops.
     */
    public InstructionHeader getContinue() {
        return (instr == null)? this : endHeader;
    }

    public InstructionHeader doTransformations(Transformation[] trafo) {
        InstructionHeader next;
        if (successors[0] != this)
            for (InstructionHeader ih = successors[0]; ih != null; ih = next) {
                if ((next = ih.doTransformations(trafo)) == null)
                    next = ih.getNextInstruction();
            }
        return super.doTransformations(trafo);
    }
}

