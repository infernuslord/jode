/* ConstantArrayOperator Copyright (C) 1998-1999 Jochen Hoenicke.
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

public class ConstantArrayOperator extends NoArgOperator {

    ConstOperator empty;
    Expression[] values;
    Type argType;
    boolean isInitializer;

    public ConstantArrayOperator(Type type, int size) {
        super(type);
        values = new Expression[size];
        argType = (type instanceof ArrayType) 
            ? Type.tSubType(((ArrayType)type).getElementType()) : Type.tError;
	Object emptyVal;
	if (argType == type.tError || argType.isOfType(Type.tUObject))
	    emptyVal = null;
	else if (argType.isOfType(Type.tBoolUInt))
	    emptyVal = new Integer(0);
	else if (argType.isOfType(Type.tLong))
	    emptyVal = new Long(0);
	else if (argType.isOfType(Type.tFloat))
	    emptyVal = new Float(0);
	else if (argType.isOfType(Type.tDouble))
	    emptyVal = new Double(0);
	else
	    throw new IllegalArgumentException("Illegal Type: "+argType);
	    
        empty  = new ConstOperator(emptyVal);
	empty.setType(argType);
        empty.makeInitializer();
    }

    public void setType(Type newtype) {
        super.setType(newtype);
        Type newArgType = (this.type instanceof ArrayType) 
            ? Type.tSubType(((ArrayType)this.type).getElementType()) 
            : Type.tError;
        if (!newArgType.equals(argType)) {
            argType = newArgType;
            empty.setType(argType);
            for (int i=0; i< values.length; i++)
                if (values[i] != null)
                    values[i].setType(argType);
        }
    }

    public boolean setValue(int index, Expression value) {
        if (index < 0 || index > values.length || values[index] != null)
            return false;
        value.setType(argType);
        setType(Type.tSuperType(Type.tArray(value.getType())));
        values[index] = value;
        value.parent = this;
        value.makeInitializer();
        return true;
    }

    public int getPriority() {
        return 200;
    }

    public void makeInitializer() {
        isInitializer = true;
    }

    public Expression simplify() {
	for (int i=0; i< values.length; i++) {
	    if (values[i] != null)
		values[i] = values[i].simplify();
	}
	return this;
    }

    public void dumpExpression(TabbedPrintWriter writer, 
			       Expression[] operands)
	throws java.io.IOException {
	if (!isInitializer) {
	    writer.print("new ");
	    writer.printType(type);
	    writer.print(" ");
	}
	writer.println("{");
	writer.tab();
        for (int i=0; i< values.length; i++) {
            if (i>0) {
		if (i % 10 == 0)
		    writer.println(",");
		else
		    writer.print(", ");
	    }
	    if (values[i] != null)
		values[i].dumpExpression(writer, 0);
	    else
		empty.dumpExpression(writer, 0);
        }
	writer.println();
	writer.untab();
	writer.print("}");
    }
}

