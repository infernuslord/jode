package jode;

public class CreateWhileStatements implements Transformation {

    public InstructionHeader transform(InstructionHeader gotoIH) {

        if (gotoIH.flowType != gotoIH.GOTO || gotoIH.nextInstruction == null ||
            gotoIH.successors[0].addr < gotoIH.nextInstruction.addr)
            return null;

        InstructionHeader block  = gotoIH.nextInstruction;
        if (block == null)
            block = gotoIH.outer.endBlock;

        InstructionHeader ifgoto = gotoIH.successors[0];

        if (ifgoto.getFlowType() != ifgoto.IFGOTO ||
            ifgoto.successors[1] != block ||
            ifgoto.outer != block.outer)
            return null;

        if(Decompiler.isVerbose)
            System.err.print("w");
        return new WhileInstructionHeader(gotoIH, ifgoto, block);
    }
}
