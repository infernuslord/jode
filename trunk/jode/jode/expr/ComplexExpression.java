/* ComplexExpression Copyright (C) 1998-1999 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id$
 */

package jode.expr;
import jode.GlobalOptions;
import jode.type.Type;
import jode.decompiler.TabbedPrintWriter;

public class ComplexExpression extends Expression {
    Operator     operator;
    Expression[] subExpressions;
    int operandcount = 0;

    public ComplexExpression(Operator op, Expression[] sub) {
        super(Type.tUnknown);
        if (sub.length != op.getOperandCount())
            throw new jode.AssertError ("Operand count mismatch: "+
                                        sub.length + " != " + 
                                        op.getOperandCount());
        operator = op;
        operator.parent = this;
        subExpressions = sub;
        for (int i=0; i< subExpressions.length; i++) {
	    if (subExpressions[i] == null)
		subExpressions[i] = new NopOperator(Type.tUnknown);
	    subExpressions[i].parent = this;
	    operandcount += subExpressions[i].getOperandCount();
	}
        updateType();
    }

    public int getOperandCount() {
	return operandcount;
    }

    public Expression addOperand(Expression op) {
	for (int i= subExpressions.length-1; i >= 0; i--) {
	    int opcount = subExpressions[i].getOperandCount();
	    if (opcount > 0) {
		subExpressions[i] = subExpressions[i].addOperand(op);
		subExpressions[i].parent = this;
		operandcount += subExpressions[i].getOperandCount() - opcount;
		updateType();
		return this;
	    }
	}
	throw new jode.AssertError("addOperand called, but no operand needed");
    }

    public Expression negate() {
        if (operator.getOperatorIndex() >= operator.COMPARE_OP && 
            operator.getOperatorIndex() < operator.COMPARE_OP+6) {
            operator.setOperatorIndex(operator.getOperatorIndex() ^ 1);
            return this;
        } else if (operator.getOperatorIndex() == operator.LOG_AND_OP || 
                   operator.getOperatorIndex() == operator.LOG_OR_OP) {
            operator.setOperatorIndex(operator.getOperatorIndex() ^ 1);
            for (int i=0; i< subExpressions.length; i++) {
		subExpressions[i] = subExpressions[i].negate();
                subExpressions[i].parent = this;
            }
            return this;
        } else if (operator.operator == operator.LOG_NOT_OP) {
	    if (subExpressions[0] != null)
		return subExpressions[0];
	    else
		return new NopOperator(Type.tBoolean);
        }

        Operator negop = 
            new UnaryOperator(Type.tBoolean, Operator.LOG_NOT_OP);
        return new ComplexExpression(negop, new Expression[] { this });
    }

    /**
     * Checks if the value of the given expression can change, due to
     * side effects in this expression.  If this returns false, the 
     * expression can safely be moved behind the current expresion.
     * @param expr the expression that should not change.
     */
    public boolean hasSideEffects(Expression expr) {
	if (operator.hasSideEffects(expr))
	    return true;
	for (int i=0; i < subExpressions.length; i++) {
	    if (subExpressions[i].hasSideEffects(expr))
		return true;
	}
	return false;
    }

    /**
     * Checks if the given Expression (which must be a CombineableOperator)
     * can be combined into this expression.
     * @param e The store expression, must be of type void.
     * @return 1, if it can, 0, if no match was found and -1, if a
     * conflict was found.  You may wish to check for >0.
     * @exception ClassCastException, if e.getOperator 
     * is not a CombineableOperator.
     */
    public int canCombine(Expression e) {
//  	GlobalOptions.err.println("Try to combine "+e+" into "+this);
	if (e.getOperator() instanceof LocalStoreOperator
	    && e.getOperandCount() == 0) {
	    // Special case for locals created on inlining methods, which may
	    // combine everywhere, as long as there are no side effects.

	    for (int i=0; i < subExpressions.length; i++) {
		int result = subExpressions[i].canCombine(e);
		if (result != 0)
		    return result;
		if (subExpressions[i].hasSideEffects(e))
		    return -1;
	    }
	}

	if (e instanceof ComplexExpression) {
	    if (((CombineableOperator) e.getOperator()).matches(operator)) {
		ComplexExpression ce = (ComplexExpression) e;
		for (int i=0; i < ce.subExpressions.length-1; i++) {
		    if (!ce.subExpressions[i].equals(subExpressions[i]))
			return -1;
		}
		return 1;
	    }
        }
        return subExpressions[0].canCombine(e);
    }

