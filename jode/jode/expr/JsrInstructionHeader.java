package jode;

/**
 * This is an InstructionHeader for an JSR (jump subroutine) opcode.
 * @author Jochen Hoenicke
 */
public class JsrInstructionHeader extends InstructionHeader {
    int dest;

    InstructionHeader destination;

    /**
     * Create an InstructionHeader for a conditional or unconditional
     * Jsr.  
     * @param addr   The address of this instruction.
     * @param length The length of this instruction.
     * @param dest   The destination address of the Jsr.
     * @param instr  The undelying Instruction, the type of must be
     *    <ul><li> boolean  for a conditional Jsr. </li>
     *        <li> void for  an unconditional Jsr. </li></ul>
     */
    public JsrInstructionHeader(int addr, int length, int dest, 
				Instruction instr) {
	super(addr,length, instr);
	this.dest = dest;
    }

    /**
     * Get the successors of this instructions.  This function mustn't
     * be called before resolveSuccessors is executed for this
     * InstructionHeaders.  
     * @return Array of successors.  
     */
    public InstructionHeader[] getSuccessors() {
	InstructionHeader[] result = { destination };
	return result;
    }

    /**
     * Resolve the successors and predecessors and build a doubly
     * linked list.  
     * @param instHeaders an array of the InstructionHeaders, indexed
     * by addresses.
     */
    public void resolveSuccessors(InstructionHeader[] instHeaders) {
	nextInstruction = instHeaders[addr+length];
	destination = instHeaders[dest];
	destination.predecessors.addElement(this);
	/* Ret.successors.addElement(nextInstruction); XXX */
    }

    public String toString() {
        return "Jsr " + dest;
    }
}
