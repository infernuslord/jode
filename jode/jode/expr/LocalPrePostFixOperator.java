/* 
 * LocalPrePostFixOperator (c) 1998 Jochen Hoenicke
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

public class LocalPrePostFixOperator extends NoArgOperator {
    IIncOperator iinc;
    boolean postfix;

    public LocalPrePostFixOperator(Type type, int op, 
                                   IIncOperator iinc, boolean postfix) {
        super(type, op);
	this.iinc = iinc;
        this.postfix = postfix;
    }
    
    public int getPriority() {
        return postfix ? 800 : 700;
    }

    public String toString(String[] operands) {
        if (postfix)
            return iinc.getLocalInfo().getName() + getOperatorString();
        else
            return getOperatorString() + iinc.getLocalInfo().getName();
    }
}
