/* 
 * CreatePostIncExpression (c) 1998 Jochen Hoenicke
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
import sun.tools.java.Type;

public class CreatePostIncExpression implements Transformation {

    public InstructionHeader transform(InstructionHeader ih)
    {
        InstructionHeader next = createLocalPostInc(ih);
        if (next != null)
            return next;
        return createPostInc(ih);
    }
    
    public InstructionHeader createLocalPostInc(InstructionHeader ih) {
	IIncOperator iinc;
	int op;
	Type type;
        try {
	    Expression iincExpr = (Expression) ih.getInstruction();
	    iinc = (IIncOperator) iincExpr.getOperator();
	    if (iinc.getOperator() == iinc.ADD_OP + iinc.OPASSIGN_OP)
                op = Operator.INC_OP;
            else if (iinc.getOperator() == iinc.NEG_OP + iinc.OPASSIGN_OP)
                op = Operator.DEC_OP;
            else
                return null;
            if (!iinc.getValue().equals("1") &&
		!iinc.getValue().equals("-1"))
                return null;
            if (iinc.getValue().equals("-1"))
		op ^= 1;
            ih = ih.getSimpleUniquePredecessor();
	    Expression loadExpr = (Expression) ih.getInstruction();
	    LocalLoadOperator load = 
		(LocalLoadOperator)loadExpr.getOperator();
	    if (!iinc.matches(load))
		return null;

	    type = MyType.intersection(load.getType(), MyType.tUInt);
        } catch (NullPointerException ex) {
            return null;
        } catch (ClassCastException ex) {
            return null;
        }
	Operator postop = new LocalPostFixOperator(type, op, iinc);
	return ih.combine(2, postop);
    }

    public InstructionHeader createPostInc(InstructionHeader ih) {
	StoreInstruction store;
	int op;
	Type type;
        try {
	    store = (StoreInstruction) ih.getInstruction();
	    if (store.getLValueOperandCount() == 0)
		return null;
            ih = ih.getSimpleUniquePredecessor();
            BinaryOperator binOp = (BinaryOperator) ih.getInstruction();
            if (binOp.getOperator() == store.ADD_OP)
                op = Operator.INC_OP;
            else if (store.getOperator() == store.NEG_OP)
                op = Operator.DEC_OP;
            else
                return null;
            ih = ih.getSimpleUniquePredecessor();
            Expression expr = (Expression) ih.getInstruction();
            ConstOperator constOp = (ConstOperator) expr.getOperator();
            if (!constOp.getValue().equals("1") &&
		!constOp.getValue().equals("-1"))
                return null;
            if (constOp.getValue().equals("-1"))
		op ^= 1;
            ih = ih.getSimpleUniquePredecessor();
            DupOperator dup = (DupOperator) ih.getInstruction();
            if (dup.getCount() != store.getLValueType().stackSize() ||
                dup.getDepth() != store.getLValueOperandCount())
                return null;
            ih = ih.getSimpleUniquePredecessor();
            Operator load = (Operator) ih.getInstruction();

	    if (!store.matches(load))
		return null;

	    ih = ih.getSimpleUniquePredecessor();
	    DupOperator dup2 = (DupOperator) ih.getInstruction();
	    if (dup2.getCount() != store.getLValueOperandCount() ||
		dup2.getDepth() != 0)
		return null;

	    type = MyType.intersection(load.getType(), store.getLValueType());
        } catch (NullPointerException ex) {
            return null;
        } catch (ClassCastException ex) {
            return null;
        }
	Operator postop = new PostFixOperator(type, op, store);
	return ih.combine(6, postop);
    }
}
