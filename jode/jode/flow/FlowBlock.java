/* 
 * FlowBlock  (c) 1998 Jochen Hoenicke
 *
 * You may distribute under the terms of the GNU General Public License.
 *
 * IN NO EVENT SHALL JOCHEN HOENICKE BE LIABLE TO ANY PARTY FOR DIRECT,
 * INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT OF
 * THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF JOCHEN HOENICKE 
 * HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * JOCHEN HOENICKE SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS"
 * BASIS, AND JOCHEN HOENICKE HAS NO OBLIGATION TO PROVIDE MAINTENANCE,
 * SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * $Id$
 */

package jode.flow;
import java.util.*;
import jode.TabbedPrintWriter;
import jode.Expression;
import jode.CodeAnalyzer;
import jode.Decompiler;

/**
 * A flow block is the structure of which the flow graph consists.  A
 * flow block contains structured code together with some conditional
 * or unconditional jumps to the head of other flow blocks.
 *
 * We do a T1/T2 analysis to combine all flow blocks to a single.  If
 * the graph isn't reducible that doesn't work, but java can only
 * produce reducible flow graphs.
 */
public class FlowBlock {

    static FlowBlock END_OF_METHOD = 
        new FlowBlock(null, Integer.MAX_VALUE, 0, new EmptyBlock());

    static {
        END_OF_METHOD.label = "END_OF_METHOD";
    }

    /**
     * The code analyzer.  This is used to pretty printing the
     * Types and to get information about all locals in this code.
     */
    CodeAnalyzer code;

    /**
     * The in locals.  This are the locals, which are used in this
     * flow block and whose values may be the result of a assignment
     * outside of this flow block.  That means, that there is a
     * path from the start of the flow block to the instruction that
     * uses that variable, on which it is never assigned 
     */
    VariableSet in = new VariableSet(); 

    /**
     * The starting address of this flow block.  This is mainly used
     * to produce the source code in code order.
     */
    int addr;

    /**
     * The length of the structured block, only needed at the beginning.
     */
    int length;

    /**
     * The outermost structructed block in this flow block.
     */
    StructuredBlock block;

    /**
     * The last modified structured block.
     */
    StructuredBlock lastModified;

    /**
     * All Jumps that this flow block contains.  The objects may be
     * null, if they were marked as deleted.  */
    Vector successors;

    /**
     * This is a vector of flow blocks, which reference this block.
     * Only if this vector contains exactly one element, it can be
     * moved into the preceding flow block.
     *
     * If this vectors contains the null element, this is the first
     * flow block in a method.
     */
    Vector predecessors;

    /**
     * The default constructor.  Creates a new flowblock containing
     * only the given structured block.
     */
    public FlowBlock(CodeAnalyzer code, int addr, int length, 
		     StructuredBlock block) {
	this.code = code;
        this.addr = addr;
        this.length = length;
        this.block = block;
        lastModified = block;
        predecessors = new Vector(); // filled in later
        successors   = new Vector();
        block.setFlowBlock(this);
        block.fillInSet(in);
        block.fillSuccessors(successors);
    }

    public int getNextAddr() {
        return addr+length;
    }


    /**
     * Create a Catch- resp. FinallyBlock (maybe even SynchronizedBlock)
     * @param sequBlock a SequentialBlock whose first sub block is a 
     * RawTryCatchBlock.
     */
    public StructuredBlock createCatchBlock(SequentialBlock sequBlock) {
        RawTryCatchBlock tryBlock = (RawTryCatchBlock) sequBlock.subBlocks[0];
        StructuredBlock catchBlock = sequBlock.subBlocks[1];
        if (tryBlock.type != null) {
            /*XXX crude hack */
            catchBlock.replace(sequBlock, tryBlock);
            CatchBlock newBlock = new CatchBlock(tryBlock);
            newBlock.replace(catchBlock, catchBlock);
            newBlock.setCatchBlock(catchBlock);
            if (sequBlock.jump != null) {
                if (newBlock.catchBlock.jump == null)
                    newBlock.catchBlock.moveJump(sequBlock.jump);
                else
                    sequBlock.removeJump();
            }
            return newBlock;
        } else {
            /* XXX implement finally */
            return sequBlock;
        }
    }
    
