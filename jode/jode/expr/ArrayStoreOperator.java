package jode;
import sun.tools.java.Type;
import sun.tools.java.ArrayType;

public class ArrayStoreOperator extends StoreInstruction {
    Type indexType;

    public ArrayStoreOperator(int addr, int length, Type type) {
        super(addr,length, type);
        indexType = UnknownType.tUIndex;
    }

    public ArrayStoreOperator(int addr, int length, Type type, int operator) {
        super(addr,length, type, operator);
        indexType = UnknownType.tUIndex;
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

    public Type getLValueOperandType(int i) {
        if (i == 0)
            return Type.tArray(lvalueType);
        else
            return indexType;
    }

    public void setLValueOperandType(Type[] t) {
        indexType = UnknownType.commonType(indexType, t[1]);
        Type arraytype = 
            UnknownType.commonType(t[0], Type.tArray(lvalueType));
        System.err.println("lvot: "+t[0]+","+Type.tArray(lvalueType)+
                           " -> "+arraytype);
        if (arraytype instanceof ArrayType)
            lvalueType = arraytype.getElementType();
        else {
            System.err.println("no array: "+arraytype);
            lvalueType = Type.tError;
        }
    }

    public String getLValueString(CodeAnalyzer ca, String[] operands) {
        return operands[0]+"["+operands[1]+"]";
    }
}
