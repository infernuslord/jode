package jode;
import sun.tools.java.Type;

public class NewOperator extends NoArgOperator {

    public NewOperator(int addr, int length, Type type) {
        super(addr,length, type);
    }

    public int getPriority() {
        return 950;
    }

    public String toString(CodeAnalyzer ca, String[] operands) {
        return "new "+getType().toString();
    }
}