    /**
     * This method optimizes the jumps to successor.
     * Returns the new appendBlock, it may have changed.
     */
    public StructuredBlock optimizeJumps(FlowBlock successor,
                                         StructuredBlock appendBlock) {
        Enumeration enum = successors.elements();
    next_jump:
        while (enum.hasMoreElements()) {
            Jump jump = (Jump) enum.nextElement();

            if (jump == null || jump.destination != successor)
                continue next_jump;

        while(jump != null) {

            if (jump.prev instanceof EmptyBlock
		&& jump.prev.outer != null
                && jump.prev.outer instanceof ConditionalBlock
		&& jump.prev.outer.jump != null) {

		if (jump.prev.outer.jump.destination == jump.destination) {
		    /* This is a weired "if (cond) empty"-block.  We
		     * transform it by hand.
		     */
		    jump.prev.removeJump();
		    continue next_jump;
		}

		/* Swap conditional blocks, that have two jumps, and where
		 * this jump is the inner jump.  
		 */
		StructuredBlock prev = jump.prev;
		ConditionalBlock cb = (ConditionalBlock) prev.outer;
		jode.Instruction instr = cb.getInstruction();
		
		/* XXX Expression clean up is necessary.  Otherwise
		 * this may lead to a ClassCastException.
		 *
		 * Our code below _depends_ on the fact that this
		 * transformation is done.
		 */
		cb.setInstruction(((jode.Expression)instr).negate());
                cb.swapJump(jump.prev);
	    }

	    /* Now move the jump as far to the outer as possible,
	     * without leaving appendBlock.
	     *
	     * Note: jump.prev != appendblock implies
	     * jump.prev.outer != null, since appendBlock is an outer
	     * block of jump.prev 
	     */
	    while (jump.prev != appendBlock
		   && jump.prev.outer.isSingleExit(jump.prev)) {
		jump.prev.outer.moveJump(jump);
	    }

            /* if the jump is the jump of the appendBlock, skip it.
             */
            if (jump.prev == appendBlock)
                continue next_jump;

            /* remove this jump if it jumps to the getNextFlowBlock().  
	     */
            if (jump.prev.outer.getNextFlowBlock(jump.prev) == successor) {
                jump.prev.removeJump();
                continue next_jump;
            }

            if (jump.prev instanceof EmptyBlock &&
                jump.prev.outer instanceof ConditionalBlock) {

                StructuredBlock prev = jump.prev;
                ConditionalBlock cb = (ConditionalBlock) prev.outer;
                jode.Instruction instr = cb.getInstruction();

                /* cb.jump is null (see above), so cb must have a *
                 * successor in this block, that means cb.outer is not
                 * null.
		 */

		/* If this is the first instruction of a while and the
		 * condition of the while is true, use the condition
		 * as while condition.  
		 */

                /* This is the first instruction in a while block */
                if (cb.outer instanceof SequentialBlock &&
                    cb.outer.getSubBlocks()[0] == cb &&
                    cb.outer.outer instanceof LoopBlock) {

                    LoopBlock loopBlock = (LoopBlock) cb.outer.outer;
                    if (loopBlock.getCondition() == LoopBlock.TRUE &&
                        loopBlock.getType() != LoopBlock.DOWHILE &&
                        loopBlock.getNextFlowBlock() == successor &&
                        instr instanceof Expression) {
                        
                        prev.removeJump();
                        loopBlock.setCondition(((Expression)instr).negate());
                        if (cb.outer.jump != null) {
			    /* XXX can this happen */
                            if (cb.outer.getSubBlocks()[1].jump != null) {
				/* XXX if above can happen,
				 * can this happen at all??? */
                                cb.outer.removeJump();
                            } else
                                cb.outer.getSubBlocks()[1].
				    moveJump(cb.outer.jump);
                        }
                        cb.outer.getSubBlocks()[1].replace
                            (cb.outer, cb.outer.getSubBlocks()[1]);
                        /* cb and cb.outer are not used any more */
                        /* Note that cb.outer != appendBlock because
                         * appendBlock contains loopBlock
                         */
                        continue next_jump;
                    }
                }

                /* Now the same for the empty loop. In this case there is
                 * no sequential block.  
                 */
                if (cb.outer instanceof LoopBlock) {
                    LoopBlock loopBlock = (LoopBlock) cb.outer;
                    if (loopBlock.getCondition() == LoopBlock.TRUE &&
                        loopBlock.getType() != LoopBlock.DOWHILE &&
                        loopBlock.getNextFlowBlock() == successor &&
                        instr instanceof Expression) {
                        
                        prev.removeJump();
                        loopBlock.setCondition(((Expression)instr).negate());
                        
                        EmptyBlock empty = new EmptyBlock();
                        empty.replace(cb, null);
                        /* cb is not used any more */
                        continue next_jump;
                    }
                }

		/* replace all conditional jumps to the successor, which
		 * are followed by a block which has the end of the block
		 * as normal successor, with "if (not condition) block".  
		 */
                if (cb.outer instanceof SequentialBlock && 
                    cb.outer.getSubBlocks()[0] == cb &&
                    (cb.outer.getNextFlowBlock() == successor ||
                     cb.outer.jumpMayBeChanged()) &&
                    instr instanceof jode.Expression) {
                    SequentialBlock sequBlock = 
                        (SequentialBlock) cb.outer;
                    
                    IfThenElseBlock newIfBlock = 
                        new IfThenElseBlock(((jode.Expression)instr).negate());

                    newIfBlock.replace(sequBlock, sequBlock.getSubBlocks()[1]);
                    newIfBlock.setThenBlock(sequBlock.getSubBlocks()[1]);

                    newIfBlock.moveJump(sequBlock.jump);
                    if (appendBlock == sequBlock)
                        appendBlock = newIfBlock;

                    if (newIfBlock.getNextFlowBlock() != successor &&
                        newIfBlock != appendBlock) {
                        newIfBlock.moveJump(jump);
                        continue;
                    } else {
                        prev.removeJump();
                        continue next_jump;
                    }
                }
            }
                

            /* if this is a jumps at the end of a then block belonging
	     * to a if-then block without else part, and the if-then
	     * block is followed by a single block, then replace the
	     * if-then block with a if-then-else block and remove the
	     * unconditional jump.  
	     */
            else if (jump.prev.outer instanceof IfThenElseBlock) {

                IfThenElseBlock ifBlock = 
                    (IfThenElseBlock)jump.prev.outer;
		if (ifBlock.elseBlock == null
		    && ifBlock.jump == null
		    && ifBlock.outer instanceof SequentialBlock
		    && ifBlock.outer.getSubBlocks()[0] == ifBlock
		    && (ifBlock.outer.getNextFlowBlock() == successor
			|| ifBlock.outer.jumpMayBeChanged())) {
		    
		    StructuredBlock elseBlock =
			ifBlock.outer.getSubBlocks()[1];

		    ifBlock.outer.removeJump();
		    ifBlock.replace(ifBlock.outer, elseBlock);

		    if (appendBlock == elseBlock.outer)
			appendBlock = ifBlock;

		    ifBlock.moveJump(jump);
		    ifBlock.setElseBlock(elseBlock);
		    continue;
		}
            }

            /* if this is a jump in a breakable block, and that block
             * has not yet a next block, then create a new jump to that
             * successor.
             *
             * The break to the block will be generated later.
             */

            for (StructuredBlock surrounder = jump.prev.outer;
                 surrounder != null && surrounder != appendBlock.outer; 
                 surrounder = surrounder.outer) {
                if (surrounder instanceof BreakableBlock) {
                    if (surrounder.getNextFlowBlock() != successor
                        && surrounder.jumpMayBeChanged()) {

                        surrounder.setJump(new Jump(successor));
                        successors.addElement(surrounder.jump);
                    }
                }
            }

// 	    /* XXX - There is a problem with this.
// 	     * The here created break could be obsoleted later.
// 	     * We should instead only add the jump to surrounder
// 	     * if necessary and add the break after all jumps
// 	     * has been considered.
// 	     */

//             /* if there are jumps in a while block or switch block and the
//              * while/switch block is followed by a jump to successor or has
//              * successor as getNextFlowBlock(), replace jump with break to
//              * the innermost such while/switch block.
//              *
//              * If the switch block hasn't been breaked before we could
//              * take some heuristics and add a jump after the switch to
//              * succesor, so that the above succeeds.
//              */
//             int breaklevel = 0;
//             for (StructuredBlock surrounder = jump.prev.outer;
//                  surrounder != null && surrounder != appendBlock.outer; 
//                  surrounder = surrounder.outer) {
//                 if (surrounder instanceof BreakableBlock) {
//                     breaklevel++;
//                     if (surrounder.getNextFlowBlock() == successor ||
//                         surrounder.jumpMayBeChanged()) {

//                         SequentialBlock sequBlock = new SequentialBlock();
//                         StructuredBlock prevBlock = jump.prev;
//                         if (surrounder.getNextFlowBlock() != successor)
//                             surrounder.moveJump(prevBlock.jump);
//                         else {
//                             prevBlock.removeJump();
// 			    jump = null;
// 			}

// 			prevBlock.appendBlock
// 			    (new BreakBlock((BreakableBlock) surrounder,
// 					    breaklevel >1));
//                         continue;
//                     }
//                 }
//             }
            continue next_jump;
        }
        }
        return appendBlock;
    }

