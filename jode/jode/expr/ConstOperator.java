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

    boolean isInitializer = false;

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

    public void makeInitializer() {
        isInitializer = true;
    }

    public String toString(String[] operands) {
        String value = this.value;
        if (type.isOfType(Type.tBoolean)) {
            if (value.equals("0"))
                return "false";
            else if (value.equals("1"))
                return "true";
        } if (type.getBottom() == Type.tChar) {
            char c = (char) Integer.parseInt(value);
            switch (c) {
            case '\0':
                return "\'\\0\'";
            case '\t':
                return "\'\\t\'";
            case '\n':
                return "\'\\n\'";
            case '\r':
                return "\'\\r\'";
            case '\\':
                return "\'\\\\\'";
            case '\"':
                return "\'\\\"\'";
            case '\'':
                return "\'\\\'\'";
            }
            if (c < 32) {
                String oct = Integer.toOctalString(c);
                return "\'\\000".substring(0, 5-oct.length())+oct+"\'";
            }
            if (c >= 32 && c < 127)
                return "\'"+c+"\'";
            else {
                String hex = Integer.toHexString(c);
                return "\'\\u0000".substring(0, 7-hex.length())+hex+"\'";
            }
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
                        value = "~0x"+Integer.toHexString(-i-1);
                    else
                        value = "0x"+Integer.toHexString(i);
                } else if (type.equals(Type.tLong)) {
                    long l = Long.parseLong(value);
                    if (l < -1) 
                        value = "~0x"+Long.toHexString(-l-1);
                    else
                        value = "0x"+Long.toHexString(l);
                }
            }
        }
        if (type.isOfType(Type.tLong))
            return value+"L";
        if (type.isOfType(Type.tFloat))
            return value+"F";
        if (!type.isOfType(Type.tInt) && type.isOfType(Type.tUInt) 
            && !isInitializer)
            /* One of the strange things in java.  All constants
             * are int and must be explicitly casted to byte,...,short.
             * But in initializers this cast is unnecessary.
             */
            return "("+type+") "+value;

        return value;
    }
}
