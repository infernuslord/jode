package jode;
import java.util.Enumeration;

public class CreateDoWhileStatements extends FlowTransformation
implements Transformation {

    public InstructionHeader transform(InstructionHeader head) {

        if (head.predecessors.size() == 0 ||
            head.flowType == head.DOWHILESTATEMENT)
            return null;

        InstructionHeader end = head;
        Enumeration enum = head.predecessors.elements();
        while (enum.hasMoreElements()) {
            InstructionHeader pre = (InstructionHeader) enum.nextElement();
            if (pre.outer == head.outer && pre.addr > end.addr)
                end = pre;
        }

        if (end != head)
            if (end.flowType == end.IFGOTO || end.flowType == end.GOTO) {
                if(Decompiler.isVerbose)
                    System.err.print("d");
                return new DoWhileInstructionHeader(head, end);
            }
        return null;
    }
}
