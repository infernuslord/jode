/* CheckCastOperator Copyright (C) 1998-1999 Jochen Hoenicke.
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

public class CheckCastOperator extends Operator {
    Type castType;

    public CheckCastOperator(Type type) {
        super(type, 0);
        castType = type;
	initOperands(1);
    }

    public int getPriority() {
        return 700;
    }

    public void updateSubTypes() {
	subExpressions[0].setType(Type.tUObject);
    }

    public void updateType() {
    }

    public void dumpExpression(TabbedPrintWriter writer)
	throws java.io.IOException {
	writer.print("(");
	writer.printType(castType);
	writer.print(") ");

	/* There are special cases where a cast isn't allowed.  We must cast
	 * to the common super type before.  This cases always give a runtime
	 * error, but we want to decompile even bad programs.
	 */
	Type superType = castType.getCastHelper(subExpressions[0].getType());
	if (superType != null) {
	    writer.print("(");
	    writer.printType(superType);
	    writer.print(") ");
	}
	subExpressions[0].dumpExpression(writer, 700);
    }
}
