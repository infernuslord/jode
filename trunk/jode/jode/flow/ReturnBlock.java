/* ReturnBlock Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.decompiler.TabbedPrintWriter;
import jode.expr.Expression;

/**
 * This is the structured block for a Return block.
 */
public class ReturnBlock extends InstructionContainer {
    /**
     * The loads that are on the stack before instr is executed.
     */
    VariableStack stack;

    public ReturnBlock() {
	super(null);
    }

    public ReturnBlock(Expression instr) {
        super(instr, new Jump(FlowBlock.END_OF_METHOD));
    }

    /**
     * This does take the instr into account and modifies stack
     * accordingly.  It then calls super.mapStackToLocal.
     * @param stack the stack before the instruction is called
     * @return stack the stack afterwards.
     */
    public VariableStack mapStackToLocal(VariableStack stack) {
	VariableStack newStack = stack;
	if (instr != null) {
	    int params = instr.getOperandCount();
	    if (params > 0) {
		this.stack = stack.peek(params);
		newStack = stack.pop(params);
	    }
	}
	if (jump != null)
	    jump.stackMap = newStack;
	return null;
    }

    public void removePush() {
	if (stack != null)
	    instr = stack.mergeIntoExpression(instr, used);
    }

    /**
     * Tells if this block needs braces when used in a if or while block.
     * @return true if this block should be sorrounded by braces.
     */
    public boolean needsBraces() {
        return declare != null && !declare.isEmpty();
    }

    public void dumpInstruction(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
        writer.println("return" + 
                       (instr == null ? "" : " " + instr) + ";");
    }
}
