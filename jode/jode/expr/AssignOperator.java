package jode;
import sun.tools.java.Type;

public class AssignOperator extends Operator {
    StoreInstruction store;

    public AssignOperator(int op, StoreInstruction store) {
        super(store.getLValueType(), op);
        this.store = store;
    }

    public int getPriority() {
	return store.getPriority();
    }
    
    public int getOperandCount() {
        return store.getOperandCount();
    }

    public int getOperandPriority(int i) {
        return store.getOperandPriority(i);
    }

    public Type getOperandType(int i) {
        return store.getOperandType(i);
    }

    /**
     * Sets the return type of this operator.
     * @return true if the operand types changed
     */
    public boolean setType(Type type) {
        boolean result = store.setLValueType(type);
        super.setType(store.getLValueType());
        return result;
    }

    /**
     * Overload this method if the resulting type depends on the input types
     */
    public void setOperandType(Type[] inputTypes) {
        store.setOperandType(inputTypes);
        this.type = store.getLValueType();
    }
    
    public String toString(String[] operands) {
        return store.toString(operands);
    }
}
