/* 
 * ClassRangeType (c) 1998 Jochen Hoenicke
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
import sun.tools.java.*;

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
public class ClassRangeType extends MyType {
    final Type bottomType;
    final Type topType;

    public ClassRangeType(Type bottomType, Type topType) {
        super(103, "-");
	if (bottomType != null && bottomType.getTypeCode() == 103)
	    bottomType = ((ClassRangeType)bottomType).bottomType;
	if (topType != null && topType.getTypeCode() == 103)
	    topType = ((ClassRangeType)topType).topType;
        typeSig = "-"+
            (bottomType == null ? "0" : bottomType.getTypeSignature()) + 
            (topType    == null ? "0" : topType.getTypeSignature());
	this.bottomType = bottomType;
	this.topType    = topType;
    }

    public static Type createRangeType(Type bottom, Type top) {
	// TODO: XXX calculate   < top, ...> \cap <..., bottom>
	// e.g.  top  ,  bottom     result
	//       x    ,  null      <x, null>
	//    tUnknown,  object    <tObject, object>
	//    Fahrrad ,  Fahrzeug   error
	//    Fahrzeug,  Fahrrad   <Fahrzeug, Fahrrad>
	//     int    ,  Fahrrad    error

	if (bottom != null && bottom.getTypeCode() == 103) {
	    bottom = ((ClassRangeType)bottom).bottomType;
	}
	if (top != null && top.getTypeCode() == 103) {
	    top = ((ClassRangeType)top).topType;
	}

	/* First the trivial cases
	 */
	if (top == tError || bottom == tError) 
	    return tError;

	/* This is always okay (right open interval, maybe left open)
	 */
	if (top == null)
	    return new ClassRangeType(bottom,top);

	/* <null, object> -> <tObject, object> 
	 * if bottom is tObject, its okay.
	 */

	if (bottom == top)
	    return bottom;

        if (top.getTypeCode() <= 4 && bottom == null)
            return top;

        if (bottom != null && bottom.getTypeCode() <= 4 && 
            top.getTypeCode() <= bottom.getTypeCode())
            return bottom;

        if (top.getTypeCode() != 9 
            && top.getTypeCode() != 10
            && top.getTypeCode() != 104)
            return tError;

        if (bottom == null || bottom == tObject)
	    return new ClassRangeType(tObject, top);

	/* now bottom != null and top != null */
	if (bottom.getTypeCode() == 9 && top.getTypeCode() == 9) {
            Type type = createRangeType(bottom.getElementType(), 
                                        top.getElementType());
            if (type == tError)
                return tError;
	    return tArray(type);
        }

        if (bottom.getTypeCode() == 10)
            bottom = new ClassInterfacesType(bottom);

        if (top.getTypeCode() == 10)
            top = new ClassInterfacesType(top);

	if (bottom.getTypeCode() != 104 || top.getTypeCode() != 104)
	    return tError;

	return ClassInterfacesType.createRangeType
            ((ClassInterfacesType) bottom, (ClassInterfacesType) top);
    }

    public Type getElementType() {
	Type bottom = bottomType != null ? bottomType.getElementType() : null;
	Type top    =    topType != null ?    topType.getElementType() : null;
	return new ClassRangeType(bottom, top);
    }

    /**
     * Returns the specialized type of t1 and t2, e.g
     *  null   ,  xx     ->  xx
     *  tObject,  object ->  object
     *  int    , short   -> short
     *  tArray(tObject), tArray(tUnknown) -> tArray(tObject)
     *  tArray(tUnknown), tObject -> tArray(tUnknown)
     */
    public static Type getSpecializedType(Type t1, Type t2) {
	if (t1 == null || t2 == tError)
	    return t2;
	if (t2 == null || t1 == tError)
	    return t1;

	if (t1.getTypeCode() == 103) {
	    t1 = ((ClassRangeType)t1).bottomType;
	    if (t1 == null)
		return t2;
	}
	if (t2.getTypeCode() == 103) {
	    t2 = ((ClassRangeType)t2).bottomType;
	    if (t2 == null)
		return t1;
	}

	if (t1 == t2)
	    return t1;

        if (t1.getTypeCode() <= 4 && t2.getTypeCode() <= 4) {
	    if (t1.getTypeCode() < t2.getTypeCode())
		return t1;
	    else
		return t2;
	}

	if ((t1.getTypeCode() != 9 
             && t1.getTypeCode() != 10 
             && t1.getTypeCode() != 104) 
            || (t2.getTypeCode() != 9 
                && t2.getTypeCode() != 10
                && t2.getTypeCode() != 104))
	    return tError;
        
	if (t1 == MyType.tObject)
	    return t2;
	if (t2 == MyType.tObject)
	    return t1;

	if (t1.getTypeCode() == 9 && t2.getTypeCode() == 9) 
	    return tArray(getSpecializedType(t1.getElementType(),
					     t2.getElementType()));

        if (t1.getTypeCode() == 10)
            t1 = new ClassInterfacesType(t1);

        if (t2.getTypeCode() == 10)
            t2 = new ClassInterfacesType(t2);

	if (t1.getTypeCode() != 104 || t2.getTypeCode() != 104)
	    return tError;

	return ClassInterfacesType.getSpecializedType
            ((ClassInterfacesType) t1, (ClassInterfacesType) t2);
    }

    /**
     * Returns the generalized type of t1 and t2, e.g
     *  tObject, tString -> tObject
     *  object , interface -> object
     *       since a sub class of object may implement interface
     *  int    , short   -> int
     *  tArray(tObject), tArray(tUnknown) -> tArray(tUnknown)
     *  tArray(tUnknown), tObject -> tObject
     *  tUnknown, tString -> tString !!
     *  null    , tString -> tString !!
     */
    public static Type getGeneralizedType(Type t1, Type t2) {
	if (t1 != null && t1.getTypeCode() == 103) 
	    t1 = ((ClassRangeType)t1).topType;
	if (t2 != null && t2.getTypeCode() == 103) 
	    t2 = ((ClassRangeType)t2).topType;

	if (t1 == t2 || 
	    t1 == tError || t2 == null)
	    return t1;
	if (t2 == tError || t1 == null)
	    return t2;

        if (t1.getTypeCode() <= 4 && t2.getTypeCode() <= 4) {
	    if (t1.getTypeCode() < t2.getTypeCode())
		return t1;
	    else
		return t2;
	}
	if ((t1.getTypeCode() != 9 
             && t1.getTypeCode() != 10 
             && t1.getTypeCode() != 104) 
            || (t2.getTypeCode() != 9 
                && t2.getTypeCode() != 10
                && t2.getTypeCode() != 104))
	    return tError;
        
	if (t1 == MyType.tObject)
	    return t1;
	if (t2 == MyType.tObject)
	    return t2;

	if (t1.getTypeCode() == 9 && t2.getTypeCode() == 9) {
            Type type = getGeneralizedType(t1.getElementType(),
                                           t2.getElementType());
            if (type == null)
                return null;
	    return tArray(type);
        }

        if (t1.getTypeCode() == 10)
            t1 = new ClassInterfacesType(t1);

        if (t2.getTypeCode() == 10)
            t2 = new ClassInterfacesType(t2);

	if (t1.getTypeCode() != 104 || t2.getTypeCode() != 104)
	    return tError;

	return ClassInterfacesType.getGeneralizedType
            ((ClassInterfacesType) t1, (ClassInterfacesType) t2);
    }
	    
    public Type getIntersection(ClassRangeType type)
    {
	Type bottom = getSpecializedType(bottomType, type.bottomType);
	Type top    = getGeneralizedType(topType, type.topType);

	Type newType = createRangeType(bottom,top);
        if (newType == tError) {
            boolean oldTypeDebugging = Decompiler.isTypeDebugging;
            Decompiler.isTypeDebugging = true;
            System.err.println("intersecting "+ this +" and "+ type + 
                               " to <" + bottom + "-" + top + 
                               "> to <error>");
            Decompiler.isTypeDebugging = oldTypeDebugging;
            Thread.dumpStack();
        } else if (Decompiler.isTypeDebugging) {
            System.err.println("intersecting "+ this +" and "+ type + 
                               " to <" + bottom + "-" + top + 
                               "> to " + newType);
	}	    
        return newType;
    }

    public boolean intersects(ClassRangeType type)
    {
	return getIntersection(type) != tError;
    }

    public String typeString(String string, boolean flag1, boolean flag2)
    {
	if (Decompiler.isTypeDebugging)
	    return "<"+bottomType+"-"+topType+">" + string;
        else if (topType != null)
            return topType.typeString(string, flag1, flag2);
        else if (bottomType != null)
            /* This means, that the local variable is never assigned to.
             * If bottom type is a ClassRangeType, there may be problems,
             * that are ignored.  They produce compiler errors.
             */
            return bottomType.typeString(string, flag1, flag2);
        else
            return "<Unknown>"+string;
    }
}
