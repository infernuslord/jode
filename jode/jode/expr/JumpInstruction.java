package jode;
import sun.tools.java.Type;

public abstract class JumpInstruction extends Operator {
    public JumpInstruction(int addr, int length) {
        super(addr, length, Type.tVoid, 0);
    }

    public int getPriority() {
        return 0;
    }
}
