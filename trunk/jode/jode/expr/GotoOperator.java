package jode;
import sun.tools.java.Type;

public class GotoOperator extends JumpInstruction {
    protected int destination;

    public GotoOperator(int addr, int length, int dest) {
        super(addr,length);
        this.destination = dest;
    }

    public int getDestination() {
        return destination;
    }

    public int getOperandCount() {
        return 0;
    }

    public int getOperandPriority(int i) {
        throw new AssertError("This operator has no operands");
    }

    public Type getOperandType(int i) {
        throw new AssertError("This operator has no operands");
    }

    public void setOperandType(Type types[]) {
        throw new AssertError("This operator has no operands");
    }

    public int[] getSuccessors() {
        int [] result = { destination };
        return result;
    }

    public String toString(CodeAnalyzer ca, String[] operands) {
        return "goto addr_" + destination;
    }
}
