/* RetBlock Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.decompiler.LocalInfo;
import jode.util.SimpleSet;

/** 
 * This block represents a ret instruction.  A ret instruction is
 * used to call the finally block, or to call the monitorexit block in
 * a synchronized block.
 *
 * @author Jochen Hoenicke
 */
public class RetBlock extends StructuredBlock {
    /**
     * The local containing the return address
     */
    LocalInfo local;

    public RetBlock(LocalInfo local) {
	this.local = local;
    }

    /**
     * Fill all in variables into the given VariableSet.
     * @param in The VariableSet, the in variables should be stored to.
     */
    public void fillInGenSet(VariableSet in, VariableSet gen) {
	in.addElement(local);
	gen.addElement(local);
    }

    /**
     * This does take the instr into account and modifies stack
     * accordingly.  It then calls super.mapStackToLocal.
     * @param stack the stack before the instruction is called
     * @return stack the stack afterwards.
     */
    public VariableStack mapStackToLocal(VariableStack stack) {
	if (!stack.isEmpty())
	    throw new IllegalArgumentException("stack is not empty at RET");
	return null;
    }

    public SimpleSet getDeclarables() {
	SimpleSet used = new SimpleSet();
	used.add(local);
	return used;
    }

    public void dumpInstruction(jode.decompiler.TabbedPrintWriter writer) 
        throws java.io.IOException 
    {
	writer.println("RET "+local);
    }
}
