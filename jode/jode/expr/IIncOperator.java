package jode;
import sun.tools.java.Type;

public class IIncOperator extends NoArgOperator 
implements LocalVarOperator {
    int slot;
    String value;
    LocalInfo local;

    public IIncOperator(int slot, String value, int operator) {
        super(MyType.tVoid, operator);
        this.slot = slot;
	this.value = value;
    }

    public String getValue() {
	return value;
    }

    public boolean isRead() {
        return true;
    }

    public boolean isWrite() {
        return true;
    }

    public void setLocalInfo(LocalInfo local) {
        local.setType(MyType.tUIndex);
	this.local = local;
    }

    public LocalInfo getLocalInfo() {
	return local;
    }

    public int getSlot() {
        return slot;
    }

    public int getPriority() {
        return 100;
    }

    public boolean matches(Operator loadop) {
        return loadop instanceof LocalLoadOperator && 
            ((LocalLoadOperator)loadop).getLocalInfo().getLocalInfo()
            == local.getLocalInfo();
    }

    public String toString(String[] operands) {
        return local.getName().toString() + 
	    getOperatorString() + value;
    }
}
