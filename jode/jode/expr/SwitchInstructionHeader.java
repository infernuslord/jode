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

    int[] cases;
    int  defaultCase;
    
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
                                   InstructionHeader []successors,
                                   int                 defaultCase,
                                   InstructionHeader   endBlock) {

        super(SWITCHSTATEMENT, switchIH.addr, endBlock.addr, 
              successors, switchIH.outer);

        this.instr = switchIH.getInstruction();
        this.movePredecessors(switchIH);
        this.endBlock = endBlock;
        this.defaultCase = defaultCase;
        this.cases = cases;

        /* switchIH may have more succesors than we:
         * CreateSwitchStatements removes case labels, that are the
         * default.  
         */
        for (int i=0; i<switchIH.successors.length; i++)
            switchIH.successors[i].predecessors.removeElement(switchIH);
        for (int i=0; i<successors.length; i++)
            successors[i].predecessors.addElement(this);

        for (int i=0; i<successors.length; i++) {
            if (successors[i].outer == outer) {
                if (successors[i].prevInstruction != null)
                    successors[i].prevInstruction.nextInstruction = null;
                successors[i].prevInstruction = null;
            }
        }
        if (endBlock.outer == outer) {
            if (endBlock.prevInstruction != null)
                endBlock.prevInstruction.nextInstruction = null;
            nextInstruction = endBlock;
            endBlock.prevInstruction = this;
        } else {
            if (endBlock != outer.endBlock) {
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
        }

        for (int i=0; i < successors.length && successors[i] != endBlock; i++)
            for (InstructionHeader ih = successors[i]; ih != null; 
                 ih = ih.nextInstruction)
                if (ih.outer == outer)
                    ih.outer = this;
    }

    /**
     * Returns the InstructionHeader where a break of this instruction
     * would jump to. Does only make sense for do/while/for-loops and
     * switch instructions.
     */
    public InstructionHeader getBreak() {
        return endBlock;
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

        for (int i=0; i<successors.length; i++) {
            if (i != defaultCase)
                writer.println("case "+cases[i]+":");

            if (successors[i] == endBlock)
                break;
            if (i+1 < successors.length && successors[i] == successors[i+1])
                continue;

            if (i == defaultCase)
                writer.println("default:");
            writer.tab();
            for (InstructionHeader ih = successors[i]; ih != null; 
                 ih = ih.nextInstruction)
                ih.dumpSource(writer);
            writer.untab();
        }
        writer.println("} ");

	if (Decompiler.isDebugging)
	    writer.untab();
    }

    public InstructionHeader doTransformations(Transformation[] trafo) {
        InstructionHeader next;
        for (int i=0; i < successors.length && successors[i] != endBlock; i++)
            for (InstructionHeader ih = successors[i]; ih != null; ih = next) {
                if ((next = ih.doTransformations(trafo)) == null)
                    next = ih.getNextInstruction();
            }
        return super.doTransformations(trafo);
    }
}
