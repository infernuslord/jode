package jode;
import sun.tools.java.Type;

public class NewConstructorOperator extends NoArgOperator {
    Expression constructor;

    public NewConstructorOperator(Type type, Expression expr) {
        super(type);
        this.constructor = expr;
    }

    public int getPriority() {
        return 950;
    }

    public String toString(CodeAnalyzer ca, String[] operands) {
        return "new "+constructor.toString(ca, 0);
    }
}
