package jode;
import sun.tools.java.Type;

public class CompareBinaryOperator extends SimpleOperator {
    public CompareBinaryOperator(Type type, int op) {
        super(Type.tBoolean, op, 2);
        operandTypes[0] = operandTypes[1] = type;
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
        return getPriority()+i;
    }

    public void setOperandType(Type[] inputTypes) {
        super.setOperandType(inputTypes);
        Type operandType = 
            MyType.intersection(operandTypes[0],operandTypes[1]);
        operandTypes[0] = operandTypes[1] = operandType;
    }

    public boolean equals(Object o) {
	return (o instanceof CompareBinaryOperator) &&
	    ((CompareBinaryOperator)o).operator == operator;
    }

    public String toString(String[] operands) {
        return operands[0] + getOperatorString() + operands[1];
    }
}
