package jode;

public class SwapOperator extends Instruction {
    public SwapOperator(int a, int l) {
        super(a,l);
    }

    public void dumpSource(TabbedPrintWriter writer, CodeAnalyzer ca) 
         throws java.io.IOException
    {
        writer.println("swap;");
    }
}