    /** 
     * Updates the in/out-Vectors of the structured block of the
     * successing flow block simultanous to a T1 transformation.
     * @param successor The flow block which is unified with this flow
     * block.  
     * @return The variables that must be defined in this block.
     */
    void updateInOut (FlowBlock successor, boolean t1Transformation) {
        /* First get the out vectors of all jumps to successor and
         * calculate the intersection.
         */
        VariableSet gens = new VariableSet();
        VariableSet kills =  null;
        Enumeration enum = successors.elements();
        while (enum.hasMoreElements()) {
            Jump jump = (Jump) enum.nextElement();
            if (jump == null || jump.destination != successor)
                continue;
            
            gens.unionExact(jump.gen);
            if (kills == null) 
                kills = jump.kill;
            else
                kills = kills.intersect(jump.kill);
        }

//         System.err.println("UpdateInOut: allOuts     : "+allOuts);
//         System.err.println("             intersectOut: "+intersectOut);
        
        /* Merge the locals used in successing block with those written
         * by this blocks
         */
        successor.in.merge(gens);
        
        /* Now update in and out set of successing block */

        if (t1Transformation)
            successor.in.subtract(kills);
        /* The gen/kill sets must be updated for every jump in the block */
        enum = successor.successors.elements();
        while (enum.hasMoreElements()) {
            Jump jump = (Jump) enum.nextElement();
            if (jump != null) {
                jump.gen.mergeGenKill(gens, jump.kill);
                if (t1Transformation)
                    jump.kill.add(kills);
            }
        }
//         System.err.println("             successor.in: "+successor.in);
        in.unionExact(successor.in);
//         System.err.println("             in          : "+in);
    }
    

    /* Special cases:
      *
      *   try-header
      *      |- first instruction
      *      |  ...
      *      |  last instruction
      *      |- optional jump (last+1)
      *      |  ...
      *      `- catch block
      *
      *  A try block may have many try-headers with different catch blocks
      *  and there may be a finally block:
      *
      *   try-header any
      *    | try-header
      *    |--|- first instruction
      *    |  |  ...
      *    |  |   every jump to outside is preceded by jsr finally
      *    |  |  ...
      *    |  |  last instruction
      *    |  |- optional jump after catch block (last+1)
      *    |  |  ...            |
      *    |  `- catch block    |
      *    |     ...            |
      *    |  ,-----------------'
      *    |  |-jump after all catch blocks
      *    |  v
      *    |  jsr finally -----------------,
      *    |- jump after finally           |
      *    `- catch any (local_n)          v
      *       jsr finally ---------------->|
      *       throw local_n;               |
      *   finally: <-----------------------'
      *      astore_n
      *      ...
      *      return_n
      *
      *
      *   flow-block
      *     finally-block
      *       ---> try-header
      *     finally {
      *       ---> first-finally-instruction
      *
      *  A synchronized block uses a similar technique:
      *
      *  local_x = monitor object;
      *  monitorenter local_x
      *  try-header any
      *   |- syncronized block
      *   |  ...
      *   |   every jump to outside is preceded by jsr monexit ---,
      *   |  ...                                                  |
      *   |- monitorexit local_x                                  |
      *   |  jump after this block (without jsr monexit)          |
      *   `- catch any (local_n)                                  |
      *      monitorexit local_x                                  |
      *      throw local_n                                        |
      *  monexit: <-----------------------------------------------'
      *    astore_n
      *    monitorexit local_x
      *    return_n
      */

