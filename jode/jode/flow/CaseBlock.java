/* CaseBlock Copyright (C) 1997-1998 Jochen Hoenicke.
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

/** 
 * This block represents a case instruction.  A case instruction is a
 * part of a switch construction and may have a subpart or not (for
 * multiple case directly after each other.
 *
 * @author Jochen Hoenicke */
public class CaseBlock extends StructuredBlock {
    /**
     * The inner block that jumps to the subroutine, or null
     * if this is a value only.
     */
    StructuredBlock subBlock;

    /**
     * The value of this case.
     */
    int value;

    /**
     * True, if this is the default case
     */
    boolean isDefault = false;

    /**
     * True, if this is the last case in a switch
     */
    boolean isLastBlock;

    /**
     * The type of the switch value.
     */
    sun.tools.java.Type type;

    public CaseBlock(int value) {
	this.value = value;
	subBlock = null;
    }

    public CaseBlock(int value, Jump dest) {
	this.value = value;
	subBlock = new EmptyBlock(dest);
	subBlock.outer = this;
    }

    /**
     * Replaces the given sub block with a new block.
     * @param oldBlock the old sub block.
     * @param newBlock the new sub block.
     * @return false, if oldBlock wasn't a direct sub block.
     */
    public boolean replaceSubBlock(StructuredBlock oldBlock, 
				   StructuredBlock newBlock) {
        if (subBlock == oldBlock)
            subBlock = newBlock;
        else
            return false;
        return true;
    }

    /**
     * Returns all sub block of this structured block.
     */
    public StructuredBlock[] getSubBlocks() {
	if (subBlock != null) {
	    StructuredBlock[] result = { subBlock };
	    return result;
	}
	return new StructuredBlock[0];
    }

    public void dumpInstruction(jode.TabbedPrintWriter writer) 
        throws java.io.IOException 
    {
	if (isDefault) {
	    if (isLastBlock
		&& subBlock instanceof EmptyBlock
		&& subBlock.jump == null)
		return;
	    writer.println("default:");
	} else
	    writer.println("case " + value /*XXX-type*/ + ":");
	if (subBlock != null) {
	    writer.tab();
	    subBlock.dumpSource(writer);
	    writer.untab();
	}
    }

    /**
     * Determines if there is a sub block, that flows through to the end
     * of this block.  If this returns true, you know that jump is null.
     * @return true, if the jump may be safely changed.
     */
    public boolean jumpMayBeChanged() {
        return subBlock.jump != null || subBlock.jumpMayBeChanged();
    }
}
