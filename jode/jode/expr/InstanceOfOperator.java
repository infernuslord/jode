package jode;
import sun.tools.java.Type;

public class InstanceOfOperator extends SimpleOperator {
    String typeString;

    public InstanceOfOperator(Type type, String typeString) {
        super(Type.tBoolean, 0, 1);
        this.operandTypes[0] = MyType.tSuperType(type);
        this.typeString = typeString;
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

    public String toString(String[] operands) {
        return operands[0] + " instanceof "+typeString;
    }
}
