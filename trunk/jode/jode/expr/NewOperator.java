package jode;
import sun.tools.java.Type;

public class NewOperator extends NoArgOperator {
    String typeString;

    public NewOperator(Type type, String typeString) {
        super(type);
        this.typeString = typeString;
    }

    public int getPriority() {
        return 950;
    }

    public String toString(String[] operands) {
        return "new "+typeString;
    }
}
