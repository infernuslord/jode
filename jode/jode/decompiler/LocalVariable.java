package jode;
import sun.tools.java.Type;
import sun.tools.java.Identifier;

public interface LocalVariable {
    public Identifier getName(int addr);
    public Type getType(int addr);
    public Type setType(int addr, Type type);
    public void combine(int addr1, int addr2);
}
