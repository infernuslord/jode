/* GetFieldOperator Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.type.NullType;
import jode.bytecode.Reference;
import jode.decompiler.CodeAnalyzer;
import jode.decompiler.TabbedPrintWriter;

public class GetFieldOperator extends Operator {
    boolean staticFlag;
    CodeAnalyzer codeAnalyzer;
    Reference ref;
    Type classType;
    boolean needCast = false;

    public GetFieldOperator(CodeAnalyzer codeAnalyzer, boolean staticFlag,
			    Reference ref) {
        super(Type.tType(ref.getType()), 0);
        this.codeAnalyzer = codeAnalyzer;
        this.staticFlag = staticFlag;
        this.classType = Type.tType(ref.getClazz());
	this.ref = ref;
        if (staticFlag)
            codeAnalyzer.useType(classType);
    }

    public int getPriority() {
        return 950;
    }

    public int getOperandCount() {
        return staticFlag?0:1;
    }

    public int getOperandPriority(int i) {
        return 900;
    }

    public Type getOperandType(int i) {
        return classType;
    }

    public void setOperandType(Type types[]) {
	if (!staticFlag)
	    needCast = types[0].getHint().equals(Type.tNull);
    }

    public void dumpExpression(TabbedPrintWriter writer, Expression[] operands)
	throws java.io.IOException {
	boolean opIsThis = 
	    (!staticFlag
	     && operands[0] instanceof LocalLoadOperator
	     && (((LocalLoadOperator) operands[0]).getLocalInfo()
		 .equals(codeAnalyzer.getParamInfo(0)))
	     && !codeAnalyzer.getMethod().isStatic());
	String fieldName = ref.getName();
	if (staticFlag) {
	    if (!classType.equals(Type.tClass(codeAnalyzer.getClazz()))
		|| codeAnalyzer.findLocal(fieldName) != null) {
		writer.printType(classType);
		writer.print(".");
	    }
	    writer.print(fieldName);
	} else if (operands[0].getType() instanceof NullType) {
	    writer.print("((");
	    writer.printType(classType);
	    writer.print(")");
	    operands[0].dumpExpression(writer, 700);
	    writer.print(").");
	    writer.print(fieldName);
	} else {
	    if (!opIsThis || codeAnalyzer.findLocal(fieldName) != null) {
		operands[0].dumpExpression(writer, 950);
		writer.print(".");
	    }
	    writer.print(fieldName);
	}
    }

    public boolean equals(Object o) {
	return o instanceof GetFieldOperator
	    && ((GetFieldOperator)o).ref.equals(ref);
    }
}
