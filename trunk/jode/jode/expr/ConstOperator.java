/* 
 * ConstOperator (c) 1998 Jochen Hoenicke
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

public class ConstOperator extends NoArgOperator {
    String value;

    public ConstOperator(Type type, String value) {
        super(type);
        if (type == Type.tString)
            value = quoted(value);
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public int getPriority() {
        return 1000;
    }

    public static String quoted(String str) {
        StringBuffer result = new StringBuffer("\"");
        for (int i=0; i< str.length(); i++) {
            switch (str.charAt(i)) {
            case '\t':
                result.append("\\t");
                break;
            case '\n':
                result.append("\\n");
                break;
            case '\\':
                result.append("\\\\");
                break;
            case '\"':
                result.append("\\\"");
                break;
            default:
                result.append(str.charAt(i));
            }
        }
        return result.append("\"").toString();
    }

    public boolean equals(Object o) {
	return (o instanceof ConstOperator) &&
	    ((ConstOperator)o).value.equals(value);
    }

    public String toString(String[] operands) {
        if (type.isOfType(Type.tBoolean)) {
            if (value.equals("0"))
                return "false";
            else if (value.equals("1"))
                return "true";
        }
        return value;
    }
}
