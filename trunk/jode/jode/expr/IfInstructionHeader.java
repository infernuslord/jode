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
     * Creates a new if statement.
     * @param ifHeader  the instruction header whichs contains the 
     *                  if goto statement, must have a nextInstruction.
     * @param elseBlock the start of the else part, same outer, may be null.
     *                  if not null, it must equals ifHeader.successors[1].
     * @param next      the next instruction after the if statement, 
     *                  same outer, may be null.
     * @param endBlock  the instruction where the control flows after the if,
     *                  outer may differ, not null.
     */
    public IfInstructionHeader(InstructionHeader ifHeader,
                               InstructionHeader elseBlock,
                               InstructionHeader next) {

        super(IFSTATEMENT, ifHeader.addr, ifHeader.addr, 
              ifHeader.successors, ifHeader.outer);

        hasElsePart = elseBlock != null;
        this.instr = ((Expression)ifHeader.getInstruction()).negate();

        this.movePredecessors(ifHeader);
        /* this.moveSuccessors(ifHeader); */
        successors[0].predecessors.removeElement(ifHeader);
        successors[1].predecessors.removeElement(ifHeader);
        successors[0].predecessors.addElement(this);
        successors[1].predecessors.addElement(this);

        /* unlink the first instruction of the if */
        ifHeader.nextInstruction.prevInstruction = null;
        /* unlink the last instruction of the if */
        if (next != null)
            next.prevInstruction.nextInstruction = null;
        
        this.hasElsePart = hasElsePart;
        if (hasElsePart) {

            /* unlink the instructions around the else */
            elseBlock.prevInstruction.nextInstruction = null;
            elseBlock.prevInstruction = null;

            for (InstructionHeader ih = successors[1]; ih != null; 
                 ih = ih.nextInstruction)
                if (ih.outer == outer)
                    ih.outer = this;
        }

        /* Do this now, because the end of the then part is cut in
         * the else part above.
         */
        for (InstructionHeader ih = successors[0]; ih != null; 
             ih = ih.nextInstruction)
            if (ih.outer == outer)
                ih.outer = this;

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

            if (successors[1].flowType == IFSTATEMENT &&
                successors[1].nextInstruction == null) {
                /* write "else if" */
                braces = false;
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
