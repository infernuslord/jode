/* 
 * StoreInstruction (c) 1998 Jochen Hoenicke
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

public abstract class StoreInstruction extends Operator {

    public String lvCasts;
    Type lvalueType;

    public StoreInstruction(Type type, int operator) {
        super(Type.tVoid, operator);
        lvalueType = MyType.tSubType(type);
        lvCasts = lvalueType.toString();
    }

    public Type getLValueType() {
        return lvalueType;
    }

    public abstract boolean matches(Operator loadop);
    public abstract int getLValueOperandCount();
    public abstract int getLValueOperandPriority(int i);
    public abstract Type getLValueOperandType(int i);
    public abstract void setLValueOperandType(Type [] t);

    /**
     * Sets the type of the lvalue (and rvalue).
     * @return true if the operand types changed
     */
    public boolean setLValueType(Type type) {
//         if (!MyType.isOfType(type, this.lvalueType)) {
//             lvCasts = type.toString()+"/*invalid*/ <- " + lvCasts;
//         } else if (type != this.lvalueType) {
//             lvCasts = type.toString()+" <- " + lvCasts;
//         }
        this.lvalueType = type;
        return false;
    }

    public abstract String getLValueString(String[] operands);

    public int getPriority() {
        return 100;
    }

    public int getOperandPriority(int i) {
        if (i == getLValueOperandCount())
            return 100;
        else
            return getLValueOperandPriority(i);
    }

    public Type getOperandType(int i) {
        if (i == getLValueOperandCount())
            return MyType.tSubType(getLValueType());
        else
            return getLValueOperandType(i);
    }

    public void setOperandType(Type[] t) {
        if (getLValueOperandCount() > 0)
            setLValueOperandType(t);
        setLValueType(MyType.intersection
		      (lvalueType, 
		       MyType.tSuperType(t[getLValueOperandCount()])));
    }

    public int getOperandCount() {
        return 1 + getLValueOperandCount();
    }

    public String toString(String[] operands)
    {
        return getLValueString(operands) + getOperatorString() +
            operands[getLValueOperandCount()];
    }
}
