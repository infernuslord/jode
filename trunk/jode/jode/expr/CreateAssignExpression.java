/* 
 * CreateAssignExpression (c) 1998 Jochen Hoenicke
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

public class CreateAssignExpression implements Transformation{

    public InstructionHeader transform(InstructionHeader ih) {
        InstructionHeader next = createAssignOp(ih);
        if (next != null)
            return next;
        return createAssignExpression(ih);
    }

    public InstructionHeader createAssignOp(InstructionHeader ih) {
        Expression rightHandSide;
        StoreInstruction store;
        BinaryOperator binop;
        try {
            store = (StoreInstruction) ih.getInstruction();
            ih = ih.getSimpleUniquePredecessor();
            binop = (BinaryOperator) ih.getInstruction();
            if (binop.getOperator() <  binop.ADD_OP ||
                binop.getOperator() >= binop.ASSIGN_OP)
                return null;
            ih = ih.getSimpleUniquePredecessor();
            rightHandSide = (Expression) ih.getInstruction();
            if (rightHandSide.isVoid())
                return null; /* XXX */
            ih = ih.getSimpleUniquePredecessor();
            Operator load = (Operator) ih.getInstruction();
            if (!store.matches(load))
                return null;
            ih = ih.getSimpleUniquePredecessor();
            DupOperator dup = (DupOperator) ih.getInstruction();
            if (dup.getDepth() != 0 && 
                dup.getCount() != store.getLValueOperandCount())
                return null;
        } catch (ClassCastException ex) {
            return null;
        } catch (NullPointerException ex) {
            return null;
        }
        ih = ih.combine(3, rightHandSide);
        InstructionHeader storeIH = ih.getNextInstruction();
        store.setOperator(store.OPASSIGN_OP+binop.getOperator());
        store.setLValueType(MyType.intersection(binop.getType(), 
                                                store.getLValueType()));
        storeIH.combine(2, store);
        return ih;
    }

    public InstructionHeader createAssignExpression(InstructionHeader ih) {
        StoreInstruction store;
        try {
            store = (StoreInstruction) ih.getInstruction();
            ih = ih.getSimpleUniquePredecessor();

            DupOperator dup = (DupOperator) ih.getInstruction();
            if (dup.getDepth() != store.getLValueOperandCount() && 
                dup.getCount() != store.getLValueType().stackSize())
                return null;
        } catch (NullPointerException ex) {
            return null;
        } catch (ClassCastException ex) {
            return null;
        }
        return ih.combine(2, new AssignOperator(Operator.ASSIGN_OP, store));
    }
}
