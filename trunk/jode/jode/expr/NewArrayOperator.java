package jode;
import sun.tools.java.Type;

public class NewArrayOperator extends SimpleOperator {
    String baseTypeString;

    public NewArrayOperator(Type arrayType, String baseTypeString, 
                            int dimensions) {
        super(arrayType, 0, dimensions);
        for (int i=0; i< dimensions; i++) {
            operandTypes[i] = MyType.tUIndex;
        }
        this.baseTypeString = baseTypeString;
    }

    public int getPriority() {
        return 900;
    }

    public int getOperandPriority(int i) {
        return 0;
    }

    public String toString(String[] operands) {
        StringBuffer result 
            = new StringBuffer("new ").append(baseTypeString);
        for (int i=0; i< getOperandCount(); i++) {
            result.append("[").append(operands[i]).append("]");
        }
        return result.toString();
    }
}
