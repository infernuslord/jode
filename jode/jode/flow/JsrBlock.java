/* JsrBlock Copyright (C) 1997-1998 Jochen Hoenicke.
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
 * This block represents a jsr instruction.  A jsr instruction is
 * used to call the finally block, or to call the monitorexit block in
 * a synchronized block.
 *
 * @author Jochen Hoenicke
 */
public class JsrBlock extends StructuredBlock {
    /**
     * The inner block that jumps to the subroutine.
     */
    StructuredBlock innerBlock;

    public JsrBlock(Jump next, Jump subroutine) {
	innerBlock = new EmptyBlock(subroutine);
	innerBlock.outer = this;
	setJump(next);
    }

    
    /* The implementation of getNext[Flow]Block is the standard
     * implementation */

    /**
     * Replaces the given sub block with a new block.
     * @param oldBlock the old sub block.
     * @param newBlock the new sub block.
     * @return false, if oldBlock wasn't a direct sub block.
     */
    public boolean replaceSubBlock(StructuredBlock oldBlock, 
				   StructuredBlock newBlock) {
        if (innerBlock == oldBlock)
            innerBlock = newBlock;
        else
            return false;
        return true;
    }

    /**
     * Returns all sub block of this structured block.
     */
    public StructuredBlock[] getSubBlocks() {
	return new StructuredBlock[] { innerBlock };
    }

    public void dumpInstruction(jode.TabbedPrintWriter writer) 
        throws java.io.IOException 
    {
	writer.println("JSR");
	writer.tab();
	innerBlock.dumpSource(writer);
	writer.untab();
    }
}
