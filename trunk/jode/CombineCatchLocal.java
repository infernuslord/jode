package jode;
import java.util.Vector;
import sun.tools.java.Identifier;

public class CombineCatchLocal implements Transformation{

    static Identifier idException = Identifier.lookup("exception");

    public InstructionHeader transform(InstructionHeader ih) {
        CatchInstructionHeader catchIH;
        LocalInfo local;
        try {
            catchIH = (CatchInstructionHeader)ih;
            ih = ih.nextInstruction;
            if (ih.getPredecessors().size() != 1)
                return null;
            Instruction instr = ih.getInstruction();
            if (instr instanceof PopOperator) {
                local = new LocalInfo(99);
                local.setName(idException);
            } else if (instr instanceof LocalStoreOperator) {
                local = ((LocalStoreOperator) instr).getLocalInfo();
                local.setName(idException);
            } else
                return null;
        } catch (ClassCastException ex) {
            return null;
        } catch (NullPointerException ex) {
            return null;
        }
        if (!catchIH.combineWithLocal(local))
            return null;
        if(Decompiler.isVerbose)
            System.err.print("c");
        return catchIH;
    }
}
