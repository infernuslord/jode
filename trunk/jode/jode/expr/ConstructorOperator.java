package jode;
import sun.tools.java.*;

public class ConstructorOperator extends Operator {
    FieldDefinition field;

    public ConstructorOperator(int addr, int length, Type type,
                               FieldDefinition field) {
        super(addr,length, type, 0);
        this.field = field;
    }

    public int getPriority() {
        return 950;
    }

    public int getOperandCount() {
        return 1 + field.getType().getArgumentTypes().length;
    }

    public int getOperandPriority(int i) {
        if (i == 0)
            return 950;
        return 0;
    }

    public Type getOperandType(int i) {
        if (i == 0)
            return type;
        return field.getType().getArgumentTypes()[i-1];
    }

    public void setOperandType(Type types[]) {
    }

    public String toString(CodeAnalyzer ca, String[] operands) {
        StringBuffer result = new StringBuffer(operands[0]).append("(");
        for (int i=0; i < field.getType().getArgumentTypes().length; i++) {
            if (i>0)
                result.append(", ");
            result.append(operands[i+1]);
        }
        return result.append(")").toString();
    }
}
