package jode;
import sun.tools.java.Type;

public class LocalStoreOperator extends StoreInstruction 
implements LocalVarOperator {
    int slot;
    LocalInfo local;

    public LocalStoreOperator(Type lvalueType, int slot, int operator) {
        super(lvalueType, operator);
        this.slot = slot;
    }

    public boolean isRead() {
        return operator != ASSIGN_OP;
    }

    public boolean isWrite() {
        return true;
    }

    public void setLocalInfo(LocalInfo local) {
        local.setType(lvalueType);
	this.local = local;
    }

    public LocalInfo getLocalInfo() {
	return local;
    }

    public Type getLValueType() {
// 	System.err.println("LocalStore.getType of "+local.getName()+": "+local.getType());
	return local.getType();
    }

    public boolean setLValueType(Type type) {
// 	System.err.println("LocalStore.setType of "+local.getName()+": "+local.getType());
	return super.setLValueType
	    (local.setType(MyType.tSuperType(type)));
    }

    public int getSlot() {
        return slot;
    }

    public boolean matches(Operator loadop) {
        return loadop instanceof LocalLoadOperator && 
            ((LocalLoadOperator)loadop).getLocalInfo().getLocalInfo()
            == local.getLocalInfo();
    }

    public int getLValueOperandCount() {
        return 0;
    }

    public int getLValueOperandPriority(int i) {
        /* shouldn't be called */
        throw new RuntimeException("LocalStoreOperator has no operands");
    }

    public Type getLValueOperandType(int i) {
        /* shouldn't be called */
        throw new RuntimeException("LocalStoreOperator has no operands");
    }

    public void setLValueOperandType(Type []t) {
        /* shouldn't be called */
        throw new RuntimeException("LocalStoreOperator has no operands");
    }

    public String getLValueString(String[] operands) {
        return local.getName().toString();
    }
}

