package jode;

public class SwapOperator extends Instruction {
    public SwapOperator() {
        super(MyType.tVoid);
    }

    public String toString() 
    {
        return "swap";
    }
}
