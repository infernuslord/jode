package jode;

public abstract class Instruction {
    int addr,length;

    Instruction(int a, int l) {
        addr = a;
        length = l;
    }

    public int getAddr() {
        return addr;
    }

    public void setAddr(int addr) {
        this.addr = addr;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int[] getSuccessors() {
        int[] result = { addr + length };
        return result;
    }

    public abstract void dumpSource(TabbedPrintWriter tpw, CodeAnalyzer ca)
         throws java.io.IOException;
}
