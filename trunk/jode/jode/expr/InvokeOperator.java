package jode;
import sun.tools.java.*;

public class InvokeOperator extends Operator {
    CodeAnalyzer codeAnalyzer;
    boolean staticFlag;
    boolean specialFlag;
    FieldDefinition field;

    public InvokeOperator(CodeAnalyzer codeAnalyzer,
                          boolean staticFlag, boolean specialFlag, 
                          FieldDefinition field) {
        super(field.getType().getReturnType(), 0);
        this.codeAnalyzer  = codeAnalyzer;
        this.staticFlag = staticFlag;
        this.specialFlag = specialFlag;
        this.field = field;
    }

    public boolean isStatic() {
        return staticFlag;
    }

    public FieldDefinition getField() {
        return field;
    }

    public Type getClassType() {
        return field.getClassDeclaration().getType();
    }

    public int getPriority() {
        return 950;
    }

    public int getOperandCount() {
        return (staticFlag?0:1) + field.getType().getArgumentTypes().length;
    }

    public int getOperandPriority(int i) {
        if (!staticFlag && i == 0)
            return 950;
        return 0;
    }

    public Type getOperandType(int i) {
        if (!staticFlag) {
            if (i == 0)
                return MyType.tSubType(field.getClassDeclaration().getType());
            i--;
        }
        return MyType.tSubType(field.getType().getArgumentTypes()[i]);
    }

    public void setOperandType(Type types[]) {
    }

    public boolean isConstructor() {
        return field.isConstructor();
    }

    public String toString(String[] operands) {
        String object;
        int arg = 0;
        if (staticFlag) {
            if (field.getClassDefinition() == codeAnalyzer.getClassDefinition())
                object = "";
            else
                object = codeAnalyzer.
                    getTypeString(field.getClassDeclaration().getType());
        } else {
            if (operands[arg].equals("this")) {
                if (specialFlag && 
                    (field.getClassDeclaration() == 
                     codeAnalyzer.getClassDefinition().getSuperClass() ||
                     (field.getClassDeclaration().getName() == 
                      Constants.idJavaLangObject &&
                      codeAnalyzer.getClassDefinition().getSuperClass() == null)))
                    object = "super";
                else if (specialFlag)
                    object = "(("+codeAnalyzer.getTypeString
                        (field.getClassDeclaration().getType())+
                        ") this)";
                else
                    object = "";
            } else {
                if (specialFlag)
                    object = "(("+codeAnalyzer.getTypeString
                        (field.getClassDeclaration().getType())+
                        ") "+operands[arg]+")";
                else
                    object = operands[arg];
            }
            arg++;
        }
        String method;
        if (isConstructor()) {
            if (object.length() == 0)
                method = "this";
            else
                method = object;
        } else {
            if (object.length() == 0)
                method = field.getName().toString();
            else
                method = object+"."+field.getName().toString();
        }
        StringBuffer params = new StringBuffer();
        for (int i=0; i < field.getType().getArgumentTypes().length; i++) {
            if (i>0)
                params.append(", ");
            params.append(operands[arg++]);
        }
        return method+"("+params+")";
    }

    public boolean equals(Object o) {
	return (o instanceof InvokeOperator) &&
	    ((InvokeOperator)o).field == field &&
	    ((InvokeOperator)o).staticFlag == staticFlag &&
	    ((InvokeOperator)o).specialFlag == specialFlag;
    }
}
