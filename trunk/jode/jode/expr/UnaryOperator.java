package jode;
import sun.tools.java.Type;

public class UnaryOperator extends SimpleOperator {
    public UnaryOperator(Type type, int op) {
        super(type, op, 1);
    }
    
    public int getPriority() {
        return 700;
    }

    public int getOperandPriority(int i) {
        return getPriority();
    }

    /**
     * Sets the return type of this operator.
     * @return true if the operand types changed
     */
    public boolean setType(Type type) {
        super.setType(type);
        Type newOpType = MyType.intersection(type, operandTypes[0]);
        if (newOpType != operandTypes[0]) {
            operandTypes[0] = newOpType;
            return true;
        }
        return false;
    }

    public boolean equals(Object o) {
	return (o instanceof UnaryOperator) &&
	    ((UnaryOperator)o).operator == operator;
    }

    public String toString(String[] operands) {
        return getOperatorString() + operands[0];
    }
}
