package jode;
import sun.tools.java.Type;

/**
 * ShiftOpcodes are special, because their second operand is an UIndex
 */
public class ShiftOperator extends BinaryOperator {
    protected Type shiftType;

    public ShiftOperator(int addr, int length, Type type, int op) {
        super(addr,length, type, op);
        shiftType = UnknownType.tUIndex;
    }

    public Type getOperandType(int i) {
        return (i==0)?operandType:shiftType;
    }

    public void setOperandType(Type[] inputTypes) {
        operandType = UnknownType.commonType(operandType, inputTypes[0]);
        shiftType   = UnknownType.commonType(shiftType, inputTypes[1]);
    }
}
