package jode;

public class CreateIfStatements implements Transformation {

    public InstructionHeader transform(InstructionHeader ifgoto) {
        if (ifgoto.getFlowType() != ifgoto.IFGOTO ||
            ifgoto.nextInstruction == null ||
            ifgoto.getSuccessors()[1].getAddress() <=
            ifgoto.getSuccessors()[0].getAddress())
            return null;

        InstructionHeader next     = ifgoto.getSuccessors()[1];
        InstructionHeader endBlock = next;
        if (ifgoto.outer != next.outer) {
            if (ifgoto.outer.endBlock != next)
                return null;
            next = null;
        }

        InstructionHeader thenStart = ifgoto.nextInstruction;
        InstructionHeader thenEnd;
        for (thenEnd = thenStart; 
             thenEnd != null && thenEnd.nextInstruction != next; 
             thenEnd = thenEnd.nextInstruction) {
        }
        if (thenEnd == null)
            return null;

        InstructionHeader elseStart = null;
        InstructionHeader elseEnd   = null;
        if (next != null &&
            thenEnd.getFlowType() == thenEnd.GOTO && 
            thenEnd.successors[0].getAddress() > next.getAddress()) {
            elseStart = next;
            endBlock = next = thenEnd.successors[0];
            if (ifgoto.outer != next.outer) {
                if (ifgoto.outer.endBlock != next)
                    return null;
                next = null;
            }
            for (elseEnd = elseStart; 
                 elseEnd != null && elseEnd.nextInstruction != next; 
                 elseEnd = elseEnd.nextInstruction) {
            }
            /* XXX return error or create if-then?
             */
            if (elseEnd == null)
                return null;
        }
        if(Decompiler.isVerbose)
            System.err.print("i");
        return new IfInstructionHeader
            (ifgoto, elseStart != null, thenEnd, elseEnd, endBlock);
    }
}



