package jode;
import sun.tools.java.Type;
import sun.tools.java.Constants;
import sun.tools.java.FieldDefinition;

public class Expression extends Instruction {
    Operator     operator;
    Expression[] subExpressions;

    public Expression(Operator op, Expression[] sub) {
        super(MyType.tUnknown);
        operator = op;
        subExpressions = sub;
        if (subExpressions.length != op.getOperandCount())
            throw new AssertError ("Operand count mismatch: "+
                                   subExpressions.length + " != " + 
                                   op.getOperandCount());
        if (subExpressions.length > 0) {
            Type types[] = new Type[subExpressions.length];
            for (int i=0; i < types.length; i++) {
                types[i] = subExpressions[i].getType();
            }
            operator.setOperandType(types);
            updateSubTypes();
        }
	this.type = operator.getType();
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
            new UnaryOperator(Type.tBoolean, Operator.LOG_NOT_OP);
        Expression[] e = { this };
        return new Expression(negop, e);
    }

    public Expression tryToCombine(Expression e) {
	if (e.operator instanceof StoreInstruction) {
	    StoreInstruction store = (StoreInstruction) e.operator;
	    Expression search = this;
	    while (search.subExpressions.length > 0) {
		if (store.matches(search.operator)) {
		    int i;
		    for (i=0; i < e.subExpressions.length-1; i++) {
			if (!e.subExpressions[i].equals
			    (search.subExpressions[i]))
			    break;
		    }
		    if (i == e.subExpressions.length-1) {
			search.operator =
			    new AssignOperator(store.getOperator(), store);
			search.subExpressions = e.subExpressions;
			return this;
		    }
		}
		search = search.subExpressions[0];
	    }
	}
	return null;
    }

    public Operator getOperator() {
        return operator;
    }

    public Expression[] getSubExpressions() {
        return subExpressions;
    }

    public void updateSubTypes() {
        for (int i=0; i < subExpressions.length; i++) {
            subExpressions[i].setType(operator.getOperandType(i));
        }
    }

    public void setType(Type type) {
        if (operator.setType(type))
            updateSubTypes();
	this.type = operator.getType();
    }

    public boolean isVoid() {
        return operator.getType() == Type.tVoid;
    }

    String toString(int minPriority) {
        String[] expr = new String[subExpressions.length];
        for (int i=0; i<subExpressions.length; i++) {
            expr[i] = subExpressions[i].
                toString(operator.getOperandPriority(i));
        }
        String result = operator.toString(expr);
        if (operator.getPriority() < minPriority) {
            result = "("+result+")";
        }
        if (operator.casts.indexOf("/*",0) >= 0 || 
            operator.casts.indexOf("<-",0) >= 0 && false)
            result = "<"+operator.casts+" "+result+">";
        return result;
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
	if (!(o instanceof Expression))
	    return false;
	Expression expr = (Expression) o;
        if (!operator.equals(expr.operator) ||
            subExpressions.length != expr.subExpressions.length)
            return false;

        for (int i=0; i<subExpressions.length; i++) {
            if (!subExpressions[i].equals(expr.subExpressions[i]))
                return false;
        }
        return true;
    }

    public String toString() {
        return toString(0);
    }

    Expression simplifyStringBuffer() {
        FieldDefinition field;
        if (operator instanceof InvokeOperator &&
            (field = ((InvokeOperator)operator).getField())
            .getClassDefinition().getName() == 
            Constants.idJavaLangStringBuffer &&
            field.getName() == Constants.idAppend &&
            field.getType().getArgumentTypes().length == 1) {

            Expression e = subExpressions[0].simplifyStringBuffer();
            if (e == null)
                return null;
            
            if (e.operator instanceof EmptyStringOperator &&
                subExpressions[1].getType() == Type.tString)
                return subExpressions[1];

            Expression[] exprs = { e, subExpressions[1] };
            return new Expression(new StringAddOperator(), exprs);
        }
        if (operator instanceof ConstructorOperator &&
            operator.getType() == MyType.tStringBuffer) {
            if (operator.getOperandCount() == 1)
                return new Expression(new EmptyStringOperator(), 
                                      new Expression[0]);
            else if (operator.getOperandCount() == 2 &&
                     subExpressions[1].getType() == MyType.tString)
                return subExpressions[1];
        }
        return null;
    }

    public Instruction simplify() {
        if (operator instanceof IfThenElseOperator &&
            operator.getType() == Type.tBoolean) {
            if (subExpressions[1].operator instanceof ConstOperator &&
                subExpressions[2].operator instanceof ConstOperator) {
                ConstOperator c1 = (ConstOperator) subExpressions[1].operator;
                ConstOperator c2 = (ConstOperator) subExpressions[2].operator;
                if (c1.getValue().equals("true") &&
                    c2.getValue().equals("false"))
                    return subExpressions[0].simplify();
                if (c2.getValue().equals("true") &&
                    c1.getValue().equals("false"))
                    return subExpressions[0].negate().simplify();
            }
        }
        if (operator instanceof CompareUnaryOperator &&
            subExpressions[0].operator instanceof CompareToIntOperator) {
            CompareBinaryOperator newOp = new CompareBinaryOperator
                (subExpressions[0].operator.getOperandType(0),
                 operator.getOperator());
            return new Expression(newOp, subExpressions[0].subExpressions).
                simplify();
        }
        if (operator instanceof CompareUnaryOperator &&
            operator.getOperandType(0) != Type.tBoolean) {
            if (subExpressions[0].operator instanceof ConstOperator) {
                ConstOperator c = (ConstOperator) subExpressions[0].operator;
                if (c.getValue().equals("0") || c.getValue().equals("1")) {
                    Type[] newType = {Type.tBoolean};
                    operator.setOperandType(newType);
                }
            }
        }
        if (operator instanceof CompareUnaryOperator &&
            operator.getOperandType(0) == Type.tBoolean) {
            if (operator.getOperator() == 26)  /* xx == false */
                return subExpressions[0].negate().simplify();
            if (operator.getOperator() == 27)  /* xx != false */
                return subExpressions[0].simplify();
        }

        if (operator instanceof InvokeOperator &&
            ((InvokeOperator)operator).getField().
            getName() == Constants.idToString &&
            ((InvokeOperator)operator).getField().
            getClassDefinition().getType() == MyType.tStringBuffer &&
            operator.getOperandCount() == 1) {
            Expression e = subExpressions[0].simplifyStringBuffer();
            if (e != null)
                return e.simplify();
        }

        for (int i=0; i< subExpressions.length; i++)
            subExpressions[i] = (Expression) subExpressions[i].simplify();

        return this;
    }
}
