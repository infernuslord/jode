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
import jode.type.ClassInterfacesType;
import jode.bytecode.FieldInfo;
import jode.bytecode.ClassInfo;
import jode.bytecode.Reference;
import jode.decompiler.MethodAnalyzer;
import jode.decompiler.ClassAnalyzer;
import jode.decompiler.MethodAnalyzer;
import jode.decompiler.FieldAnalyzer;
import jode.decompiler.TabbedPrintWriter;
import jode.decompiler.Scope;

public class GetFieldOperator extends FieldOperator {
    public GetFieldOperator(MethodAnalyzer methodAnalyzer, boolean staticFlag,
			    Reference ref) {
        super(methodAnalyzer, staticFlag, ref);
    }

    public Expression simplify() {
	if (!staticFlag) {
	    subExpressions[0] = subExpressions[0].simplify();
	    subExpressions[0].parent = this;
	    if (subExpressions[0] instanceof ThisOperator) {
		FieldAnalyzer field = getField();
		/* This should check for isFinal(), but sadly,
		 * sometimes jikes doesn't make a val$ field final.  I
		 * don't know when, or why, so I currently ignore
		 * isFinal.  
		 */
		if (field != null && field.isSynthetic()) {
		    Expression constant = field.getConstant();
		    if (constant instanceof ThisOperator
			|| constant instanceof OuterLocalOperator)
			return constant;
		}
	    }
	}
	return this;
    }

    public boolean opEquals(Operator o) {
	return o instanceof GetFieldOperator
	    && ((GetFieldOperator)o).ref.equals(ref);
    }
}
