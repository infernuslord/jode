/* CreateForInitializer Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.GlobalOptions;
import jode.expr.*;

public class CreateForInitializer {

    /**
     * This combines an variable initializer into a for statement
     * @param forBlock the for block
     * @param last  the lastModified of the flow block.
     */
    public static boolean transform(LoopBlock forBlock, StructuredBlock last) {

        if (!(last.outer instanceof SequentialBlock))
            return false;

        SequentialBlock sequBlock = (SequentialBlock) last.outer;

        if (!(sequBlock.subBlocks[0] instanceof InstructionBlock))
            return false;

        InstructionBlock init = (InstructionBlock) sequBlock.subBlocks[0];
            
        if (!init.getInstruction().isVoid()
	    || !(init.getInstruction().getOperator() 
		 instanceof CombineableOperator)
            || !forBlock.conditionMatches(init))
            return false;

        if (GlobalOptions.verboseLevel > 0)
            GlobalOptions.err.print('f');

        forBlock.setInit((InstructionBlock) sequBlock.subBlocks[0]);
        return true;
    }
}
