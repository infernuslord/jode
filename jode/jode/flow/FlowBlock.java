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
import jode.*;

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
     * This contains a map of all successing flow blocks and there
     * jumps.  The key of this dictionary are the flow blocks, while
     * the elements are Stacks of jumps.
     */
    Dictionary successors = new Hashtable();

    /**
     * This is a vector of flow blocks, which reference this block.
     * Only if this vector contains exactly one element, it can be
     * moved into the preceding flow block.
     *
     * If this vectors contains the null element, this is the first
     * flow block in a method.
     */
    Vector predecessors = new Vector();

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
        block.setFlowBlock(this);
        block.fillInSet(in);
    }

    public int getNextAddr() {
        if (block instanceof RawTryCatchBlock)
            return ((RawTryCatchBlock)block).getTryBlock()
                .jump.destination.getNextAddr();
        else
            return addr+length;
    }

    /**
     * This method optimizes the jumps to successor.
     * @param appendBlock the block where the successor is appended to.
     * @param jumps The jumps that jump to successor.  All jumps that
     * can be optimized are removed from this stack.
     * @return the new appendBlock, it may have changed.
     */
    public StructuredBlock optimizeJumps(StructuredBlock appendBlock,
                                         Stack jumps) {
        Stack remainingJumps = new Stack();

    next_jump:
        while (!jumps.isEmpty()) {
            Jump jump = (Jump) jumps.pop();

            FlowBlock successor = jump.destination;

            if (jump.prev instanceof EmptyBlock
		&& jump.prev.outer != null
                && jump.prev.outer instanceof ConditionalBlock
		&& jump.prev.outer.jump != null) {

		if (jump.prev.outer.jump.destination == jump.destination) {
		    /* This is a weired "if (cond) empty"-block.  We
		     * transform it by hand.
		     */
		    jump.prev.removeJump();
		    continue;
		}

		/* Swap conditional blocks, that have two jumps, and where
		 * this jump is the inner jump.  
		 */
		StructuredBlock prev = jump.prev;
		ConditionalBlock cb = (ConditionalBlock) prev.outer;
		Instruction instr = cb.getInstruction();
		
		/* XXX Expression clean up is necessary.  Otherwise
		 * this may lead to a ClassCastException.
		 *
		 * Our code below _depends_ on the fact that this
		 * transformation is done.
		 */
		cb.setInstruction(((Expression)instr).negate());
                cb.swapJump(jump.prev);
	    }

            /* if the jump is the jump of the appendBlock, skip it.
             */
            if (jump.prev == appendBlock) {
                remainingJumps.push(jump);
                continue;
            }

	    /* Now move the jump as far to the outer as possible,
	     * without leaving appendBlock.
	     *
	     * Note: jump.prev != appendblock implies
	     * jump.prev.outer != null, since appendBlock is an outer
	     * block of jump.prev 
	     */
	    while (jump.prev.outer.isSingleExit(jump.prev)) {
                if (jump.prev.outer == appendBlock) {
                    /* Okay we have reached the top.
                     * appendBlock should already have the right jump.
                     */
                    jump.prev.removeJump();
                    continue next_jump;
                }
                /* this changed jump.prev */
		jump.prev.outer.moveJump(jump);
	    }

            /* remove this jump if it jumps to the getNextFlowBlock().  
	     */
            if (jump.destination
                == jump.prev.outer.getNextFlowBlock(jump.prev)) {
                jump.prev.removeJump();
                continue;
            }

            if (jump.prev instanceof EmptyBlock &&
                jump.prev.outer instanceof ConditionalBlock) {

                StructuredBlock prev = jump.prev;
                ConditionalBlock cb = (ConditionalBlock) prev.outer;
                Instruction instr = cb.getInstruction();

                /* cb.jump is null (see above), so cb must have a
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
                        /*XXX remove following after expression cleanup */
                        instr instanceof Expression) {
                        
                        prev.removeJump();
                        loopBlock.setCondition(((Expression)instr).negate());
                        loopBlock.moveDefinitions(cb, null);

                        /* Now remove the whole ConditionalBlock including
                         * the surrounding SequentialBlock.
                         */
                        if (cb.outer.jump != null) {
                            /* Well can this happen???
                             *
                             * Currently a sequential block can only
                             * temporarily have a jump (assigned in
                             * this method, see above).  
                             *
                             * But in any case we know, that the
                             * second sub block can't have a jump, so
                             * it takes the jump of its parent.  
                             */
                            cb.outer.getSubBlocks()[1].
                                moveJump(cb.outer.jump);
                        }
                        cb.outer.getSubBlocks()[1].replace
                            (cb.outer, cb.outer.getSubBlocks()[1]);

                        /* Note that cb.outer != appendBlock because
                         * appendBlock contains loopBlock.  This is
                         * because lastModified can't be an
                         * instruction inside a loop block.  
                         *
                         * XXX I have to make a clear precondition for this.
                         */
                        continue;
                    }
                }

                /* Now the same for the empty loop. In this case there is
                 * no sequential block, which simplifies the situation.
                 */
                else if (cb.outer instanceof LoopBlock) {
                    LoopBlock loopBlock = (LoopBlock) cb.outer;
                    if (loopBlock.getCondition() == LoopBlock.TRUE &&
                        loopBlock.getType() != LoopBlock.DOWHILE &&
                        loopBlock.getNextFlowBlock() == successor &&
                        instr instanceof Expression) {
                        
                        prev.removeJump();
                        loopBlock.setCondition(((Expression)instr).negate());
                        loopBlock.moveDefinitions(cb, null);
                        
                        EmptyBlock empty = new EmptyBlock();
                        empty.replace(cb, null);
                        /* cb is not used any more */
                        continue;
                    }
                } else if (cb.outer instanceof SequentialBlock 
                           && cb.outer.getSubBlocks()[1] == cb) {

                    /* And a third time for do/while loops, where the jump
                     * is at the end of the loop.  Note that all ifs are
                     * disjunct.
                     */
                    
                    /* First find the beginning of the loop */
                    StructuredBlock sb = cb.outer;
                    while (sb.outer instanceof SequentialBlock) {
                        sb = sb.outer;
                    }
                    /* sb is now the first and cb is the last
                     * instruction in the current block.
                     */
                    if (sb.outer instanceof LoopBlock) {
                        LoopBlock loopBlock = (LoopBlock) sb.outer;
                        if (loopBlock.getCondition() == LoopBlock.TRUE &&
                            loopBlock.getType() == LoopBlock.WHILE &&
                            loopBlock.getNextFlowBlock() == successor &&
                            instr instanceof Expression) {
                            
                            prev.removeJump();
                            loopBlock.setType(LoopBlock.DOWHILE);
                            loopBlock.setCondition(((Expression)instr).negate());
                            loopBlock.moveDefinitions(cb, null);

                            /* We pretend here that cb.outer has no jump.
                             * (Note that cb.outer is a sequential block)
                             */
                            cb.outer.getSubBlocks()[0].replace(cb.outer, 
                                                               cb.outer);
                            /* cb and cb.outer are not used any more */
                            continue;
                        }
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
                    instr instanceof Expression) {
                    SequentialBlock sequBlock = 
                        (SequentialBlock) cb.outer;
                    
                    IfThenElseBlock newIfBlock = 
                        new IfThenElseBlock(((Expression)instr).negate());

                    newIfBlock.replace(sequBlock, sequBlock.getSubBlocks()[1]);
                    newIfBlock.setThenBlock(sequBlock.getSubBlocks()[1]);

                    newIfBlock.moveJump(sequBlock.jump);
                    if (appendBlock == sequBlock)
                        appendBlock = newIfBlock;

                    if (newIfBlock.getNextFlowBlock() != successor &&
                        newIfBlock != appendBlock) {
                        newIfBlock.moveJump(jump);
                        /* consider this jump again */
                        jumps.push(jump);
                        continue;
                    } else {
                        prev.removeJump();
                        continue;
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
		    && ifBlock.outer instanceof SequentialBlock
		    && ifBlock.outer.getSubBlocks()[0] == ifBlock) {
		    
		    StructuredBlock elseBlock =
			ifBlock.outer.getSubBlocks()[1];
                    
                    if (ifBlock.outer.jump != null)
                        /* If the sequential block has a jump, we
                         * move it to the else block.
                         * Note that only one of these blocks can
                         * have a jump.
                         */
                        elseBlock.moveJump(ifBlock.outer.jump);

                    if (elseBlock.getNextFlowBlock() == successor
                        || elseBlock.jumpMayBeChanged()) {
                        
                        if (appendBlock == elseBlock.outer)
                            appendBlock = ifBlock;

                        ifBlock.replace(ifBlock.outer, elseBlock);
                        if (elseBlock.jump != null)
                            ifBlock.moveJump(elseBlock.jump);

                        ifBlock.setElseBlock(elseBlock);

                        if (ifBlock.getNextFlowBlock() != successor) {
                            ifBlock.moveJump(jump);
                            /* consider this jump again */
                            jumps.push(jump);
                        } else
                            jump.prev.removeJump();

                        continue;
                    }
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
                        jumps.push(surrounder.jump);
                        break;
                    }
                }
            }
            remainingJumps.push(jump);
        }
        while(!remainingJumps.isEmpty())
            jumps.push(remainingJumps.pop());
        return appendBlock;
    }

    /**
     * Move the successors of the given flow block to this flow block.
     * @param succ the other flow block 
     */
    void mergeSuccessors(FlowBlock succ) {
        /* Merge the sucessors from the successing flow block
         */
        Enumeration keys = succ.successors.keys();
        Enumeration stacks = succ.successors.elements();
        while (keys.hasMoreElements()) {
            FlowBlock dest = (FlowBlock) keys.nextElement();
            Stack hisJumps = (Stack) stacks.nextElement();
            Stack myJumps = (Stack) successors.get(dest);

            dest.predecessors.removeElement(succ);
            if (myJumps == null) {
                dest.predecessors.addElement(this);
                successors.put(dest, hisJumps);
            } else {
                while (!hisJumps.isEmpty())
                    myJumps.push(hisJumps.pop());
            }
        }
    }
    
    /**
     * Resolve remaining jumps to the successor by generating break
     * instructions.  As last resort generate a do while(false) block.
     * @param appendBlock the block where the successor is appended to.
     * @param jumps The jumps that should be resolved.
     * @return the new appendBlock, it may have changed.
     */
    StructuredBlock resolveRemaining(StructuredBlock appendBlock,
                                     Stack jumps) {
        LoopBlock doWhileFalse = null;
        boolean removeAppendJump = false;
    next_jump:
        while (!jumps.isEmpty()) {
            Jump jump = (Jump) jumps.pop();
            StructuredBlock prevBlock = jump.prev;
	    
            if (prevBlock == appendBlock) {
                /* handled below */
                removeAppendJump = true;
                continue;
            }
            
            int breaklevel = 0;
            BreakableBlock breakToBlock = null;
            for (StructuredBlock surrounder = prevBlock.outer;
                 surrounder != null && surrounder != appendBlock.outer; 
                 surrounder = surrounder.outer) {
                if (surrounder instanceof BreakableBlock) {
                    breaklevel++;
                    if (surrounder.getNextFlowBlock() == jump.destination) {
                        breakToBlock = (BreakableBlock) surrounder;
                        break;
                    }
                }
            }
            
            prevBlock.removeJump();
            
            if (breakToBlock == null) {
                /* Nothing else helped, so put a do/while(0)
                 * block around appendBlock and break to that
                 * block.
                 */
                if (doWhileFalse == null) {
                    doWhileFalse = new LoopBlock(LoopBlock.DOWHILE, 
                                                 LoopBlock.FALSE);
                    doWhileFalse.setJump(new Jump(jump.destination));
                }
                prevBlock.appendBlock
                    (new BreakBlock(doWhileFalse, breaklevel > 0));
            } else
                prevBlock.appendBlock
                    (new BreakBlock(breakToBlock, breaklevel > 1));
        }
        
        if (doWhileFalse != null) {
            doWhileFalse.replace(appendBlock, appendBlock);
            doWhileFalse.setBody(appendBlock);
            doWhileFalse.jump = null;
        }

        /* Now remove the jump of the appendBlock if it points to
         * successor.  
         */
        if (removeAppendJump)
            appendBlock.removeJump();

        return appendBlock;
    }

    /** 
     * Updates the in/out-Vectors of the structured block of the
     * successing flow block simultanous to a T1 transformation.
     * @param successor The flow block which is unified with this flow
     * block.  
     * @return The variables that must be defined in this block.
     */
    void updateInOut (FlowBlock successor, boolean t1Transformation,
                      Stack jumps) {
        /* First get the out vectors of all jumps to successor and
         * calculate the intersection.
         */
        VariableSet gens = new VariableSet();
        VariableSet kills =  null;

        Enumeration enum = jumps.elements();
        while (enum.hasMoreElements()) {
            Jump jump = (Jump) enum.nextElement();

            gens.unionExact(jump.gen);
            if (kills == null) 
                kills = jump.kill;
            else
                kills = kills.intersect(jump.kill);
        }
        
        /* Merge the locals used in successing block with those written
         * by this blocks
         */
        successor.in.merge(gens);
        
        /* Now update in and out set of successing block */

        if (t1Transformation)
            successor.in.subtract(kills);

        /* The gen/kill sets must be updated for every jump 
         * in the other block */
        Enumeration stacks = successor.successors.elements();
        while (stacks.hasMoreElements()) {
            enum = ((Stack) stacks.nextElement()).elements();
            while (enum.hasMoreElements()) {

                Jump jump = (Jump) enum.nextElement();
                if (jump != null) {
                    jump.gen.mergeGenKill(gens, jump.kill);
                    if (t1Transformation)
                        jump.kill.add(kills);
                }
            }
        }
        in.unionExact(successor.in);

        if (Decompiler.debugInOut) {
            System.err.println("UpdateInOut: gens : "+gens);
            System.err.println("             kills: "+kills);
            System.err.println("             s.in : "+successor.in);
            System.err.println("             in   : "+in);
        }
    }
    
    public void checkConsistent() {
        if (!Decompiler.doChecks)
            return;

        if (block.outer != null || block.flowBlock != this) {
            throw new AssertError("Inconsistency");
        }
        block.checkConsistent();
        Enumeration preds = predecessors.elements();
        while (preds.hasMoreElements()) {
            FlowBlock pred = (FlowBlock)preds.nextElement();
            if (pred == null)
                /* The special start marker */
                continue;
            if (pred.successors.get(this) == null)
                throw new AssertError("Inconsistency");
        }

        Enumeration keys = successors.keys();
        Enumeration stacks = successors.elements();
        while (keys.hasMoreElements()) {
            FlowBlock dest = (FlowBlock) keys.nextElement();
            if (!dest.predecessors.contains(this))
                throw new AssertError("Inconsistency");
            
            Enumeration enum = ((Stack)stacks.nextElement()).elements();
            if (!enum.hasMoreElements())
                throw new AssertError("Inconsistency");

            while (enum.hasMoreElements()) {
                Jump jump = (Jump) enum.nextElement();
                
                if (jump.destination != dest)
                    throw new AssertError("Inconsistency");
                
                if (jump.prev.flowBlock != this ||
                    jump.prev.jump != jump)
                    throw new AssertError("Inconsistency");
                
            prev_loop:
                for (StructuredBlock prev = jump.prev; prev != block;
                     prev = prev.outer) {
                    if (prev.outer == null)
                        throw new RuntimeException("Inconsistency");
                    StructuredBlock[] blocks = prev.outer.getSubBlocks();
                    int i;
                    for (i=0; i<blocks.length; i++)
                        if (blocks[i] == prev)
                            continue prev_loop;
                    
                    throw new RuntimeException("Inconsistency");
                }
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

        checkConsistent();
        succ.checkConsistent();

        /* First find the innermost block that contains all jumps to this
         * successor and the last modified block.
         * Build the stack of interesting jumps on the fly.
         */
        Stack jumps = (Stack) successors.remove(succ);

        StructuredBlock appendBlock = lastModified;
        Enumeration enum = jumps.elements();
        while(enum.hasMoreElements()) {
            Jump jump = (Jump) enum.nextElement();
            while (!appendBlock.contains(jump.prev)) {
                appendBlock = appendBlock.outer;
            }
            /* appendBlock can't be null now, because the
             * outermost block contains every structured block.  
             */
        }

        /* Update the in/out-Vectors now */
        updateInOut(succ, true, jumps);

        /* Prepare the unification of the blocks: Make sure if
         * possible that appendBlock has a successor outside of
         * this block.  
         *
         * This doesn't change the semantics, since appendBlock
         * is the last block that could be modified. 
         */
        if (appendBlock.jump == null) {
            Jump jump = new Jump(succ);
            appendBlock.setJump(jump);
            jumps.push(jump);
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
    
        mergeSuccessors(succ);

        /* Try to eliminate as many jumps as possible.
         */

	/* appendBlock may be zero, if this is the switchcase with
	 * precedingcase = null.  But in this case, there can't be
	 * any jumps.
	 */
	if (appendBlock != null) {
	    
            appendBlock = optimizeJumps(appendBlock, jumps);
            appendBlock = resolveRemaining(appendBlock, jumps);
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
        checkConsistent();
        return true;
    }


    public boolean doT2(int start, int end) {
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
                && predFlow.addr >= start && predFlow.addr < end) {
                return false;
            }
        }

        checkConsistent();

        Stack jumps = (Stack) successors.remove(this);

        /* Update the in/out-Vectors now */
        updateInOut(this, false, jumps);

	
	while (lastModified != block)
	    lastModified = lastModified.outer;

    transformation:
        do {
            /* If there is only one jump to the beginning and it is the
             * last jump and (there is a do/while(0) block surrounding
             * everything but the last instruction, or the last
             * instruction is a increase/decrease statement), replace the
             * do/while(0) with a for(;;last_instr) resp. create a new one
             * and replace breaks to do/while with continue to for.  
             */

            if (jumps.size() == 1 &&
                ((Jump)jumps.peek()).prev instanceof InstructionBlock) {

                InstructionBlock prev = 
                    (InstructionBlock) ((Jump)jumps.peek()).prev;

                /* Now check if the jump is the last block in a chain
                 * of SequentialBlocks that have no surrounding blocks.
                 */
                StructuredBlock sb = prev;
                while (sb.outer != null 
                   && sb.outer instanceof SequentialBlock
                       && sb.outer.getSubBlocks()[1] == sb)
                    sb = sb.outer;

                if (sb.outer == null && prev.outer != null
                    && prev.outer instanceof SequentialBlock
                    && prev.outer.getSubBlocks()[0] instanceof LoopBlock) {
                    LoopBlock lb = (LoopBlock) prev.outer.getSubBlocks()[0];
                    if (lb.cond == lb.FALSE && lb.type == lb.DOWHILE) {

                        /* The jump is directly following a
                         * do-while(false) block 
                         *
                         * Remove do/while, create a for(;;last_instr)
                         * and replace break to that block with
                         * continue to for.  
                         */
                        
                        LoopBlock forBlock = 
                            new LoopBlock(LoopBlock.FOR, LoopBlock.TRUE);
                        forBlock.replace(sb, sb);
                        forBlock.setBody(sb);

                        prev.outer.getSubBlocks()[0].replace(prev.outer, null);
                        forBlock.incr = prev.getInstruction();

                        forBlock.replaceBreakContinue(lb);
                        lb.bodyBlock.replace(lb, null);

                        prev.removeJump();
                        lastModified = lb;
                        break transformation;
                    }
                }

                Instruction instr = prev.getInstruction();
                Operator    op;
                if (sb.outer == null 
                    && instr instanceof Expression
                    && ((op = ((Expression)instr).getOperator())
                        instanceof StoreInstruction 
                        || op instanceof IIncOperator)) {

                    /* The only jump is the jump of the last
                     * instruction prev */

                    LoopBlock forBlock = 
                        new LoopBlock(LoopBlock.FOR, LoopBlock.TRUE);
                    forBlock.replace(sb, sb);
                    forBlock.setBody(sb);

                    prev.outer.getSubBlocks()[0].replace(prev.outer, null);
                    forBlock.incr = instr;
                    prev.removeJump();
                    lastModified = forBlock;
                    break transformation;
                }
            }

            /* Creating a for block didn't succeed; create a
             * while block instead.  */
            StructuredBlock bodyBlock = block;
            
            /* Prepare the unification of the blocks: Make sure that
             * bodyBlock has a jump.  */
            if (bodyBlock.jump == null) {
                Jump jump = new Jump(this);
                bodyBlock.setJump(jump);
                jumps.push(jump);
            }

            LoopBlock whileBlock = 
                new LoopBlock(LoopBlock.WHILE, LoopBlock.TRUE);
            
            whileBlock.replace(bodyBlock, bodyBlock);
            whileBlock.setBody(bodyBlock);
            
            /* Try to eliminate as many jumps as possible.
             */
            bodyBlock = optimizeJumps(bodyBlock, jumps);
            
            /* if there are further jumps to this, replace every jump with a
             * continue to while block and return true.  
             */
            while (!jumps.isEmpty()) {
                Jump jump = (Jump) jumps.pop();
                
                if (jump.prev == bodyBlock)
                    /* handled later */
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
            
            /* Now remove the jump of bodyBlock if it points to this.
             */
            if (bodyBlock.jump != null 
                && bodyBlock.jump.destination == this)
                bodyBlock.removeJump();

            lastModified = whileBlock;
        } while (false);

        /* remove ourself from the predecessor list.
         */
        predecessors.removeElement(this);

        /* T2 analysis succeeded */
        checkConsistent();

        return true;
    }


    /**
     * Do a T1 transformation with the end_of_method block.
     */
    public void mergeEndBlock() {
        checkConsistent();

        /* First find the innermost block that contains all jumps to the
         * END_OF_METHOD block.
         */
        Stack jumps = new Stack();
        Stack allJumps = (Stack) successors.remove(END_OF_METHOD);
        Enumeration enum = allJumps.elements();
        StructuredBlock appendBlock = lastModified;
        while (enum.hasMoreElements()) {
            Jump jump = (Jump) enum.nextElement();

            if (jump.prev instanceof ReturnBlock) {
                /* This jump is implicit */
                jump.prev.removeJump();
                continue;
            }
            jumps.push(jump);

            while (!appendBlock.contains(jump.prev))
                appendBlock = appendBlock.outer;
            
            /* appendBlock can't be null now, because the
             * outermost block contains every structured block.  
             */
        }
            

        /* Try to eliminate as many jumps as possible.
         */
        appendBlock = optimizeJumps(appendBlock, jumps);
            
    next_jump:
        while (!jumps.isEmpty()) {
            Jump jump = (Jump) jumps.pop();

            StructuredBlock prevBlock = jump.prev;
	    
            if (prevBlock == appendBlock)
                /* handled later */
                continue;

            BreakableBlock breakToBlock = null;
            for (StructuredBlock surrounder = prevBlock.outer;
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
        checkConsistent();
    }


    static Transformation[] exprTrafos = {
        new RemoveEmpty(),
        new CreateExpression(),
        new CreatePrePostIncExpression(),
        new CreateAssignExpression(),
        new CreateNewConstructor(),
        new CombineIfGotoExpressions(),
        new CreateIfThenElseOperator(),
        new CreateConstantArray(),
        new CreateForInitializer(),
        new CompleteSynchronized(),
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
        Enumeration keys = successors.keys();
        FlowBlock succ = null;
        while (keys.hasMoreElements()) {
            FlowBlock fb = (FlowBlock) keys.nextElement();
            if (fb.addr < start || fb.addr >= end || fb == this)
                continue;
            if (succ == null || fb.addr < succ.addr) {
                succ = fb;
            }
        }
        return succ;
    }

    /**
     * The main analyzation.  This calls doT1 and doT2 on apropriate
     * regions until the whole function is transformed to a single
     * block.  
     */
    public void analyze() {
        analyze(0, Integer.MAX_VALUE);
        mergeEndBlock();
    }

    /**
     * The main analyzation.  This calls doT1 and doT2 on apropriate
     * regions.  Only blocks whose address lies in the given address
     * range are considered.
     * @param start the start of the address range.
     * @param end the end of the address range.
     */
    public boolean analyze(int start, int end) {
        if (Decompiler.debugAnalyze)
            System.err.println("analyze("+start+", "+end+")");

        boolean changed = false;

        if (block instanceof RawTryCatchBlock) {
            /* analyze the try and catch blocks separately
             * and create a new CatchBlock afterwards.
             */
            changed |= analyzeCatchBlock(start, end);
        }

        while (true) {
                
            if (Decompiler.isFlowDebugging)
                System.err.println("before Transformation: "+this);

            /* First do some non flow transformations. */
            int i=0;
            while (i < exprTrafos.length) {
                if (exprTrafos[i].transform(this))
                    i = 0;
                else
                    i++;
                checkConsistent();
            }
            
            if (Decompiler.isFlowDebugging)
                System.err.println("after Transformation: "+this);

            if (doT2(start, end)) {

                if (Decompiler.isFlowDebugging)
                    System.err.println("after T2: "+this);

                if (Decompiler.debugAnalyze)
                    System.err.println("T2("+addr+","+(addr+length)
                                       +") succeeded");
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
                    if (Decompiler.debugAnalyze)
                        System.err.println
                            ("No more successors applicable: "
                             + start + " - " + end + "; "
                             + addr + " - " + (addr+length));
                    return changed;
                } else {
                    if (succ.block instanceof SwitchBlock) {
                        /* analyze succ, the new region is the
                         * continuous region of
                         * [start,end) \cap \compl [addr, addr+length)
                         * where succ.addr lies in.
                         */
                        int newStart = (succ.addr > addr)
                            ? addr+length : start;
                        int newEnd   = (succ.addr > addr)
                            ? end         : addr;
                        if (succ.analyzeSwitch(newStart, newEnd))
                            break;

                    } 
                    if (succ.block instanceof RawTryCatchBlock) {
                        /* analyze the try and catch blocks separately
                         * and create a new CatchBlock afterwards.
                         */
                        int newStart = (succ.addr > addr)
                            ? addr+length : start;
                        int newEnd   = (succ.addr > addr)
                            ? end         : addr;
                        if (succ.analyzeCatchBlock(newStart, newEnd)) {
                            break;
                        }
                    } 
                    if ((succ.addr == addr+length 
                         || succ.addr+succ.length == addr)
                        /* Only do T1 transformation if the blocks are
                         * adjacent.  */
                        && doT1(succ)) {
                        /* T1 transformation succeeded. */
                        changed = true;
                            
                        if (Decompiler.isFlowDebugging)
                            System.err.println("after T1: "+this);
                        break;
                    } 

                    /* Check if all predecessors of succ
                     * lie in range [start,end).  Otherwise
                     * we have no chance to combine succ
                     */
                    Enumeration enum = succ.predecessors.elements();
                    while (enum.hasMoreElements()) {
                        int predAddr = 
                            ((FlowBlock)enum.nextElement()).addr;
                        if (predAddr < start || predAddr >= end) {
                            if (Decompiler.debugAnalyze)
                                System.err.println
                                    ("breaking analyze("
                                     + start + ", " + end + "); "
                                     + addr + " - " + (addr+length));
                            return changed;
                        }
                    }                            
                    /* analyze succ, the new region is the
                     * continuous region of
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
                    
                /* Try the next successor.
                 */
                succ = getSuccessor(succ.addr+1, end);
            }
        }
    }

    public void removeJSR(FlowBlock subRoutine) {
        Enumeration enum = ((Stack)successors.remove(subRoutine)).elements();
        while (enum.hasMoreElements()) {
            Jump jump = (Jump)enum.nextElement();

            StructuredBlock prev = jump.prev;
            prev.removeJump();
            if (prev instanceof EmptyBlock
                && prev.outer instanceof JsrBlock) {
                if (prev.outer.getNextFlowBlock() != null) {
                    /* The jsr is directly before a jump, okay. */
                    prev.outer.removeBlock();
                    continue;
                }
                if (prev.outer.outer instanceof SequentialBlock
                    && prev.outer.outer.getSubBlocks()[0] == prev.outer) {
                    SequentialBlock seq = (SequentialBlock) prev.outer.outer;
                    if (seq.subBlocks[1] instanceof JsrBlock) {
                        /* The jsr is followed by a jsr, okay. */
                        prev.outer.removeBlock();
                        continue;
                    }
                    if (seq.subBlocks[1] instanceof ReturnBlock
                        && !(seq.subBlocks[1] instanceof ThrowBlock)) {

                        /* The jsr is followed by a return, okay. */
                        ReturnBlock ret = (ReturnBlock) seq.subBlocks[1];
                        prev.outer.removeBlock();

                        if (ret.outer != null 
                            && ret.outer instanceof SequentialBlock) {
                            /* Try to eliminate the local that javac uses
                             * in this case.
                             */
                            try {
                                ComplexExpression expr = (ComplexExpression)
                                    ((InstructionBlock)
                                     ret.outer.getSubBlocks()[0]).instr;
                                LocalStoreOperator store = 
                                    (LocalStoreOperator) expr.getOperator();
                                if (store.matches((LocalLoadOperator)
                                                  ret.getInstruction())) {
                                    ret.setInstruction(expr.
                                                       getSubExpressions()[0]);
                                    ret.replace(ret.outer, null);
                                    ret.used.removeElement
                                        (store.getLocalInfo());
                                }
                            } catch(ClassCastException ex) {
                                /* didn't succeed */
                            }
                        }
                        continue;
                    }
                } 
            }
            /* Now we have a dangling JSR at the wrong place.
             * We don't do anything, so that JSR will show up in
             * the output.
             */
        }
    }
    
    public void checkAndRemoveJSR(FlowBlock subRoutine) {
        Enumeration keys = successors.keys();
        Enumeration stacks = successors.elements();
        while (keys.hasMoreElements()) {
            Stack jumps = (Stack) stacks.nextElement();
            if (keys.nextElement() == subRoutine)
                continue;

            Enumeration enum = jumps.elements();
            while (enum.hasMoreElements()) {
                Jump jump = (Jump)enum.nextElement();

                StructuredBlock prev = jump.prev;
                if (prev instanceof ThrowBlock) {
                    /* The jump is a throw.  We have a catch-all block
                     * that will do the finally.
                     */
                    continue;
                }
                if (prev instanceof JsrBlock) {
                    /* The jump is directly preceeded by a jsr.
                     * Everything okay.
                     */
                    continue;
                }
                
                if (prev instanceof EmptyBlock
                    && prev.outer instanceof JsrBlock) {
                    /* If jump is a jsr check the outer
                     * block instead.
                     */
                    prev = prev.outer;
                }
                if ((prev instanceof ReturnBlock
                     || prev instanceof JsrBlock)
                    && prev.outer instanceof SequentialBlock) {
                    SequentialBlock seq = (SequentialBlock) prev.outer;
                    if (seq.subBlocks[1] == prev
                        && (seq.subBlocks[0] instanceof JsrBlock)) {
                        /* The jump is preceeded by another jsr, okay.
                         */
                        continue;
                    }
                    if (seq.subBlocks[0] == prev
                        && seq.outer instanceof SequentialBlock
                        && (seq.outer.getSubBlocks()[0] instanceof JsrBlock)) {
                        /* Again the jump is preceeded by another jsr, okay.
                         */
                        continue;
                    }
                }
                /* Now we have a jump with a wrong destination.
                 * Complain!
                 */
                System.err.println("non well formed try-finally block");
            }
        }
        removeJSR(subRoutine);
    }

    public boolean isMonitorExit(Instruction instr, LocalInfo local) {
        if (instr instanceof ComplexExpression) {
            ComplexExpression expr = (ComplexExpression)instr;
            if (expr.getOperator() instanceof MonitorExitOperator
                && expr.getSubExpressions()[0] instanceof LocalLoadOperator
                && (((LocalLoadOperator) expr.getSubExpressions()[0])
                    .getLocalInfo().getSlot() == local.getSlot())) {
                return true;
            }
        }
        return false;
    }

    public void checkAndRemoveMonitorExit(LocalInfo local, int end) {
        FlowBlock subRoutine = null;
        Enumeration stacks = successors.elements();
    dest_loop:
        while (stacks.hasMoreElements()) {
            Enumeration enum = ((Stack) stacks.nextElement()).elements();
            while (enum.hasMoreElements()) {
                Jump jump = (Jump)enum.nextElement();

                StructuredBlock prev = jump.prev;
                if (prev instanceof EmptyBlock
                    && prev.outer instanceof JsrBlock
                    && subRoutine == null) {
                    
                    subRoutine = jump.destination;
                    subRoutine.analyzeSubRoutine(addr+length, end);
                
                    if (subRoutine.block instanceof InstructionBlock) {
                        Instruction instr = 
                            ((InstructionBlock)subRoutine.block)
                            .getInstruction();
                        if (isMonitorExit(instr, local)) {

                            updateInOut(subRoutine, true, 
                                        (Stack)successors.get(subRoutine));
                            length += subRoutine.length;
                            continue dest_loop;
                        }
                    }
                }

                if (prev instanceof ThrowBlock) {
                    /* The jump is a throw.  We have a catch all block
                     * that will do the monitorexit.
                     */
                    continue;
                }
                if (prev instanceof JsrBlock) {
                    /* The jump is directly preceeded by a jsr.
                     */
                    continue;
                }
                if (prev instanceof EmptyBlock
                    && prev.outer instanceof JsrBlock) {
                    /* If jump is a jsr check the outer
                     * block instead.
                     */
                    prev = prev.outer;
                }

                if ((prev instanceof JsrBlock
                     || prev instanceof ReturnBlock)
                    && prev.outer instanceof SequentialBlock) {
                    SequentialBlock seq = (SequentialBlock) prev.outer;
                    StructuredBlock pred = null;
                    if (seq.subBlocks[1] == prev)
                        pred = seq.subBlocks[0];
                    else if (seq.outer instanceof SequentialBlock)
                        pred = seq.outer.getSubBlocks()[0];
                    
                    if (pred != null) {
                        if (pred instanceof JsrBlock  || pred.jump != null)
                            /* The jump is preceeded by another jump
                             * or jsr and last in its block, okay.
                             */
                            continue;
                        
                        if (pred instanceof InstructionBlock) {
                            Instruction instr = 
                                ((InstructionBlock)pred).getInstruction();
                            if (instr instanceof ComplexExpression
                                && ((ComplexExpression)instr)
                                .getOperator() instanceof MonitorExitOperator
                                && ((ComplexExpression)instr)
                                .getSubExpressions()[0] 
                                instanceof LocalLoadOperator
                                && (((LocalLoadOperator) 
                                     ((ComplexExpression)instr)
                                     .getSubExpressions()[0]).getLocalInfo()
                                    .getSlot() == local.getSlot())) 
                                continue;
                        }
                    }
                }

                if (prev instanceof InstructionBlock
                    && isMonitorExit(((InstructionBlock)prev).instr, local)) {
                    /* This is probably the last expression in the
                     * synchronized block, and has the right monitor exit
                     * attached.  Remove this block.
                     */
                    prev.removeBlock();
                    continue;
                }

                /* Now we have a jump that is not preceded by a monitorexit.
                 * Complain!
                 */
                System.err.println("non well formed synchronized block");
            }  
        }

        if (subRoutine != null)
            removeJSR(subRoutine);
    }
    
    
    /**
     * Create a Catch- resp. FinallyBlock (maybe even SynchronizedBlock).
     * The root block MUST be a RawTryCatchBlock.
     * @param start the start address
     * @param end and the end address of FlowBlocks, we may use.
     */
    public boolean analyzeCatchBlock(int start, int end) {
        if (Decompiler.debugAnalyze)
            System.err.println("analyzeCatch("+start+", "+end+")");
        RawTryCatchBlock rawBlock = (RawTryCatchBlock) block;
        FlowBlock tryFlow = rawBlock.tryBlock.jump.destination;
        FlowBlock catchFlow = rawBlock.catchBlock.jump.destination;

        /* This are the only jumps in this block.  Make sure they
         * are disjunct, the successing code relies on that!
         */
        if (tryFlow == catchFlow)
            throw new AssertError("try == catch");

        boolean changed = false;
        tryFlow.analyze(addr, catchFlow.addr);
        catchFlow.analyze(catchFlow.addr, end);

        updateInOut(tryFlow, true, (Stack) successors.remove(tryFlow));
        rawBlock.tryBlock.removeJump();
        tryFlow.getBlock().replace(rawBlock.tryBlock, null);
        mergeSuccessors(tryFlow);
        length += tryFlow.length;
        
        updateInOut(catchFlow, true, (Stack) successors.remove(catchFlow));
        rawBlock.catchBlock.removeJump();
        catchFlow.block.replace(rawBlock.catchBlock, null);
        mergeSuccessors(catchFlow);
        length += catchFlow.length;
        
        if (rawBlock.type != null) {
            /* simple try catch block:
             *
             *   try-header
             *      |- first instruction
             *      |  ...
             *      |  last instruction
             *      |- optional jump (last+1)
             *      |  ...
             *      `- catch block
             */

            CatchBlock newBlock = new CatchBlock(rawBlock.type);
            newBlock.replace(rawBlock, rawBlock);
            newBlock.setTryBlock(rawBlock.tryBlock);
            newBlock.setCatchBlock(rawBlock.catchBlock);
            lastModified = newBlock;
            changed = true;
        } else if (catchFlow.block instanceof SequentialBlock
                   && catchFlow.block.getSubBlocks()[0] 
                   instanceof InstructionBlock) {

            SequentialBlock catchBlock = (SequentialBlock) catchFlow.block;
            int type = 0;

            Instruction instr = 
                ((InstructionBlock)catchBlock.subBlocks[0]).instr;

            if (instr instanceof ComplexExpression
                && ((ComplexExpression)instr).getOperator() 
                instanceof MonitorExitOperator
                && ((ComplexExpression)instr).getSubExpressions()[0] 
                instanceof LocalLoadOperator
                && catchBlock.subBlocks[1] instanceof ThrowBlock
                && ((ThrowBlock)catchBlock.subBlocks[1]).instr 
                instanceof NopOperator) {
                
                /* This is a synchronized block:
                 *
                 *  local_x = monitor object;  // later
                 *  monitorenter local_x       // later
                 *  try-header any
                 *   |- syncronized block
                 *   |  ...
                 *   |   every jump to outside is preceded by jsr monexit ---,
                 *   |  ...                                                  |
                 *   |- monitorexit local_x                                  |
                 *   |  jump after this block (without jsr monexit)          |
                 *   `- catch any                                            |
                 *      local_n = stack                                      |
                 *      monitorexit local_x                                  |
                 *      throw local_n                                        |
                 *  monexit: <-----------------------------------------------'
                 *    astore_n
                 *    monitorexit local_x
                 *    return_n
                 */

                /* First remove the jump after the throw */
                removeSuccessor(catchBlock.subBlocks[1].jump);
                catchBlock.subBlocks[1].removeJump();

                ComplexExpression monexit = (ComplexExpression)
                    ((InstructionBlock) catchBlock.subBlocks[0]).instr;
                LocalInfo local = 
                    ((LocalLoadOperator)monexit.getSubExpressions()[0])
                    .getLocalInfo();
                checkAndRemoveMonitorExit(local, end);

                SynchronizedBlock syncBlock = new SynchronizedBlock(local);
                syncBlock.replace(rawBlock, rawBlock);
                syncBlock.moveJump(rawBlock.jump);
                syncBlock.setBodyBlock(rawBlock.tryBlock);
                lastModified = syncBlock;
                changed = true;

            } else if (catchBlock.subBlocks[1] instanceof SequentialBlock
                     && catchBlock.subBlocks[1].getSubBlocks()[0] 
                     instanceof JsrBlock
                     && instr instanceof LocalStoreOperator
                     && catchBlock.subBlocks[1].getSubBlocks()[1]
                     instanceof ThrowBlock
                     && ((ThrowBlock)catchBlock.subBlocks[1]
                         .getSubBlocks()[1]).instr
                     instanceof LocalLoadOperator
                     && ((LocalStoreOperator) instr)
                     .matches((LocalLoadOperator) 
                              ((ThrowBlock)catchBlock.subBlocks[1]
                               .getSubBlocks()[1]).instr)) {
                /* Wow that was complicated :-)
                 * But now we know that the catch block looks
                 * exactly like an try finally block:
                 *
                 *   try-header any
                 *    | 
                 *    |- first instruction
                 *    |  ...
                 *    |  every jump to outside is preceded by jsr finally
                 *    |  ...
                 *    |  jsr finally -----------------,
                 *    |- jump after finally           |
                 *    `- catch any                    |
                 *       local_n = stack              v
                 *       jsr finally ---------------->|
                 *       throw local_n;               |
                 *   finally: <-----------------------'
                 *      astore_n
                 *      ...
                 *      return_n
                 */
                FlowBlock subRoutine = 
                    ((JsrBlock)catchBlock.subBlocks[1].getSubBlocks()[0])
                    .innerBlock.jump.destination;

                /* Now remove the two jumps of the catch block
                 * so that we can forget about them.
                 * This are the throw and the jsr.
                 */
                removeSuccessor(catchBlock.subBlocks[1]
                                .getSubBlocks()[1].jump);
                removeSuccessor(catchBlock.subBlocks[1]
                                .getSubBlocks()[0].getSubBlocks()[0].jump);
                catchBlock.subBlocks[1].getSubBlocks()[1].removeJump();
                catchBlock.subBlocks[1]
                    .getSubBlocks()[0].getSubBlocks()[0].removeJump();

                
                subRoutine.analyzeSubRoutine(addr+length, end);
                updateInOut(subRoutine, true, 
                            (Stack)successors.get(subRoutine));
                if (subRoutine.successors.size() != 0)
                    throw new AssertError("Jump inside subroutine");
                length += subRoutine.length;
                checkAndRemoveJSR(subRoutine);
                
                CatchFinallyBlock newBlock = new CatchFinallyBlock();
                newBlock.replace(rawBlock, rawBlock);
                newBlock.setTryBlock(rawBlock.tryBlock);
                newBlock.setFinallyBlock(subRoutine.block);
                newBlock.moveJump(rawBlock.jump);
                lastModified = newBlock;
                changed = true;
            }
        } else if (catchFlow.block instanceof InstructionBlock 
                   && ((InstructionBlock) catchFlow.block).instr
                   instanceof PopOperator
                   && successors.size() == 1) {

            /* This is a special try/finally-block, where
             * the finally block ends with a break, return or
             * similar.
             */
            FlowBlock succ = catchFlow.block.jump.destination;
            Stack jumps = (Stack) successors.get(succ);
            Stack newJumps = new Stack();
            newJumps.push(catchFlow.block.jump);
            successors.put(succ, newJumps);
            jumps.removeElement(catchFlow.block.jump);

            if (rawBlock.tryBlock.jump == null) {
                Jump jump = new Jump(succ);
                rawBlock.tryBlock.setJump(jump);
                jumps.push(jump);
            }

            optimizeJumps(rawBlock.tryBlock, jumps);
            resolveRemaining(rawBlock.tryBlock, jumps);

            CatchFinallyBlock newBlock = new CatchFinallyBlock();
            newBlock.replace(rawBlock, rawBlock);
            newBlock.setTryBlock(rawBlock.tryBlock);
            newBlock.setFinallyBlock(new EmptyBlock());
            newBlock.moveJump(rawBlock.jump);
            lastModified = newBlock.finallyBlock;
            lastModified.moveJump(catchFlow.block.jump);
            changed = true;
        }
        checkConsistent();
        if (Decompiler.debugAnalyze)
            System.err.println("analyzeCatch("+start+", "+end+") "
                               +(changed?"succeeded":"failed")
                               +"; "+addr+","+(addr+length));
        return changed;
    }

    public void analyzeSubRoutine(int start, int end) {
        analyze(start, end);
        /* throws ClassCastException if something isn't as exspected. */
        SequentialBlock sequBlock = (SequentialBlock) block;
        LocalStoreOperator store = (LocalStoreOperator)
            ((Expression)((InstructionBlock)sequBlock.subBlocks[0]).instr)
            .getOperator();

        while (sequBlock.subBlocks[1] instanceof SequentialBlock)
            sequBlock = (SequentialBlock) sequBlock.subBlocks[1];

        if (! ((RetBlock)sequBlock.subBlocks[1]).local
            .equals(store.getLocalInfo()))
            throw new AssertError("Ret doesn't match");

        if (sequBlock.outer == null) {
            new EmptyBlock().replace(sequBlock, sequBlock);
        } else {
            sequBlock.subBlocks[0].replace(sequBlock, sequBlock);
            block.getSubBlocks()[1].replace(block, block);
        }
    }
    
    /**
     * The switch analyzation.  This calls doSwitchT1 and doT2 on apropriate
     * regions.  Only blocks whose address lies in the given address
     * range are considered and it is taken care of, that the switch
     * is never leaved. <p>
     * The current flow block must contain the switch block as main
     * block.
     * @param start the start of the address range.
     * @param end the end of the address range.
     */
    public boolean analyzeSwitch(int start, int end) {
        SwitchBlock switchBlock = (SwitchBlock) block;
        boolean changed = false;
        StructuredBlock lastBlock = null;
        lastModified = block;
        /* XXX - move to switchBlock??? */
        for (int i=0; i < switchBlock.caseBlocks.length; i++) {
            if (switchBlock.caseBlocks[i].subBlock != null
                && switchBlock.caseBlocks[i].subBlock.jump != null) {
                FlowBlock next = switchBlock.caseBlocks[i].
                    subBlock.jump.destination;
                if (next.addr >= end) 
                    return changed;
                else if (next.addr >= start) {
                    
                    /* First analyze the next block.  It may
                     * return early after a T2 trafo so call it
                     * until nothing more is possible.  
                     */
                    while (next.analyze(addr + length, end))
                        changed = changed || true;
                    
                    if (next.addr != addr + length)
                        return changed;
                    
                    /* Check if next has only the previous case
                     * and this case as predecessor. Otherwise
                     * break the analysis.
                     */
                    if (next.predecessors.size() != 1
                        || next.predecessors.elementAt(0) != this)
                            return changed;
                    
                    boolean lastContains = false;
                    Stack jumps = new Stack();

                    Enumeration enum = 
                        ((Stack) successors.get(next)).elements();
                    while (enum.hasMoreElements()) {
                        Jump jump = (Jump) enum.nextElement();
                        if (jump == switchBlock.caseBlocks[i].subBlock.jump)
                            continue;

                        jumps.push(jump);
                        if (lastBlock == null
                            || !lastBlock.contains(jump.prev))
                            
                            return changed;
                    }
                    checkConsistent();
                    
                    updateInOut(next, true, (Stack) successors.remove(next));
                    switchBlock.caseBlocks[i].subBlock.removeJump();
                    next.block.replace(switchBlock.caseBlocks[i].subBlock, 
                                       null);
                    
                    mergeSuccessors(next);
                    if (!jumps.isEmpty()) {
                        lastBlock = optimizeJumps(lastBlock, jumps);
                        lastBlock = resolveRemaining(lastBlock, jumps);
                    }
                    
                    /* Set length to correct value */
                    length += next.length;
                    
                    lastBlock = next.block;
                    checkConsistent();
                    changed = true;
                }
            }
        }
        return changed;
    }
    
    /**
     * Resolves the destinations of all jumps.  This will also create
     * the successors map.
     */
    public void resolveJumps(FlowBlock[] instr) {
        if (block instanceof RawTryCatchBlock) {
            ((RawTryCatchBlock)block).getTryBlock()
                .jump.destination.resolveJumps(instr);
        }
        Vector allJumps = new Vector();
        block.fillSuccessors(allJumps);
        Enumeration enum = allJumps.elements();
        while (enum.hasMoreElements()) {
            Jump jump = (Jump) enum.nextElement();
            if (jump != null && jump.destination == null) {
                if (jump.destAddr == -1) 
                    jump.destination = END_OF_METHOD;
                else
                    jump.destination = instr[jump.destAddr];
            }
            addSuccessor(jump);
        }
    }

    /**
     * Mark the flow block as first flow block in a method.
     */
    public void makeStartBlock() {
        predecessors.addElement(null);
    }

    public void removeSuccessor(Jump jump) {
        Stack destJumps = (Stack) successors.get(jump.destination);
        if (!destJumps.removeElement(jump))
            throw new AssertError("removing non existent jump");
        if (destJumps.isEmpty())
            successors.remove(jump.destination);
    }

    public void addSuccessor(Jump jump) {
        Stack destJumps = (Stack) successors.get(jump.destination);
        if (destJumps == null) {
            jump.destination.predecessors.addElement(this);
            destJumps = new Stack();
            successors.put(jump.destination, destJumps);
        }
        destJumps.addElement(jump);
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

        if (Decompiler.debugInOut) {
            writer.println("in: "+in);
        }

        block.dumpSource(writer);
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

    public String toString() {
        try {
            java.io.StringWriter strw = new java.io.StringWriter();
            TabbedPrintWriter writer = 
                new TabbedPrintWriter(strw, "    ");
            writer.println(super.toString());
            writer.tab();
            dumpSource(writer);
            return strw.toString();
        } catch (java.io.IOException ex) {
            return super.toString();
        }
    }
}
