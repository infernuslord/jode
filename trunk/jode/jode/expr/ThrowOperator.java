package jode;
import sun.tools.java.Type;

public class ThrowOperator extends ReturnOperator {

    public ThrowOperator() {
        super(MyType.tUObject);
    }

    public String toString(String[] operands) {
        return "throw " + operands[0];
    }
}
