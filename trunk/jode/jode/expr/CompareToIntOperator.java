package jode;
import sun.tools.java.Type;

public class CompareToIntOperator extends SimpleOperator {
    public CompareToIntOperator(Type type, int lessGreater) {
        super(Type.tInt, 0, 2);
        operandTypes[0] = operandTypes[1] = type;
    }

    public int getPriority() {
        return 499;
    }

    public int getOperandPriority(int i) {
        return 550;
    }

    public void setOperandType(Type[] inputTypes) {
        super.setOperandType(inputTypes);
        Type operandType = 
            MyType.intersection(operandTypes[0],operandTypes[1]);
        operandTypes[0] = operandTypes[1] = operandType;
    }

    public boolean equals(Object o) {
	return (o instanceof CompareToIntOperator);
    }

    public String toString(String[] operands) {
        return operands[0] + " <=> " + operands[1];
    }
}
