package jode;
import java.util.Enumeration;

public class CreateSwitchStatements implements Transformation {

    public InstructionHeader transform(InstructionHeader ih) {
        if (ih.getFlowType() != ih.SWITCH)
            return null;

        SimpleSwitchInstructionHeader switchIH = 
            (SimpleSwitchInstructionHeader) ih;

        int defaultCase = switchIH.successors.length - 1;
        int addr = switchIH.nextInstruction.addr;
        int count = 1;
        for (int i=0; i < switchIH.successors.length; i++) {
            if (switchIH.successors[i].addr < addr)
                return null;
            if (switchIH.successors[i] != switchIH.successors[defaultCase])
                count ++;
        }

        int[] cases  = new int[count];
        InstructionHeader[] sorted = new InstructionHeader[count];
        count = 0;
        for (int i=0; i < switchIH.successors.length; i++) {
            if (i != defaultCase &&
                switchIH.successors[i] == switchIH.successors[defaultCase])
                continue;
            int insert;
            for (insert = 0; insert < count; insert++) {
                if (sorted[insert].addr > switchIH.successors[i].addr) 
                    break;
            }
            if (insert < count) {
                System.arraycopy(cases, insert, 
                                 cases, insert+1, count-insert);
                System.arraycopy(sorted, insert, 
                                 sorted, insert+1, count-insert);
            }
            if (i == defaultCase)
                defaultCase = insert;
            else
                cases[insert]  = switchIH.cases[i];
            sorted[insert] = switchIH.successors[i];
            count++;
        }
        InstructionHeader endBlock = switchIH.outer.endBlock;
        ih = sorted[count-1];
        if (ih.outer == switchIH.outer) {
        EndSearch:
            while (ih != null) {
                Enumeration enum = ih.getPredecessors().elements();
                while (enum.hasMoreElements()) {
                    InstructionHeader pred = 
                        (InstructionHeader)enum.nextElement();
                    if (pred.addr < sorted[count-1].addr &&
                        (pred.flowType == ih.GOTO ||
                         (pred.flowType == ih.IFGOTO && 
                          pred.successors[1] == ih))) {
                        endBlock = ih;
                        break EndSearch;
                    }
                }
//                 if (ih.flowType == ih.GOTO) {
//                     /* XXX: while loops in default part versus 
//                      * while loops after switches 
//                      */
//                     endBlock = ih.successors[0];
//                     break EndSearch;
//                 }
                ih = ih.nextInstruction;
            }
        } else
            endBlock = ih;

        if(Decompiler.isVerbose)
            System.err.print("s");
        return new SwitchInstructionHeader
            (switchIH, cases, sorted, defaultCase, endBlock);
    }
}


