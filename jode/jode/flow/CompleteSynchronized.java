/* CompleteSynchronized Copyright (C) 1998-1999 Jochen Hoenicke.
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

public class CompleteSynchronized {

    /**
     * This combines the monitorenter into a synchronized statement
     * @param flow The FlowBlock that is transformed 
     */
    public static boolean enter(SynchronizedBlock synBlock, 
                                StructuredBlock last) {

        if (!(last.outer instanceof SequentialBlock))
            return false;
        
        /* If the program is well formed, the following succeed */
        try {
            SequentialBlock sequBlock = (SequentialBlock) synBlock.outer;
            
            ComplexExpression monenter = (ComplexExpression)
                ((InstructionBlock) sequBlock.subBlocks[0]).getInstruction();
            
            if (!(monenter.getOperator() instanceof MonitorEnterOperator)
                || ((LocalLoadOperator) monenter.getSubExpressions()[0]).
                getLocalInfo() != synBlock.local.getLocalInfo())
                return false;
            
        } catch (ClassCastException ex) {
            return false;
        }
        
        if (GlobalOptions.verboseLevel > 0)
            GlobalOptions.err.print('s');
        
        synBlock.isEntered = true;
        synBlock.moveDefinitions(last.outer,last);
        last.replace(last.outer);
        return true;
    }

    /**
     * This combines the initial expression describing the object
     * into a synchronized statement
     * @param flow The FlowBlock that is transformed 
     */
    public static boolean combineObject(SynchronizedBlock synBlock, 
                                        StructuredBlock last) {

        /* Is there another expression? */
        if (!(last.outer instanceof SequentialBlock))
            return false;

        Expression object;
        try {
            SequentialBlock sequBlock = (SequentialBlock) last.outer;

            ComplexExpression assign = (ComplexExpression)
                ((InstructionBlock) sequBlock.subBlocks[0]).getInstruction();

            if (((LocalStoreOperator) assign.getOperator()).
                getLocalInfo() != synBlock.local.getLocalInfo())
                return false;
        
            object = assign.getSubExpressions()[0];

        } catch (ClassCastException ex) {
            return false;
        }

        synBlock.object = object;
        synBlock.moveDefinitions(last.outer,last);
        last.replace(last.outer);
        return true;
    }
}
