package jode;
import sun.tools.java.Type;

public class ReturnOperator extends SimpleOperator {
    public ReturnOperator(Type type) {
        super(Type.tVoid, 0, (type == Type.tVoid)?0:1);
        if (type != Type.tVoid)
            operandTypes[0] = type;
    }

    public int getPriority() {
        return 0;
    }

    public int getOperandPriority(int i) {
        return 0;
    }

    public String toString(String[] operands) {
        StringBuffer result = new StringBuffer("return");
        if (getOperandCount() != 0)
            result.append(" ").append(operands[0]);
        return result.toString();
    }
}
