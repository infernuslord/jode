package jode;
import sun.tools.java.*;

public class GetFieldOperator extends Operator {
    boolean staticFlag;
    FieldDefinition field;
    CodeAnalyzer codeAnalyzer;

    public GetFieldOperator(CodeAnalyzer codeAnalyzer, boolean staticFlag, 
                            FieldDefinition field) {
        super(field.getType(), 0);
        this.codeAnalyzer = codeAnalyzer;
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
        return MyType.tSubType(field.getClassDeclaration().getType());
    }

    public void setOperandType(Type types[]) {
    }

    public String toString(String[] operands) {
        String object;
        if (staticFlag) {
            if (field.getClassDefinition() == codeAnalyzer.getClassDefinition())
                return field.getName().toString();
            object = 
                codeAnalyzer.getTypeString(field.getClassDeclaration().getType()); 
        } else {
            if (operands[0].equals("this"))
                return field.getName().toString();
            object = operands[0];
        }
        return object + "." + field.getName();
    }

    public boolean equals(Object o) {
	return (o instanceof GetFieldOperator) &&
	    ((GetFieldOperator)o).field == field;
    }
}