    public void checkConsistent() {
        if (!Decompiler.doChecks)
            return;
        if (block.outer != null || block.flowBlock != this) {
            throw new RuntimeException("Inconsistency");
        }
        block.checkConsistent();
        Enumeration enum = successors.elements();
        while (enum.hasMoreElements()) {
            Jump jump = (Jump) enum.nextElement();
            if (jump == null)
                continue;
                
            if (jump.prev.flowBlock != this ||
                jump.prev.jump != jump)
                throw new RuntimeException("Inconsistency");

            StructuredBlock sb = jump.prev;
            while (sb != block) {
                if (sb.outer == null)
                    throw new RuntimeException("Inconsistency");
                StructuredBlock[] blocks = sb.outer.getSubBlocks();
                int i;
                for (i=0; i<blocks.length; i++)
                    if (blocks[i] == sb)
                        break;
                if (i == blocks.length)
                    throw new RuntimeException("Inconsistency");
                sb = sb.outer;
            }
        }
    }

    /**
     * Do a T1 transformation with succ if possible.  It is possible,
     * iff succ has exactly this block as predecessor.
     * @param succ the successor block, must be a valid successor of this
     * block, i.e. not null
     */
    public boolean doT1(FlowBlock succ) {
        /* check if this successor has only this block as predecessor. 
         * if the predecessor is not unique, return false. */
        if (succ.predecessors.size() != 1 ||
            succ.predecessors.elementAt(0) != this)
            return false;

        try{
            checkConsistent();
            succ.checkConsistent();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            try {
                jode.TabbedPrintWriter writer = 
                    new jode.TabbedPrintWriter(System.err, "    ");
                writer.tab();
                block.dumpSource(writer);
                    succ.block.dumpSource(writer);
            } catch (java.io.IOException ioex) {
            }
        }

        /* First find the innermost block that contains all jumps to this
         * successor and the last modified block.
         */
        Enumeration enum = successors.elements();
        StructuredBlock appendBlock = lastModified;
        while(enum.hasMoreElements()) {
            Jump jump = (Jump) enum.nextElement();
            if (jump == null || jump.destination != succ)
                continue;

            while (!appendBlock.contains(jump.prev)) {
                appendBlock = appendBlock.outer;
                if (appendBlock instanceof SequentialBlock
                    && appendBlock.getSubBlocks()[0] 
                    instanceof RawTryCatchBlock) {
                    
                    /* We leave the catch block of a raw-try-catch-block.
                     * We shall now create the Catch- resp. FinallyBlock.
                     */
                    appendBlock = 
                        createCatchBlock((SequentialBlock)appendBlock);
                }
            }
            /* appendBlock can't be null now, because the
             * outermost block contains every structured block.  
             */
        }

        /* Update the in/out-Vectors now */
        updateInOut(succ, true);

        /* The switch "fall through" case: if the appendBlock is a
         * switch, and the successor is the address of a case, and all
         * other successors are inside the block preceding that case.  
         */
        StructuredBlock precedingcase = null;
        StructuredBlock nextcase = null;
	if (appendBlock instanceof SwitchBlock) {
	    nextcase = ((SwitchBlock) appendBlock).findCase(succ);
	    precedingcase = 
		((SwitchBlock) appendBlock).prevCase(nextcase);
	    
	    enum = successors.elements();
	    while (nextcase != null && enum.hasMoreElements()) {
		Jump jump = (Jump) enum.nextElement();
		if (jump == null
		    || jump.destination != succ 
		    || jump.prev == nextcase
		    || (precedingcase != null 
			&& precedingcase.contains(jump.prev)))
		    continue;
                nextcase = null;
	    }
	}
        if (nextcase != null) {
            SwitchBlock switchBlock = (SwitchBlock) appendBlock;

            /* Now put the succ.block into the next case.
             */
	    nextcase.removeJump();
            succ.block.replace(nextcase, succ.block);
            /* nextcase is not referenced any more */

            /* Do the following modifications on the struct block. */
            appendBlock = precedingcase;

        } else {

            /* Prepare the unification of the blocks: Make sure if
             * possible that appendBlock has a successor outside of
             * this block.  
             *
             * This doesn't change the semantics, since appendBlock
             * is the last block that could be modified. 
             * XXX (is this true for switches)*/
            if (appendBlock.jump == null) {
                Jump jump = new Jump(succ);
                appendBlock.setJump(jump);
                successors.addElement(jump);
            }

            /* Now unify the blocks: Create a new SequentialBlock
             * containing appendBlock and successor.block.  Then replace
             * appendBlock with the new sequential block.
             */
            SequentialBlock sequBlock = 
                new SequentialBlock();
            sequBlock.replace(appendBlock, appendBlock);
            sequBlock.setFirst(appendBlock);
            sequBlock.setSecond(succ.block);
        }

        /* Merge the sucessors from the successing flow block
         */
        enum = succ.successors.elements();
        while (enum.hasMoreElements()) {
            Jump jump = (Jump) enum.nextElement();
            if (jump == null)
                continue;
            successors.addElement(jump);
            if (jump.destination.predecessors.contains(succ)) {
                /*XXX comment and make clearer, better etc.*/
                jump.destination.predecessors.removeElement(succ);
                if (!jump.destination.predecessors.contains(this))
                    jump.destination.predecessors.addElement(this);
            }
        }

        try {
            checkConsistent();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            try {
                jode.TabbedPrintWriter writer = 
                    new jode.TabbedPrintWriter(System.err, "    ");
                writer.tab();
                block.dumpSource(writer);
            } catch (java.io.IOException ioex) {
            }
        }

        /* Try to eliminate as many jumps as possible.
         */

	/* appendBlock may be zero, if this is the switchcase with
	 * precedingcase = null.  But in this case, there can't be
	 * any jumps.
	 */
	if (appendBlock != null) {
	    
            appendBlock = optimizeJumps(succ, appendBlock);
            
            try {
                checkConsistent();
            } catch (RuntimeException ex) {
                ex.printStackTrace();
                try {
                    jode.TabbedPrintWriter writer = 
                        new jode.TabbedPrintWriter(System.err, "    ");
                    writer.tab();
                    block.dumpSource(writer);
                } catch (java.io.IOException ioex) {
                }
            }

	    LoopBlock doWhileFalse = null;
	    enum = successors.elements();
        next_jump:
	    while (enum.hasMoreElements()) {
		Jump jump = (Jump) enum.nextElement();
	    
		if (jump == null || jump.destination != succ
                    || jump.prev == appendBlock)
		    continue;
		
                int breaklevel = 0;
                BreakableBlock breakToBlock = null;
                for (StructuredBlock surrounder = jump.prev.outer;
                     surrounder != null && surrounder != appendBlock.outer; 
                     surrounder = surrounder.outer) {
                    if (surrounder instanceof BreakableBlock) {
                        breaklevel++;
                        if (surrounder.getNextFlowBlock() == succ) {
                            breakToBlock = (BreakableBlock) surrounder;
                            break;
                        }
                    }
                }

                StructuredBlock prevBlock = jump.prev;
                prevBlock.removeJump();

                if (breakToBlock == null) {
                    /* Nothing else helped, so put a do/while(0)
                     * block around appendBlock and break to that
                     * block.
                     */
                    if (doWhileFalse == null)
                        doWhileFalse = new LoopBlock(LoopBlock.DOWHILE, 
                                                     LoopBlock.FALSE);
                    prevBlock.appendBlock
                        (new BreakBlock(doWhileFalse, breaklevel > 0));
                } else
                    prevBlock.appendBlock
                        (new BreakBlock(breakToBlock, breaklevel > 1));
	    }
	    
	    if (doWhileFalse != null) {
		doWhileFalse.replace(appendBlock, appendBlock);
		doWhileFalse.setBody(appendBlock);
	    }

	    /* Now remove the jump of the appendBlock if it points to
	     * successor.  
	     */
	    if (appendBlock.jump != null
		&& appendBlock.jump.destination == succ)
		appendBlock.removeJump();
	}
	    
	/* Believe it or not: Now the rule, that the first part of a
	 * SequentialBlock shouldn't be another SequentialBlock is
	 * fulfilled. <p>
	 *
	 * This isn't easy to prove, it has a lot to do with the
         * transformation in optimizeJump and the fact that
         * appendBlock was the innermost Block containing all jumps
         * and lastModified.
         */

        /* Set last modified to correct value.  */
        lastModified = succ.lastModified;

        /* Set addr+length to (semi-)correct value */
        if (succ.addr < addr)
            addr = succ.addr;
        length += succ.length;

        /* T1 transformation succeeded */
        try {
//             System.err.println("T1 succeeded:");
//             System.err.println("===in: "+in);
            checkConsistent();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            try {
                jode.TabbedPrintWriter writer = 
                    new jode.TabbedPrintWriter(System.err, "    ");
                writer.tab();
                block.dumpSource(writer);
            } catch (java.io.IOException ioex) {
            }
        }
        return true;
    }

