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
import jode.Expression;
import jode.PopOperator;

/**
 * 
 * @author Jochen Hoenicke
 */
public class CatchBlock extends StructuredBlock {
    
    /**
     * The try block.  This may be another CatchBlock!
     */
    StructuredBlock tryBlock;

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

    public CatchBlock(Type type) {
        exceptionType = type;
    }

    static int serialno=0;

    public void setTryBlock(StructuredBlock tryBlock) {
        this.tryBlock = tryBlock;
        tryBlock.outer = this;
        tryBlock.setFlowBlock(flowBlock);
    }

    /** 
     * Sets the catch block.
     * @param catchBlock the catch block.
     */
    public void setCatchBlock(StructuredBlock catchBlock) {
        this.catchBlock = catchBlock;
        catchBlock.outer = this;
        catchBlock.setFlowBlock(flowBlock);

        StructuredBlock firstInstr = (catchBlock instanceof SequentialBlock)
            ? catchBlock.getSubBlocks()[0] : catchBlock;

        if (firstInstr instanceof InstructionBlock) {
            Expression instr = 
                ((InstructionBlock) firstInstr).getInstruction();
            if (instr instanceof PopOperator
                && ((PopOperator) instr).getCount() == 1) {
                /* The exception is ignored.  Create a dummy local for it */
                exceptionLocal = new LocalInfo(-1);
                exceptionLocal.setName("exception_"+(serialno++)+"_");

            } else if (instr instanceof jode.LocalStoreOperator) {
                /* The exception is stored in a local variable */
                exceptionLocal = 
                    ((jode.LocalStoreOperator) instr).getLocalInfo();
            }
        }

        if (exceptionLocal != null)
            firstInstr.removeBlock();
        else {
            exceptionLocal = new LocalInfo(-1);
            exceptionLocal.setName("ERROR!!!");
        }

        exceptionLocal.setType(exceptionType);
        used.addElement(exceptionLocal);
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
        if (tryBlock == oldBlock)
            tryBlock = newBlock;
        else if (catchBlock == oldBlock)
            catchBlock = newBlock;
        else
            return false;
        return true;
    }

    /**
     * Returns all sub block of this structured block.
     */
    public StructuredBlock[] getSubBlocks() {        
        return new StructuredBlock[] { tryBlock, catchBlock };
    }

    /**
     * Determines if there is a sub block, that flows through to the end
     * of this block.  If this returns true, you know that jump is null.
     * @return true, if the jump may be safely changed.
     */
    public boolean jumpMayBeChanged() {
        return (  tryBlock.jump != null || tryBlock.jumpMayBeChanged())
            && (catchBlock.jump != null || catchBlock.jumpMayBeChanged());
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
        /* avoid ugly nested tries */
        if (!(outer instanceof CatchBlock)) {
            writer.println("try {");
            writer.tab();
        }
        tryBlock.dumpSource(writer);
        writer.untab();
        writer.println("} catch ("+exceptionType.toString() + " "
                       + exceptionLocal.getName().toString()+ ") {");
        writer.tab();
        catchBlock.dumpSource(writer);
        if (!(outer instanceof CatchBlock)) {
            writer.untab();
            writer.println("}");
        }
    }
}
