package jode;
import sun.tools.java.Type;

public abstract class StoreInstruction extends Operator {

    public String lvCasts;
    Type lvalueType;

    public StoreInstruction(int addr, int length, Type type) {
        this (addr,length, type, ASSIGN_OP);
    }

    public StoreInstruction(int addr, int length, Type type, int operator) {
        super(addr,length, Type.tVoid, operator);
        lvalueType = type;
        lvCasts = lvalueType.toString();
    }

    public Type getLValueType() {
        return lvalueType;
    }

    public abstract boolean matches(Operator loadop);
    public abstract int getLValueOperandCount();
    public abstract int getLValueOperandPriority(int i);
    public abstract Type getLValueOperandType(int i);
    public abstract void setLValueOperandType(Type [] t);

    /**
     * Sets the type of the lvalue (and rvalue).
     * @return true if the operand types changed
     */
    public boolean setLValueType(Type type) {
        if (!UnknownType.isOfType(type, this.lvalueType)) {
            lvCasts = type.toString()+"/*invalid*/ <- " + lvCasts;
        } else if (type != this.lvalueType) {
            lvCasts = type.toString()+" <- " + lvCasts;
        }
        this.lvalueType = type;
        return false;
    }

    public abstract String getLValueString(CodeAnalyzer ca, String[] operands);

    public int getPriority() {
        return 100;
    }

    public int getOperandPriority(int i) {
        if (i == getLValueOperandCount())
            return 100;
        else
            return getLValueOperandPriority(i);
    }

    public Type getOperandType(int i) {
        if (i == getLValueOperandCount())
            return getLValueType();
        else
            return getLValueOperandType(i);
    }

    public void setOperandType(Type[] t) {
        if (getLValueOperandCount() > 0)
            setLValueOperandType(t);
        setLValueType
            (UnknownType.commonType(lvalueType, t[getLValueOperandCount()]));
    }

    public int getOperandCount() {
        return 1 + getLValueOperandCount();
    }

    public String toString(CodeAnalyzer ca, String[] operands)
    {
        return "{"+lvCasts+" "+getLValueString(ca, operands) + "} "+
            getOperatorString() +" "+
            operands[getLValueOperandCount()];
    }
}
