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

    /**
     * The starting address of this flow block.  This is mainly used
     * to produce the source code in code order.
     */
    int addr;

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
     */
    Vector predecessors;
    
    /**
     * This method optimizes the jumps to successor.
     * Returns the new appendBlock, it may have changed.
     */
    public StructuredBlock optimizeJumps(FlowBlock successor,
                                         StructuredBlock appendBlock) {
        Enumeration enum = successors.elements();
        while (enum.hasMoreElements()) {
            Jump jump = (Jump) enum.nextElement();

            if (jump == null || jump.destination != successor)
                continue;

            /* if the jump is the jump of the appendBlock, skip it.
             */
            if (jump == appendBlock.jump)
                continue;

            /* Note: jump.parent.outer != null, since appendBlock is
             * an outer block of jump.parent
             */
        
            /* remove all jumps to the successor which have the successor
             * as getNextFlowBlock().  */
            if (jump.parent.outer.getNextFlowBlock(jump.parent) == successor)
                jump.parent.removeJump();

            /* replace all conditional jumps to the successor, which
             * are followed by a block which has the end of the block
             * as normal successor, with "if (not condition) block".  
             */
            if (jump.parent instanceof ConditionalBlock &&
                jump.parent.outer instanceof SequentialBlock && 
                jump.parent.outer.getSubBlocks()[0] = jump.parent &&
                jump.parent.outer.getNextFlowBlock() == successor) {

                ConditionalBlock cb = (ConditionalBlock) jump.parent;
                cb.removeJump();

                SequentialBlock sequBlock = 
                    (SequentialBlock) cb.outer;

                IfThenElseBlock newIfBlock = 
                    new IfThenElseBlock(cb.getCondition().negate(),
                                        sequBlock.getSubBlocks()[1], null);

                newIfBlock.replace(sequBlock);

                if (appendBlock == sequBlock)
                    appendBlock = newIfBlock;
                continue;
            }

            /* if the successor is the dummy return instruction, replace all
             * jumps with a return.
             */
            if (successor.block.instanceof ReturnBlock) {
                SequentialBlock sequBlock = new SequentialBlock();
                StructuredBlock prevBlock = jump.parent;
                prevBlock.removeJump();
                sequBlock.replace(prevBlock);
                sequBlock.setFirst(prevBlock);
                sequBlock.setSecond(new ReturnBlock());
                continue;
            }

            /* If this is a conditional jump, the first instruction of
             * a while and the condition of the while is true,  use
             * the condition as while condition.
             */

            /* This is the first instruction in a while block */
            if (jump.parent instanceof ConditionalBlock &&
                jump.parent.outer instanceof SequentialBlock &&
                jump.parent.outer.getSubBlocks()[0] == this &&
                jump.parent.outer.outer instanceof LoopBlock) {
                ConditionalBlock cb = (ConditionalBlock) jump.parent;
                LoopBlock loopBlock = (LoopBlock) cb.outer.outer;
                if (loopBlock.getCondition() == LoopBlock.TRUE &&
                    loopBlock.getType() != LoopBlock.DOWHILE &&
                    loopBlock.getNextFlowBlock() == successor) {

                    cb.removeJump();
                    loopBlock.setCondition(cb);
                    cb.outer.getSubBlocks()[1].replace(cb.outer);
                    /* cb and cb.outer are not used any more */
                    /* Note that cb.outer != appendBlock because
                     * appendBlock contains loopBlock
                     */
                }
            }
            /* Now the same for the empty loop. In this case there is
             * no sequential block.  
             */
            if (jump.parent instanceof ConditionalBlock &&
                jump.parent.outer instanceof LoopBlock) {
                ConditionalBlock cb = (ConditionalBlock) jump.parent;
                LoopBlock loopBlock = (LoopBlock) cb.outer;
                if (loopBlock.getCondition() == LoopBlock.TRUE &&
                    loopBlock.getType() != LoopBlock.DOWHILE &&
                    loopBlock.getNextFlowBlock() == successor) {

                    cb.removeJump();
                    loopBlock.setCondition(cb);
                    EmptyBlock empty = new EmptyBlock();
                    empty.replace(cb);
                    /* cb is not used any more */
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
            for (StructuredBlock surrounder = jump.parent.outer;
                 surrounder != null; surrounder = surrounder.outer) {
                if (surrounder instanceof BreakableBlock) {
                    breaklevel++;
                    if (surrounder.getNextFlowBlock() == successor ||
                        surrounder.jumpMayBeChanged()) {

                        SequentialBlock sequBlock = new SequentialBlock();
                        StructuredBlock prevBlock = jump.parent;
                        if (surrounder.getNextFlowBlock() != successor) {
                            surrounder.jump = jump;
                            prevBlock.jump = null;
                            jump.parent = surrounder;
                        } else {
                            prevBlock.removeJump();
                        }
                        sequBlock.replace(prevBlock);
                        sequBlock.setFirst(prevBlock);
                        sequBlock.setSecond(new BreakBlock(surrounder, 
                                                           breaklevel > 1));
                        continue;
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
                jump.parent.outer.getNextBlock(jump.parent);
            if (elseBlock != null && 
                elseBlock.outer != null &&
                elseBlock.outer instanceof SequentialBlock &&
                elseBlock.outer.getSubBlocks()[0] instanceof IfThenElseBlock &&
                (elseBlock.outer.getNextFlowBlock() == successor ||
                 elseBlock.outer.jumpMayBeChanged())) {
                IfThenElseBlock ifBlock = 
                    (IfThenElseBlock)elseBlock.outer.getSubBlocks()[0];
                if (ifBlock.getElseBlock() == null) {
                    if (elseBlock.getNextFlowBlock() != successor) {
                        elseBlock.outer.jump = jump;
                        jump.parent.jump = null;
                        jump.parent = elseBlock.outer;
                    } else {
                        jump.parent.removeJump();
                    }
                    ifBlock.replace(elseBlock.outer);
                    ifBlock.setElseBlock(elseBlock);
                    if (appendBlock = elseBlock.outer)
                        appendBlock = ifBlock;
                }
            }
        }
        return appendBlock;
    }

    /** 
     * Updates the in/out-Vectors of the structured block of the
      * successing flow block simultanous to a T1 transformation.
      * @param successor The flow block which is unified with this flow
      * block.  
      */
    void updateInOut (FlowBlock successor, boolean t1Transformation) {
        /* First get the out vectors of all jumps to successor and
         * calculate the intersection.
         */
        VariableSet allOuts = new VariableSet();
        VariableSet intersectOut = null;
        Enumeration enum = successors;
        while (enum.hasMoreElement()) {
            Jump jump = (Jump) enum.nextElement();
            if (jump == null || jump.destination != successor)
                continue;

            allOuts.union(jump.parent.out);
            if (intersectOut == null) 
                intersectOut = jump.parent.out;
            else
                intersectOut = intersectOut.intersect(jump.parent.out);
        }

        /* Now work on each block of the successor */
        Stack todo = new Stack();
        todo.push(successor.block);
        while (!todo.empty()) {
            StructuredBlock block = (StructuredBlock) todo.pop();
            StructuredBlock[] subBlocks = block.getSubBlocks();
            for (int i=0; i<subBlocks.length; i++)
                todo.push(subBlocks[i]);

            /* Merge the locals used in successing block with those written
             * by this blocks
             */
            block.in.merge(allOuts);

            if (t1Transformation) {
                /* Now update in and out set of successing block */
                block.in.subtract(intersectOut);
                block.out.add(intersectOut);
            }
        }
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

    public boolean doT1 {
        /* search successor with smallest addr. */
        Enumeration enum = successors.elements();
        FlowBlock succ = null;
        while (enum.hasMoreElements()) {
            Jump jump = (Jump) enum.nextElement();
            if (jump == null)
                continue;
            FlowBlock fb = jump.destination;
            if (succ == null || fb.addr < succ.addr) {
                succ = fb;
            }
        }
        if (succ == null) {
            /* There are no successors at all */
            return false;
        }

        /* check if this successor has only this block as predecessor. */
        /* if not, return false. */
        if (succ.predecessors.size() != 1)
            return false;

         /* First find the innermost block that contains all jumps to this
          * successor and the last modified block.
          */
        Enumeration enum = successors.elements();
        StructuredBlock appendBlock = lastModified;
        while(enum.hasMoreElements()) {
            Jump jump = (Jump) enum.nextElement();
            if (jump == null || jump.destination != successors)
                continue;

            while (!appendBlock.contains(jump.parent))
                appendBlock = appendBlock.outer;
            /* appendBlock can't be null now, because the
             * outermost block contains every structured block.  
             */
        }

        /* Update the in/out-Vectors now */
        updateInOut(successor, true);

        /* The switch "fall through" case: if the appendBlock is a
          * switch, and the successor is the address of a case, and all
          * other successors are inside the block preceding that case.  
          */
        if (case != null) {
            SwitchBlock switchBlock = (StructuredBlock) appendBlock;

            /* Now put the succ.block into the next case.
             */
            switchBlock.replaceSubBlock(nextcase,succ.block);
            succ.block.outer = switchBlock;
            /* nextcase is not referenced any more */

            /* Do the following modifications on the struct block. */
            appendBlock = precedingcase;

        } else {

            /* Prepare the unification of the blocks: Make sure that
             * appendBlock has a successor outside of this block.  This is
             * always possible, because it contains lastModified.  
             */
            if (appendBlock.jump == null) {
                /* assert(appendBlock.jump.getNextFlowBlock() != null) */
                appendBlock.setJump(appendBlock.getNextFlowBlock());
            }

            /* Now unify the blocks: Create a new SequentialBlock
             * containing appendBlock and successor.block.  Then replace
             * appendBlock with the new sequential block.
             */
            StructuredBlock outer = appendBlock.outer;
            StructuredBlock sequBlock = 
                new SequentialBlock(appendBlock, switchBlock);
            outer.replaceSubBlock(appendBlock, sequBlock);
            sequBlock.outer = outer;
        }

        /* Try to eliminate as many jumps as possible.
         */

        optimizeJumps(succ, appendBlock);

        /* Now remove the jump of the appendBlock if it points to successor.
         */

        if (appendBlock.jump == succ)
            appendBlock.removeJump();

        /* If there are further jumps, put a do/while(0) block around
         * appendBlock and replace every remaining jump with a break
         * to the do/while block.
         */
        
        /* Merge the sucessors from the successing flow block
         */
        enum = succ.successors.elements();
        while (enum.hasMoreElements()) {
            successors.addElement(enum.nextElement());
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
    }

    public boolean doT2() {
        /* If there are no jumps to the beginning of this flow block
         * or if this block has other predecessors which weren't
         * considered yet, return false.  The second condition make
         * sure that the while isn't created up to the first continue.
         */

        /* Update the in/out-Vectors now */
        updateInOut(successor, false);

        /* If there is only one jump to the beginning and it is the
         * last jump and (there is a do/while(0) block surrounding
         * everything but the last instruction, or the last
         * instruction is a increase/decrease statement), replace the
         * do/while(0) with a for(;;last_instr) resp. create a new one
         * and replace breaks to do/while with continue to for.  
         */
        {
            /* Otherwise: */

            /* create a new while(true) block.
             */

            /* Try to eliminate as many jumps as possible.
             */
            optimizeJumps(this, block);

            /* Now remove the jump of block if it points to this.
             */

            /* if there are further jumps to this, replace every jump with a
             * continue to while block and return true.  
             */
        }
    }

    public void removeSuccessor(Jump jump) {
        successors.setElementAt(null, successors.indexOf(jump));
    }
}