    public boolean doT2(int start, int end  /* Vector triedBlocks */) {
        /* If there are no jumps to the beginning of this flow block
         * or if this block has other predecessors with a higher
         * address, return false.  The second condition make sure that
         * the while isn't created up to the first continue.  */
        if (!predecessors.contains(this))
            return false;

        Enumeration preds = predecessors.elements();
        while (preds.hasMoreElements()) {
            FlowBlock predFlow = (FlowBlock) preds.nextElement();
            if (predFlow != null && predFlow != this
                && predFlow.addr >= start && predFlow.addr < end
                /*&& !triedBlocks.contains(predFlow)*/) {
//                 System.err.println("refusing T2 on: "+getLabel()+
//                                    " because of "+predFlow.getLabel());
                /* XXX Is this enough to refuse T2 trafo ??? */
                return false;
            }
        }

        try {
//             System.err.println("doing T2 analysis on: "+getLabel());
            checkConsistent();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            try {
                jode.TabbedPrintWriter writer = 
                    new jode.TabbedPrintWriter(System.err, "    ");
                writer.tab();
                block.dumpSource(writer);
            } catch (java.io.IOException ioex) {
            }
        }

        /* Update the in/out-Vectors now */
        updateInOut(this, false);

	
	while (lastModified != block) {
	    lastModified = lastModified.outer;
	    if (lastModified instanceof SequentialBlock
		&& lastModified.getSubBlocks()[0] 
		instanceof RawTryCatchBlock) {
		
		/* We leave the catch block of a raw-try-catch-block.
		 * We shall now create the Catch- resp. FinallyBlock.
		 */
		lastModified = 
		    createCatchBlock((SequentialBlock)lastModified);
	    }
	}

        /* If there is only one jump to the beginning and it is the
         * last jump and (there is a do/while(0) block surrounding
         * everything but the last instruction, or the last
         * instruction is a increase/decrease statement), replace the
         * do/while(0) with a for(;;last_instr) resp. create a new one
         * and replace breaks to do/while with continue to for.  
         */
        /* XXX implement above */
        /* XXX condition for do/while(cond) blocks */
        {
            /* Otherwise: */

            /* create a new while(true) block.
             */
            StructuredBlock bodyBlock = block;
            
            /* Prepare the unification of the blocks: Make sure that
             * bodyBlock has a jump.  */
            if (bodyBlock.jump == null) {
                Jump jump = new Jump(this);
                bodyBlock.setJump(jump);
                successors.addElement(jump);
            }

            LoopBlock whileBlock = 
                new LoopBlock(LoopBlock.WHILE, LoopBlock.TRUE);

            whileBlock.replace(bodyBlock, bodyBlock);
            whileBlock.setBody(bodyBlock);

            /* Try to eliminate as many jumps as possible.
             */
            bodyBlock = optimizeJumps(this, bodyBlock);

            /* if there are further jumps to this, replace every jump with a
             * continue to while block and return true.  
             */
            Enumeration enum = successors.elements();
            while (enum.hasMoreElements()) {
                Jump jump = (Jump) enum.nextElement();
                
                if (jump == null || jump.destination != this
                    || jump.prev == bodyBlock)
                    continue;
                
                int breaklevel = 0, continuelevel = 0;
                BreakableBlock breakToBlock = null;
                for (StructuredBlock surrounder = jump.prev.outer;
                     surrounder != whileBlock; 
                     surrounder = surrounder.outer) {
                    if (surrounder instanceof BreakableBlock) {
                        if (surrounder instanceof LoopBlock)
                            continuelevel++;
                        breaklevel++;
                        if (surrounder.getNextFlowBlock() == this) {
                            breakToBlock = (BreakableBlock) surrounder;
                            break;
                        }
                    }
                }
                StructuredBlock prevBlock = jump.prev;
                prevBlock.removeJump();
                if (breakToBlock == null)
                    prevBlock.appendBlock
                        (new ContinueBlock(whileBlock, continuelevel > 0));
                else
                    prevBlock.appendBlock
                        (new BreakBlock(breakToBlock, breaklevel > 1));
                    
            }

            /* Now remove the jump of block if it points to this.
             */
            if (bodyBlock.jump != null &&
                bodyBlock.jump.destination == this)
                bodyBlock.removeJump();

            lastModified = whileBlock;
        }

        /* remove ourself from the predecessor list.
         */
        predecessors.removeElement(this);

        /* T2 analysis succeeded */
        try {
//             System.err.println("T2 succeeded:");
            checkConsistent();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            try {
                jode.TabbedPrintWriter writer = 
                    new jode.TabbedPrintWriter(System.err, "    ");
                writer.tab();
                block.dumpSource(writer);
            } catch (java.io.IOException ioex) {
            }
        }

        return true;
    }


