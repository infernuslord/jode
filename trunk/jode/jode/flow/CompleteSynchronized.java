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

        SynchronizedBlock synBlock;
        try {
            synBlock = (SynchronizedBlock) flow.lastModified;

            SequentialBlock sequBlock = 
                (SequentialBlock) synBlock.outer;

            ComplexExpression monenter = (ComplexExpression)
                ((InstructionBlock) sequBlock.subBlocks[0]).getInstruction();

            if (!(monenter.getOperator() instanceof MonitorEnterOperator)
                || ((LocalLoadOperator) monenter.getSubExpressions()[0]).
                getLocalInfo() != synBlock.local.getLocalInfo())
                return false;
        } catch (ClassCastException ex) {
            return false;
        } catch (NullPointerException ex) {
            return false;
        }

        if (jode.Decompiler.isVerbose)
            System.err.print("f");

        synBlock.isEntered = true;
        synBlock.replace(synBlock.outer, synBlock);

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
        } catch (NullPointerException ex) {
            return true;
        }

        synBlock.object = object;
        synBlock.replace(synBlock.outer, synBlock);
        return true;
    }
}
