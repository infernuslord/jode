/* 
 * CreateForInitializer (c) 1998 Jochen Hoenicke
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
import jode.Decompiler;
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

        if (Decompiler.isVerbose)
            Decompiler.err.print('f');

        forBlock.setInit((InstructionBlock) sequBlock.subBlocks[0]);
        return true;
    }
}
