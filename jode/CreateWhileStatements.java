package jode;

public class CreateWhileStatements extends FlowTransformation
implements Transformation {

    public InstructionHeader transform(InstructionHeader gotoIH) {

        if (gotoIH.flowType == gotoIH.IFGOTO &&
            gotoIH.successors[1] == gotoIH)
            /* This is an empty while loop
             */
            return new WhileInstructionHeader(gotoIH, gotoIH);

        if (gotoIH.flowType != gotoIH.GOTO || 
            gotoIH.nextInstruction == null ||
            gotoIH.successors[0].addr < gotoIH.nextInstruction.addr ||
            gotoIH.outer != gotoIH.successors[0].outer)
            return null;

        InstructionHeader ifgoto = gotoIH.successors[0];

        if (ifgoto.getFlowType() != ifgoto.IFGOTO ||
            ifgoto.outer != ifgoto.successors[1].outer)
            return null;

        InstructionHeader next = UnoptimizeWhileLoops(ifgoto.successors[1]);
        if (next != gotoIH.nextInstruction)
            return null;

        if (next != ifgoto.successors[1]) {
            ifgoto.successors[1].predecessors.removeElement(ifgoto);
            ifgoto.successors[1] = next;
            ifgoto.successors[1].predecessors.addElement(ifgoto);
        }

        if(Decompiler.isVerbose)
            System.err.print("w");
        return new WhileInstructionHeader(gotoIH, ifgoto);
    }
}
