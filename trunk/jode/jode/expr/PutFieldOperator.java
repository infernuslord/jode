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
import jode.Type;
import jode.bytecode.Reference;
import jode.decompiler.CodeAnalyzer;
import jode.decompiler.FieldAnalyzer;

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
        this.classType = Type.tClass(ref.getClazz());
        if (staticFlag)
            classType.useType();
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
        return (classType.equals(Type.tClass(codeAnalyzer.getClazz().
                                             getName())));
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

    public int getLValueOperandCount() {
        return staticFlag?0:1;
    }

    public int getLValueOperandPriority(int i) {
        return 900;
    }

    public Type getLValueOperandType(int i) {
        return classType;
    }

    public void setLValueOperandType(Type[] t) {
    }

    public String getLValueString(String[] operands) {
	String fieldName = getFieldName();
        return staticFlag
            ? (classType.equals(Type.tClass(codeAnalyzer.getClazz().getName()))
               && codeAnalyzer.findLocal(fieldName) == null
               ? fieldName 
               : classType.toString() + "." + fieldName)
            : ((operands[0].equals("this")
		&& codeAnalyzer.findLocal(fieldName) == null
		? fieldName
		: operands[0].equals("null")
		? "((" + classType + ") null)." + fieldName
		: operands[0] + "." + fieldName));
    }

    public boolean equals(Object o) {
	return o instanceof PutFieldOperator
	    && ((PutFieldOperator)o).ref.equals(ref);
    }
}
