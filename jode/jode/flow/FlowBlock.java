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
     * All jumps that this flow block contains. 
     */
    Vector successors;

    /**
     * This is a vector of flow blocks, which reference this block.
     * Only if this vector contains exactly one element, it can be
     * moved into the preceding flow block.
     */
    Vector predecessors;
    
    public void optimizeJumps(FlowBlock successor) {
        /* remove all unconditional jumps to the successor which have the
         * end of the block as normal successor.
         */

        /* replace all conditional jumps to the successor, which are followed
         * by a block which has the end of the block as normal successor,
         * with "if (not condition) block".
         */

        /* if there are jumps in a while block, which has the end of
         * the block as normal successor or is followed by a
         * unconditional jump to successor, replace jumps with breaks
         * to the while block.  If the condition of the while is true,
         * and the first instruction is converted to a conditional break
         * remove that break and use the condition in the while block.
         */
           

        /* if there are jumps in an switch block, which has as normal
         * successor the end of this FlowBlock, replace them with a
         * break to the switch block.
         * if there successor isn't the end of this FlowBlock, but
         * there are many, or the next rule could work (some more
         * heuristics?)  and there is currently no flow to the end of
         * the switch block, replace all such successors with a break
         * and add a new unconditional jump after the switch block.  */

        /* if there are unconditional jumps in an if-then block, which
         * have as normal successor the end of the if-then block, and
         * the if-then block is followed by a single block, which has
         * as normal successor the end of the block,  then replace
         * the if-then block with a if-then-else block and remove the
         * unconditional jumps.
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
           
        /* check if this successor has only this block as predecessor. */
        /* if not, return false. */

        /* The switch case: if the last instruction in this block is a
         * switch block, and all jumps to successor lie in a single case,
         * or if all jumps
         * which doesn't have the end of the block as normal
         * successor, combine the successor with that block. But this
         * doesn't catch all cases.
         */

        optimizeJumps(successor);
        
        /* if the successor is the dummy return instruction, replace all
         * jumps with a return.
         */

        /* If there are further jumps, put a do/while(0) block around
         * current block and replace every necessary jump with a break
         * to the do/while block.  Then combine the block with a
         * sequential composition.  */
    }

    public boolean doT2() {
        /* If there are no jumps to the beginning of this flow block
         * or if this block has other predecessors which weren't
         * considered yet, return false.  
         */

        /* If there is only one jump to the beginning and it is the
         * last jump and (there is a do/while(0) block surrounding
         * everything but the last instruction, or the last
         * instruction is a increase/decrease statement), replace the
         * do/while(0) with a for(;;last_instr) resp. create a new one
         * and replace breaks to do/while with continue.  
         */
        {
            /* Otherwise: */
            optimizeJumps(this);
            
            /* create a new while(true) block.
             */
        }

        /* if there are further jumps to this, replace every jump with a
         * continue to while block and return true.  
         */
    }
}

/**
 * A structured block describes a structured program.  There may be a
 * Jump after a block (even after a inner structured block), but the 
 * goal is, to remove all such jumps and make a single flow block.<p>
 *
 * There are following types of structured blocks: <ul>
 * <li>if-then-(else)-block  (IfThenElseBlock)</li>
 * <li>(do)-while/for-block  (LoopBlock)
 * <li>switch-block          (SwitchBlock)
 * <li>try-catch-finally-block (TryBlock)
 * <li>A block consisting of sub blocks  (SequentialBlock)
 * </ul>
 */
public class StructuredBlock {
    /** The surrounding block, or null if this is the outermost block in a
     * flow block.
     */
    StructuredBlock parent;

    /** The flow block to which this structured block belongs.
     */
    FlowBlock flow;

}

public class ConditionalJump extends Jump {
    Expression condition;
    StructuredBlock prev;
    FlowBlock parent;
    FlowBlock jump, nojump;
}

public class UnconditionalJump extends Jump {
    StructuredBlock prev;
    FlowBlock parent;
    FlowBlock jump;
}
