package jode;
import sun.tools.java.Type;

public class CompareUnaryOperator extends SimpleOperator {
    public CompareUnaryOperator(int addr, int length, Type type, int op) {
        super(addr,length, Type.tBoolean, op, 1);
        operandTypes[0] = type;
    }

    public int getPriority() {
        switch (getOperator()) {
        case 26:
        case 27:
            return 500;
        case 28:
        case 29:
        case 30:
        case 31: 
            return 550;
        }
        throw new RuntimeException("Illegal operator");
    }

    public int getOperandPriority(int i) {
        return getPriority();
    }

    public String toString(CodeAnalyzer ca, String[] operands) {
        if (operandTypes[0] == Type.tBoolean) {
            if (operator == 26)  /* xx == false */
                return "! ("+operands[0]+")"; /*XXX Make operators */
            else if (operator == 27) /* xx != false */
                return operands[0];
        }
        return operands[0] + " "+opString[operator]+" "+
            (UnknownType.isOfType(operandTypes[0], 
                                  UnknownType.tObject)?"null":"0");
    }
}
