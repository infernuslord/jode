package jode;
import sun.tools.java.Type;
import sun.tools.java.ArrayType;

public class ArrayStoreOperator extends StoreInstruction {
    Type indexType;

    public ArrayStoreOperator(Type type, int operator) {
        super(type, operator);
        indexType = MyType.tUIndex;
    }

    public ArrayStoreOperator(Type type) {
        this(type, ASSIGN_OP);
    }


    public boolean matches(Operator loadop) {
        return loadop instanceof ArrayLoadOperator;
    }

    public int getLValueOperandCount() {
        return 2;
    }

    public int getLValueOperandPriority(int i) {
        if (i == 0)
            return 950;
        else
            return 0;
    }

    /**
     * Sets the type of the lvalue (and rvalue).
     * @return true since the operand types changed
     */
    public boolean setLValueType(Type type) {
        this.lvalueType = type;
	System.err.println("Setting Lvalue type to "+lvalueType);
        return true;
    }

    public Type getLValueOperandType(int i) {
        if (i == 0)
            return Type.tArray(lvalueType);
        else
            return indexType;
    }

    public void setLValueOperandType(Type[] t) {
        indexType = MyType.intersection(indexType, t[1]);
        Type arrayType = 
            MyType.intersection(t[0], Type.tArray(lvalueType));
	try {
            lvalueType = arrayType.getElementType();
	} catch (sun.tools.java.CompilerError err) {
            lvalueType = Type.tError;
        }
    }

    public String getLValueString(String[] operands) {
        return operands[0]+"["+operands[1]+"]";
    }
}
