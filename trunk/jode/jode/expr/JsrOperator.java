package jode;

public class JsrOperator extends Instruction {
    int destination;

    public JsrOperator() {
        super(MyType.tVoid);
    }

    public String toString() 
    {
        return "JSR";
    }
}
