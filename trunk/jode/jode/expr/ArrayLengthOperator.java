package jode;
import sun.tools.java.*;

public class ArrayLengthOperator extends Operator {

    Type arrayType;

    public ArrayLengthOperator() {
        super(Type.tInt, 0);
        arrayType = Type.tArray(MyType.tUnknown);
    }

    public int getPriority() {
        return 950;
    }

    public int getOperandCount() {
        return 1;
    }

    public int getOperandPriority(int i) {
        return 900;
    }

    public Type getOperandType(int i) {
        return arrayType;
    }

    public void setOperandType(Type[] types) {
        arrayType = MyType.intersection(arrayType,types[0]);
    }

    public String toString(String[] operands) {
        return operands[0] + ".length";
    }
}
