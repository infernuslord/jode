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
 * @author Jochen Hoenicke
 * @date 98/08/06
 */
public class RangeType extends Type {
    final Type bottomType;
    final Type topType;

    public RangeType(Type bottomType, Type topType) {
        super(TC_RANGE);
	this.bottomType = bottomType;
	this.topType    = topType;
    }

    public Type getBottom() {
        return bottomType;
    }

    public Type getTop() {
        return topType;
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
        topType.useType();
    }

    public String toString()
    {
        if (jode.Decompiler.isTypeDebugging)
            return "<" + bottomType + "-" + topType + ">";
        return topType.toString();
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
