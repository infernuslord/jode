package jode;

public class RetOperator extends Instruction {
    int slot;

    public RetOperator(int addr, int length, int slot) {
        super(addr,length);
        this.slot = slot;
    }

    public int[] getSuccessors() {
        return new int[0];
    }

    public void dumpSource(TabbedPrintWriter writer, CodeAnalyzer ca) 
         throws java.io.IOException
    {
        writer.println("ret;");
    }
}
