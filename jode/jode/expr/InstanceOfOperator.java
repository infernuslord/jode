package jode;
import sun.tools.java.Type;

public class InstanceOfOperator extends SimpleOperator {
    Type classType;

    public InstanceOfOperator(int addr, int length, Type type) {
        super(addr, length, Type.tBoolean, 0, 1);
        this.operandTypes[0] = UnknownType.tSubClass(type);
        this.classType = type;
    }
    public int getOperandCount() {
        return 1;
    }

    public int getPriority() {
        return 550;
    }

    public int getOperandPriority(int i) {
        return getPriority();
    }

    public String toString(CodeAnalyzer ca, String[] operands) {
        return operands[0] + " instanceof "+ca.getTypeString(classType);
    }
}
