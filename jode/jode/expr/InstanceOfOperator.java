/* InstanceOfOperator Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.decompiler.TabbedPrintWriter;

public class InstanceOfOperator extends SimpleOperator {

    Type instanceType;
    /**
     * There are special cases where a instanceof isn't allowed.  We must cast
     * to the common super type before.  This cases always give a runtime
     * error, but we want to decompile even bad programs.
     */
    Type superType = null;
    

    public InstanceOfOperator(Type type) {
        super(Type.tBoolean, 0, 1);
        this.instanceType = type;
        this.operandTypes[0] = Type.tUnknown;
    }
    public int getOperandCount() {
        return 1;
    }

    public int getPriority() {
        return 550;
    }

    public void setOperandType(Type[] type) {
	super.setOperandType(type);
	superType = instanceType.getCastHelper(type[0]);
    }

    public void dumpExpression(TabbedPrintWriter writer,
			       Expression[] operands) 
	throws java.io.IOException {
	/* There are special cases where a cast isn't allowed.  We must cast
	 * to the common super type before.  This cases always give a runtime
	 * error, but we want to decompile even bad programs.
	 */
	Type superType = instanceType.getCastHelper(operands[0].getType());
	if (superType != null) {
	    writer.print("(");
	    writer.printType(superType);
	    writer.print(") ");
	    operands[0].dumpExpression(writer, 700);
	} else
	    operands[0].dumpExpression(writer, 550);
        writer.print(" instanceof ");
	writer.printType(instanceType);
    }
}
