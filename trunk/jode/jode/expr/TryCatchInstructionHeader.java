/* 
 * TryCatchInstructionHeader (c) 1998 Jochen Hoenicke
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

/**
 * This instruction header represents an if instruction.  The
 * linkage of the instructions is as follow:
 * <pre>
 *  A: ....
 * <p>
 *     prev = A, next = H, pred = normal, succ = {C,E}
 *  B: try {
 * <p>
 *          prev = null, next = D, pred = {B}, succ = {D}
 *       C: then-instr 1
 * <p>
 *          prev = C, next = H!, pred = normal succ = normal
 *       D: instructions of then part
 * <p>
 *     } catch (...) {
 * <p>
 *          prev = null, next = D, pred = {B}, succ = {D}
 *       E: else-instr 1
 * <p>
 *          prev = E, next = null, pred = normal, succ = normal
 *       F: instructions of then part
 * <p>
 *     }
 *     prev = B, ..., pred = normal, succ = normal
 *  H: ...
 * </pre>
 *
 * TODO:  Implement finally.
 */
public class TryCatchInstructionHeader extends InstructionHeader {

    /**
     * Creates a try catch statement.  There are several conditions that
     * must be met:
     * <ul>
     * <li><code>
     * </code></li>
     * </ul>
     * @param successors the successors of this try, that is
     *                   the try header and the catch headers.
     * @param endHeader  the end hader of the first try block.
     *    This must be successors[1] or a single goto or return
     *    statement jumping to endBlock.
     * @param remaining  the remaining successors of the old try header.
     * @param endBlock the endBlock of this try statement 
     */
    public TryCatchInstructionHeader(InstructionHeader[] successors,
                                     InstructionHeader endHeader,
                                     InstructionHeader[] remaining,
                                     InstructionHeader endBlock) {

        super(TRYCATCHBLOCK, successors[0].addr, endBlock.addr, 
              successors, successors[0].outer);

        InstructionHeader tryHeader = successors[0];
        this.movePredecessors(tryHeader);
        if (remaining.length > 1) {
            tryHeader.predecessors = new Vector();
            tryHeader.predecessors.addElement(this);
            tryHeader.prevInstruction = null;
            tryHeader.successors = remaining;
        } else {
            successors[0] = tryHeader.successors[0];
            successors[0].predecessors.removeElement(tryHeader);
            successors[0].predecessors.addElement(this);
            if (tryHeader.nextInstruction != null)
                tryHeader.nextInstruction.prevInstruction = null;
        }

        if (endBlock.outer == outer) {
            nextInstruction = endBlock;
            endBlock.prevInstruction = this;
        } else if (endBlock.getShadow() != outer.getEndBlock()) {
                /* Create a goto after this block, that
                 * jumps to endBlock
                 */
                nextInstruction = new InstructionHeader
                    (GOTO, endBlock.addr, endBlock.addr,
                     new InstructionHeader[1], this);
                nextInstruction.instr = new NopOperator(MyType.tVoid);
                nextInstruction.prevInstruction = this;
                nextInstruction.successors[0] = endBlock;
                endBlock.predecessors.addElement(nextInstruction);
        } else
            nextInstruction = null;

        if (endHeader != successors[1])
            endHeader.successors[0].predecessors.removeElement(endHeader);

        for (int i=1; i< successors.length; i++) {
            endHeader.predecessors.removeElement(tryHeader);
            successors[i].predecessors.removeElement(tryHeader);
            successors[i].predecessors.addElement(this);
        }
        
        for (InstructionHeader ih = successors[0]; ih != null; 
             ih = ih.nextInstruction) {
            if (ih.outer == outer)
                ih.outer = this;
            if (ih.nextInstruction == endHeader)
                ih.nextInstruction = null;
        }
        for (int i=1; i< successors.length; i++) {
            for (InstructionHeader ih = successors[i]; ih != null; 
                 ih = ih.nextInstruction) {
                if (ih.outer == outer)
                    ih.outer = this;
                if ((i < successors.length-1 &&
                     ih.nextInstruction == successors[i+1])
                    || ih.nextInstruction == endBlock)

                    ih.nextInstruction = null;
            }
        }
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

        writer.println("try {");
        writer.tab();
        if (successors[0] == getEndBlock())
            writer.print("/* empty?? */");
        else {
            for (InstructionHeader ih = successors[0]; ih != null; 
                 ih = ih.nextInstruction)
                ih.dumpSource(writer);
        }
        writer.untab();

        for (int i=1; i< successors.length; i++) {
            InstructionHeader catchIH = successors[i];
            catchIH.dumpSource(writer);

            writer.tab();
            if (catchIH.nextInstruction == null)
                writer.println("/* empty */");
            else {
                for (InstructionHeader ih = catchIH.nextInstruction;
                     ih != null; ih = ih.nextInstruction)
                    ih.dumpSource(writer);
            }
            writer.untab();
        }
        writer.println("} ");

	if (Decompiler.isDebugging)
	    writer.untab();
    }

    public InstructionHeader doTransformations(Transformation[] trafo) {
        InstructionHeader next;
        for (int i=0; i < successors.length; i++) {
            for (InstructionHeader ih = successors[i]; ih != null; ih = next) {
                if ((next = ih.doTransformations(trafo)) == null)
                    next = ih.getNextInstruction();
            }
        }
        return null;
    }
}
