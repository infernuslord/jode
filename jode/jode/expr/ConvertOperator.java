package jode;
import sun.tools.java.Type;

public class ConvertOperator extends Operator {
    Type from;

    public ConvertOperator(Type from, Type to) {
        super(to, 0);
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
        from = MyType.intersection(from, inputTypes[0]);
    }

    public String toString(String[] operands)
    {
        return "("+type.toString()+") "+operands[0];
    }
}
