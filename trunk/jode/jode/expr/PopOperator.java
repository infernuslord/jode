package jode;
import sun.tools.java.Type;

public class PopOperator extends SimpleOperator {
    int count;

    public PopOperator(int count) {
        super(Type.tVoid, 0, 1);
        operandTypes[0] = MyType.tUnknown;
        this.count = count;
    }

    public int getPriority() {
        return 0;
    }

    public int getOperandPriority(int i) {
        return 0;
    }

    public String toString(String[] operands) {
        return operands[0];
    }
}
