package jode;
import sun.tools.java.*;

public class GetFieldOperator extends Operator {
    boolean staticFlag;
    FieldDefinition field;

    public GetFieldOperator(int addr, int length, boolean staticFlag,
                          FieldDefinition field) {
        super(addr, length, field.getType(), 0);
        this.staticFlag = staticFlag;
        this.field = field;
    }

    public int getPriority() {
        return 950;
    }

    public int getOperandCount() {
        return staticFlag?0:1;
    }

    public int getOperandPriority(int i) {
        if (staticFlag) {
            /* shouldn't be called */
            throw new RuntimeException("Field is static");
        }
        return 900;
    }

    public Type getOperandType(int i) {
        if (staticFlag) {
            /* shouldn't be called */
            throw new RuntimeException("Field is static");
        }
        return field.getClassDeclaration().getType();
    }

    public void setOperandType(Type types[]) {
    }

    public String toString(CodeAnalyzer ca, String[] operands) {
        String object;
        if (staticFlag) {
            if (field.getClassDefinition() == ca.getClassDefinition())
                return field.getName().toString();
            object = 
                ca.getTypeString(field.getClassDeclaration().getType()); 
        } else {
            if (operands[0].equals("this"))
                return field.getName().toString();
            object = operands[0];
        }
        return object + "." + field.getName();
    }
}
