/* 
 * RangeType (c) 1998 Jochen Hoenicke
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
import java.util.Hashtable;

/**
 * This class represents an object type which isn't fully known.  
 * The real object type lies in a range of types between topType
 * and bottomType. <p>
 *
 * For a totally unknown type topType is tObject and bottomType is
 * null.  It is always garanteed that topType is an Array or an Object
 * and that bottomType is null or an Array or an Object. <p>
 *
 * The hintType gives a hint which type it probably is.  It is used to
 * generate the type declaration.
 *
 * @author Jochen Hoenicke
 * @date 98/08/06
 */
public class RangeType extends Type {
    final Type bottomType;
    final Type topType;
    final Type hintType;

    public RangeType(Type bottomType, Type topType, Type hintType) {
        super(TC_RANGE);
	this.bottomType = bottomType;
	this.topType    = topType;
	this.hintType   = hintType;
    }

    public RangeType(Type bottomType, Type topType) {
        super(TC_RANGE);
	this.bottomType = bottomType;
	this.topType    = topType;
	this.hintType   = bottomType.isValidType() ? bottomType : topType;
    }

    public Type getBottom() {
        return bottomType;
    }

    public Type getTop() {
        return topType;
    }

    public Type getHint() {
	return hintType.getHint();
    }

    /**
     * Create the type corresponding to the range from bottomType to this.
     * @param bottomType the start point of the range
     * @return the range type, or tError if not possible.
     */
    public Type createRangeType(Type bottomType) {
        throw new AssertError("createRangeType called on RangeType");
    }

    /**
     * Returns the common sub type of this and type.
     * @param type the other type.
     * @return the common sub type.
     */
    public Type getSpecializedType(Type type) {
        throw new AssertError("getSpecializedType called on RangeType");
    }

    /**
     * Returns the common super type of this and type.
     * @param type the other type.
     * @return the common super type.
     */
    public Type getGeneralizedType(Type type) {
        throw new AssertError("getGeneralizedType called on RangeType");
    }
	    
    /**
     * Marks this type as used, so that the class is imported.
     */
    public void useType() {
        /* The topType will be printed */
        if (topType.isClassType() || !bottomType.isValidType())
            topType.useType();
        else
            bottomType.useType();
    }

    /**
     * Checks if we need to cast to a middle type, before we can cast from
     * fromType to this type.
     * @return the middle type, or null if it is not necessary.
     */
    public Type getCastHelper(Type fromType) {
	return topType.getCastHelper(fromType);
    }

    public String getTypeSignature() {
        if (topType.isClassType() || !bottomType.isValidType())
            return topType.getTypeSignature();
        else
            return bottomType.getTypeSignature();
    }

    public String toString()
    {
        if (jode.Decompiler.isTypeDebugging)
            return "<" + bottomType + "-" + hintType + "-" + topType + ">";
	return hintType.toString();
    }

    public String getDefaultName() {
	return hintType.getDefaultName();
    }

    public int hashCode() {
	int hashcode = topType.hashCode();
	return (hashcode << 16 | hashcode >>> 16) ^ bottomType.hashCode();
    }

    public boolean equals(Object o) {
        if (o instanceof RangeType) {
            RangeType type = (RangeType) o;
            return topType.equals(type.topType) 
                && bottomType.equals(type.bottomType);
        }
        return false;
    }
}
