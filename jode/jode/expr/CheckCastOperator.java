package jode;
import sun.tools.java.Type;

public class CheckCastOperator extends SimpleOperator {
    public CheckCastOperator(int addr, int length, Type type) {
        super(addr,length, type, 0, 1);
        operandTypes[0] = UnknownType.tSubClass(type);
    }

    public int getPriority() {
        return 700;
    }

    public int getOperandPriority(int i) {
        return getPriority();
    }

    public String toString(CodeAnalyzer ca, String[] operands) {
        return "("+ca.getTypeString(type) + ")" + operands[0];
    }
}
