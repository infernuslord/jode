/* InstructionBlock Copyright (C) 1998-1999 Jochen Hoenicke.
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

package jode.flow;
import jode.type.Type;
import jode.decompiler.TabbedPrintWriter;
import jode.decompiler.LocalInfo;
import jode.expr.ComplexExpression;
import jode.expr.Expression;
import jode.expr.LocalStoreOperator;

/**
 * This is the structured block for atomic instructions.
 */
public class InstructionBlock extends InstructionContainer {
    /**
     * The loads that are on the stack before cond is executed.
     */
    VariableStack stack;
    /**
     * The local to which we push to, if the instruction is non void
     */
    LocalInfo pushedLocal = null;

    /**
     * Tells if this expression is a initializing declaration.  This
     * can only be set to true and then never be reset.  It is changed
     * by makeDeclaration.  */
    boolean isDeclaration = false;

    public InstructionBlock(Expression instr) {
        super(instr);
    }

    public InstructionBlock(Expression instr, Jump jump) {
        super(instr, jump);
    }

    /**
     * This does take the instr into account and modifies stack
     * accordingly.  It then calls super.mapStackToLocal.
     * @param stack the stack before the instruction is called
     * @return stack the stack afterwards.
     */
    public VariableStack mapStackToLocal(VariableStack stack) {
	VariableStack newStack;
	int params = instr.getOperandCount();
	if (params > 0)
	    this.stack = stack.peek(params);

	if (instr.getType() != Type.tVoid) {
	    pushedLocal = new LocalInfo();
	    pushedLocal.setType(instr.getType());
	    newStack = stack.poppush(params, pushedLocal);
	} else if (params > 0) {
	    newStack = stack.pop(params);
	} else
	    newStack = stack;
	return super.mapStackToLocal(newStack);
    }

    public void removePush() {
	if (stack != null)
	    instr = stack.mergeIntoExpression(instr, used);
	if (pushedLocal != null) {
	    LocalStoreOperator store = new LocalStoreOperator
		(pushedLocal.getType(), pushedLocal, 
		 LocalStoreOperator.ASSIGN_OP);
	    instr = new ComplexExpression(store, new Expression[] { instr });
	    used.addElement(pushedLocal);
	}
	super.removePush();
    }

    /**
     * Tells if this block needs braces when used in a if or while block.
     * @return true if this block should be sorrounded by braces.
     */
    public boolean needsBraces() {
        return declare != null && !declare.isEmpty();
    }

    /**
     * Make the declarations, i.e. initialize the declare variable
     * to correct values.  This will declare every variable that
     * is marked as used, but not done.
     * @param done The set of the already declare variables.
     */
    public void makeDeclaration(VariableSet done) {
        if (instr.getOperator() instanceof LocalStoreOperator) {
	    LocalInfo local = 
		((LocalStoreOperator) instr.getOperator()).getLocalInfo();
            if (!done.contains(local)) {
		/* Special case: This is a variable assignment, and
		 * the variable has not been declared before.  We can
		 * change this to a initializing variable declaration.  
		 */
		isDeclaration = true;
		if (instr instanceof ComplexExpression)
		    ((ComplexExpression) instr)
			.getSubExpressions()[0].makeInitializer();
		done.addElement(local);
		super.makeDeclaration(done);
		done.removeElement(local);
		return;
	    }
	}
	super.makeDeclaration(done);
    }

    public void dumpInstruction(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
        if (isDeclaration) {
            LocalInfo local = ((LocalStoreOperator) instr.getOperator())
                .getLocalInfo();
            writer.println(local.getType().getHint() + " " + instr);
        } else {
            if (instr.getType() != Type.tVoid)
                writer.print("PUSH ");
            writer.println(instr.toString()+";");
        }
    }
}
