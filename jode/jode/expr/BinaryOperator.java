package jode;
import sun.tools.java.Type;

public class BinaryOperator extends Operator {
    protected Type operandType;

    public BinaryOperator(int addr, int length, Type type, int op) {
        super(addr,length, type, op);
        operandType = type;
    }
    
    public int getOperandCount() {
        return 2;
    }

    public int getPriority() {
        switch (operator) {
        case 1: case 2:
            return 610;
        case 3: case 4: case 5:
            return 650;
        case 6: case 7: case 8:
            return 600;
        case 9: 
            return 450;
        case 10:
            return 410;
        case 11:
            return 420;
        case 12: case 13: case 14: case 15: case 16: case 17: 
        case 18: case 19: case 20: case 21: case 22: case 23:
            return 100;
        case LOG_OR_OP:
            return 310;
        case LOG_AND_OP:
            return 350;
        }
        throw new RuntimeException("Illegal operator");
    }

    public int getOperandPriority(int i) {
        return getPriority() + i;
    }

    public Type getOperandType(int i) {
        return operandType;
    }

    public void setOperandType(Type[] inputTypes) {
        operandType = UnknownType.commonType
            (operandType, UnknownType.commonType(inputTypes[0], 
                                                 inputTypes[1]));
        type = operandType;
    }

    /**
     * Sets the return type of this operator.
     * @return true if the operand types changed
     */
    public boolean setType(Type newType) {
        operandType = UnknownType.commonType(operandType, newType);
        if (type != operandType) {
            type = operandType;
            return true;
        }
        return false;
    }

    public String toString(CodeAnalyzer ca, String[] operands) {
        return operands[0] + " "+getOperatorString()+" "+ operands[1];
    }
}