    /**
     * Do a T1 transformation with the end_of_method block.
     */
    public void mergeEndBlock() {
        try{
            checkConsistent();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            try {
                jode.TabbedPrintWriter writer = 
                    new jode.TabbedPrintWriter(System.err, "    ");
                writer.tab();
                block.dumpSource(writer);
            } catch (java.io.IOException ioex) {
            }
        }

        /* First find the innermost block that contains all jumps to the
         * END_OF_METHOD block.
         */
        Enumeration enum = successors.elements();
        StructuredBlock appendBlock = lastModified;
        while(enum.hasMoreElements()) {
            Jump jump = (Jump) enum.nextElement();
            if (jump == null || jump.destination != END_OF_METHOD)
                continue;

            while (!appendBlock.contains(jump.prev)) {
                appendBlock = appendBlock.outer;
                if (appendBlock instanceof SequentialBlock
                    && appendBlock.getSubBlocks()[0] 
                    instanceof RawTryCatchBlock) {
                    
                    /* We leave the catch block of a raw-try-catch-block.
                     * We shall now create the Catch- resp. FinallyBlock.
                     */
                    appendBlock = 
                        createCatchBlock((SequentialBlock)appendBlock);
                }
            }
            /* appendBlock can't be null now, because the
             * outermost block contains every structured block.  
             */
        }

        /* Try to eliminate as many jumps as possible.
         */

        appendBlock = optimizeJumps(END_OF_METHOD, appendBlock);
            
        enum = successors.elements();
    next_jump:
        while (enum.hasMoreElements()) {
            Jump jump = (Jump) enum.nextElement();
	    
            if (jump == null || jump.destination != END_OF_METHOD ||
                jump.prev == appendBlock)
                continue;
            
            BreakableBlock breakToBlock = null;
            for (StructuredBlock surrounder = jump.prev.outer;
                 surrounder != null && surrounder != appendBlock.outer; 
                 surrounder = surrounder.outer) {
                if (surrounder instanceof BreakableBlock) {
                    if (surrounder.getNextFlowBlock() == END_OF_METHOD)
                        breakToBlock = (BreakableBlock) surrounder;

                    /* We don't want labeled breaks, because we can
                     * simply return.  */
                    break;
                }
            }
            StructuredBlock prevBlock = jump.prev;
            prevBlock.removeJump();

            if (breakToBlock == null)
                /* The successor is the dummy return instruction, so
                 * replace the jump with a return.  
                 */
                prevBlock.appendBlock(new ReturnBlock());
            else
                prevBlock.appendBlock
                    (new BreakBlock(breakToBlock, false));
        }	    

        /* Now remove the jump of the appendBlock if it points to
         * successor.  
         */
        if (appendBlock.jump != null
            && appendBlock.jump.destination == END_OF_METHOD)
            appendBlock.removeJump();

        /* transformation succeeded */
        try {
            checkConsistent();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            try {
                jode.TabbedPrintWriter writer = 
                    new jode.TabbedPrintWriter(System.err, "    ");
                writer.tab();
                block.dumpSource(writer);
            } catch (java.io.IOException ioex) {
            }
        }
    }


