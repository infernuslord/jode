/* ConstructorOperator Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.type.Type;
import jode.type.MethodType;
import jode.decompiler.TabbedPrintWriter;

public class ConstructorOperator extends Operator 
    implements MatchableOperator {
    MethodType methodType;
    Type classType;

    public ConstructorOperator(Type type, MethodType methodType, 
                               boolean isVoid) {
        super(isVoid ? Type.tVoid : type, 0);
        this.classType  = type;
        this.methodType = methodType;
    }

    /**
     * Checks if the value of the given expression can change, due to
     * side effects in this expression.  If this returns false, the 
     * expression can safely be moved behind the current expresion.
     * @param expr the expression that should not change.
     */
    public boolean hasSideEffects(Expression expr) {
	return expr.containsConflictingLoad(this);
    }

    /**
     * Checks if the value of the operator can be changed by this expression.
     */
    public boolean matches(Operator loadop) {
        return (loadop instanceof InvokeOperator
		|| loadop instanceof ConstructorOperator
		|| loadop instanceof GetFieldOperator);
    }

    public int getPriority() {
        return 950;
    }

    public int getOperandCount() {
        return methodType.getParameterTypes().length;
    }

    public int getOperandPriority(int i) {
        return 0;
    }

    public Type getClassType() {
        return classType;
    }

    public Type getOperandType(int i) {
        return methodType.getParameterTypes()[i];
    }

    public void setOperandType(Type types[]) {
    }

    public Expression simplifyStringBuffer() {
        return (getClassType() == Type.tStringBuffer)
            ? EMPTYSTRING : null;
    }

    public void dumpExpression(TabbedPrintWriter writer,
			       Expression[] operands) 
	throws java.io.IOException {
	writer.print("new ");
	writer.printType(classType);
	writer.print("(");
        for (int i=0; i < methodType.getParameterTypes().length; i++) {
            if (i>0)
		writer.print(", ");
            operands[i].dumpExpression(writer, 0);
        }
        writer.print(")");
    }
}


