package jode;

public class DupOperator extends Instruction {
    int count, depth;

    public DupOperator(int a, int l, int depth, int count) {
        super(a,l);
        this.count = count;
        this.depth = depth;
    }

    public int getCount(){
        return count;
    }

    public int getDepth(){
        return depth;
    }

    public void dumpSource(TabbedPrintWriter tpw, CodeAnalyzer ca) 
         throws java.io.IOException
    {
        tpw.println("dup"+count+"_x"+depth+";");
    }
}
