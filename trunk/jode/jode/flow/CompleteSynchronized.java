/* 
 * CompleteSynchronized (c) 1998 Jochen Hoenicke
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
import jode.decompiler.*;

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
        
        if (jode.Decompiler.isVerbose)
            System.err.print('s');
        
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
