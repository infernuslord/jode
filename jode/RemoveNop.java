package jode; 

public class RemoveNop implements Transformation {
    public InstructionHeader transform (InstructionHeader ih) {
        Instruction pred;
        try {
            NopOperator op = (NopOperator) ih.getInstruction();
            ih = ih.getSimpleUniquePredecessor();
            pred = ih.getInstruction();
            if (pred == null)
                return null;
        } catch (NullPointerException ex) {
            return null;
        } catch (ClassCastException ex) {
            return null;
        }
        return ih.combine(2, pred);
    }
}
