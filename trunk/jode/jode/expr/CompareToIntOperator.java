/* CompareToIntOperator Copyright (C) 1998-1999 Jochen Hoenicke.
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

public class CompareToIntOperator extends Operator {
    boolean allowsNAN;
    boolean greaterOnNAN;
    Type compareType;

    public CompareToIntOperator(Type type, boolean greaterOnNAN) {
        super(Type.tInt, 0);
        compareType = type;
	this.allowsNAN = (type == Type.tFloat || type == Type.tDouble);
	this.greaterOnNAN = greaterOnNAN;
	initOperands(2);
    }

    public int getPriority() {
        return 499;
    }

    public void updateSubTypes() {
	subExpressions[0].setType(Type.tSubType(compareType));
	subExpressions[1].setType(Type.tSubType(compareType));
    }

    public void updateType() {
    }

    public boolean opEquals(Operator o) {
	return (o instanceof CompareToIntOperator);
    }

    public void dumpExpression(TabbedPrintWriter writer)
	throws java.io.IOException
    {
        subExpressions[0].dumpExpression(writer, 550);
	writer.print(" <=>");
	if (allowsNAN)
	    writer.print(greaterOnNAN ? "g" : "l");
	writer.print(" ");
        subExpressions[1].dumpExpression(writer, 551);
    }
}