    /**
     * Checks if this expression contains a conflicting load, that
     * matches the given CombineableOperator.  The sub expressions are
     * not checked.
     * @param op The combineable operator.
     * @return if this expression contains a matching load.  */
    public boolean containsConflictingLoad(MatchableOperator op) {
	if (op.matches(operator))
	    return true;
	for (int i=0; i < subExpressions.length; i++) {
	    if (subExpressions[i].containsConflictingLoad(op))
		return true;
	}
	return false;
    }

    /**
     * Checks if this expression contains a conflicting load, that matches the
     * given Expression (which must be a
     * StoreInstruction/IIncOperator).
     * @param e The store expression.
     * @return if this expression contains a matching load.
     * @exception ClassCastException, if e.getOperator 
     * is not a CombineableOperator.
     */
    public boolean containsMatchingLoad(Expression e) {
	if (e instanceof ComplexExpression
            && e.getOperator() instanceof StoreInstruction
            && ((StoreInstruction) e.getOperator()).matches(operator)) {

            ComplexExpression ce = (ComplexExpression) e;
            int i;
            for (i=0; i < ce.subExpressions.length-1; i++) {
		if (!ce.subExpressions[i].equals(subExpressions[i]))
                    break;
            }
            if (i == ce.subExpressions.length-1)
                return true;
        }
        for (int i=0; i < subExpressions.length; i++) {
            if (subExpressions[i].containsMatchingLoad(e))
                return true;
        }
	return false;
    }
    
