/* 
 * CreateExpression (c) 1998 Jochen Hoenicke
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

public class CreateExpression implements Transformation {

    public InstructionHeader transform(InstructionHeader ih) {
        Operator op;
        Expression exprs[];
        int params;
        try {
            op = (Operator) ih.getInstruction();
            params  = op.getOperandCount();
            exprs = new Expression[params];
            for (int i = params-1; i>=0; i--) {
                ih = ih.getSimpleUniquePredecessor();
                exprs[i] = (Expression) ih.getInstruction();
                if (exprs[i].isVoid()) {
		    if (i == params-1)
			return null;
		    Expression e = exprs[i+1].tryToCombine(exprs[i]);
		    if (e == null)
			return null;
		    i++;
		    exprs[i] = e;
                    ih = ih.combine(2, e);
		}
            }
        } catch (NullPointerException ex) {
            return null;
        } catch (ClassCastException ex) {
            return null;
        }
        if(Decompiler.isVerbose && params > 0)
            System.err.print("x");
        return ih.combine(params+1, new Expression(op, exprs));
    }
}
