package jode;
import sun.tools.java.Type;

public class CompareUnaryOperator extends SimpleOperator {
    boolean objectType;

    public CompareUnaryOperator(Type type, int op) {
        super(Type.tBoolean, op, 1);
        operandTypes[0] = type;
        objectType = (type == MyType.tUObject);
    }

    public int getPriority() {
        switch (getOperator()) {
        case 26:
        case 27:
            return 500;
        case 28:
        case 29:
        case 30:
        case 31: 
            return 550;
        }
        throw new RuntimeException("Illegal operator");
    }

    public int getOperandPriority(int i) {
        return getPriority();
    }

    public boolean equals(Object o) {
	return (o instanceof CompareUnaryOperator) &&
	    ((CompareUnaryOperator)o).operator == operator;
    }

    public String toString(String[] operands) {
        return operands[0] + getOperatorString() + (objectType?"null":"0");
    }
}
