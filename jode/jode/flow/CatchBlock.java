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
import jode.LocalInfo;
import sun.tools.java.Identifier;

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
    sun.tools.java.Type exceptionType;
    
    /**
     * The local containing the exception.
     */
    LocalInfo exceptionLocal;

    public CatchBlock(RawTryCatchBlock rawBlock) {
        exceptionType = rawBlock.type;
        this.tryBlock = rawBlock.tryBlock;
        tryBlock.outer = this;
    }

    static int serialno=0;

    /** 
     * Sets the catch block.
     * @param catchBlock the catch block.
     */
    public void setCatchBlock(StructuredBlock catchBlock) {
        if ((catchBlock instanceof SequentialBlock
             && catchBlock.getSubBlocks()[0] instanceof InstructionBlock)
            || catchBlock instanceof InstructionBlock) {
            
            jode.Instruction instr = 
                ((InstructionBlock) (catchBlock instanceof InstructionBlock 
                                     ? catchBlock
                                     : catchBlock.getSubBlocks()[0])
                 ).getInstruction();

            if (instr instanceof jode.PopOperator) {
                exceptionLocal = new LocalInfo(-1);
                exceptionLocal.setName
                    (Identifier.lookup("exception_"+(serialno++)+"_"));
                exceptionLocal.setType(exceptionType);
            } else if (instr instanceof jode.LocalStoreOperator) {
                exceptionLocal = 
                    ((jode.LocalStoreOperator) instr).getLocalInfo();
            }

            if (exceptionLocal != null) {
                StructuredBlock newCatchBlock = 
                    (catchBlock instanceof InstructionBlock 
                     ? new EmptyBlock()
                     : catchBlock.getSubBlocks()[1]);

		newCatchBlock.moveJump(catchBlock.jump);
                catchBlock = newCatchBlock;
            }
        }
        if (exceptionLocal == null) { 
            exceptionLocal = new LocalInfo(-1);
            exceptionLocal.setName(Identifier.lookup("ERROR!!!"));
            exceptionLocal.setType(exceptionType);
        }
        used.addElement(exceptionLocal);
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
        StructuredBlock[] result = { tryBlock, catchBlock };
        return result;
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
             *
             * This is currently broken for nested tries... XXX
             */
            super.dumpDeclaration(writer, local);
        }
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
        writer.println("} catch ("+/*XXX*/exceptionType.typeString
                       (exceptionLocal.getName().toString())+") {");
        writer.tab();
        catchBlock.dumpSource(writer);
        if (!(outer instanceof CatchBlock
              /* XXX || outer instanceof FinallyBlock*/)) {
            writer.untab();
            writer.println("}");
        }
    }
}