    /**
     * Combines the given Expression (which should be a StoreInstruction)
     * into this expression.  You must only call this if
     * canCombine returns the value 1.
     * @param e The store expression.
     * @return The combined expression.
     * @exception ClassCastException, if e.getOperator 
     * is not a CombineableOperator.
     */
    public Expression combine(Expression e) {

        CombineableOperator op = (CombineableOperator) e.getOperator();
        if (op.matches(operator)) {
            op.makeNonVoid();
            e.type = e.getOperator().getType();
            return e;
        }
        for (int i=0; i < subExpressions.length; i++) {
	    Expression combined = subExpressions[i].combine(e);
	    if (combined != null) {
		subExpressions[i] = combined;
		subExpressions[i].parent = this;
		return this;
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

    public void setSubExpressions(int i, Expression expr) {
	int diff = expr.getOperandCount()
	    - subExpressions[i].getOperandCount();
        subExpressions[i] = expr;
	for (ComplexExpression ce = this; ce != null; 
	     ce = (ComplexExpression) ce.parent)
	    ce.operandcount += diff;
        updateType();
    }

    void updateSubTypes() {
        for (int i=0; i < subExpressions.length; i++) {
            Type opType;
            if (operator instanceof CheckNullOperator
		|| i == 0 && operator instanceof ArrayStoreOperator) {
                /* No rule without exception:
                 * We can always use tSubType, except for the
		 * check null operator and the
                 * array operand of an array store instruction.
                 */
                opType = operator.getOperandType(i);
            } else
                opType = Type.tSubType(operator.getOperandType(i));
	    if (opType != Type.tError) {
		Type exprType = subExpressions[i].getType();
		opType = opType.intersection(exprType);
		if (!opType.equals(exprType)) {
		    if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_TYPES) != 0)
			GlobalOptions.err.println("change in "+this+": "
					       +exprType+"->"+opType);
		    if (opType == Type.tError)
			GlobalOptions.err.println("Type error in "+this+": "
					       +exprType+"->"
					       +operator.getOperandType(i));
		    subExpressions[i].setType(opType);
		}
            }
        }
    }

    public void updateType() {
	while (true) {
	    updateSubTypes();
	    Type types[] = new Type[subExpressions.length];
	    boolean changed = false;
	    for (int i=0; i < types.length; i++) {
		if (operator instanceof CheckNullOperator
		    || i == 0 && operator instanceof ArrayStoreOperator) {
		    /* No rule without exception:
		     * We can always use tSuperType, except for the
		     * array operand of an array store instruction.
		     */
		    types[i] = subExpressions[i].getType();
		} else
		    types[i] = Type.tSuperType
			(subExpressions[i].getType());
		Type opType = operator.getOperandType(i);
		if (types[i] == Type.tError)
		    continue;
		types[i] = types[i].intersection(opType);
		if (types[i].equals(opType))
		    continue;
		
		if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_TYPES) != 0)
		    GlobalOptions.err.println("change in "+this+" at "+i+": "
					   +opType+"->"+types[i]);
		if (types[i] == Type.tError)
		    GlobalOptions.err.println("Type error in "+this+" at "+i+": "
					   +subExpressions[i].getType()
					   +"->"+opType);
		else
		    changed = true;
	    }
	    if (!changed)
		break;
	    operator.setOperandType(types);
	}
	Type newType = type.intersection(operator.getType());
        if (!newType.equals(type)) {
            type = newType;
            if (parent != null)
                parent.updateType();
        }
    }

    public void setType(Type newType) {
        operator.setType(newType);
        updateType();
    }

    public boolean isVoid() {
        return operator.getType() == Type.tVoid;
    }

    public void dumpExpression(TabbedPrintWriter writer)
	throws java.io.IOException {
	operator.dumpExpression(writer, subExpressions);
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
	if (!(o instanceof ComplexExpression))
	    return false;
	ComplexExpression expr = (ComplexExpression) o;
        if (!operator.equals(expr.operator) ||
            subExpressions.length != expr.subExpressions.length)
            return false;

        for (int i=0; i<subExpressions.length; i++) {
	    if(!subExpressions[i].equals(expr.subExpressions[i]))
                return false;
        }
        return true;
    }

    /** 
     * This method should remove local variables that are only written
     * and read one time directly after another.  <br>
     *
     * In this case this is a non void LocalStoreOperator, whose local
     * isn't used in other places.
     * @return an expression where the locals are removed.
     */
    public Expression removeOnetimeLocals() {
//  	System.err.println("removeOneTimeLocals: "+this);
	if (operator instanceof LocalStoreOperator
	    && operator.getType() != Type.tVoid) {
	    jode.decompiler.LocalInfo local = ((LocalStoreOperator)operator).getLocalInfo();
	    if ((local.getUseCount() == 2 /*XXX*/)) {
		/* remove LocalInfo somehow XXX */
		return subExpressions[0].removeOnetimeLocals();
	    } //  else
//  		System.err.println("Can't remove local "+local);
	}
        for (int i=0; i< subExpressions.length; i++) {
	    subExpressions[i] = subExpressions[i].removeOnetimeLocals();
	    subExpressions[i].parent = this;
        }
	return this;
    }

    public Expression simplifyStringBuffer() {
        if (operator instanceof InvokeOperator
            && (((InvokeOperator)operator).getClassType()
                .equals(Type.tStringBuffer))
            && !((InvokeOperator)operator).isStatic() 
            && (((InvokeOperator)operator).getMethodName().equals("append"))
            && (((InvokeOperator)operator).getMethodType()
                .getParameterTypes().length == 1)) {

            Expression e = subExpressions[0].simplifyStringBuffer();
            if (e == null)
                return null;
            
            if (e == EMPTYSTRING
		&& subExpressions[1].getType().isOfType(Type.tString))
                return subExpressions[1];

	    subExpressions[1] = subExpressions[1].simplifyString();

            return new ComplexExpression
                (new StringAddOperator(), new Expression[]
		 { e, subExpressions[1] });
        }
        if (operator instanceof ConstructorOperator
            && (((ConstructorOperator) operator).getClassType() 
                == Type.tStringBuffer)) {
            
            if (subExpressions.length == 1
		&& subExpressions[0].getType().isOfType(Type.tString))
                return subExpressions[0].simplifyString();
        }
        return null;
    }

    public Expression simplifyString() {
        if (operator instanceof InvokeOperator) {
            InvokeOperator invoke = (InvokeOperator) operator;
            if (invoke.getMethodName().equals("toString")
                && !invoke.isStatic()
                && invoke.getClassType().equals(Type.tStringBuffer)
                && subExpressions.length == 1) {
                Expression simple = subExpressions[0].simplifyStringBuffer();
                if (simple != null)
                    return simple;
                        
            }
            else if (invoke.getMethodName().equals("valueOf")
                     && invoke.isStatic() 
                     && invoke.getClassType().equals(Type.tString)
                     && subExpressions.length == 1) {
                
                if (subExpressions[0].getType().isOfType(Type.tString))
                    return subExpressions[0];
                
                return new ComplexExpression
                    (new StringAddOperator(), new Expression[] 
                     { EMPTYSTRING, subExpressions[0] });
            }
            /* The pizza way (pizza is the compiler of kaffe) */
            else if (invoke.getMethodName().equals("concat")
                     && !invoke.isStatic()
                     && invoke.getClassType().equals(Type.tString)) {

                Expression left = subExpressions[0].simplify();
                Expression right = subExpressions[1].simplify();
                if (right instanceof ComplexExpression
                    && right.getOperator() instanceof StringAddOperator
                    && (((ComplexExpression) right).subExpressions[0]
                        == EMPTYSTRING))
                    right = ((ComplexExpression)right).subExpressions[1];

                return new ComplexExpression
                    (new StringAddOperator(), new Expression[] 
		     { left, right });
            } else if (jode.Decompiler.stringDecrypting) {
		Expression expr = subExpressions[0].simplifyString();
		if (expr instanceof ConstOperator) {
		    expr = invoke.deobfuscateString((ConstOperator)expr);
		    if (expr != null)
			return expr;
		}
	    }
        }
        return this;
    }

    public Expression simplify() {
        if (operator instanceof IfThenElseOperator &&
            operator.getType().isOfType(Type.tBoolean)) {
            if (subExpressions[1].getOperator() instanceof ConstOperator
		&& subExpressions[2].getOperator() instanceof ConstOperator) {
                ConstOperator c1 = 
                    (ConstOperator) subExpressions[1].getOperator();
                ConstOperator c2 = 
                    (ConstOperator) subExpressions[2].getOperator();
                if (c1.getValue().equals("1") &&
                    c2.getValue().equals("0"))
                    return subExpressions[0].simplify();
                if (c2.getValue().equals("1") &&
                    c1.getValue().equals("0"))
                    return subExpressions[0].negate().simplify();
            }
        }
        else if (operator instanceof StoreInstruction 
                 && (subExpressions[subExpressions.length-1]
		     .getOperator() instanceof ConstOperator)) {

            StoreInstruction store = (StoreInstruction) operator;
            ConstOperator one = (ConstOperator) 
                subExpressions[subExpressions.length-1].getOperator();

            if ((operator.getOperatorIndex() == 
                 operator.OPASSIGN_OP+operator.ADD_OP ||
                 operator.getOperatorIndex() == 
                 operator.OPASSIGN_OP+operator.SUB_OP) &&
                (one.getValue().equals("1")
		 || one.getValue().equals("1.0"))) {

                int op = (operator.getOperatorIndex() == 
                          operator.OPASSIGN_OP+operator.ADD_OP)
                    ? operator.INC_OP : operator.DEC_OP;

                Operator ppfixop = new PrePostFixOperator
                    (getType(), op, store, isVoid());
                if (subExpressions.length == 1)
                    return ppfixop.simplify();

                operator = ppfixop;
                ppfixop.parent = this;
            }
        }
        else if (operator instanceof CompareUnaryOperator 
		 && (subExpressions[0].getOperator() 
		     instanceof CompareToIntOperator)) {
            
            CompareBinaryOperator newOp = new CompareBinaryOperator
                (subExpressions[0].getOperator().getOperandType(0),
                 operator.getOperatorIndex());
            
            if (subExpressions[0] instanceof ComplexExpression) {
                return new ComplexExpression
                    (newOp, 
                     ((ComplexExpression)subExpressions[0]).subExpressions).
                    simplify();
            } else
                return newOp.simplify();
        }
        else if (operator instanceof CompareUnaryOperator &&
            operator.getOperandType(0).isOfType(Type.tBoolean)) {
            /* xx == false */
            if (operator.getOperatorIndex() == operator.EQUALS_OP)
                return subExpressions[0].negate().simplify();
            /* xx != false */
            if (operator.getOperatorIndex() == operator.NOTEQUALS_OP)
                return subExpressions[0].simplify();
        } else if (operator instanceof IfThenElseOperator) {
	    if ((subExpressions[0] instanceof ComplexExpression)
		&& (subExpressions[0].getOperator() 
		    instanceof CompareUnaryOperator)
		&& (subExpressions[0].getOperator().getOperatorIndex()
		    == Operator.NOTEQUALS_OP)
		&& (subExpressions[1] instanceof GetFieldOperator)
		&& (subExpressions[2] instanceof ComplexExpression)
		&& (subExpressions[2].getOperator()
		    instanceof PutFieldOperator)) {
		// Check for
		//   class$classname != null ? class$classname :
		//       (class$classname = class$("classname"))
		// and replace with
		//   classname.class
		ComplexExpression cmp = (ComplexExpression) subExpressions[0];
		GetFieldOperator get = (GetFieldOperator) subExpressions[1];
		ComplexExpression ass = (ComplexExpression) subExpressions[2];
		PutFieldOperator put = (PutFieldOperator) ass.getOperator();
		if (put.getField() != null
		    && put.getField().isSynthetic() && put.matches(get)
		    && cmp.subExpressions[0] instanceof GetFieldOperator
		    && put.matches((GetFieldOperator)cmp.subExpressions[0])
		    && ass.subExpressions[0] instanceof ComplexExpression
		    && (ass.subExpressions[0].getOperator() 
			instanceof InvokeOperator)) {
		    InvokeOperator invoke = (InvokeOperator) 
			ass.subExpressions[0].getOperator();
		    Expression param = 
			((ComplexExpression)ass.subExpressions[0])
			.subExpressions[0];
		    if (invoke.isGetClass()
			&& param instanceof ConstOperator
			&& param.getType().equals(Type.tString)) {
			String clazz = ((ConstOperator)param).getValue();
			if (put.getFieldName()
			    .equals("class$" + clazz.replace('.', '$'))
			    || put.getFieldName()
			    .equals("class$L" + clazz.replace('.', '$'))) {
			    put.getField().analyzedSynthetic();
			    return new ClassFieldOperator(Type.tClass(clazz));
			}
		    }
		}
	    }
        } else {
            Expression stringExpr = simplifyString();
            if (stringExpr != this)
                return stringExpr.simplify();
        }
        for (int i=0; i< subExpressions.length; i++) {
	    subExpressions[i] = subExpressions[i].simplify();
	    subExpressions[i].parent = this;
        }
        return this;
    }

    public void makeInitializer() {
        operator.makeInitializer();
    }

    public boolean isConstant() {
        if (!operator.isConstant())
            return false;
        for (int i=0; i< subExpressions.length; i++)
            if (!subExpressions[i].isConstant())
                return false;
        return true;
    }
}
