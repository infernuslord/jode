package jode;
import sun.tools.java.Type;

public class IfGotoOperator extends JumpInstruction {
    protected int destination;

    public IfGotoOperator(int addr, int length, int dest) {
        super(addr,length);
        destination = dest;
    }

    public int getDestination() {
        return destination;
    }

    public int getOperandCount() {
        return 1;
    }

    public int getOperandPriority(int i) {
        return 200;  /* force parentheses around assignments */
    }

    public Type getOperandType(int i) {
        return Type.tBoolean;
    }

    public void setOperandType(Type types[]) {
    }

    public int[] getSuccessors() {
        int [] result = { destination, getAddr() + getLength() };
        return result;
    }

    public String toString(CodeAnalyzer ca, String[] operands) {
        return "if ("+operands[0]+") goto addr_" + destination;
    }
}
