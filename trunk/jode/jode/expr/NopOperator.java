/* 
 * NopOperator (c) 1998 Jochen Hoenicke
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

package jode.expr;
import jode.Type;

/**
 * A NopOperator takes one or zero arguments and returns it again.  It
 * is mainly used as placeholder when the real operator is not yet
 * known (e.g. in SwitchBlock).  But there also exists a nop opcode in
 * the java virtual machine (The compiler can't create such a opcode,
 * though).
 *
 * @author Jochen Hoenicke */
public class NopOperator extends SimpleOperator {
    public NopOperator(Type type) {
	super(type, 0, 1);
    }

    public NopOperator() {
        super(Type.tVoid, 0, 0);
    }

    public int getPriority() {
        return 0;
    }

    public int getOperandPriority(int i) {
        return 0;
    }

    public boolean equals(Object o) {
	return (o instanceof NopOperator);
    }

    public String toString(String[] operands) {
        if (type == Type.tVoid)
            return "/* nop */";
        return operands[0];
    }
}
