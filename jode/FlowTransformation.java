/* 
 * FlowTransformation (c) 1998 Jochen Hoenicke
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

package jode;

abstract public class FlowTransformation {

    /**
     * This class reverses javac's optimization of while loops.
     * A while loop is normally implemented as follows:
     *
     * <pre>
     * loop: goto cond;
     * head:   <inner block>
     * cond: if <condition> goto head;
     * </pre>
     *
     * The problem is, if a while loop is the destination of a jump
     * (e.g. it's in the else-part of an if, but there are many more
     * cases).  Then the first goto may or may not be removed (depends
     * on the special case) and the previous instruction jumps
     * directly to cond. <p>
     *
     * If we detect an if, that jumps backwards, but in the same block
     * we assume that it belongs to a while statement and bring it to
     * the above standard format. <p>
     *
     * If this function returns with another Header, it is guaranteed
     * that lies in the same block.
     *
     * @param cond The destination of a jump.
     * @return loop, if this is an while.  loop is generated on
     * the fly if it didn't exists before.  If this isn't a while at
     * all, the parameter is returned unchanged.
     */
    public InstructionHeader UnoptimizeWhileLoops(InstructionHeader dest) {
        if (dest.flowType != dest.IFGOTO ||
            dest.successors[1].addr >= dest.addr ||
            dest.successors[1].outer != dest.outer)
            return dest;

        /* Okay, initial checking done,  this really looks like a while
         * statement.  Now we call us recursively, in case the first
         * instruction of this while loop is a while loop again.<p>
         *
         * This won't lead to infinite recursion because the 
         * address of the instruction will always decrease.
         */

        InstructionHeader head = UnoptimizeWhileLoops(dest.successors[1]);

        /* Now we are at head.  Look in front of this if loop is
         * already existing.  If this is the case simply return it.  
         */
        if (head.prevInstruction != null &&
            head.prevInstruction.flowType == head.GOTO &&
            head.prevInstruction.successors[0] == dest)
            return head.prevInstruction;

        /* No there was no loop label.  Create a new one.
         */
        InstructionHeader[] successors = { dest };
        InstructionHeader loop = 
            new InstructionHeader(head.GOTO, head.addr, head.addr,
                                  successors, head.outer);

        /* Connect it in the prev/next Instruction chain.
         */
        loop.prevInstruction = head.prevInstruction;
        if (loop.prevInstruction != null)
            loop.prevInstruction.nextInstruction = loop;
        loop.nextInstruction = head;
        head.prevInstruction = loop;

        /* Loop has no predecessors, but a successor namely dest.
         * The calling function may change this situation.
         */
        dest.predecessors.addElement(loop);
        return loop;
    }

}
