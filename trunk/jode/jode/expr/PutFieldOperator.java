package jode;
import sun.tools.java.*;

public class PutFieldOperator extends StoreInstruction {
    CodeAnalyzer codeAnalyzer;
    boolean staticFlag;
    FieldDefinition field;

    public PutFieldOperator(CodeAnalyzer codeAnalyzer, boolean staticFlag, 
                            FieldDefinition field) {
        super(field.getType(), ASSIGN_OP);
        this.codeAnalyzer = codeAnalyzer;
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
        return MyType.tSubType(field.getClassDefinition().getType());
    }

    public void setLValueOperandType(Type[] t) {
        if (staticFlag) {
            /* shouldn't be called */
            throw new AssertError("Field is static");
        }
        return;
    }

    public String getLValueString(String[] operands) {
        String object;
        if (staticFlag) {
            if (field.getClassDefinition()
                == codeAnalyzer.getClassDefinition())
                return field.getName().toString();
            object = 
                codeAnalyzer.getTypeString
                (field.getClassDeclaration().getType())+"."; 
        } else {
            if (operands[0].equals("this"))
                return field.getName().toString();
            object = operands[0];
        }
        return object + "." + field.getName();
    }

    public boolean equals(Object o) {
	return (o instanceof PutFieldOperator) &&
	    ((PutFieldOperator)o).field == field;
    }
}
