package jode;

public class SimplifyExpression implements Transformation {
    public InstructionHeader transform(InstructionHeader ih) {
        if (ih.getInstruction() != null)
            ih.setInstruction(ih.getInstruction().simplify());
        return null;
    }
}
