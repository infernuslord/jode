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
import jode.Type;
import jode.bytecode.Reference;
import jode.decompiler.CodeAnalyzer;

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
        this.classType = Type.tClass(ref.getClazz());
	this.ref = ref;
        if (staticFlag)
            classType.useType();
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

    public String toString(String[] operands) {
	String fieldName = ref.getName();
        return staticFlag
            ? (classType.equals(Type.tClass(codeAnalyzer.getClazz().getName()))
               && codeAnalyzer.findLocal(fieldName) == null
               ? fieldName 
               : classType.toString() + "." + fieldName)
            : (operands[0].equals("null")
	       ? "((" + classType + ") null)." + fieldName
	       : (operands[0].equals("this")
		  && codeAnalyzer.findLocal(fieldName) == null
		  ? fieldName
		  : operands[0] + "." + fieldName));
    }

    public boolean equals(Object o) {
	return o instanceof GetFieldOperator
	    && ((GetFieldOperator)o).ref.equals(ref);
    }
}