    static Transformation[] exprTrafos = {
        new RemoveEmpty(),
        new CreateExpression(),
        new CreatePostIncExpression(),
        new CreateAssignExpression(),
        new CreateNewConstructor(),
        new CombineIfGotoExpressions(),
        new CreateIfThenElseOperator(),
        new CreateConstantArray(),
        new SimplifyExpression()
    };


    /**
     * Search for an apropriate successor.
     * @param prevSucc The successor, that was previously tried.
     * @param start The minimum address.
     * @param end   The maximum address + 1.
     * @return the successor with smallest address greater than prevSucc
     *  or null if there isn't any further successor at all.
     */
    FlowBlock getSuccessor(int start, int end) {
        /* search successor with smallest addr. */
        Enumeration enum = successors.elements();
        FlowBlock succ = null;
        while (enum.hasMoreElements()) {
            Jump jump = (Jump) enum.nextElement();
            if (jump == null)
                continue;
            FlowBlock fb = jump.destination;
            if (fb.addr < start || fb.addr >= end || fb == this)
                continue;
            if (succ == null || fb.addr < succ.addr) {
//                 System.err.println("trying "+fb.getLabel());
                succ = fb;
            }
        }
        return succ;
    }

    /**
     * The main analyzation.  This calls doT1 and doT2 on apropriate
     * regions.
     */
    public void analyze() {
        analyze(0, Integer.MAX_VALUE);
        mergeEndBlock();
    }

//     /**
//      * The main analyzation.  This calls doT1 and doT2 on apropriate
//      * regions.  Only blocks whose address lies in the given address
//      * range are considered.
//      * @param start the start of the address range.
//      * @param end the end of the address range.
//      */
//     public void analyze(int start, int end) {
//         /* XXX optimize */
//         Stack todo = new Stack();
//         FlowBlock flow = this;
//         try {
//             jode.TabbedPrintWriter writer = null;
//             if (Decompiler.isFlowDebugging) {
//                 writer = new jode.TabbedPrintWriter(System.err, "    ");
//             }
//         analyzation:
//             while (true) {

//                 if (Decompiler.isFlowDebugging) {
//                     writer.println("before Transformation: ");
//                     writer.tab();
//                     flow.dumpSource(writer);
//                     writer.untab();
//                 }

//                 /* First do some non flow transformations. */
//                 int i=0;
//                 while (i < exprTrafos.length) {
//                     if (exprTrafos[i].transform(flow))
//                         i = 0;
//                     else
//                         i++;
//                 }
            
//                 if (Decompiler.isFlowDebugging) {
//                     writer.println("after Transformation: ");
//                     writer.tab();
//                     flow.dumpSource(writer);
//                     writer.untab();
//                 }

//                 if (flow.doT2(todo)) {

//                     if (Decompiler.isFlowDebugging) {
//                         writer.println("after T2: ");
//                         writer.tab();
//                         flow.dumpSource(writer);
//                         writer.untab();
//                     }

//                     /* T2 transformation succeeded.  This may
//                      * make another T1 analysis in the previous
//                      * block possible.  
//                      */
//                     if (!todo.isEmpty())
//                         flow = (FlowBlock) todo.pop();
//                 }

//                 FlowBlock succ = flow.getSuccessor(start, end);
//                 while (true) {
//                     if (succ == null) {
//                         /* the Block has no successor where t1 is applicable.
//                          *
//                          * If everything is okay the stack should be empty now,
//                          * and the program is transformed correctly.
//                          */
//                         if (todo.isEmpty())
//                             break analyzation;
                            
//                         /* Otherwise pop the last flow block from stack and
//                          * try another successor.
//                          */
//                         succ = flow;
//                         flow = (FlowBlock) todo.pop();
//                     } else {
//                         if (succ.block instanceof RawTryCatchBlock) {
//                             int subStart = succ.addr;
//                             int subEnd = (subStart >= addr)? end : addr;
//                             succ.analyze(subStart, subEnd);
//                         }
//                         if (flow.doT1(succ)) {
//                             /* T1 transformation succeeded. */
                            
//                             if (Decompiler.isFlowDebugging) {
//                                 writer.println("after T1: ");
//                                 writer.tab();
//                                 flow.dumpSource(writer);
//                                 writer.untab();
//                             }

//                             if (Decompiler.isVerbose)
//                                 System.err.print(".");
                            
//                             continue analyzation;
//                         } else if (!todo.contains(succ) && succ != flow) {
//                             /* succ wasn't tried before, succeed with
//                              * successor and put flow on the stack.  
//                              */
//                             todo.push(flow);
//                             flow = succ;
//                             continue analyzation;
//                         }
//                     }
                
//                     /* Try the next successor.
//                      */
//                     succ = flow.getSuccessor(succ.addr+1, end);
//                 }
//             }
//         } catch (java.io.IOException ioex) {
//         }
//     }

