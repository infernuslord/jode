package jode;
import sun.tools.java.Type;

/**
 * ShiftOpcodes are special, because their second operand is an UIndex
 */
public class ShiftOperator extends BinaryOperator {
    protected Type shiftType;

    public ShiftOperator(Type type, int op) {
        super(type, op);
        shiftType = MyType.tUIndex;
    }

    public Type getOperandType(int i) {
        return (i==0)?operandType:shiftType;
    }

    public void setOperandType(Type[] inputTypes) {
        operandType = MyType.intersection(operandType, inputTypes[0]);
        shiftType   = MyType.intersection(shiftType, inputTypes[1]);
    }
}
