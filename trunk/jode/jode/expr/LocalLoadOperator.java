package jode;
import sun.tools.java.Type;

public class LocalLoadOperator extends ConstOperator 
implements LocalVarOperator {
    int slot;
    LocalInfo local;

    public LocalLoadOperator(Type type, int slot) {
        super(type, "");
        this.slot = slot;
    }

    public boolean isRead() {
        return true;
    }

    public boolean isWrite() {
        return false;
    }

    public void setLocalInfo(LocalInfo local) {
        local.setType(type);
	this.local = local;
    }

    public LocalInfo getLocalInfo() {
	return local;
    }

    public Type getType() {
	System.err.println("LocalLoad.getType of "+local.getName()+": "+local.getType());
	return local.getType();
    }

    public boolean setType(Type type) {
	System.err.println("LocalLoad.setType of "+local.getName()+": "+local.getType());
	return super.setType(local.setType(type));
    }

    public int getSlot() {
        return slot;
    }

    public String toString(String[] operands) {
        return local.getName().toString();
    }

    public boolean equals(Object o) {
        return (o instanceof LocalLoadOperator &&
                ((LocalLoadOperator) o).slot == slot);
    }
}

