/* PutFieldOperator Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.decompiler.FieldAnalyzer;
import jode.decompiler.TabbedPrintWriter;

public class PutFieldOperator extends StoreInstruction {
    CodeAnalyzer codeAnalyzer;
    boolean staticFlag;
    Reference ref;
    Type classType;

    public PutFieldOperator(CodeAnalyzer codeAnalyzer, boolean staticFlag, 
                            Reference ref) {
        super(Type.tType(ref.getType()), ASSIGN_OP);
        this.codeAnalyzer = codeAnalyzer;
        this.staticFlag = staticFlag;
	this.ref = ref;
        this.classType = Type.tType(ref.getClazz());
        if (staticFlag)
            codeAnalyzer.useType(classType);
    }

    public boolean isStatic() {
        return staticFlag;
    }

    /**
     * Checks, whether this is a call of a method from this class.
     * @XXX check, if this class implements the method and if not
     * allow super class
     */
    public boolean isThis() {
        return (classType.equals(Type.tClass(codeAnalyzer.getClazz())));
    }

    public FieldAnalyzer getField() {
	if (!isThis())
	    return null;
	return codeAnalyzer.getClassAnalyzer()
	    .getField(ref.getName(), Type.tType(ref.getType()));
    }

    public String getFieldName() {
        return ref.getName();
    }

    public Type getFieldType() {
        return Type.tType(ref.getType());
    }

    public boolean matches(Operator loadop) {
        return loadop instanceof GetFieldOperator
	    && ((GetFieldOperator)loadop).ref.equals(ref);
    }

    public int getLValuePriority() {
        return 950;
    }

    public int getLValueOperandCount() {
        return staticFlag?0:1;
    }

    public Type getLValueOperandType(int i) {
        return classType;
    }

    public void setLValueOperandType(Type[] t) {
    }

    public void dumpLValue(TabbedPrintWriter writer, Expression[] operands)
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
	return o instanceof PutFieldOperator
	    && ((PutFieldOperator)o).ref.equals(ref);
    }
}
