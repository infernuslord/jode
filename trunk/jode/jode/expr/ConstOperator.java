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
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public int getPriority() {
        return 1000;
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
        } if (type == Type.tChar) {
            char i = (char) Integer.parseInt(value);
            switch (i) {
            case '\t':
                return "\'\\t\'";
            case '\n':
                return "\'\\n\'";
            case '\\':
                return "\'\\\\\'";
            case '\"':
                return "\'\\\"\'";
            case '\'':
                return "\'\\\'\'";
            }
            if (i >= 32 && i <128)
                return "\'"+i+"\'";
        } else if (parent != null) {
            int opindex = parent.getOperator().getOperatorIndex();
            if (opindex >= OPASSIGN_OP + ADD_OP
                && opindex <  OPASSIGN_OP + ASSIGN_OP)
                opindex -= OPASSIGN_OP;

            if (opindex >= AND_OP && opindex < AND_OP + 3) {
                /* For bit wise and/or/xor change representation.
                 */
                if (type.isOfType(Type.tUInt)) {
                    int i = Integer.parseInt(value);
                    if (i < -1) 
                        return "~0x"+Integer.toHexString(-i-1);
                    else
                        return "0x"+Integer.toHexString(i);
                } else if (type.equals(Type.tLong)) {
                    long l = Long.parseLong(value);
                    if (l < -1) 
                        return "~0x"+Long.toHexString(-l-1);
                    else
                        return "0x"+Long.toHexString(l);
                }
            }
        }
        return value;
    }
}
