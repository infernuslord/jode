package jode;
import sun.tools.java.Type;

public class UnknownSubType extends MyType {
    Type elemType;

    public UnknownSubType(Type type) {
        super(103, "<");
        elemType = type;
    }

    public Type getElementType()
    {
        return elemType;
    }

    public String typeString(String string, boolean flag1, boolean flag2)
    {
        return "<superclass of "+
            String.valueOf(elemType.typeString(string, flag1, flag2))+">";
    }
}
