/* CatchBlock Copyright (C) 1997-1998 Jochen Hoenicke.
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
import jode.Type;
import jode.LocalInfo;

/**
 * 
 * @author Jochen Hoenicke
 */
public class CatchBlock extends StructuredBlock {
    
    /**
     * The catch block.
     */
    StructuredBlock catchBlock;

    /**
     * The type of the exception.
     */
    Type exceptionType;
    
    /**
     * The local containing the exception.
     */
    LocalInfo exceptionLocal;

    CatchBlock() {
    }

    public CatchBlock(Type type, LocalInfo local) {
        exceptionType = type;
        exceptionLocal = local;
        used.addElement(exceptionLocal);
    }


    /** 
     * Sets the catch block.
     * @param catchBlock the catch block.
     */
    public void setCatchBlock(StructuredBlock catchBlock) {
        this.catchBlock = catchBlock;
        catchBlock.outer = this;
        catchBlock.setFlowBlock(flowBlock);
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
        if (catchBlock == oldBlock)
            catchBlock = newBlock;
        else
            return false;
        return true;
    }

    /**
     * Returns all sub block of this structured block.
     */
    public StructuredBlock[] getSubBlocks() {        
        return new StructuredBlock[] { catchBlock };
    }

    /**
     * Print the code for the declaration of a local variable.
     * @param writer The tabbed print writer, where we print to.
     * @param local  The local that should be declared.
     */
    public void dumpDeclaration(jode.TabbedPrintWriter writer, LocalInfo local)
        throws java.io.IOException
    {
        if (local != exceptionLocal) {
            /* exceptionLocal will be automatically declared in
             * dumpInstruction.
             */
            super.dumpDeclaration(writer, local);
        }
    }

    public void dumpInstruction(jode.TabbedPrintWriter writer) 
        throws java.io.IOException {
        writer.println("} catch ("+exceptionType.toString() + " "
                       + exceptionLocal.getName().toString()+ ") {");
        writer.tab();
        catchBlock.dumpSource(writer);
        writer.untab();
    }
}
