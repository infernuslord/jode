package jode;
import sun.tools.java.Type;

public class CheckCastOperator extends SimpleOperator {
    String typeString;

    public CheckCastOperator(Type type, String typeString) {
        super(type, 0, 1);
        this.typeString = typeString;
        operandTypes[0] = MyType.tSuperType(type);
    }

    public int getPriority() {
        return 700;
    }

    public int getOperandPriority(int i) {
        return getPriority();
    }

    public String toString(String[] operands) {
        return "(" + typeString + ")" + operands[0];
    }
}
