package jode;
import sun.tools.java.Type;

public class PostFixOperator extends SimpleOperator {
    public PostFixOperator(int addr, int length, Type type, int op) {
        super(addr,length, type, op, 1);
    }
    
    public int getPriority() {
        return 800;
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
        Type newOpType = UnknownType.commonType(type, operandTypes[0]);
        if (newOpType != operandTypes[0]) {
            operandTypes[0] = newOpType;
            return true;
        }
        return false;
    }

    public String toString(CodeAnalyzer ca, String[] operands) {
        return operands[0] + getOperatorString();
    }
}
