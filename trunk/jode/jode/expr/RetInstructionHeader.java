package jode;

/**
 * This is an InstructionHeader for an RET (return from JSR) opcode.
 * @author Jochen Hoenicke
 */
public class RetInstructionHeader extends InstructionHeader {
    int dest;
    boolean conditional;

    InstructionHeader destination;
    InstructionHeader[] successors;/*XXX*/

    /**
     * Create an InstructionHeader for a conditional or unconditional
     * Ret.  
     * @param addr   The address of this instruction.
     * @param length The length of this instruction.
     * @param instr  The underlying Instruction of int type (ret addr).
     */
    public RetInstructionHeader(int addr, int length, Instruction instr) {
	super(addr, length, instr);
    }

    /**
     * Get the successors of this instructions.  This function mustn't
     * be called before resolveSuccessors is executed for this
     * InstructionHeaders.  
     * @return Array of successors.  
     */
    public InstructionHeader[] getSuccessors() {
	/* XXX */
	return successors;
    }

    /**
     * Resolve the successors and predecessors and build a doubly
     * linked list.  
     * @param instHeaders an array of the InstructionHeaders, indexed
     * by addresses.
     */
    public void resolveSuccessors(InstructionHeader[] instHeaders) {
	nextInstruction = instHeaders[addr+length];
    }
}
