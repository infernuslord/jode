package jode;
import sun.tools.java.Type;

public class PostFixOperator extends Operator {
    StoreInstruction store;

    public PostFixOperator(Type type, int op, StoreInstruction store) {
        super(type, op);
	this.store = store;
    }
    
    public int getPriority() {
        return 800;
    }

    public int getOperandPriority(int i) {
        return getPriority();
    }

    public Type getOperandType(int i) {
	return store.getLValueOperandType(i);
    }

    public int getOperandCount() {
        return store.getLValueOperandCount();
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

    public void setOperandType(Type[] inputTypes) {
        store.setLValueOperandType(inputTypes);
    }

    public String toString(String[] operands) {
        return store.getLValueString(operands) + getOperatorString();
    }
}
