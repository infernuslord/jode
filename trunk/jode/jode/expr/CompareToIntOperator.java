package jode;
import sun.tools.java.Type;

public class CompareToIntOperator extends BinaryOperator {
    public CompareToIntOperator(int addr, int length, Type type, int op) {
        super(addr,length, Type.tInt, op);
        operandType = type;
    }

    public int getPriority() {
        switch (getOperator()) {
        case 25: 
        case 26:
            return 500;
        case 27:
        case 28:
        case 29:
        case 30:
            return 550;
        }
        throw new RuntimeException("Illegal operator");
    }

    public String toString(CodeAnalyzer ca, String[] operands) {
        return operands[0] + " "+opString[operator]+" "+operands[1];
    }
}
