package jode;
import java.util.Vector;

/**
 * This class is the end point of the InstructionHeader list.
 * @author Jochen Hoenicke
 */
public class MethodInstructionHeader extends InstructionHeader {
    /**
     * Create a new InstructionHeader.
     * @param addr   The address of this Instruction.
     * @param length The length of this Instruction.
     * @param instr  The underlying Instruction.
     */
    public MethodInstructionHeader(InstructionHeader[] instr) {
	super(-1,1,null);
	nextInstruction = instr[0];
        nextInstruction.predecessors.addElement(this);
	for (int addr = 0; addr < instr.length; ) {
	    instr[addr].resolveSuccessors(instr);
	    if (instr[addr].succs.length == 0)
		predecessors.addElement(instr[addr]);
	    addr = instr[addr].getNextAddr();
	}
    }

    /**
     * Resolve the successors and predecessors and build a doubly
     * linked list.  
     * @param instHeaders an array of the InstructionHeaders, indexed
     * by addresses.
     */
    public void resolveSuccessors(InstructionHeader[] instHeaders) {
    }
}
