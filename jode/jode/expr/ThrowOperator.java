package jode;
import sun.tools.java.Type;

public class ThrowOperator extends ReturnOperator {

    public ThrowOperator(int addr, int length) {
        super(addr,length, UnknownType.tUObject);
    }

    public String toString(CodeAnalyzer ca, String[] operands) {
        return "throw " + operands[0];
    }
}
