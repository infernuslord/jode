package jode;
import sun.tools.java.Type;

public class NewConstructorOperator extends NoArgOperator {
    Expression constructor;

    public NewConstructorOperator(int addr, int length, Type type, 
                                  Expression expr) {
        super(addr,length, type);
        this.constructor = expr;
    }

    public int getPriority() {
        return 950;
    }

    public String toString(CodeAnalyzer ca, String[] operands) {
        return "new "+constructor.toString(ca, 0);
    }
}
