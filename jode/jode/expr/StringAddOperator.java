package jode;
import sun.tools.java.Type;

public class StringAddOperator extends SimpleOperator {
    protected Type operandType;

    public StringAddOperator() {
        super(MyType.tString, ADD_OP, 2);
        operandTypes[1] = MyType.tUnknown;
    }
    
    public int getPriority() {
        return 610;
    }

    public int getOperandPriority(int i) {
        return 610 + i;
    }

    public boolean equals(Object o) {
	return (o instanceof StringAddOperator);
    }

    public String toString(String[] operands) {
        return operands[0] + getOperatorString() + operands[1];
    }
}
