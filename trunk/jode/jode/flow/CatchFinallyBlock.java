/* CatchFinallyBlock Copyright (C) 1997-1998 Jochen Hoenicke.
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
 * 
 * @author Jochen Hoenicke
 */
public class CatchFinallyBlock extends CatchBlock {

    StructuredBlock finallyBlock;

    public CatchFinallyBlock() {
        super(null);
    }

    public void setFinallyBlock(StructuredBlock fin) {
        finallyBlock = fin;
        fin.outer = this;
        fin.setFlowBlock(flowBlock);
    }

    /**
     * Replaces the given sub block with a new block.
     * @param oldBlock the old sub block.
     * @param newBlock the new sub block.
     * @return false, if oldBlock wasn't a direct sub block.
     */
    public boolean replaceSubBlock(StructuredBlock oldBlock, 
                                   StructuredBlock newBlock) {
        if (tryBlock == oldBlock)
            tryBlock = newBlock;
        else if (finallyBlock == oldBlock)
            finallyBlock = newBlock;
        else
            return false;
        return true;
    }

    /**
     * Returns all sub block of this structured block.
     */
    public StructuredBlock[] getSubBlocks() {
        return new StructuredBlock[] { tryBlock, finallyBlock };
    }

    /**
     * Determines if there is a sub block, that flows through to the end
     * of this block.  If this returns true, you know that jump is null.
     * @return true, if the jump may be safely changed.
     */
    public boolean jumpMayBeChanged() {
        return (  tryBlock.jump != null || tryBlock.jumpMayBeChanged())
            && (finallyBlock.jump != null || finallyBlock.jumpMayBeChanged());
    }

    public void dumpInstruction(jode.TabbedPrintWriter writer) 
        throws java.io.IOException {
        /* avoid ugly nested tries */
        if (!(outer instanceof CatchBlock
              /* XXX || outer instanceof FinallyBlock*/)) {
            writer.println("try {");
            writer.tab();
        }
        tryBlock.dumpSource(writer);
        writer.untab();
        writer.println("} finally {");
        writer.tab();
        finallyBlock.dumpSource(writer);
        if (!(outer instanceof CatchBlock
              /* XXX || outer instanceof FinallyBlock*/)) {
            writer.untab();
            writer.println("}");
        }
    }
}



