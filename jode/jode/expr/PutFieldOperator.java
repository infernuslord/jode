package jode;
import sun.tools.java.*;

public class PutFieldOperator extends StoreInstruction {
    boolean staticFlag;
    FieldDefinition field;

    public PutFieldOperator(int addr, int length, boolean staticFlag,
                          FieldDefinition field) {
        super(addr, length, field.getType());
        this.staticFlag = staticFlag;
        this.field = field;
    }

    public boolean matches(Operator loadop) {
        return loadop instanceof GetFieldOperator &&
            ((GetFieldOperator)loadop).field == field;
    }

    public int getLValueOperandCount() {
        return staticFlag?0:1;
    }

    public int getLValueOperandPriority(int i) {
        if (staticFlag) {
            /* shouldn't be called */
            throw new RuntimeException("Field is static");
        }
        return 900;
    }

    public Type getLValueOperandType(int i) {
        if (staticFlag) {
            /* shouldn't be called */
            throw new AssertError("Field is static");
        }
        return field.getClassDefinition().getType();
    }

    public void setLValueOperandType(Type[] t) {
        if (staticFlag) {
            /* shouldn't be called */
            throw new AssertError("Field is static");
        }
        return;
    }

    public String getLValueString(CodeAnalyzer ca, String[] operands) {
        String object;
        if (staticFlag) {
            if (field.getClassDefinition() == ca.getClassDefinition())
                return field.getName().toString();
            object = 
                ca.getTypeString(field.getClassDeclaration().getType())+"."; 
        } else {
            if (operands[0].equals("this"))
                return field.getName().toString();
            object = operands[0];
        }
        return object + "." + field.getName();
    }
}
