package jode;
import sun.tools.java.Type;
import sun.tools.java.Identifier;

public interface LocalVariable {
    public LocalInfo getInfo(int addr);
    public void combine(int addr1, int addr2);
}
