/* 
 * CreateIfThenElseOperator (c) 1998 Jochen Hoenicke
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
import java.util.Enumeration;
import java.util.Vector;

public class CreateIfThenElseOperator implements Transformation{

    public InstructionHeader createFunny(InstructionHeader ih) {
        Expression cond = null;
        try {
            InstructionHeader ifHeader= ih;
            if (ifHeader.getFlowType() != ifHeader.IFGOTO)
                return null;
            CompareUnaryOperator compare = 
                (CompareUnaryOperator) ifHeader.getInstruction();
            if ((compare.getOperator() & ~1) != compare.EQUALS_OP)
                return null;
            Enumeration enum = ih.getPredecessors().elements();
            while (enum.hasMoreElements()) {
                try {
                    ih = (InstructionHeader) enum.nextElement();

                    if (ih.flowType == ih.GOTO)
                        ih = ih.getUniquePredecessor();

                    Expression zeroExpr = (Expression) ih.getInstruction();
                    ConstOperator zero = 
                        (ConstOperator) zeroExpr.getOperator();
                    if (!zero.getValue().equals("0"))
                        continue;

                    ih = ih.getUniquePredecessor();
                    if (ih.getFlowType() != ih.IFGOTO)
                        continue;
                    
                    if (compare.getOperator() == compare.EQUALS_OP && 
                        ih.getSuccessors()[1] != ifHeader.getSuccessors()[0]
                        || compare.getOperator() == compare.NOTEQUALS_OP &&
                        ih.getSuccessors()[1] != ifHeader.getSuccessors()[1])
                        continue;
                    
                    cond = (Expression) ih.getInstruction();
                    break;
                } catch (ClassCastException ex) {
                } catch (NullPointerException ex) {
                }
            }

            if (cond == null)
                return null;

        } catch (ClassCastException ex) {
            return null;
        } catch (NullPointerException ex) {
            return null;
        }

        InstructionHeader next = ih.nextInstruction;
        ih.successors[1].predecessors.removeElement(ih);
        next.instr        = ih.instr;
        next.movePredecessors(ih);
        return next;
    }

    public InstructionHeader create(InstructionHeader ih2) {
        InstructionHeader ifHeader;
        InstructionHeader gotoIH;
        Expression e[] = new Expression[3];
        try {
            Vector predec = ih2.getPredecessors();

            if (predec.size() != 1)
                return null;
            ifHeader = (InstructionHeader) predec.elementAt(0);
            if (ifHeader.getFlowType() != ifHeader.IFGOTO)
                return null;

            InstructionHeader ih1 = ifHeader.getSuccessors()[0];
            gotoIH = ih1.getNextInstruction();
            
            if (ifHeader.getSuccessors()[1] != ih2 ||
                ih1.flowType != ifHeader.NORMAL ||
                gotoIH.flowType != ifHeader.GOTO ||
                ih2.flowType != ifHeader.NORMAL ||
                gotoIH.getNextInstruction() != ih2 ||
                gotoIH.getSuccessors()[0] != ih2.getNextInstruction())
                return null;

            e[1] = (Expression) ih1.getInstruction();
            if (e[1].isVoid())
                return null;
            e[2] = (Expression) ih2.getInstruction();
            if (e[2].isVoid())
                return null;
            e[0] = (Expression) ifHeader.getInstruction();
        } catch (ClassCastException ex) {
            return null;
        } catch (NullPointerException ex) {
            return null;
        }

        if (Decompiler.isVerbose)
            System.err.print("?");

        e[0] = e[0].negate();
        IfThenElseOperator iteo = new IfThenElseOperator
            (MyType.intersection(e[1].getType(),e[2].getType()));

        ih2.instr = new Expression(iteo, e);
        ih2.movePredecessors(ifHeader);
        ih2.nextInstruction.predecessors.removeElement(gotoIH);
        return ih2;
    }

    public InstructionHeader transform(InstructionHeader ih) {
        InstructionHeader next = createFunny(ih);
        if (next != null)
            return next;
        return create(ih);
    }
}
