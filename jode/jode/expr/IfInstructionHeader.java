package jode;
import java.util.Enumeration;

/**
 * This instruction header represents an if instruction.  The
 * linkage of the instructions is as follow:
 * <pre>
 *  A: ....
 * <p>
 *     prev = A, next = H, pred = normal, succ = {C,E}
 *  B: if ( instr ) {
 * <p>
 *          prev = null, next = D, pred = {B}, succ = {D}
 *       C: then-instr 1
 * <p>
 *          prev = C, next = H!, pred = normal succ = normal
 *       D: instructions of then part
 * <p>
 *     } else {
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
 */
public class IfInstructionHeader extends InstructionHeader {

    boolean hasElsePart;
    
    /**
     * Creates a new if statement.  There are several conditions that
     * must be met:
     * <ul>
     * <li><code>   ifHeader.successors[0] == thenStart
     * </code></li>
     * <li><code>   ifHeader.successors[1] == thenEnd.nextInstruction 
     * </code></li>
     * <li><code>   elseStart == null || (thenEnd.flowType = GOTO &&
     *               elseEnd.nextInstruction == thenEnd.successors[0])
     * </code></li>
     * <li><code>   elseStart == null || elseStart = thenEnd.nextInstruction 
     * </code></li>
     * <li><code>   thenStart.nextInstruction....nextInstruction = thenEnd
     * </code></li>
     * <li><code>   elseStart.nextInstruction....nextInstruction = elseEnd
     * </code></li>
     * </ul>
     * @param ifHeader  the instruction header whichs contains the 
     *                  if goto statement.
     * @param thenStart the start of the then part.
     * @param thenEnd   the end of the then part.
     * @param elseStart the start of the else part.
     * @param elseEnd   the end of the then part.
     * @param next      the next instruction after the if statement.
     */
    public IfInstructionHeader(InstructionHeader ifHeader,
                               boolean           hasElsePart,
                               InstructionHeader thenEnd,
                               InstructionHeader elseEnd,
                               InstructionHeader endBlock) {

        super(IFSTATEMENT, ifHeader.addr, endBlock.addr, 
              ifHeader.successors, ifHeader.outer);

        this.instr = ((Expression)ifHeader.getInstruction()).negate();

        this.movePredecessors(ifHeader);
        this.outer    = ifHeader.outer;
        this.endBlock = endBlock;
        successors[0].predecessors.removeElement(ifHeader);
        successors[1].predecessors.removeElement(ifHeader);
        successors[0].predecessors.addElement(this);
        successors[1].predecessors.addElement(this);

        successors[0].prevInstruction = null;
        InstructionHeader next = thenEnd.nextInstruction;
        thenEnd.nextInstruction = null;

        for (InstructionHeader ih = successors[0]; ih != null; 
             ih = ih.nextInstruction)
            if (ih.outer == outer)
                ih.outer = this;

        this.hasElsePart = hasElsePart;
        if (hasElsePart) {
            thenEnd.flowType = thenEnd.NORMAL;

            successors[1].prevInstruction = null;
            next = elseEnd.nextInstruction;
            elseEnd.nextInstruction = null;

            for (InstructionHeader ih = successors[1]; ih != null; 
                 ih = ih.nextInstruction)
                if (ih.outer == outer)
                    ih.outer = this;
        }

        this.nextInstruction = next;
        if (next != null)
            next.prevInstruction = this;
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

        boolean braces = successors[0].flowType != NORMAL ||
            successors[0].nextInstruction != null;
        writer.println("if (" + instr.toString() + ")" + (braces ? " {": ""));

        writer.tab();        
	for (InstructionHeader ih = successors[0]; ih != null; 
             ih = ih.nextInstruction)
	    ih.dumpSource(writer);
        writer.untab();

        if (hasElsePart) {
            if (braces)
                writer.print("} ");
            
            braces = successors[1].flowType != NORMAL ||
                successors[1].nextInstruction != null;

            if (!braces && successors[1].flowType == IFSTATEMENT) {
                writer.print("else ");
                successors[1].dumpSource(writer);
            } else {
                writer.println("else" + (braces ? " {": ""));
                writer.tab();
                for (InstructionHeader ih = successors[1]; 
                     ih != null; ih = ih.nextInstruction)
                    ih.dumpSource(writer);
                writer.untab();
            }
        }
        if (braces)
            writer.println("} ");

	if (Decompiler.isDebugging)
	    writer.untab();
    }

    public InstructionHeader doTransformations(Transformation[] trafo) {
        InstructionHeader next;
        for (InstructionHeader ih = successors[0]; ih != null; ih = next) {
            if ((next = ih.doTransformations(trafo)) == null)
                next = ih.getNextInstruction();
        }
        if (hasElsePart) {
            for (InstructionHeader ih = successors[1]; ih != null; ih = next) {
                if ((next = ih.doTransformations(trafo)) == null)
                    next = ih.getNextInstruction();
            }
        }
        return super.doTransformations(trafo);
    }
}
