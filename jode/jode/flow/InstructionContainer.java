/* InstructionContainer (c) 1998 Jochen Hoenicke
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
import jode.decompiler.LocalInfo;
import jode.expr.Expression;
import jode.expr.LocalVarOperator;

/**
 * This is a method for block containing a single instruction.
 */
public abstract class InstructionContainer extends StructuredBlock {
    /**
     * The instruction.
     */
    Expression instr;

    public InstructionContainer(Expression instr) {
        this.instr = instr;
        if (instr instanceof LocalVarOperator)
	  used.addElement(((LocalVarOperator)instr).getLocalInfo());
    }

    public InstructionContainer(Expression instr, Jump jump) {
        this.instr = instr;
        if (instr instanceof LocalVarOperator) {
            LocalVarOperator varOp = (LocalVarOperator) instr;
	    used.addElement(varOp.getLocalInfo());
            jump.gen.addElement(varOp.getLocalInfo());
            jump.kill.addElement(varOp.getLocalInfo());
        }
        setJump(jump);
    }

    /**
     * Fill all in variables into the given VariableSet.
     * @param in The VariableSet, the in variables should be stored to.
     */
    public void fillInGenSet(VariableSet in, VariableSet gen) {
        if (instr instanceof LocalVarOperator) {
            LocalVarOperator varOp = (LocalVarOperator) instr;
            if (varOp.isRead()) {
                in.addElement(varOp.getLocalInfo());
            }
            gen.addElement(varOp.getLocalInfo());
        }
    }

    public boolean doTransformations() {
        StructuredBlock last = flowBlock.lastModified;
        return CreateNewConstructor.transform(this, last)
            || CreateAssignExpression.transform(this, last)
            || CreateExpression.transform(this, last)
            || CreatePrePostIncExpression.transform(this, last)
            || CreateIfThenElseOperator.create(this, last)
            || CreateConstantArray.transform(this, last)
	    || CreateCheckNull.transformJavac(this, last);
    }

    /**
     * Get the contained instruction.
     * @return the contained instruction.
     */
    public final Expression getInstruction() {
        return instr;
    }

    /**
     * Set the contained instruction.
     * @param instr the new instruction.
     */
    public final void setInstruction(Expression instr) {
        this.instr = instr;
    }
}
