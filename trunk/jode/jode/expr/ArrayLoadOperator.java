package jode;
import sun.tools.java.Type;
import sun.tools.java.ArrayType;

public class ArrayLoadOperator extends SimpleOperator {
    String value;

    public ArrayLoadOperator(Type type) {
        super(type, 0, 2);
        operandTypes[0] = Type.tArray(type);
        operandTypes[1] = MyType.tUIndex;
    }

    public int getPriority() {
        return 950;
    }

    public int getOperandPriority(int i) {
        return (i==0)?950:0;
    }

    /**
     * Sets the return type of this operator.
     * @return true if the operand types changed
     */
    public boolean setType(Type type) {
        if (type != this.type) {
            super.setType(type);
            operandTypes[0] = Type.tArray(type);
            return true;
        }
        return false;
    }

    public void setOperandType(Type[] t) {
        super.setOperandType(t);
	try {
            type = operandTypes[0].getElementType();
	} catch (sun.tools.java.CompilerError err) {
            System.err.println("No Array type: "+operandTypes[0]);
            type = Type.tError;
        }
    }

    public String toString(String[] operands) {
        return operands[0]+"["+operands[1]+"]";
    }
}
