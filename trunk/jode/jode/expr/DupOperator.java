package jode;
import sun.tools.java.Type;

public class DupOperator extends Instruction {
    int count, depth;

    public DupOperator(int depth, int count) {
        super(MyType.tUnknown);
        this.count = count;
        this.depth = depth;
    }

    public int getCount(){
        return count;
    }

    public int getDepth(){
        return depth;
    }

    public String toString() 
    {
        return "dup"+count+"_x"+depth;
    }
}
