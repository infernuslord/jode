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
        new FlowBlock(Integer.MAX_VALUE, 0, new EmptyBlock());

    static {
        END_OF_METHOD.label = "END_OF_METHOD";
    }

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
    public FlowBlock(int addr, int length, StructuredBlock block) {
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

        same_jump: while(true) {
            /* if the jump is the jump of the appendBlock, skip it.
             */
            if (jump.prev == appendBlock)
                continue next_jump;

            /* Note: jump.prev.outer != null, since appendBlock is
             * an outer block of jump.prev
             */
        
            /* remove all jumps to the successor which have the successor
             * as getNextFlowBlock().  
             */
            if (jump.prev.outer.getNextFlowBlock(jump.prev) == successor) {
                jump.prev.removeJump();
                continue next_jump;
            }

            /* replace all conditional jumps to the successor, which
             * are followed by a block which has the end of the block
             * as normal successor, with "if (not condition) block".  
             */
            if (jump.prev instanceof EmptyBlock &&
                jump.prev.outer instanceof ConditionalBlock) {

                StructuredBlock prev = jump.prev;
                ConditionalBlock cb = (ConditionalBlock) prev.outer;
                jode.Instruction instr = cb.getInstruction();


                if ((cb == appendBlock || 
                     cb.outer.getNextFlowBlock(cb) == successor 
                     /*XXX jumpMayBeChanged()??? */) &&
                    instr instanceof jode.Expression) {

                    cb.setInstruction(((jode.Expression)instr).negate());
                    prev.removeJump();
                    prev.moveJump(cb);
                    continue next_jump;
                }

                /* cb.outer is not null,
                 * since appendBlock outers cb */
                
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

                    newIfBlock.moveJump(sequBlock);
                    if (appendBlock == sequBlock)
                        appendBlock = newIfBlock;

                    if (newIfBlock.getNextFlowBlock() != successor &&
                        newIfBlock != appendBlock) {
                        newIfBlock.moveJump(prev);
                        continue same_jump;
                    } else {
                        prev.removeJump();
                        continue next_jump;
                    }
                }
            }

            /* if there are jumps in an if-then block, which
             * have as normal successor the end of the if-then block, and
             * the if-then block is followed by a single block, then replace
             * the if-then block with a if-then-else block and remove the
             * unconditional jump.
             */
            StructuredBlock elseBlock = 
                jump.prev.outer.getNextBlock(jump.prev);
            if (elseBlock != null
                && elseBlock.outer != null
                && elseBlock.outer instanceof SequentialBlock
                && elseBlock.outer.getSubBlocks()[0] instanceof IfThenElseBlock
                && (elseBlock.outer.getNextFlowBlock() == successor
		    || elseBlock.outer.jumpMayBeChanged())) {

                IfThenElseBlock ifBlock = 
                    (IfThenElseBlock)elseBlock.outer.getSubBlocks()[0];

                if (ifBlock.getSubBlocks().length == 1) {

		    /* make sure that only sequential blocks are between
		     * jump.prev and the ifBlock 
		     */
		    StructuredBlock block = jump.prev.outer;
		    while (block instanceof SequentialBlock)
			block = block.outer;

		    if (block == ifBlock) {
			elseBlock.outer.removeJump();
			ifBlock.replace(elseBlock.outer, elseBlock);
			if (appendBlock == elseBlock.outer)
			    appendBlock = ifBlock;
			ifBlock.moveJump(jump.prev);
			ifBlock.setElseBlock(elseBlock);
			continue same_jump;
		    }
                }
            }

            /* if the successor is the dummy return instruction, replace all
             * jumps with a return.
             */
            if (successor == END_OF_METHOD) {
                SequentialBlock sequBlock = new SequentialBlock();
                StructuredBlock prevBlock = jump.prev;
                prevBlock.removeJump();
                sequBlock.replace(prevBlock, prevBlock);
                sequBlock.setFirst(prevBlock);
                sequBlock.setSecond(new ReturnBlock());
                continue next_jump;
            }

            /* If this is a conditional jump, the first instruction of
             * a while and the condition of the while is true,  use
             * the condition as while condition.
             */
            if (jump.prev instanceof EmptyBlock &&
                jump.prev.outer instanceof ConditionalBlock &&
                jump.prev.outer.jump == null) {

                StructuredBlock prev = jump.prev;
                ConditionalBlock cb = (ConditionalBlock) prev.outer;
                jode.Instruction instr = cb.getInstruction();

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
                            if (cb.outer.getSubBlocks()[1].jump != null)
                                cb.outer.removeJump();
                            else
                                cb.outer.getSubBlocks()[1].moveJump(cb.outer);
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
            }
                
            /* if there are jumps in a while block or switch block and the
             * while/switch block is followed by a jump to successor or has
             * successor as getNextFlowBlock(), replace jump with break to
             * the innermost such while/switch block.
             *
             * If the switch block hasn't been breaked before we could
             * take some heuristics and add a jump after the switch to
             * succesor, so that the above succeeds.
             */
            int breaklevel = 0;
            for (StructuredBlock surrounder = jump.prev.outer;
                 surrounder != null && surrounder != appendBlock.outer; 
                 surrounder = surrounder.outer) {
                if (surrounder instanceof BreakableBlock) {
                    breaklevel++;
                    if (surrounder.getNextFlowBlock() == successor ||
                        surrounder.jumpMayBeChanged()) {

                        SequentialBlock sequBlock = new SequentialBlock();
                        StructuredBlock prevBlock = jump.prev;
                        if (surrounder.getNextFlowBlock() != successor)
                            surrounder.moveJump(prevBlock);
                        else
                            prevBlock.removeJump();

                        sequBlock.replace(prevBlock, prevBlock);
                        sequBlock.setFirst(prevBlock);
                        sequBlock.setSecond
                            (new BreakBlock((BreakableBlock) surrounder, 
                                            breaklevel > 1));
                        continue next_jump;
                    }
                }
            }
            
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
    VariableSet updateInOut (FlowBlock successor, boolean t1Transformation) {
        /* First get the out vectors of all jumps to successor and
         * calculate the intersection.
         */
        VariableSet allOuts = new VariableSet();
        VariableSet intersectOut = null;
        Enumeration enum = successors.elements();
        while (enum.hasMoreElements()) {
            Jump jump = (Jump) enum.nextElement();
            if (jump == null || jump.destination != successor)
                continue;
            
            allOuts.union(jump.out);
            if (intersectOut == null) 
                intersectOut = jump.out;
            else
                intersectOut = intersectOut.intersect(jump.out);
        }

        System.err.println("UpdateInOut: allOuts     : "+allOuts);
        System.err.println("             intersectOut: "+intersectOut);
        
        /* Merge the locals used in successing block with those written
         * by this blocks
         */
        VariableSet defineHere = successor.in.merge(allOuts);
        defineHere.subtractIdentical(in);
        
        System.err.println("             defineHere  : "+defineHere);
        if (t1Transformation) {
            /* Now update in and out set of successing block */
            successor.in.subtract(intersectOut);
            /* The out set must be updated for every jump in the block */
            enum = successor.successors.elements();
            while (enum.hasMoreElements()) {
                Jump jump = (Jump) enum.nextElement();
                if (jump != null)
                    jump.out.add(intersectOut);
            }
        }
        System.err.println("             successor.in: "+successor.in);
        in.union(successor.in);
        System.err.println("             in          : "+in);
        /* XXX - do something with defineHere */
        return defineHere; /*XXX - correct???*/
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

    /**
     * Search for an apropriate successor.
     * @param prevSucc The successor, that was previously tried.
     * @return the successor with smallest address greater than prevSucc
     *  or null if there isn't any further successor at all.
     */
    public FlowBlock getSuccessor(FlowBlock prevSucc) {
        /* search successor with smallest addr. */
        Enumeration enum = successors.elements();
        FlowBlock succ = null;
        while (enum.hasMoreElements()) {
            Jump jump = (Jump) enum.nextElement();
            if (jump == null)
                continue;
            FlowBlock fb = jump.destination;
            if (prevSucc != null && fb.addr <= prevSucc.addr)
                continue;
            if (succ == null || fb.addr < succ.addr) {
                System.err.println("trying "+fb.getLabel());
                succ = fb;
            }
        }
        return succ;
    }

    /**
     * Search for an apropriate successor.
     * @return the successor with smallest address
     *  or null if there isn't a successor at all.
     */
    public FlowBlock getSuccessor() {
        return getSuccessor(null);
    }

    public void checkConsistent() {
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
        if (succ != END_OF_METHOD && 
            (succ.predecessors.size() != 1 ||
             succ.predecessors.elementAt(0) != this))
            return false;

        try{
            System.err.println("doing T1 analysis on: "+getLabel());
            System.err.println("***in: "+in);
            checkConsistent();
            System.err.println("and "+succ.getLabel());
            System.err.println("+++in: "+succ.in);
            succ.checkConsistent();
        } catch (RuntimeException ex) {
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

            while (!appendBlock.contains(jump.prev))
                appendBlock = appendBlock.outer;
            /* appendBlock can't be null now, because the
             * outermost block contains every structured block.  
             */
        }

        /* Update the in/out-Vectors now */
        VariableSet defineHere = updateInOut(succ, true);

        /* The switch "fall through" case: if the appendBlock is a
         * switch, and the successor is the address of a case, and all
         * other successors are inside the block preceding that case.  
         */
        StructuredBlock precedingcase = null;
        StructuredBlock nextcase = null;
        /*XXX*/
        if (succ == END_OF_METHOD) {
        } else if (nextcase != null) {
            SwitchBlock switchBlock = (SwitchBlock) appendBlock;

            /* Now put the succ.block into the next case.
             */
            switchBlock.replaceSubBlock(nextcase,succ.block);
            succ.block.outer = switchBlock;
            /* nextcase is not referenced any more */

            /* Do the following modifications on the struct block. */
            appendBlock = precedingcase;
            succ.block.setFlowBlock(this);
            switchBlock.define(defineHere);

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
            succ.block.setFlowBlock(this);
            sequBlock.define(defineHere);
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
            System.err.println("before optimizeJump: "+getLabel());
            checkConsistent();
        } catch (RuntimeException ex) {
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

        appendBlock = optimizeJumps(succ, appendBlock);

        try {
            System.err.println("after optimizeJump: "+getLabel());
            checkConsistent();
        } catch (RuntimeException ex) {
            try {
                jode.TabbedPrintWriter writer = 
                    new jode.TabbedPrintWriter(System.err, "    ");
                writer.tab();
                block.dumpSource(writer);
            } catch (java.io.IOException ioex) {
            }
        }

        /* Now remove the jump of the appendBlock if it points to successor.
         */
        if (appendBlock.jump != null &&
            appendBlock.jump.destination == succ)
            appendBlock.removeJump();

        /* If there are further jumps, put a do/while(0) block around
         * appendBlock and replace every remaining jump with a break
         * to the do/while block.
         */
        LoopBlock doWhileFalse = null;
        enum = successors.elements();
        while (enum.hasMoreElements()) {
            Jump jump = (Jump) enum.nextElement();

            if (jump == null || jump.destination != succ)
                continue;

            if (doWhileFalse == null)
                doWhileFalse = new LoopBlock(LoopBlock.DOWHILE, 
                                             LoopBlock.FALSE);

            int breaklevel = 1;
            for (StructuredBlock surrounder = jump.prev.outer;
                 surrounder != appendBlock.outer; 
                 surrounder = surrounder.outer) {
                if (surrounder instanceof BreakableBlock) {
                    breaklevel++;
                }
            }

            SequentialBlock sequBlock = new SequentialBlock();
            StructuredBlock prevBlock = jump.prev;
            prevBlock.removeJump();
            
            sequBlock.replace(prevBlock, prevBlock);
            sequBlock.setFirst(prevBlock);
            sequBlock.setSecond(new BreakBlock(doWhileFalse, breaklevel > 1));
        }

        if (doWhileFalse != null) {
            doWhileFalse.replace(appendBlock, appendBlock);
            doWhileFalse.setBody(appendBlock);
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
            System.err.println("T1 succeeded:");
            System.err.println("===in: "+in);
            checkConsistent();
        } catch (RuntimeException ex) {
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

    public boolean doT2(Vector triedBlocks) {
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
                && !triedBlocks.contains(predFlow)) {
                System.err.println("refusing T2 on: "+getLabel()+
                                   " because of "+predFlow.getLabel());
                /* XXX Is this enough to refuse T2 trafo ??? */
                return false;
            }
        }

        try {
            System.err.println("doing T2 analysis on: "+getLabel());
            checkConsistent();
        } catch (RuntimeException ex) {
            try {
                jode.TabbedPrintWriter writer = 
                    new jode.TabbedPrintWriter(System.err, "    ");
                writer.tab();
                block.dumpSource(writer);
            } catch (java.io.IOException ioex) {
            }
        }

        /* Update the in/out-Vectors now */
        VariableSet defineHere = updateInOut(this, false);

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
            whileBlock.define(defineHere);

            /* Try to eliminate as many jumps as possible.
             */
            bodyBlock = optimizeJumps(this, bodyBlock);

            /* Now remove the jump of block if it points to this.
             */
            if (bodyBlock.jump != null &&
                bodyBlock.jump.destination == this)
                bodyBlock.removeJump();

            /* if there are further jumps to this, replace every jump with a
             * continue to while block and return true.  
             */
            Enumeration enum = successors.elements();
            while (enum.hasMoreElements()) {
                Jump jump = (Jump) enum.nextElement();
                
                if (jump == null || jump.destination != this)
                    continue;
                
                int continuelevel = 1;
                for (StructuredBlock surrounder = jump.prev.outer;
                     surrounder != whileBlock;
                     surrounder = surrounder.outer) {
                    if (surrounder instanceof LoopBlock) {
                        continuelevel++;
                    }
                }
                
                SequentialBlock sequBlock = new SequentialBlock();
                StructuredBlock prevBlock = jump.prev;
                prevBlock.removeJump();
                
                sequBlock.replace(prevBlock, prevBlock);
                sequBlock.setFirst(prevBlock);
                sequBlock.setSecond(new ContinueBlock(whileBlock, 
                                                      continuelevel > 1));
            }
            lastModified = whileBlock;
        }

        /* remove ourself from the predecessor list.
         */
        predecessors.removeElement(this);

        /* T2 analysis succeeded */
        try {
            System.err.println("T2 succeeded:");
            checkConsistent();
        } catch (RuntimeException ex) {
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

    public void makeDeclaration() {
	block.makeDeclaration();
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

    public void removeSuccessor(Jump jump) {
        successors.setElementAt(null, successors.indexOf(jump));
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

        if (jode.Decompiler.isDebugging) {
            writer.print("in: ");
            java.util.Enumeration enum = in.elements();
            while(enum.hasMoreElements()) {
                writer.print(((jode.LocalInfo)enum.nextElement()).getName()
                             + " ");
            }
            writer.println("");
        }

        block.dumpSource(writer);
        FlowBlock succ = getSuccessor();
        if (succ != null)
            succ.dumpSource(writer);
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
}
