/* 
 * ConvertOperator (c) 1998 Jochen Hoenicke
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

public class ConvertOperator extends Operator {
    Type from;

    public ConvertOperator(Type from, Type to) {
        super(to, 0);
        this.from = from;
    }
    
    public int getPriority() {
        return 700;
    }

    public int getOperandPriority(int i) {
        return 700;
    }

    public int getOperandCount() {
        return 1;
    }

    public Type getOperandType(int i) {
        return from;
    }

    public void setOperandType(Type[] inputTypes) {
        from = from.intersection(inputTypes[0]);
    }

    public String toString(String[] operands)
    {
        return "("+type.toString()+") "+operands[0];
    }
}
