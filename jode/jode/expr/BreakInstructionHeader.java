package jode;
import java.util.Enumeration;

/** This instruction header represents an break, continue or an
 * if + break/continue statement.  
 */

public class BreakInstructionHeader extends InstructionHeader {

    boolean conditional;
    boolean isBreak;
    String breakLabel;
    
    /**
     * Creates a new break statement.  
     * @param gotoHeader the instruction header whichs contains the 
     *                   (maybe conditonal) goto statement.
     * @param label      the label where to break to, may be null.
     * @param isBreak    is this a break or a continue.
     */
    public BreakInstructionHeader(InstructionHeader gotoHeader,
                               String            label,
                               boolean           isBreak) {

        super(BREAKSTATEMENT, gotoHeader.addr, gotoHeader.nextAddr,
              gotoHeader.successors, gotoHeader.outer);

        this.instr = gotoHeader.getInstruction();
        this.isBreak  = isBreak;
        this.conditional = (gotoHeader.flowType == IFGOTO);
        this.breakLabel = label;

        this.movePredecessors(gotoHeader);
        this.successors = gotoHeader.successors;
        for (int i=0; i< successors.length; i++) {
            successors[i].predecessors.removeElement(gotoHeader);
            successors[i].predecessors.addElement(this);
        }
        this.nextInstruction = gotoHeader.nextInstruction;
        if (nextInstruction != null)
            nextInstruction.prevInstruction = this;
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

        if (conditional) {
            writer.println("if ("+instr.toString()+")");
            writer.tab();
        } else {
            if (!(instr instanceof NopOperator)) {
                if (instr.getType() != MyType.tVoid)
                    writer.print("push ");
                writer.println(instr.toString()+";");
            }
        }
        writer.println((isBreak?"break":"continue") +
                       (breakLabel != null?" "+breakLabel:"")+";");
        
        if (conditional)
            writer.untab();

	if (Decompiler.isDebugging)
	    writer.untab();
    }
}
