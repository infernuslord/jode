package jode;

public class Block extends Instruction {
    Expression[] exprs;

    public Block(int addr, int length, Expression[] exprs) {
        super(addr,length);
        this.exprs = exprs;
    }

    public void dumpSource(TabbedPrintWriter writer, CodeAnalyzer ca)
         throws java.io.IOException 
    {
        for (int i=0; i< exprs.length; i++)
            exprs[i].dumpSource(writer,ca);
    }
}

