package jode;
import sun.tools.java.Type;

public abstract class SimpleOperator extends Operator {
    protected Type[] operandTypes;

    public SimpleOperator(Type type, int operator, 
                          int operandCount) {
        super(type, operator);
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
            if (MyType.intersection(operandTypes[i], t[i]) == Type.tError)
                System.err.println("Error: "+operandTypes[i]+","+t[i]);
            operandTypes[i] = MyType.intersection(operandTypes[i], t[i]);
        }
    }
}
