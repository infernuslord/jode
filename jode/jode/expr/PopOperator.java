package jode;
import sun.tools.java.Type;

public class PopOperator extends SimpleOperator {
    int count;

    public PopOperator(int a, int l, int count) {
        super(a,l, Type.tVoid, 0, 1);
        operandTypes[0] = UnknownType.tUnknown;
        this.count = count;
    }

    public int getPriority() {
        return 0;
    }

    public int getOperandPriority(int i) {
        return 0;
    }

    public String toString(CodeAnalyzer ca, String[] operands) {
        return operands[0];
    }
}
