package jode;
import sun.tools.java.Type;

public abstract class Instruction {
    protected Type type;

    Instruction(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public Instruction simplify() {
        return this;
    }

    public abstract String toString();
}
