/* RemoveEmpty Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.Decompiler;
import jode.expr.*;

public class RemoveEmpty {
    
    public static boolean removeSwap(SpecialBlock swapBlock,
                                     StructuredBlock last) {

        /* Remove non needed swaps; convert:
         *
         *   PUSH expr1
         *   PUSH expr2
         *   SWAP
         *
         * to:
         *
         *   PUSH expr2
         *   PUSH expr1
         */
        if (last.outer instanceof SequentialBlock
            && last.outer.outer instanceof SequentialBlock
            && last.outer.getSubBlocks()[0] instanceof InstructionBlock
            && last.outer.outer.getSubBlocks()[0] 
            instanceof InstructionBlock) {

            InstructionBlock block1 
                = (InstructionBlock) last.outer.outer.getSubBlocks()[0];
            InstructionBlock block2
                = (InstructionBlock) last.outer.getSubBlocks()[0];

            /* XXX check if blocks may be swapped 
             * (there mustn't be side effects in one of them).
             */
            Decompiler.err.println("WARNING: this program contains a SWAP "
			  +"opcode and may not be translated correctly.");

            if (block1.getInstruction().isVoid()
                || block2.getInstruction().isVoid())
                return false;

            /* PUSH expr1 == block1
             * PUSH expr2
             * SWAP
             * ...
             */
            last.outer.replace(block1.outer);
            /* PUSH expr2
             * SWAP
             * ...
             */
            block1.replace(swapBlock);
            block1.moveJump(swapBlock.jump);
            /* PUSH expr2
             * PUSH expr1
             */
            block1.flowBlock.lastModified = block1;
            return true;
        }
        return false;
    }

    public boolean removeEmpty(FlowBlock flow) {
        StructuredBlock lastBlock = flow.lastModified;
        if (lastBlock instanceof EmptyBlock &&
            lastBlock.outer instanceof SequentialBlock &&
            lastBlock.outer.getSubBlocks()[1] == lastBlock) {
            
            StructuredBlock block = lastBlock.outer.getSubBlocks()[0];
            block.replace(block.outer);
            if (lastBlock.jump != null)
                block.moveJump(lastBlock.jump);
            flow.lastModified = block;
            return true;
        }
        if (lastBlock.outer instanceof SequentialBlock &&
            lastBlock.outer.getSubBlocks()[0] instanceof EmptyBlock &&
            lastBlock.outer.getSubBlocks()[0].jump == null) {
            
            lastBlock.replace(lastBlock.outer);
            flow.lastModified = lastBlock;
            return true;
        }
        return false;
    }
}
