package jode;
import java.util.Vector;
import sun.tools.java.BinaryExceptionHandler;
import sun.tools.java.Type;

/**
 * This class is the end point of the InstructionHeader list.
 * @author Jochen Hoenicke
 */
public class MethodInstructionHeader extends InstructionHeader {
    InstructionHeader first, last;

    /**
     * Create a new InstructionHeader.
     * @param addr   The address of this Instruction.
     * @param length The length of this Instruction.
     * @param instr  The underlying Instruction.
     */
    public MethodInstructionHeader(JodeEnvironment env,
                                   InstructionHeader[] instr,
                                   BinaryExceptionHandler[] handlers) {
        super(METHOD, 0, instr.length, new InstructionHeader[0], null);
        first = new InstructionHeader(EMPTY, 0, this);
        last  = new InstructionHeader(EMPTY, instr.length, this);

        first.nextInstruction = instr[0];
        instr[0].prevInstruction = first;

	for (int addr = 0; ; addr = instr[addr].nextAddr) {

            instr[addr].outer = this;
	    instr[addr].resolveSuccessors(instr);

	    if (instr[addr].flowType == RETURN) {
                InstructionHeader[] lastArr = { last };
                instr[addr].successors = lastArr;
		last.predecessors.addElement(instr[addr]);
            }

            if (instr[addr].nextAddr == instr.length) {
                instr[addr].nextInstruction = last;
                last.prevInstruction = instr[addr];
                break;
            }
	    
	}
        for (int i=0; i<handlers.length; i++) {
            InstructionHeader tryIH   = instr[handlers[i].startPC];
            if (tryIH.flowType != TRY)
                instr[handlers[i].startPC] = tryIH = 
                    new TryInstructionHeader(tryIH, this);

            Type type = handlers[i].exceptionClass.getType();
            instr[handlers[i].handlerPC] = 
                new CatchInstructionHeader
                (type, env.getTypeString(type),
                 instr[handlers[i].handlerPC], this);

            InstructionHeader endIH   = instr[handlers[i].endPC];
            InstructionHeader catchIH = instr[handlers[i].handlerPC];
            ((TryInstructionHeader)tryIH).addHandler(endIH, catchIH);
        }
        endBlock = last;
    }

    public InstructionHeader getFirst() {
        return first.nextInstruction;
    }

    public Vector getReturns() {
        return last.predecessors;
    }

    public void dumpSource(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
	for (InstructionHeader ih = first.nextInstruction; 
             ih != last; ih = ih.nextInstruction)
	    ih.dumpSource(writer);
    }

    public InstructionHeader doTransformations(Transformation[] trafo) {
        InstructionHeader next;
        for (InstructionHeader ih = first.nextInstruction; 
             ih != last; ih = next) {
            if ((next = ih.doTransformations(trafo)) == null)
                next = ih.getNextInstruction();
        }
        return null;
    }
}
