package jode;
import sun.tools.java.Type;

public class ReturnOperator extends SimpleOperator {
    public ReturnOperator(int addr, int length, Type type) {
        super(addr,length, Type.tVoid, 0, (type == Type.tVoid)?0:1);
        if (type != Type.tVoid)
            operandTypes[0] = type;
    }

    public int[] getSuccessors() {
        return new int[0];
    }

    public int getPriority() {
        return 0;
    }

    public int getOperandPriority(int i) {
        return 0;
    }

    public String toString(CodeAnalyzer ca, String[] operands) {
        StringBuffer result = new StringBuffer("return");
        if (getOperandCount() != 0)
            result.append(" ").append(operands[0]);
        return result.toString();
    }
}
