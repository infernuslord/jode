package jode;
import sun.tools.java.Type;

public class MonitorExitOperator extends SimpleOperator {
    public MonitorExitOperator() {
        super(Type.tVoid, 0, 1);
        operandTypes[0] = Type.tObject;
    }

    public int getPriority() {
        return 0;
    }

    public int getOperandPriority(int i) {
        return 0;
    }

    public Type getOperandType(int i) {
        return MyType.tObject;
    }

    public String toString(String[] operands) {
        return "monitorexit "+operands[0];
    }
}
