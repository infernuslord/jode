package jode;

public class RetOperator extends Instruction {
    int slot;

    public RetOperator(int slot) {
        super(MyType.tVoid);
        this.slot = slot;
    }

    public String toString() 
    {
        return "ret";
    }
}
