package jode;

public class JsrOperator extends Instruction {
    int destination;

    public JsrOperator(int addr, int length, int dest) {
        super(addr,length);
        this.destination = dest;
    }

    public void dumpSource(TabbedPrintWriter writer, CodeAnalyzer ca) 
         throws java.io.IOException
    {
        writer.println("jsr addr_"+destination+";");
    }
}