    /**
     * The main analyzation.  This calls doT1 and doT2 on apropriate
     * regions.  Only blocks whose address lies in the given address
     * range are considered.
     * @param start the start of the address range.
     * @param end the end of the address range.
     */
    public boolean analyze(int start, int end) {
        try {
            jode.TabbedPrintWriter writer = null;
            if (Decompiler.isFlowDebugging)
                writer = new jode.TabbedPrintWriter(System.err, "    ");

            boolean changed = false;

            while (true) {
                
                if (Decompiler.isFlowDebugging) {
                    writer.println("before Transformation: ");
                    writer.tab();
                    dumpSource(writer);
                    writer.untab();
                }

                /* First do some non flow transformations. */
                int i=0;
                while (i < exprTrafos.length) {
                    if (exprTrafos[i].transform(this))
                        i = 0;
                    else
                        i++;
                }
            
                if (Decompiler.isFlowDebugging) {
                    writer.println("after Transformation: ");
                    writer.tab();
                    dumpSource(writer);
                    writer.untab();
                }

                if (doT2(start, end)) {

                    if (Decompiler.isFlowDebugging) {
                        writer.println("after T2: ");
                        writer.tab();
                        dumpSource(writer);
                        writer.untab();
                    }

                    /* T2 transformation succeeded.  This may
                     * make another T1 analysis in the previous
                     * block possible.  
                     */
                    if (addr != 0)
                        return true;
                }

                FlowBlock succ = getSuccessor(start, end);
                while (true) {
                    if (succ == null) {
                        /* the Block has no successor where t1 is applicable.
                         * Finish this analyzation.
                         */
                        return changed;
                    } else {
                        if (succ.block instanceof RawTryCatchBlock) {
                            int subStart = succ.addr;
                            int subEnd = (subStart > addr)? end : addr;
                            succ.analyze(subStart, subEnd);
                        }
                        /* Only do T1 transformation if the blocks are
                         * adjacent.  */
                        if ((succ.addr == addr+length 
                             || succ.addr+succ.length == addr)
                            && doT1(succ)) {
                            /* T1 transformation succeeded. */
                            changed = true;
                            
                            if (Decompiler.isFlowDebugging) {
                                writer.println("after T1: ");
                                writer.tab();
                                dumpSource(writer);
                                writer.untab();
                            }                            
                            break;
                        } else {
                            /* analyze succ, the new region is the
                             * continous region of
                             * [start,end) \cap \compl [addr, addr+length)
                             * where succ.addr lies in.
                             */
                            int newStart = (succ.addr > addr)
                                ? addr+length : start;
                            int newEnd   = (succ.addr > addr)
                                ? end         : addr;
                            if (succ.analyze(newStart, newEnd))
                                break;
                        }
                    }
                
                    /* Try the next successor.
                     */
                    succ = getSuccessor(succ.addr+1, end);
                }
            }
        } catch (java.io.IOException ioex) {
            return false;
        }
    }
    

    /**
     * Resolves the destinations of all jumps.
     */
    public void resolveJumps(FlowBlock[] instr) {
        Enumeration enum = successors.elements();
        while (enum.hasMoreElements()) {
            Jump jump = (Jump) enum.nextElement();
            if (jump != null) {
                if (jump.destAddr == -1) 
                    jump.destination = END_OF_METHOD;
                else
                    jump.destination = instr[jump.destAddr];
                if (!jump.destination.predecessors.contains(this))
                    jump.destination.predecessors.addElement(this);
            }
        }
    }

    /**
     * Mark the flow block as first flow block in a method.
     */
    public void makeStartBlock() {
        predecessors.addElement(null);
    }

    public void addSuccessor(Jump jump) {
        successors.addElement(jump);
        if (!jump.destination.predecessors.contains(this))
            jump.destination.predecessors.addElement(this);
    }

    public void removeSuccessor(Jump jump) {
        successors.setElementAt(null, successors.indexOf(jump));
    }

    public void makeDeclaration(VariableSet param) {
	in.merge(param);
	in.subtract(param);
	block.propagateUsage();
	block.makeDeclaration(param);
    }

    /**
     * Print the source code for this structured block.  This handles
     * everything that is unique for all structured blocks and calls
     * dumpInstruction afterwards.
     * @param writer The tabbed print writer, where we print to.
     */
    public void dumpSource(TabbedPrintWriter writer)
        throws java.io.IOException
    {
        if (predecessors.size() != 1 || 
            predecessors.elementAt(0) != null) {
            writer.untab();
            writer.println(label+":");
            writer.tab();
        }

        if (!in.isEmpty()) {
            writer.println("in: "+in);
        }

        block.dumpSource(writer);
//         FlowBlock succ = getSuccessor();
//         if (succ != null)
//             succ.dumpSource(writer);
    }

    /**
     * The serial number for labels.
     */
    static int serialno = 0;

    /**
     * The label of this instruction, or null if it needs no label.
     */
    String label = null;

    /**
     * Returns the label of this block and creates a new label, if
     * there wasn't a label previously.
     */
    public String getLabel() {
        if (label == null)
            label = "flow_"+addr+"_"+(serialno++)+"_";
        return label;
    }

    /**
     * Returns the structured block, that this flow block contains.
     */
    public StructuredBlock getBlock() {
        return block;
    }
}
