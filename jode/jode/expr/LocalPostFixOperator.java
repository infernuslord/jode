package jode;
import sun.tools.java.Type;

public class LocalPostFixOperator extends NoArgOperator {
    IIncOperator iinc;

    public LocalPostFixOperator(Type type, int op, IIncOperator iinc) {
        super(type, op);
	this.iinc = iinc;
    }
    
    public int getPriority() {
        return 800;
    }

    public String toString(String[] operands) {
        return iinc.getLocalInfo().getName() + getOperatorString();
    }
}
