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
     * @param prev     the goto instruction in front of this while loop.
     * @param ifHeader the instruction header which contains the 
     *                 if-goto statement.
     * @param blockStart the start of the inner block.
     * @param blockEnd   the end of the inner block.
     * @param next       the instruction header after this if block.
     */
    public WhileInstructionHeader(InstructionHeader prev,
                                  InstructionHeader ifHeader,
                                  InstructionHeader block) {

        super(WHILESTATEMENT, 
              ifHeader.successors[1].addr, ifHeader.successors[0].addr, 
              ifHeader.successors, ifHeader.outer);

        this.instr = ifHeader.instr;

        this.outer = ifHeader.outer;
        this.endBlock = this;

        this.movePredecessors(ifHeader);
        this.addr = successors[1].addr;
        prev.flowType = NORMAL;

        this.prevInstruction = successors[1].prevInstruction;
        if (prevInstruction != null)
            prevInstruction.nextInstruction = this;

        this.nextInstruction = ifHeader.nextInstruction;
        if (nextInstruction != null)
            nextInstruction.prevInstruction = this;

        successors[1].prevInstruction = null;
        ifHeader.prevInstruction.nextInstruction = null;

        successors[0].predecessors.removeElement(ifHeader);
        successors[1].predecessors.removeElement(ifHeader);
        successors[0].predecessors.addElement(this);
        successors[1].predecessors.addElement(this);

        for (InstructionHeader ih = successors[1]; ih != null;
             ih = ih.nextInstruction)
            if (ih.outer == outer)
                ih.outer = this;
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

        boolean braces = successors[1].nextInstruction != null;
        writer.println("while (" + instr.toString() + ")" + 
                       (braces ? " {": ""));

        writer.tab();        
	for (InstructionHeader ih = successors[1]; ih != null; 
             ih = ih.nextInstruction)
	    ih.dumpSource(writer);
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
        for (InstructionHeader ih = successors[1]; ih != null; ih = next) {
            if ((next = ih.doTransformations(trafo)) == null)
                next = ih.getNextInstruction();
        }
        return super.doTransformations(trafo);
    }
}
