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
import jode.MonitorEnterOperator;
import jode.ComplexExpression;
import jode.LocalLoadOperator;
import jode.LocalStoreOperator;
import jode.Expression;

public class CompleteSynchronized implements Transformation {

    /**
     * This combines the monitorenter and the initial expression
     * into a synchronized statement
     * @param flow The FlowBlock that is transformed 
     */
    public boolean transform(FlowBlock flow) {

        if (!(flow.lastModified instanceof SynchronizedBlock)
            || flow.lastModified.outer == null)
            return false;

        /* If the program is well formed, the following succeed */

        SynchronizedBlock synBlock = (SynchronizedBlock) flow.lastModified;
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
        synBlock.moveDefinitions(synBlock.outer,synBlock);
        synBlock.replace(synBlock.outer);

        /* Is there another expression? */
        if (synBlock.outer == null)
            return false;

        Expression object;
        try {
            SequentialBlock sequBlock = 
                (SequentialBlock) synBlock.outer;

            ComplexExpression assign = (ComplexExpression)
                ((InstructionBlock) sequBlock.subBlocks[0]).getInstruction();

            if (((LocalStoreOperator) assign.getOperator()).
                getLocalInfo() != synBlock.local.getLocalInfo())
                return true;

            object = assign.getSubExpressions()[0];

        } catch (ClassCastException ex) {
            return true;
        }

        synBlock.object = object;
        synBlock.moveDefinitions(synBlock.outer,synBlock);
        synBlock.replace(synBlock.outer);
        return true;
    }
}
