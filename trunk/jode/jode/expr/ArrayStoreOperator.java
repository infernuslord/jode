/* ArrayStoreOperator Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.type.ArrayType;
import jode.decompiler.TabbedPrintWriter;

public class ArrayStoreOperator extends StoreInstruction {
    Type indexType;

    public ArrayStoreOperator(Type type, int operator) {
        super(type, operator);
        indexType = Type.tInt;
    }

    public ArrayStoreOperator(Type type) {
        this(type, ASSIGN_OP);
    }


    public boolean matches(Operator loadop) {
        return loadop instanceof ArrayLoadOperator;
    }

    public int getLValuePriority() {
        return 950;
    }

    public int getLValueOperandCount() {
        return 2;
    }

    public Type getLValueOperandType(int i) {
        if (i == 0)
            return Type.tArray(lvalueType);
        else
            return indexType;
    }

    public void setLValueOperandType(Type[] t) {
        indexType = indexType.intersection(t[1]);
        Type arrayType = t[0].intersection(Type.tArray(lvalueType));
        if (arrayType == Type.tError)
            lvalueType = Type.tError;
	else
            lvalueType = ((ArrayType)arrayType).getElementType();
    }

    public void dumpLValue(TabbedPrintWriter writer,
			   Expression[] operands) 
	throws java.io.IOException {
	operands[0].dumpExpression(writer, 950);
	writer.print("[");
	operands[1].dumpExpression(writer, 0);
	writer.print("]");
    }
}
