package jode;
import sun.tools.java.Type;

public class StoreOperator extends StoreInstruction {
    LocalVariable slot;

    public StoreOperator(int addr, int length, Type type, 
                       LocalVariable slot, int operator) {
        super(addr,length, 
              UnknownType.commonType(type,slot.getType(addr+length)), 
              operator);
        this.slot = slot;
    }

    public LocalVariable getSlot() {
        return slot;
    }

    public boolean matches(Operator loadop) {
        return loadop instanceof LoadOperator && 
            ((LoadOperator)loadop).getSlot() == slot;
    }

    public int getLValueOperandCount() {
        return 0;
    }

    public int getLValueOperandPriority(int i) {
        /* shouldn't be called */
        throw new RuntimeException("StoreOperator has no operands");
    }

    public Type getLValueOperandType(int i) {
        /* shouldn't be called */
        throw new RuntimeException("StoreOperator has no operands");
    }

    public void setLValueOperandType(Type []t) {
        /* shouldn't be called */
        throw new RuntimeException("StoreOperator has no operands");
    }

    public String getLValueString(CodeAnalyzer ca, String[] operands) {
        return slot.getName(getAddr()+getLength()).toString();
    }
}
