package jode;
import sun.tools.java.Type;

public class NewArrayOperator extends SimpleOperator {
    Type baseType;

    public NewArrayOperator(int addr, int length, 
                          Type baseType, int dimensions) {
        super(addr, length, baseType, 0, dimensions);
        this.baseType = baseType;
        for (int i=0; i< dimensions; i++) {
            this.type = Type.tArray(this.type);
            operandTypes[i] = UnknownType.tUIndex;
        }
    }

    public NewArrayOperator(int addr, int length, Type baseType) {
        this(addr, length, baseType, 1);
    }

    public int getPriority() {
        return 900;
    }

    public int getOperandPriority(int i) {
        return 0;
    }

    public String toString(CodeAnalyzer ca, String[] operands) {
        StringBuffer result 
            = new StringBuffer("new ").append(ca.getTypeString(baseType));
        for (int i=0; i< getOperandCount(); i++) {
            result.append("[").append(operands[i]).append("]");
        }
        return result.toString();
    }
}
