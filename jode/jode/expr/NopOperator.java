package jode;
import sun.tools.java.Type;

public class NopOperator extends Instruction {
    public NopOperator(Type type) {
	super(type);
    }

    public NopOperator() {
        this(MyType.tVoid);
    }

    public boolean equals(Object o) {
	return (o instanceof NopOperator);
    }

    public String toString() 
    {
        return "nop";
    }
}
