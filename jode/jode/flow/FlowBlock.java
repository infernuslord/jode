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
     * All Jumps that this flow block contains. 
     */
    Vector successors;

    /**
     * This is a vector of flow blocks, which reference this block.
     * Only if this vector contains exactly one element, it can be
     * moved into the preceding flow block.
     */
    Vector predecessors;
    
    /**
     * This method optimizes the jumps to successor.
     * Returns true, if all jumps could be eliminated.
     */
    public boolean optimizeJumps(FlowBlock successor,
                                 StructuredBlock structBlock) {
        /* if the jump is the jump of the structBlock, skip it.
         */
        
        /* remove all jumps to the successor which have the successor
         * as getNextFlowBlock().  */

        /* replace all conditional jumps to the successor, which are followed
         * by a block which has the end of the block as normal successor,
         * with "if (not condition) block".
         */

        /* if the successor is the dummy return instruction, replace all
         * jumps with a return.
         */

        /* if there are jumps in a while block or switch block and the
         * while/switch block is followed by a jump to successor or has
         * successor as getNextFlowBlock(), replace jump with break to
         * the innermost such while/switch block.
         *
         * If the switch block hasn't been breaked before we could
         * take some heuristics and add a jump after the switch to
         * succesor, so that the above succeeds.
         *
         * If this is a while and the condition of the while is true,
         * and this jump belongs to the first instruction in this
         * while block and is inside a ConditionalBlock remove that
         * ConditionalBlock an move the condition to the while block.  
         */
           
        /* if there are jumps in an if-then block, which
         * have as normal successor the end of the if-then block, and
         * the if-then block is followed by a single block, then replace
         * the if-then block with a if-then-else block and remove the
         * unconditional jump.
         */
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
            FlowBlock fb = ((Jump) enum.nextElement()).destination;
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
        StructuredBlock structBlock = lastModified;
        while(enum.hasMoreElements()) {
            Jump jump = (Jump) enum.nextElement();

            while (!structBlock.contains(jump.parent))
                structBlock = structBlock.outer;
            /* structBlock can't be null now, because the
             * outermost block contains every structured block.  
             */
        }

        /* The switch "fall through" case: if the structBlock is a
         * switch, and the successor is the address of a case, and all
         * other successors are inside the block preceding that case.  
         */
        if (case != null) {
            SwitchBlock switchBlock = (StructuredBlock) structBlock;

            /* Now put the succ.block into the next case.
             */
            switchBlock.replaceSubBlock(nextcase,succ.block);

            /* Do the following modifications on the struct block. */
            structBlock = precedingcase;

        } else {

            /* Prepare the unification of the blocks: Make sure that
             * structBlock has a successor outside of this block.  This is
             * always possible, because it contains lastModified.  
             */
            if (structBlock.jump == null) {
                /* assert(structBlock.jump.getNextFlowBlock() != null) */
                structBlock.setJump(structBlock.getNextFlowBlock());
            }
            
            /* Now unify the blocks: Create a new SequentialBlock
             * containing structBlock and successor.block.  Then replace
             * structBlock with the new sequential block.
             */
        }

        /* Try to eliminate as many jumps as possible.
         */
            
        optimizeJumps(succ, structBlock);

        /* Now remove the jump of the structBlock if it points to successor.
         */

        if (structBlock.jump == null)
            structBlock.removeJump();

        /* If there are further jumps, put a do/while(0) block around
         * structBlock and replace every remaining jump with a break
         * to the do/while block.
         */

        /* Set last modified to correct value.
         */
        lastModified = succ.lastModified;
    }

    public boolean doT2() {
        /* If there are no jumps to the beginning of this flow block
         * or if this block has other predecessors which weren't
         * considered yet, return false.  The second condition make
         * sure that the while isn't created up to the first continue.
         */

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
}




