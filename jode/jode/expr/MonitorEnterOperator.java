package jode;
import sun.tools.java.Type;

public class MonitorEnterOperator extends SimpleOperator {
    public MonitorEnterOperator() {
        super(Type.tVoid, 0, 1);
        operandTypes[0] = Type.tObject;
    }

    public int getPriority() {
        return 0;
    }

    public int getOperandPriority(int i) {
        return 0;
    }

    public String toString(String[] operands) {
        return "monitorenter "+operands[0];
    }
}
