package jode;
import java.util.Vector;
import sun.tools.java.Type;

/**
 * This class maintains the connections between the
 * InstructionHeaders.  They are connected in a doubly linked list
 * (but a instruction may have multiple successors and predecessors).
 * @see JumpInstructionHeader
 * @see SwitchInstructionHeader
 * @author Jochen Hoenicke
 */
public class InstructionHeader {
    int addr, length;
    Instruction  instr;
    InstructionHeader nextInstruction;

    Type switchType;
    int[] cases;
    int[] succs;
    InstructionHeader[] successors;

    Vector predecessors = new Vector();
   
    /**
     * Create a new InstructionHeader.
     * @param addr   The address of this Instruction.
     * @param length The length of this Instruction.
     * @param instr  The underlying Instruction.
     */
    public InstructionHeader(int addr, int length, Instruction instr) {
        int[] succs = { addr + length }; 
	this.addr = addr;
	this.length = length;
	this.instr = instr;
        switchType = MyType.tVoid;
        this.cases = new int[0];
        this.succs = succs;
    }

    /**
     * Create a new InstructionHeader.
     * @param addr   The address of this Instruction.
     * @param length The length of this Instruction.
     * @param instr  The underlying Instruction.
     * @param type   The type of the switch
     * @param cases  The possible cases
     * @param succs  The destinations (one longer for default)
     */
    public InstructionHeader(int addr, int length, Instruction instr,
                             Type type, int[] cases, int[] succs) {
	this.addr = addr;
	this.length = length;
	this.instr = instr;
        switchType = type;
        this.cases = cases;
        this.succs = succs;
    }

    /**
     * Create an InstructionHeader for a return
     * @param addr   The address of this instruction.
     * @param length The length of this instruction.
     * @param instr  The underlying Instruction.
     */
    public static InstructionHeader ret(int addr, int length, 
                                        Instruction instr) {
         return new InstructionHeader (addr, length, instr, 
                                       MyType.tVoid, new int[0], new int[0]);
    }

    /**
     * Create an InstructionHeader for an unconditional jump.  
     * @param addr   The address of this instruction.
     * @param length The length of this instruction.
     * @param instr  The underlying Instruction.
     * @param dest   The destination address of the jump.
     */
    public static InstructionHeader jump(int addr, int length, int dest,
                                         Instruction instr) {
         int [] succs = { dest };
         return new InstructionHeader (addr, length, instr, 
                                       MyType.tVoid, new int[0], succs);
    }

    /**
     * Create an InstructionHeader for a conditional jump.  
     * @param addr   The address of this instruction.
     * @param length The length of this instruction.
     * @param instr  The underlying Instruction.
     * @param dest   The destination address of the jump.
     */
    public static InstructionHeader conditional(int addr, int length, int dest,
                                                Instruction instr) {
        int[] cases = { 0 };
        int[] succs = { addr+length , dest };
        return new InstructionHeader (addr, length, instr, 
                                      MyType.tBoolean, cases, succs);
    }

    public String toString() {
        return instr.toString();
    }

    /**
     * Get the address of this instruction.
     * @return The address.
     */
    public int getAddress() {
        return addr;
    }

    /**
     * Get the next address in code order.
     * @return The next instruction
     */
    public int getNextAddr() {
	return addr+length;
    }

    /**
     * Get the underlying instruction.
     * @return The underlying instruction.
     */
    public Instruction getInstruction() {
	return instr;
    }

    /**
     * Get the next instruction in code order.  This function mustn't
     * be called before resolveSuccessors is executed for this
     * InstructionHeaders.  
     * @return The next instruction
     */
    public InstructionHeader getNextInstruction() {
	return nextInstruction;
    }
    
    /**
     * Get the successors of this instructions.  This function mustn't
     * be called before resolveSuccessors is executed for this
     * InstructionHeaders.  
     * @return Array of successors.  
     */
    public InstructionHeader[] getSuccessors() {
	return successors;
    }

    public boolean hasDirectPredecessor() {
	return predecessors.size() == 1 &&
	    ((InstructionHeader)predecessors.elementAt(0)).
	    getNextInstruction() == this;
    }

