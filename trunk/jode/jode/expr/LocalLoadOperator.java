/* LocalLoadOperator Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.GlobalOptions;
import jode.type.Type;
import jode.decompiler.CodeAnalyzer;
import jode.decompiler.ClassAnalyzer;
import jode.decompiler.LocalInfo;
import jode.decompiler.TabbedPrintWriter;

public class LocalLoadOperator extends Operator
    implements LocalVarOperator {
    CodeAnalyzer codeAnalyzer;
    LocalInfo local;

    public LocalLoadOperator(Type type, CodeAnalyzer codeAnalyzer,
			     LocalInfo local) {
        super(type);
	this.codeAnalyzer = codeAnalyzer;
        this.local = local;
        local.setOperator(this);
	initOperands(0);
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

    public int getPriority() {
        return 1000;
    }

    public LocalInfo getLocalInfo() {
	return local.getLocalInfo();
    }

    public void setCodeAnalyzer(CodeAnalyzer ca) {
	codeAnalyzer = ca;
    }

    public void setLocalInfo(LocalInfo newLocal) {
	local = newLocal;
	updateType();
    }

    public void updateSubTypes() {
        if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_TYPES) != 0)
	    GlobalOptions.err.println("setType of "+local.getName()+": "
				      +local.getType());
	local.setType(type);
    }

    public void updateType() {
        if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_TYPES) != 0)
            GlobalOptions.err.println("local "+local.getName()+" changed: "
                               +type+" to "+local.getType()
                               +" in "+parent);
	updateParentType(local.getType());
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

    public void dumpExpression(TabbedPrintWriter writer)
	throws java.io.IOException {
	writer.print(local.getName());
    }
}
