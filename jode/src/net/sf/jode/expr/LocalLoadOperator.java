/* LocalLoadOperator Copyright (C) 1998-2002 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; see the file COPYING.LESSER.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id$
 */

package net.sf.jode.expr;
import net.sf.jode.type.Type;
import net.sf.jode.decompiler.MethodAnalyzer;
import net.sf.jode.decompiler.LocalInfo;

public class LocalLoadOperator extends LocalVarOperator {

    MethodAnalyzer methodAnalyzer;

    public LocalLoadOperator(Type type, MethodAnalyzer methodAnalyzer,
			     LocalInfo local) {
        super(type, local);
	this.methodAnalyzer = methodAnalyzer;
    }

    public boolean isRead() {
        return true;
    }

    public boolean isWrite() {
        return false;
    }

    public boolean isConstant() {
        return false;
    }

    public void setMethodAnalyzer(MethodAnalyzer ma) {
	methodAnalyzer = ma;
    }

    public boolean opEquals(Operator o) {
        return (o instanceof LocalLoadOperator &&
                ((LocalLoadOperator) o).local.getSlot() == local.getSlot());
    }

    public Expression simplify() {
	if (local.getExpression() != null)
	    return local.getExpression().simplify();
	return super.simplify();
    }
}
