package jode;
import sun.tools.java.Type;

public class ConstOperator extends NoArgOperator {
    String value;

    public ConstOperator(Type type, String value) {
        super(type);
        if (type == MyType.tString)
            value = quoted(value);
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public int getPriority() {
        return 1000;
    }

    public static String quoted(String str) {
        StringBuffer result = new StringBuffer("\"");
        for (int i=0; i< str.length(); i++) {
            switch (str.charAt(i)) {
            case '\t':
                result.append("\\t");
                break;
            case '\n':
                result.append("\\n");
                break;
            case '\\':
                result.append("\\\\");
                break;
            case '\"':
                result.append("\\\"");
                break;
            default:
                result.append(str.charAt(i));
            }
        }
        return result.append("\"").toString();
    }

    public boolean equals(Object o) {
	return (o instanceof ConstOperator) &&
	    ((ConstOperator)o).value.equals(value);
    }

    public String toString(String[] operands) {
        if (type == Type.tBoolean) {
            if (value.equals("0"))
                return "false";
            else if (value.equals("1"))
                return "true";
        }
        return value;
    }
}
