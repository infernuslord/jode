package jode;
import sun.tools.java.Type;

public class EmptyStringOperator extends NoArgOperator {

    public EmptyStringOperator() {
        super(Type.tString, 0);
    }

    public int getPriority() {
        return 1000;
    }

    public boolean equals(Object o) {
	return (o instanceof EmptyStringOperator);
    }

    public String toString(String[] operands) {
        return "\"\"";
    }
}

