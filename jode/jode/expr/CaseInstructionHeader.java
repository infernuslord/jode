package jode;
import java.util.Enumeration;

public class CaseInstructionHeader extends InstructionHeader {

    int label;
    boolean isDefault;

    public CaseInstructionHeader(int label, boolean isDefault,
                                 //Type type,
                                 int addr,
                                 InstructionHeader parent) {
        super(CASESTATEMENT, addr, addr,
              new InstructionHeader[1], parent);
        this.label = label;
        this.isDefault = isDefault;
    }

    public void dumpSource(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
	if (Decompiler.isDebugging) {
            dumpDebugging(writer);
	    writer.tab();
	}
        if (isDefault) {
            if (nextInstruction == null && successors[0] == null)
                return;
            writer.println("default:");
        } else
            writer.println("case "+label+":" );

        writer.tab();
        for (InstructionHeader ih = successors[0]; ih != null; 
             ih = ih.nextInstruction)
                ih.dumpSource(writer);
        writer.untab();
    }

    /** 
     * Get the instruction header where the next instruction is.
     */
    InstructionHeader getShadow() {
        return (successors[0] != null ? successors[0].getShadow() : 
                getEndBlock());
    }

    public InstructionHeader doTransformations(Transformation[] trafo) {
        InstructionHeader next;
        if (successors[0] != getEndBlock()) {
            for (InstructionHeader ih = successors[0]; ih != null; ih = next) {
                if ((next = ih.doTransformations(trafo)) == null)
                    next = ih.getNextInstruction();
            }
        }
        return super.doTransformations(trafo);
    }
}
