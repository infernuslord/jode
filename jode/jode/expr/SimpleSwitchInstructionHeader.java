package jode;

/**
 * This is an InstructionHeader for simple switch statements.  Simple
 * means that this is without blocks.
 * @author Jochen Hoenicke
 */
public class SimpleSwitchInstructionHeader extends InstructionHeader {

    /**
     * The case labels of a switch instruction header.
     */
    int[] cases;

    /**
     * Create an InstructionHeader for a conditional or unconditional
     * Switch.  
     * @param addr   The address of this instruction.
     * @param nextAddr The address of the next instruction header.
     * @param instr  The underlying Instruction, the type of must be
     *               an integer type.
     * @param cases  The value of the cases.
     * @param dests  The destination addresses belonging to the cases
     *               plus the default destination address.
     */
    public SimpleSwitchInstructionHeader(int addr, int nextAddr, 
				   Instruction instr, 
				   int[] cases, int[] dests) {
	super(SWITCH, addr, nextAddr, instr, dests);
	this.cases = cases;
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

        writer.println("switch ("+instr.toString()+") {");
        writer.tab();
        for (int i=0; i<cases.length; i++)
            writer.println("case "+cases[i]+": goto "+
                           successors[i].getLabel());
        writer.println("default: "+
                       successors[successors.length-1].getLabel());
        writer.println("}");
        writer.untab();

	if (Decompiler.isDebugging)
	    writer.untab();
    }
}

