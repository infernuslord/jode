package jode;
import sun.tools.java.Type;

public class Expression extends Instruction {
    Operator     operator;
    Expression[] subExpressions;

    protected Expression(int addr, int length) {
        super(addr, length);
    }

    public Expression(int addr, int length, Operator op, Expression[] sub) {
        super(addr, length);
        operator = op;
        subExpressions = sub;
        if (op.getOperandCount() > 0) {
            Type types[] = new Type[subExpressions.length];
            for (int i=0; i < types.length; i++) {
                types[i] = subExpressions[i].getType();
            }
            operator.setOperandType(types);
            updateSubTypes();
        }
    }

    public Expression negate() {
        if (operator.operator >= operator.COMPARE_OP && 
            operator.operator < operator.COMPARE_OP+6) {
            operator.setOperator(operator.getOperator() ^ 1);
            return this;
        } else if (operator.operator == operator.LOG_AND_OP || 
                   operator.operator == operator.LOG_OR_OP) {
            operator.setOperator(operator.getOperator() ^ 1);
            for (int i=0; i< subExpressions.length; i++) {
                subExpressions[i] = subExpressions[i].negate();
            }
            return this;
        } else if (operator.operator == operator.LOG_NOT_OP) {
            return subExpressions[0];
        }

        Operator negop = 
            new UnaryOperator(getAddr(), getLength(), 
                            Type.tBoolean, Operator.LOG_NOT_OP);
        Expression[] e = { this };
        return new Expression(getAddr(), getLength(), negop, e);
    }

    public Operator getOperator() {
        return operator;
    }

    public Expression[] getSubExpressions() {
        return subExpressions;
    }

    public Type getType() {
        return operator.getType();
    }

    public void updateSubTypes() {
        for (int i=0; i < subExpressions.length; i++) {
            subExpressions[i].setType(operator.getOperandType(i));
        }
    }

    public void setType(Type type) {
        if (operator.setType(type))
            updateSubTypes();
    }

    public int[] getSuccessors() {
        return operator.getSuccessors();
    }

    public boolean isVoid() {
        return operator.getType() == Type.tVoid;
    }

    String toString(CodeAnalyzer ca, int minPriority) {
        String[] expr = new String[subExpressions.length];
        for (int i=0; i<subExpressions.length; i++) {
            expr[i] = subExpressions[i].
                toString(ca, operator.getOperandPriority(i));
        }
        String result = operator.toString(ca, expr);
        if (operator.getPriority() < minPriority) {
            result = "("+result+")";
        }
        if (operator.casts.indexOf("/*",0) >= 0 || 
            operator.casts.indexOf("<-",0) >= 0 && false)
            result = "<"+operator.casts+" "+result+">";
        return result;
    }

    public boolean equals(Expression expr) {
        if (this == expr)
            return true;
        if (!operator.equals(expr.operator) ||
            subExpressions.length != expr.subExpressions.length)
            return false;
        for (int i=0; i<subExpressions.length; i++) {
            if (subExpressions[i] != expr.subExpressions[i])
                return false;
        }
        return true;
    }

    public void dumpSource(TabbedPrintWriter writer, CodeAnalyzer ca) 
         throws java.io.IOException
    {
        if (writer.verbosity > 6) {
            writer.print("< "+
                         ca.getTypeString(operator.getType())+" "+
                         operator.getClass().getName()+"(");
            for (int i=0; i< subExpressions.length; i++) {
                if (i>0)
                    writer.print(", ");
                writer.print("("+ca.getTypeString(operator.getOperandType(i))+
                             ") "+ca.getTypeString(subExpressions[i].getType()));
            }
            writer.println(") >");
        }
        if (!isVoid())
            writer.print("push ");
        writer.println(toString(ca, 0)+";");
    }
}
