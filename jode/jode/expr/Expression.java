/* 
 * Expression (c) 1998 Jochen Hoenicke
 *
 * You may distribute under the terms of the GNU General Public License.
 *
 * IN NO EVENT SHALL JOCHEN HOENICKE BE LIABLE TO ANY PARTY FOR DIRECT,
 * INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT OF
 * THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF JOCHEN HOENICKE 
 * HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * JOCHEN HOENICKE SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS"
 * BASIS, AND JOCHEN HOENICKE HAS NO OBLIGATION TO PROVIDE MAINTENANCE,
 * SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * $Id$
 */

package jode;
import sun.tools.java.Type;
import sun.tools.java.Constants;
import sun.tools.java.FieldDefinition;

public class Expression extends Instruction {
    Operator     operator;
    Expression[] subExpressions;
    Expression   parent = null;

    public Expression(Operator op, Expression[] sub) {
        super(MyType.tUnknown);
        operator = op;
        subExpressions = sub;
        operator.setExpression(this);
        if (subExpressions.length != op.getOperandCount())
            throw new AssertError ("Operand count mismatch: "+
                                   subExpressions.length + " != " + 
                                   op.getOperandCount());
        if (subExpressions.length > 0) {
            Type types[] = new Type[subExpressions.length];
            for (int i=0; i < types.length; i++) {
                subExpressions[i].parent = this;
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
	    if (store.matches(operator)) {
		int i;
		for (i=0; i < e.subExpressions.length-1; i++) {
		    if (!e.subExpressions[i].equals
			(subExpressions[i]))
			break;
		}
		if (i == e.subExpressions.length-1) {
		    operator =
			new AssignOperator(store.getOperator(), store);
		    subExpressions = e.subExpressions;
		    return this;
		}
	    }
	    for (int i=0; i < subExpressions.length; i++) {
		Expression combined = subExpressions[i].tryToCombine(e);
		if (combined != null) {
		    subExpressions[i] = combined;
		    return this;
		}
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

    public void setType(Type newType) {
	newType = MyType.intersection(type, newType);
        if (newType != type) {
            type = newType;
            operator.setType(type);
        }
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
        if (operator.getType() == MyType.tError)
            result = "(/*type error */" + result+")";
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

    static Expression emptyString = 
        new Expression(new EmptyStringOperator(), new Expression[0]);

    Expression simplifyStringBuffer() {
        FieldDefinition field;
        if (operator instanceof InvokeOperator &&
            (field = ((InvokeOperator)operator).getField())
            .getClassDefinition().getName() == 
            Constants.idJavaLangStringBuffer &&
            !((InvokeOperator)operator).isStatic() &&
            field.getName() == Constants.idAppend &&
            field.getType().getArgumentTypes().length == 1) {

            Expression e = subExpressions[0].simplifyStringBuffer();
            if (e == null)
                return null;
            
            if (e.operator instanceof EmptyStringOperator &&
                MyType.isOfType(subExpressions[1].getType(), Type.tString))
                return subExpressions[1];

            Expression[] exprs = { e, 
                                   (Expression)subExpressions[1].simplify() };
            return new Expression(new StringAddOperator(), exprs);
        }
        if (operator instanceof ConstructorOperator &&
            MyType.isOfType(operator.getType(), MyType.tStringBuffer)) {
            if (operator.getOperandCount() == 1)
                return emptyString;
            else if (operator.getOperandCount() == 2 &&
                     MyType.isOfType(subExpressions[1].getType(), 
                                     MyType.tString))
                return (Expression) subExpressions[1].simplify();
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
//         if ((operator instanceof AssignOperator ||
//              operator instanceof StoreInstruction) &&
//             subExpressions[subExpressions.length-1]
//             .operator instanceof ConstOperator) {
//             StoreInstruction store;
//             if (operator instanceof AssignOperator)
//                 store = ((AssignOperator)operator).getStore();
//             else
//                 store = (StoreInstruction)operator;

//             ConstOperator one = (ConstOperator) 
//                 subExpressions[subExpressions.length-1].operator;

//             if ((operator.getOperator() == 
//                  operator.OPASSIGN_OP+operator.ADD_OP ||
//                  operator.getOperator() == 
//                  operator.OPASSIGN_OP+operator.NEG_OP) &&
//                 (one.getValue().equals("1") || 
//                  one.getValue().equals("-1"))) {

//                 int op = ((operator.getOperator() == 
//                            operator.OPASSIGN_OP+operator.ADD_OP) ==
//                           one.getValue().equals("1"))?
//                     operator.INC_OP : operator.DEC_OP;

//                 return new Expression
//                     (new PostFixOperator
//                      (store.getType(), op, store, 
//                       operator instanceof StoreInstruction),
//                      new Expression[0]).simplify();
//             }
//         }
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
            !((InvokeOperator)operator).isStatic() &&
            ((InvokeOperator)operator).getField().
            getClassDefinition().getType() == MyType.tStringBuffer &&
            operator.getOperandCount() == 1) {
            Instruction simple = subExpressions[0].simplifyStringBuffer();
            if (simple != null)
                return simple;
        }
        if (operator instanceof InvokeOperator &&
            ((InvokeOperator)operator).getField().
            getName() == Constants.idValueOf &&
            ((InvokeOperator)operator).isStatic() &&
            ((InvokeOperator)operator).getField().
            getClassDefinition().getType() == MyType.tString &&
            operator.getOperandCount() == 1) {
            if (subExpressions[0].getType() == MyType.tString)
                return subExpressions[0].simplify();
            else {
                Expression[] exprs = {
                    emptyString, 
                    (Expression) subExpressions[0].simplify() 
                };
                return new Expression(new StringAddOperator(), exprs);
            }
        }
        for (int i=0; i< subExpressions.length; i++)
            subExpressions[i] = (Expression) subExpressions[i].simplify();

        return this;
    }
}
