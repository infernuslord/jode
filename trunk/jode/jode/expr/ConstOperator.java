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

package jode.expr;
import jode.Type;
import jode.IntegerType;

public class ConstOperator extends NoArgOperator {
    Object value;
    boolean isInitializer = false;

    private static final Type tBoolConstInt 
	= new IntegerType(IntegerType.IT_I | IntegerType.IT_C 
			  | IntegerType.IT_Z
			  | IntegerType.IT_S | IntegerType.IT_B);

    public ConstOperator(Object constant) {
	super(Type.tUnknown);
	if (constant instanceof Boolean) {
	    setType(Type.tBoolean);
	    constant = new Integer(((Boolean)constant).booleanValue() ? 1 : 0);
	} else if (constant instanceof Integer) {
	    int intVal = ((Integer) constant).intValue();
	    setType 
		((intVal == 0 || intVal == 1) ? tBoolConstInt
		 : (intVal < Short.MIN_VALUE 
		    || intVal > Character.MAX_VALUE) ? Type.tInt
		 : new IntegerType
		 ((intVal < Byte.MIN_VALUE) 
		  ?    IntegerType.IT_S|IntegerType.IT_I
		  : (intVal < 0)
		  ?    IntegerType.IT_S|IntegerType.IT_B|IntegerType.IT_I
		  : (intVal <= Byte.MAX_VALUE)
		  ?    (IntegerType.IT_S|IntegerType.IT_B
			|IntegerType.IT_C|IntegerType.IT_I)
		  : (intVal <= Short.MAX_VALUE)
		  ?    IntegerType.IT_S|IntegerType.IT_C|IntegerType.IT_I
		  :    IntegerType.IT_C|IntegerType.IT_I));
	} else if (constant instanceof Long)
	    setType(Type.tLong);
	else if (constant instanceof Float)
	    setType(Type.tFloat);
	else if (constant instanceof Double)
	    setType(Type.tDouble);
	else if (constant instanceof String)
	    setType(Type.tString);
	else if (constant == null)
	    setType(Type.tUObject);
	else
	    throw new IllegalArgumentException("Illegal constant type: "
					       +constant.getClass());
	value = constant;
    }

    public String getValue() {
        return String.valueOf(value);
    }

    public int getPriority() {
        return 1000;
    }

    public boolean equals(Object o) {
	if (o instanceof ConstOperator) {
	    Object otherValue = ((ConstOperator)o).value;
	    return value == null
		? otherValue == null : value.equals(otherValue);
	}
	return false;
    }

    public void makeInitializer() {
        isInitializer = true;
    }

    private static String quoted(String str) {
        StringBuffer result = new StringBuffer("\"");
        for (int i=0; i< str.length(); i++) {
            char c;
            switch (c = str.charAt(i)) {
            case '\0':
                result.append("\\0");
                break;
            case '\t':
                result.append("\\t");
                break;
            case '\n':
                result.append("\\n");
                break;
            case '\r':
                result.append("\\r");
                break;
            case '\\':
                result.append("\\\\");
                break;
            case '\"':
                result.append("\\\"");
                break;
            default:
                if (c < 32) {
                    String oct = Integer.toOctalString(c);
                    result.append("\\000".substring(0, 4-oct.length()))
                        .append(oct);
                } else if (c >= 32 && c < 127)
                    result.append(str.charAt(i));
                else {
                    String hex = Integer.toHexString(c);
                    result.append("\\u0000".substring(0, 6-hex.length()))
                        .append(hex);
                }
            }
        }
        return result.append("\"").toString();
    }

    public String toString(String[] operands) {
        String strVal = String.valueOf(value);
        if (type.isOfType(Type.tBoolean)) {
	    int intVal = ((Integer)value).intValue();
            if (intVal == 0)
                return "false";
            else if (intVal == 1)
                return "true";
	    else 
		throw new jode.AssertError
		    ("boolean is neither false nor true");
        } 
	if (type.getHint().equals(Type.tChar)) {
            char c = (char) ((Integer) value).intValue();
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
	} else if (type.equals(Type.tString)) {
	    return quoted(strVal);
        } else if (parent != null) {
            int opindex = parent.getOperator().getOperatorIndex();
            if (opindex >= OPASSIGN_OP + ADD_OP
                && opindex <  OPASSIGN_OP + ASSIGN_OP)
                opindex -= OPASSIGN_OP;

            if (opindex >= AND_OP && opindex < AND_OP + 3) {
                /* For bit wise and/or/xor change representation.
                 */
                if (type.isOfType(Type.tUInt)) {
                    int i = ((Integer) value).intValue();
                    if (i < -1) 
                        strVal = "~0x"+Integer.toHexString(-i-1);
                    else
                        strVal = "0x"+Integer.toHexString(i);
                } else if (type.equals(Type.tLong)) {
                    long l = ((Long) value).longValue();
                    if (l < -1) 
                        strVal = "~0x"+Long.toHexString(-l-1);
                    else
                        strVal = "0x"+Long.toHexString(l);
                }
            }
        }
        if (type.isOfType(Type.tLong))
            return strVal+"L";
        if (type.isOfType(Type.tFloat))
            return strVal+"F";
        if (!type.isOfType(Type.tInt) 
	    && (type.getHint().equals(Type.tByte)
		|| type.getHint().equals(Type.tShort))
            && !isInitializer
	    && (parent == null 
		|| parent.getOperator().getOperatorIndex() != ASSIGN_OP)) {
            /* One of the strange things in java.  All constants
             * are int and must be explicitly casted to byte,...,short.
             * But in assignments and initializers this cast is unnecessary.
	     * See JLS section 5.2
             */
            return "("+type.getHint()+") "+strVal;
	}

        return strVal;
    }
}
