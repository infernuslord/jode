package jode;
import sun.tools.java.Type;

public abstract class NoArgOperator extends Operator {

    public NoArgOperator(Type type, int operator) {
        super(type, operator);
    }

    public NoArgOperator(Type type) {
        this(type, 0);
    }

    public int getOperandCount() {
        return 0;
    }

    public int getOperandPriority(int i) {
        throw new AssertError("This operator has no operands");
    }

    public Type getOperandType(int i) {
        throw new AssertError("This operator has no operands");
    }

    public void setOperandType(Type[] types) {
        throw new AssertError("This operator has no operands");
    }
}

