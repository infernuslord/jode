package jode;

public class CreateBreakStatement implements Transformation {
    public InstructionHeader transform(InstructionHeader ih) {
        InstructionHeader breakDest;
        if (ih.getFlowType() == ih.GOTO)
            breakDest = ih.successors[0];
        else if (ih.getFlowType() == ih.IFGOTO)
            breakDest = ih.successors[1];
        else
            return null;

        boolean needBreakLabel = false, needContLabel = false;
        InstructionHeader outer = ih.outer;
        while (outer != null) {
            if (outer.getBreak() == breakDest) {
                if (Decompiler.isVerbose)
                    System.err.print("b");
                return new BreakInstructionHeader
                    (ih, needBreakLabel?outer.getLabel(): null, true);
            }
            if (outer.getContinue() == breakDest) {
                if (Decompiler.isVerbose)
                    System.err.print("b");
                return new BreakInstructionHeader
                    (ih, needContLabel?outer.getLabel(): null, false);
            }
            if (outer.getBreak() != null)
                needBreakLabel = true;
            if (outer.getContinue() != null)
                needContLabel = true;
            outer = outer.outer;
        }
        return null;
    }
}

