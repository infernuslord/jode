package jode;
import java.util.Enumeration;

/**
 * This instruction header represents an if instruction.  The
 * linkage of the instructions is as follow:
 * <pre>
 *  A: ....
 * <p>
 *     prev = A, next = H, pred = normal, succ = {C,E}
 *  B: switch ( value ) {
 * <p>
 *     case 1:
 * <p>
 *          prev = null, next = D, pred = {B}, succ = {D}
 *       C: then-instr 1
 * <p>
 *          prev = C, next = H!, pred = normal succ = {E}
 *       D: instructions of then part
 * <p>
 *     case 2:
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
public class SwitchInstructionHeader extends InstructionHeader {

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
     * @param cases     the values belonging to the case labels.
     * @param successors the successors of this instruction (the cases).
     *        This array must be sorted from low to high addresses.
     * @param defaultCase  the position of the default case in the array.
     * @param endBlock  the next instruction after this switch statement.
     */
    public SwitchInstructionHeader(InstructionHeader   switchIH,
                                   int               []cases,
                                   InstructionHeader []caseIHs,
                                   int                 defaultCase,
                                   InstructionHeader   endBlock) {

        super(SWITCHSTATEMENT, switchIH.addr, switchIH.nextAddr, 
              new InstructionHeader[caseIHs.length], switchIH.outer);

        this.instr = switchIH.getInstruction();
        this.movePredecessors(switchIH);

        /* switchIH may have more succesors than we:
         * CreateSwitchStatements removes case labels, that are the
         * default.  
         * And the successors of this switch may differ in case that
         * dummy gotos were inserted.
         */
        for (int i=0; i<switchIH.successors.length; i++)
            switchIH.successors[i].predecessors.removeElement(switchIH);

        if (endBlock.outer == outer) {
            if (endBlock.prevInstruction != null)
                endBlock.prevInstruction.nextInstruction = null;
            nextInstruction = endBlock;
            endBlock.prevInstruction = this;
        } else if (endBlock.getShadow() == outer.getEndBlock())
            nextInstruction = null;
        else {
            /* Create a goto after this block, that
             * jumps to endBlock
             */
            nextInstruction = new InstructionHeader
                (GOTO, endBlock.addr, endBlock.addr,
                 new InstructionHeader[1], outer);
            nextInstruction.prevInstruction = this;
            nextInstruction.successors[0] = endBlock;
            endBlock.predecessors.addElement(nextInstruction);
        }

        int label = 0;
        InstructionHeader lastHeader = null;

        InstructionHeader ih = switchIH.nextInstruction;
        while (ih != null) {
            ih.prevInstruction.nextInstruction = null;
            ih.prevInstruction = null;
            while (label < caseIHs.length && caseIHs[label] == ih) {
                successors[label] = new CaseInstructionHeader
                    (cases[label], label == defaultCase, ih.addr, this);
                
                if (label > 0) {
                    successors[label-1].
                        nextInstruction = successors[label];
                    successors[label].
                        prevInstruction = successors[label-1];
                }
                label++;
            }
            successors[label-1].successors[0] = ih;
            ih.predecessors.addElement(successors[label-1]);
            
            while (ih != null && 
                   (label == caseIHs.length || 
                    caseIHs[label] != ih)) {
                if (ih.outer == outer)
                    ih.outer = successors[label-1];
                ih = ih.nextInstruction;
            }
        }

        while (label < caseIHs.length) {
            successors[label] = new CaseInstructionHeader
                (cases[label], label == defaultCase, getEndBlock().addr, this);
            if (label > 0) {
                successors[label-1].
                    nextInstruction = successors[label];
                successors[label].
                    prevInstruction = successors[label-1];
            }
            label++;
        }
    }

    /**
     * Returns the InstructionHeader where a break of this instruction
     * would jump to. Does only make sense for do/while/for-loops and
     * switch instructions.
     */
    public InstructionHeader getBreak() {
        return getEndBlock();
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

        writer.println("switch (" + instr.toString() + ") {");

        int label = 0;
        for (InstructionHeader ih = successors[0]; ih != null; 
             ih = ih.nextInstruction)
            ih.dumpSource(writer);
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
        return super.doTransformations(trafo);
    }
}
