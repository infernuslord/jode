package jode;
import sun.tools.java.Type;

public class CompareBinaryOperator extends SimpleOperator {
    public CompareBinaryOperator(int addr, int length, Type type, int op) {
        super(addr,length, Type.tBoolean, op, 2);
        operandTypes[0] = operandTypes[1] = type;
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
        return getPriority()+i;
    }

    public void setOperandType(Type[] inputTypes) {
        super.setOperandType(inputTypes);
        Type operandType = 
            UnknownType.commonType(operandTypes[0],operandTypes[1]);
        operandTypes[0] = operandTypes[1] = operandType;
    }

    public String toString(CodeAnalyzer ca, String[] operands) {
        return operands[0] + " "+opString[operator]+" "+operands[1];
    }
}
