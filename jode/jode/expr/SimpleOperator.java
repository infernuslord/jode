package jode;
import sun.tools.java.Type;

public abstract class SimpleOperator extends Operator {
    protected Type[] operandTypes;

    public SimpleOperator(int addr, int length, Type type, int operator, 
                          int operandCount) {
        super(addr, length, type, operator);
        operandTypes = new Type[operandCount];
        for (int i=0; i< operandCount; i++) {
            operandTypes[i] = type;
        }
    }

    public int getOperandCount() {
        return operandTypes.length;
    }

    public Type getOperandType(int i) {
        return operandTypes[i];
    }

    public void setOperandType(Type[] t) {
        for (int i=0; i< operandTypes.length; i++) {
            if (UnknownType.commonType(operandTypes[i], t[i]) == Type.tError)
                System.err.println("Error: "+operandTypes[i]+","+t[i]);
            operandTypes[i] = UnknownType.commonType(operandTypes[i], t[i]);
        }
    }
}
