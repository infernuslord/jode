package jode;
import sun.tools.java.Type;

public class ConvertOperator extends Operator {
    Type from;

    public ConvertOperator(int addr, int length, Type from, Type to) {
        super(addr,length, to, 0);
        this.from = from;
    }
    
    public int getPriority() {
        return 700;
    }

    public int getOperandPriority(int i) {
        return 700;
    }

    public int getOperandCount() {
        return 1;
    }

    public Type getOperandType(int i) {
        return from;
    }

    public void setOperandType(Type[] inputTypes) {
        from = UnknownType.commonType(from, inputTypes[0]);
    }

    public String toString(CodeAnalyzer ca, String[] operands)
    {
        return "("+ca.getTypeString(type)+") "+operands[0];
    }
}
