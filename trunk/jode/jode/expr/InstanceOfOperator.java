/* 
 * InstanceOfOperator (c) 1998 Jochen Hoenicke
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

public class InstanceOfOperator extends SimpleOperator {

    Type instanceType;
    /**
     * There are special cases where a instanceof isn't allowed.  We must cast
     * to the common super type before.  This cases always give a runtime
     * error, but we want to decompile even bad programs.
     */
    Type superType = null;
    

    public InstanceOfOperator(Type type) {
        super(Type.tBoolean, 0, 1);
        this.instanceType = type;
        this.operandTypes[0] = Type.tUnknown;
    }
    public int getOperandCount() {
        return 1;
    }

    public int getPriority() {
        return 550;
    }

    public int getOperandPriority(int i) {
        return getPriority();
    }

    public void setOperandType(Type[] type) {
	super.setOperandType(type);
	superType = instanceType.getCastHelper(type[0]);
    }

    public String toString(String[] operands) {
        StringBuffer sb = new StringBuffer();
	if (superType != null)
	    sb.append("((").append(superType).append(")");
	sb.append(operands[0]);
	if (superType != null)
	    sb.append(")");
        return sb.append(" instanceof ").append(instanceType).toString();
    }
}
