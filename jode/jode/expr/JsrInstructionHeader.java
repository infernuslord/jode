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
				Instruction instr, int[] succs) {
	super(JSR, addr, addr+length, instr, succs);
	this.dest = dest;
    }

    /**
     * Resolve the successors and predecessors and build a doubly
     * linked list.  
     * @param instHeaders an array of the InstructionHeaders, indexed
     * by addresses.
     */
    public void resolveSuccessors(InstructionHeader[] instHeaders) {
	nextInstruction = instHeaders[nextAddr];
	destination = instHeaders[dest];
	destination.predecessors.addElement(this);
	/* Ret.successors.addElement(nextInstruction); XXX */
    }

    public String toString() {
        return "Jsr " + dest;
    }
}
