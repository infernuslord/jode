/* 
 * CreateNewConstructor (c) 1998 Jochen Hoenicke
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

public class CreateNewConstructor implements Transformation{

    public InstructionHeader transform(InstructionHeader ih) {
        InvokeOperator constrCall;
        Expression exprs[];
        try {
            constrCall = (InvokeOperator) ih.getInstruction();
            if (!constrCall.isConstructor())
                return null;
            int params  = constrCall.getOperandCount();
            exprs = new Expression[params];
            for (int i = params-1; i>0; i--) {
                ih = ih.getSimpleUniquePredecessor();
                exprs[i] = (Expression) ih.getInstruction();
                if (exprs[i].isVoid())
                    return null; /* XXX */
            }
            ih = ih.getSimpleUniquePredecessor();
            DupOperator dup = (DupOperator) ih.getInstruction();
            if (dup.getCount() != 1 && dup.getDepth() != 0)
                return null;
            ih = ih.getSimpleUniquePredecessor();
            exprs[0] = (Expression) ih.getInstruction();
            if (exprs[0].isVoid())
                return null;
            NewOperator op = (NewOperator) exprs[0].getOperator();
            if (constrCall.getClassType() != op.getType())
                return null;
        } catch (ClassCastException ex) {
            return null;
        } catch (NullPointerException ex) {
            return null;
        }
        ConstructorOperator conOp = 
            new ConstructorOperator(constrCall.getClassType(), 
                                    constrCall.getField());

        return ih.combine(exprs.length+2, new Expression(conOp, exprs));
    }
}
