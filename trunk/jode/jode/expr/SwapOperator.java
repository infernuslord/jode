package jode;

public class SwapOperator extends Instruction {
    public SwapOperator() {
        super(MyType.tError);
    }

    public String toString() 
    {
        return "swap";
    }
}