    /**
     * Get the unique predecessor or null if there isn't a 
     * unique predecessor.
     */
    public InstructionHeader getUniquePredecessor() {
        if (predecessors.size() != 1)
            return null;
        InstructionHeader pre = (InstructionHeader)predecessors.elementAt(0);
        return (pre.getNextInstruction() == this &&
                pre.getSuccessors().length != 1) ? null : pre;
    }
    
    /**
     * Get the predecessors of this instruction.  This function mustn't
     * be called before resolveSuccessors is executed for all
     * InstructionHeaders.  
     * @return Vector of predecessors.
     */
    public Vector getPredecessors() {
	return predecessors;
    }

    /**
     * Resolve the successors and predecessors and build a doubly
     * linked list.  
     * @param instHeaders an array of the InstructionHeaders, indexed
     * by addresses.
     */
    public void resolveSuccessors(InstructionHeader[] instHeaders) {
        if (addr+length < instHeaders.length)
            nextInstruction = instHeaders[addr+length];
        else
            nextInstruction = null;
        successors = new InstructionHeader[succs.length];
        for (int i=0; i< succs.length; i++) {
            successors[i] = instHeaders[succs[i]];
            successors[i].predecessors.addElement(this);
        }
    }

    public void dumpSource(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
	if (writer.verbosity > 5) {
	    writer.println("<"+addr + " - "+(addr+length-1)+">");
	    writer.tab();
	}
	if (!hasDirectPredecessor() && addr != 0)
	    writer.print("addr_"+addr+": ");

        if (switchType == MyType.tBoolean) {

            writer.println("if ("+instr.toString()+") goto addr_"+succs[1]);
            if (succs[0] != addr + length)
                writer.println("goto addr_"+succs[0]);

        } else if (switchType == MyType.tVoid) {
            
            if (instr.getType() != MyType.tVoid)
                writer.print("push ");
            writer.println(instr.toString()+";");
            if (succs.length > 0 && succs[0] != addr + length)
                writer.println("goto addr_"+succs[0]);

        } else {
            writer.println("switch ("+instr.toString()+") {");
            writer.tab();
            writer.untab();
        }
	if (writer.verbosity > 5)
	    writer.untab();
    }

    /**
     * This method replaces multiple InstructionHeaders by a single one.
     * The next count Instructions must be unique.
     * @param count the number of InstructionHeaders that should be replaced.
     * @param instr the new instruction; this should be equivalent to the
     *              old <em>count</em instructions.
     */
    public void combine(int count, Instruction newInstr) {
        InstructionHeader last = this;
        this.instr = newInstr;
        for (int i=1; i < count; i++) {
            last = last.getSuccessors()[0];
            length += last.length;
        }
        switchType = last.switchType;
        cases      = last.cases;
        succs      = last.succs;
        successors = last.successors;
        nextInstruction = last.nextInstruction;
        for (int i=0; i< successors.length; i++) {
            successors[i].predecessors.removeElement(last);
            successors[i].predecessors.addElement(this);
        }
    }

    /**
     * This method replaces two conditional InstructionHeaders by a
     * single one.  You must make sure that this and the next instruction
     * are both conditional Instructions and the destinations matches.
     *
     * @param newCondition the new instruction; this should be equivalent
     *              to the old two conditions.
     */
    public void combineConditional(Instruction newCondition) {
        nextInstruction.successors[0].predecessors.
	    removeElement(nextInstruction);
        nextInstruction.successors[1].predecessors.
	    removeElement(nextInstruction);
        instr = newCondition;

        succs[0] = nextInstruction.succs[0];     // aid debugging
        successors[0] = nextInstruction.successors[0];

        if (successors[1] != nextInstruction.successors[1]) {
            succs[1]      = nextInstruction.succs[1];   // aid debugging
            successors[1] = nextInstruction.successors[1];
            successors[1].predecessors.addElement(this);
        } else {
            successors[0].predecessors.addElement(this);
        }

        length += nextInstruction.length;        // aid debugging
        nextInstruction = nextInstruction.nextInstruction;
    }
}
