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
import jode.expr.Expression;
import jode.expr.StoreInstruction;
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
	int params = instr.getFreeOperandCount();
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
	    instr = stack.mergeIntoExpression(instr);
	if (pushedLocal != null) {
	    Expression store = new StoreInstruction
		(new LocalStoreOperator
		 (pushedLocal.getType(), pushedLocal)).addOperand(instr);
	    instr = store;
	}
	super.removePush();
    }

    /**
     * Tells if this block needs braces when used in a if or while block.
     * @return true if this block should be sorrounded by braces.
     */
    public boolean needsBraces() {
        return isDeclaration || (declare != null && !declare.isEmpty());
    }

    /**
     * Check if this is an local store instruction to a not yet declared
     * variable.  In that case mark this as declaration and return the 
     * variable.
     */
    public void checkDeclaration(VariableSet declareSet) {
        if (instr instanceof StoreInstruction
	    && (((StoreInstruction)instr).getLValue() 
		instanceof LocalStoreOperator)) {
	    StoreInstruction storeOp = (StoreInstruction) instr;
	    LocalInfo local = 
		((LocalStoreOperator) storeOp.getLValue()).getLocalInfo();
            if (declareSet.contains(local)) {
		/* Special case: This is a variable assignment, and
		 * the variable has not been declared before.  We can
		 * change this to a initializing variable declaration.  
		 */
		isDeclaration = true;
		storeOp.getSubExpressions()[1].makeInitializer();
		declareSet.removeElement(local);
	    }
	}
    }

    /**
     * Make the declarations, i.e. initialize the declare variable
     * to correct values.  This will declare every variable that
     * is marked as used, but not done.
     * @param done The set of the already declare variables.
     */
    public void makeDeclaration(VariableSet done) {
	super.makeDeclaration(done);
	checkDeclaration(declare);
    }

    public void dumpInstruction(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
	if (instr.getType() != Type.tVoid)
	    writer.print("PUSH ");
        else if (isDeclaration) {
            LocalInfo local = 
		((LocalStoreOperator) ((StoreInstruction) instr).getLValue())
		.getLocalInfo();
	    writer.printType(local.getType().getHint());
	    writer.print(" ");
	}
	instr.dumpExpression(writer);
	writer.println(";");
    }
}
