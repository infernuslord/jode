package jode;
import sun.tools.java.Type;

public class ConstantArrayOperator extends SimpleOperator {

    public ConstantArrayOperator(Type type, int size) {
        super(type, 0, size);
        for (int i=0; i< size; i++)
            operandTypes[i] = MyType.tSubType(type.getElementType());
    }

    public int getPriority() {
        return 200;
    }

    public int getOperandPriority(int i) {
        return 0;
    }

    public String toString(String[] operands) {
        StringBuffer result 
            = new StringBuffer("{");
        for (int i=0; i< getOperandCount(); i++) {
            if (i>0)
                result.append(", ");
            result.append(operands[i]);
        }
        return result.append("}").toString();
    }
}
