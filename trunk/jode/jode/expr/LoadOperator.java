package jode;
import sun.tools.java.Type;

public class LoadOperator extends ConstOperator {
    LocalVariable slot;

    public LoadOperator(int addr, int length, Type type, LocalVariable slot) {
        super(addr,length, 
              UnknownType.commonType(type,slot.getType(addr)), "");
        this.slot = slot;
    }

    public LocalVariable getSlot() {
        return slot;
    }

    public String toString(CodeAnalyzer ca, String[] operands) {
        return slot.getName(getAddr()).toString();
    }

    public boolean equals(Object o) {
        return (o instanceof LoadOperator &&
                ((LoadOperator) o).slot == slot);
    }
}
