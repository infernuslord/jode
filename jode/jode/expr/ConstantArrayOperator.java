/* 
 * ConstantArrayOperator (c) 1998 Jochen Hoenicke
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

package jode.decompiler;
import jode.Type;
import jode.ArrayType;

public class ConstantArrayOperator extends NoArgOperator {

    ConstOperator empty;
    Expression[] values;
    Type argType;
    boolean isInitializer;

    public ConstantArrayOperator(Type type, int size) {
        super(type);
        values = new Expression[size];
        argType = (type instanceof ArrayType) 
            ? Type.tSubType(((ArrayType)type).getElementType()) : Type.tError;
        empty  = new ConstOperator(argType, "0");
        empty.makeInitializer();
    }

    public void setType(Type newtype) {
        super.setType(newtype);
        Type newArgType = (this.type instanceof ArrayType) 
            ? Type.tSubType(((ArrayType)this.type).getElementType()) 
            : Type.tError;
        if (!newArgType.equals(argType)) {
            argType = newArgType;
            empty.setType(argType);
            for (int i=0; i< values.length; i++)
                if (values[i] != null)
                    values[i].setType(argType);
        }
    }

    public boolean setValue(int index, Expression value) {
        if (index < 0 || index > values.length || values[index] != null)
            return false;
        value.setType(argType);
        setType(Type.tSuperType(Type.tArray(value.getType())));
        values[index] = value;
        value.parent = this;
        value.makeInitializer();
        return true;
    }

    public int getPriority() {
        return 200;
    }

    public void makeInitializer() {
        isInitializer = true;
    }

    public String toString(String[] operands) {
        StringBuffer result = isInitializer ? new StringBuffer("{ ")
            : new StringBuffer("new ").append(type).append(" { ");
        for (int i=0; i< values.length; i++) {
            if (i>0)
                result.append(", ");
            result.append((values[i] != null) ? values[i] : empty);
        }
        return result.append(" }").toString();
    }
}
