package jode;

public class NopOperator extends Instruction {
    public NopOperator(int a, int l) {
        super(a,l);
    }

    public void dumpSource(TabbedPrintWriter tpw, CodeAnalyzer ca) 
         throws java.io.IOException
    {
        tpw.println("nop;");
    }
}
