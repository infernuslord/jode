package jode;
import java.util.Enumeration;
import java.util.Vector;

public class CreateIfThenElseOperator implements Transformation{

    public InstructionHeader createFunny(InstructionHeader ih) {
        Expression cond = null;
        try {
            InstructionHeader ifHeader= ih;
            if (ifHeader.getFlowType() != ifHeader.IFGOTO)
                return null;
            CompareUnaryOperator compare = 
                (CompareUnaryOperator) ifHeader.getInstruction();
            if ((compare.getOperator() & ~1) != compare.EQUALS_OP)
                return null;
            Enumeration enum = ih.getPredecessors().elements();
            while (enum.hasMoreElements()) {
                try {
                    ih = (InstructionHeader) enum.nextElement();
                    Expression zeroExpr = (Expression) ih.getInstruction();
                    ConstOperator zero = 
                        (ConstOperator) zeroExpr.getOperator();
                    if (!zero.getValue().equals("0"))
                        continue;

                    ih = ih.getUniquePredecessor();
                    if (ih.getFlowType() != ih.IFGOTO)
                        continue;
                    
                    if (compare.getOperator() == compare.EQUALS_OP && 
                        ih.getSuccessors()[1] != ifHeader.getSuccessors()[0]
                        || compare.getOperator() == compare.NOTEQUALS_OP &&
                        ih.getSuccessors()[1] != ifHeader.getSuccessors()[1])
                        continue;
                    
                    cond = (Expression) ih.getInstruction();
                    break;
                } catch (ClassCastException ex) {
                } catch (NullPointerException ex) {
                }
            }

            if (cond == null)
                return null;

        } catch (ClassCastException ex) {
            return null;
        } catch (NullPointerException ex) {
            return null;
        }

        InstructionHeader next = ih.nextInstruction;
        ih.successors[1].predecessors.removeElement(ih);
        next.instr        = ih.instr;
        next.movePredecessors(ih);
        return next;
    }

    public InstructionHeader create(InstructionHeader ih) {
        InstructionHeader ifHeader;
        Expression e[] = new Expression[3];
        InstructionHeader[] succs;
        try {
            Vector predec = ih.getPredecessors();

            if (predec.size() != 1)
                return null;
            ifHeader = (InstructionHeader) predec.elementAt(0);
            if (ifHeader.getFlowType() != ifHeader.IFGOTO)
                return null;
            succs = ifHeader.getSuccessors();
            if (succs[1] != ih ||
                succs[0].getNextInstruction() != succs[1] ||
                succs[0].getSuccessors().length != 1 ||
                succs[1].getSuccessors().length != 1 ||
                succs[0].getSuccessors()[0] != succs[1].getSuccessors()[0])
                return null;

            e[0] = ((Expression) ifHeader.getInstruction()).negate();
            e[1] = (Expression) succs[0].getInstruction();
            if (e[1].isVoid())
                return null;
            e[2] = (Expression) succs[1].getInstruction();
            if (e[2].isVoid())
                return null;
        } catch (ClassCastException ex) {
            return null;
        } catch (NullPointerException ex) {
            return null;
        }

        IfThenElseOperator iteo = new IfThenElseOperator
            (MyType.intersection(e[1].getType(),e[2].getType()));

        ih.instr = new Expression(iteo, e);
        ih.movePredecessors(ifHeader);
        ih.nextInstruction.predecessors.removeElement(ifHeader.successors[0]);
        return ih;
    }

    public InstructionHeader transform(InstructionHeader ih) {
        InstructionHeader next = createFunny(ih);
        if (next != null)
            return next;
        return create(ih);
    }
}
