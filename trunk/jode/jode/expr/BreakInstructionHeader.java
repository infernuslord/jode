package jode;
import java.util.Enumeration;

/** This instruction header represents an break, continue or an
 * if + break/continue statement.  
 */

public class BreakInstructionHeader extends InstructionHeader {

    boolean conditional;
    String breakLabel;
    
    /**
     * Creates a new break statement.  
     * @param gotoHeader the instruction header whichs contains the 
     *                   (maybe conditonal) goto statement.
     * @param label      the label where to break to, may be null.
     * @param isBreak    is this a break or a continue.
     */
    public BreakInstructionHeader(int               flowType,
                                  InstructionHeader gotoHeader,
                                  String            label) {

        super(flowType, gotoHeader.addr, gotoHeader.nextAddr,
              gotoHeader.successors, gotoHeader.outer);

        this.instr = gotoHeader.getInstruction();
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
        }
        writer.println((flowType == BREAK ? "break" :
                        flowType == CONTINUE ? "continue" : "return") +
                       (breakLabel != null?" "+breakLabel:"")+";");
        
        if (conditional)
            writer.untab();

	if (Decompiler.isDebugging)
	    writer.untab();
    }
}
