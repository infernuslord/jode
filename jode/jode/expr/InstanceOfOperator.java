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

package jode;
import sun.tools.java.Type;

public class InstanceOfOperator extends SimpleOperator {
    String typeString;

    public InstanceOfOperator(Type type, String typeString) {
        super(Type.tBoolean, 0, 1);
        this.operandTypes[0] = MyType.tSuperType(type);
        this.typeString = typeString;
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

    public String toString(String[] operands) {
        return operands[0] + " instanceof "+typeString;
    }
}
