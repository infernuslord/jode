/* 
 * Type (c) 1998 Jochen Hoenicke
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
 * This is my type class.  It differs from java.lang.class, in
 * that it maintains a type range.  This type range may be implicit or
 * explicit. <br>
 *
 *
 * Think of this global type hierarchie:
 * <pre>
 *
 *          tUnknown
 *          /   |   \
 *         /    |    \
 *  tObject  boolean  int
 *    /  \            /  \
 *   /  tArray     short char
 * other             | 
 * classes          byte 
 * </pre>
 *
 * int implements the "interface" tBoolInt.  boolean and byte
 * implement the "interface" tBoolByte which extends tBoolInt. <br>
 *
 * The type tBoolInt is <tBoolInt, tUnknown>, the type tBoolByte is
 * <tBoolByte, tUnknown> (hard coded in <code>getTop</code>).  The
 * type tUInt is <int, byte>. <br>
 *
 * Note that tUnknown is no valid type, so we can replace
 * <tUnknown,  byte>  with <int, byte>
 * <tUnknown, tArray> with <tObject, tArray>, <br>
 *
 * Arrays extend Object and implement java.lang.Cloneable and 
 * java.io.Serializable, as defined in jls.
 *
 * The main operation on a type range is the intersection.  To do this
 * on class ranges we need three more operations: specialization,
 * generalization and createRange. <p>
 *
 * specialization chooses the common sub type of the two types.  It
 * is used to find the start point of the intersected interval. <p>
 *
 * generalization chooses the common super type of two types. It
 * is used to find the end point of the intersected interval. <p>
 *
 * When the new interval is created with <code>createRangeType</code>
 * the start and end points are adjusted so that they only consists of
 * possible types.
 *
 * @author Jochen Hoenicke */
public class Type {

    public static final int TC_BOOLEAN = 0;
    public static final int TC_BYTE = 1;
    public static final int TC_CHAR = 2;
    public static final int TC_SHORT = 3;
    public static final int TC_INT = 4;
    public static final int TC_LONG = 5;
    public static final int TC_FLOAT = 6;
    public static final int TC_DOUBLE = 7;
    public static final int TC_NULL = 8;
    public static final int TC_ARRAY = 9;
    public static final int TC_CLASS = 10;
    public static final int TC_VOID = 11;
    public static final int TC_METHOD = 12;
    public static final int TC_ERROR = 13;
    public static final int TC_UNKNOWN = 101;
    public static final int TC_RANGE = 103;
    public static final int TC_INTEGER = 107;

    protected static JodeEnvironment env;

    public static final Hashtable classHash = new Hashtable();
    public static final Hashtable arrayHash = new Hashtable();    

    public static final Type tBoolean = new IntegerType(IntegerType.IT_Z);
    public static final Type tByte    = new IntegerType(IntegerType.IT_B);
    public static final Type tChar    = new IntegerType(IntegerType.IT_C);
    public static final Type tShort   = new IntegerType(IntegerType.IT_S);
    public static final Type tInt     = new IntegerType(IntegerType.IT_I);
    public static final Type tLong    = new Type(TC_LONG);
    public static final Type tFloat   = new Type(TC_FLOAT);
    public static final Type tDouble  = new Type(TC_DOUBLE);
    public static final Type tVoid    = new Type(TC_VOID);
    public static final Type tError   = new Type(TC_ERROR);
    public static final Type tUnknown = new Type(TC_UNKNOWN);
    public static final Type tUInt    = new IntegerType(IntegerType.IT_I
							| IntegerType.IT_B
							| IntegerType.IT_C
							| IntegerType.IT_S);
    public static final Type tBoolInt = new IntegerType(IntegerType.IT_I
							| IntegerType.IT_Z);
    public static final Type tBoolUInt= new IntegerType(IntegerType.IT_I
							| IntegerType.IT_B
							| IntegerType.IT_C
							| IntegerType.IT_S
							| IntegerType.IT_Z);
    public static final Type tBoolByte= new IntegerType(IntegerType.IT_B
							| IntegerType.IT_Z);
    public static final ClassInterfacesType tObject = tClass("java.lang.Object");
    public static final ClassInterfacesType tNull   = new NullType();
    public static final Type tUObject = tRange(tObject, tNull);
    public static final Type tString  = tClass("java.lang.String");
    public static final Type tStringBuffer = tClass("java.lang.StringBuffer");
    public static final Type tJavaLangClass = tClass("java.lang.Class");

    public static final Type tType(String type) {
        if (type == null || type.length() == 0)
            return tError;
        switch(type.charAt(0)) {
        case 'Z':
            return tBoolean;
        case 'B':
            return tByte;
        case 'C':
            return tChar;
        case 'S':
            return tShort;
        case 'I':
            return tInt;
        case 'F':
            return tFloat;
        case 'J':
            return tLong;
        case 'D':
            return tDouble;
        case 'V':
            return tVoid;
        case '[':
            return tArray(tType(type.substring(1)));
        case 'L':
            int index = type.indexOf(';');
            if (index != type.length()-1)
                return tError;
            return tClass(type.substring(1, index));
        }
        throw new AssertError("Unknown type signature: "+type);

    }

    public static final ClassInterfacesType tClass(String clazzname) {
        clazzname = clazzname.replace('/', '.');
        Object result = classHash.get(clazzname);
        if (result == null) {
            result = new ClassInterfacesType(clazzname);
            classHash.put(clazzname, result);
        }
        return (ClassInterfacesType) result;
    }

    public static final Type tArray(Type type) {
        if (type == tError) 
            return type;
        Type result = (Type) arrayHash.get(type);
        if (result == null) {
            result = new ArrayType(type);
            arrayHash.put(type, result);
        }
        return result;
    }

    public static final Type tRange(ClassInterfacesType bottom, 
				    ClassInterfacesType top) {
        return new RangeType(bottom, top);
    }

    public static Type tSuperType(Type type) {
        return type.getSuperType();
    }

    public static Type tSubType(Type type) {
        return type.getSubType();
    }
	
    public static Type tClassOrArray(String ident) {
        if (ident.charAt(0) == '[')
            return Type.tType(ident);
        else
            return Type.tClass(ident);
    }

    public static void setEnvironment(JodeEnvironment e) {
	env = e;
    }

    int typecode;

    /**
     * Create a new type with the given type code.
     */
    protected Type(int typecode) {
        this.typecode = typecode;
    }

    public Type getSubType() {
        return this;
    }

    public Type getSuperType() {
        return this;
    }

//      public Type getBottom() {
//          return this;
//      }

//      public Type getTop() {
//          return this;
//      }

    public Type getHint() {
	return this;
    }

    /**
     * @return the type code of the type.
     */
    public final int getTypeCode() {
        return typecode;
    }

    public int stackSize()
    {
        switch(typecode) {
        case TC_VOID:
        case TC_ERROR:
            return 0;
        default:
            return 1;
        case TC_DOUBLE:
        case TC_LONG:
            return 2;
        }
    }

//      /**
//       * Returns the common sub type of this and type.
//       * @param type the other type.
//       * @return the common sub type.
//       */
//      public Type getSpecializedType(Type type) {
//          /*  tError  , x       -> tError
//           *  tUnknown, x       -> x
//           *  x       , x       -> x
//           */
//          return (this == tError || type == tError) ? tError
//              :  (this == type || type == tUnknown) ? this
//              :  (this == tUnknown)                 ? type
//              : tError;
//      }

//      /**
//       * Returns the common super type of this and type.
//       * @param type the other type.
//       * @return the common super type.
//       */
//      public Type getGeneralizedType(Type type) {
//          /* Note that type can't be boolint/boolbyte  (set getBottom) */
//          /*  tError  , x        -> tError
//           *  tUnknown, x        -> x
//           *  x       , x        -> x
//           *  byte    , short    -> short
//           */

//          return (this == tError || type == tError) ? tError
//              :  (this == type || type == tUnknown) ? this
//              :  (this == tUnknown)                 ? type
//              :     tError;
//      }

//      /**
//       * Create the type corresponding to the range from bottomType to this.
//       * @param bottomType the start point of the range
//       * @return the range type, or tError if not possible.
//       */
//      public Type createRangeType(Type bottomType) {
//          /* Note that this can't be tBoolByte or tBoolInt */
//          /*  x       , tError   -> tError
//           *  x       , x        -> x
//           *  tUnknown, x        -> x
//           *  object  , tUnknown -> <object, tUnknown>
//           *  int     , tUnknown -> int
//  	 *  x       , tUnknown -> x
//           */

//          return (this == tError || bottomType == tError) ? tError
//              :  (this == bottomType)                     ? this
//              :  (bottomType == tUnknown)                 ? this
//              :  (this == tUnknown) 
//              ?  ((bottomType.typecode == TC_ARRAY
//  		 || bottomType.typecode == TC_CLASS)
//  		? tRange(bottomType, this) 
//  		: bottomType)
//  	    :      tError;
//      }

    /**
     * Intersect this type with another type and return the new type.
     * @param type the other type.
     * @return the intersection, or tError, if a type conflict happens.
     */
    public Type intersection(Type type) {
	if (this == tError || type == tError)
	    return tError;
	if (this == tUnknown)
	    return type;
	if (type == tUnknown || this == type)
	    return this;
	Decompiler.err.println("intersecting "+ this +" and "+ type
			       + " to <error>");
	return tError;
    }

    /**
     * Checks if we need to cast to a middle type, before we can cast from
     * fromType to this type.
     * @return the middle type, or null if it is not necessary.
     */
    public Type getCastHelper(Type fromType) {
	return null;
    }

    /**
     * Checks if this type represents a valid type instead of a list
     * of minimum types.
     */
    public boolean isValidType() {
	return typecode <= TC_DOUBLE;
    }

    /**
     * Checks if this type represents a class or an array of a class
     */
    public boolean isClassType() {
        return false;
    }

    /**
     * Check if this and &lt;unknown -- type&rt; are not disjunct.
     * @param type  a simple type; this mustn't be a range type.
     * @return true if this is the case.
     */
    public boolean isOfType(Type type) {
	return (this == tUnknown || (this == type && this != tError));
    }

    /**
     * Marks this type as used, so that the class is imported.
     */
    public void useType() {
        /* No action needed for simple types */
    }

    public String getDefaultName() {
        switch (typecode) {
        case TC_LONG:
            return "l";
        case TC_FLOAT:
            return "f";
        case TC_DOUBLE:
            return "d";
        default:
            return "local";
        }
    }

    public String getTypeSignature() {
        switch (typecode) {
        case TC_LONG:
            return "J";
        case TC_FLOAT:
            return "F";
        case TC_DOUBLE:
            return "D";
        default:
            return "?";
        }
    }
    
    public String toString() {
        switch (typecode) {
        case TC_LONG:
            return "long";
        case TC_FLOAT:
            return "float";
        case TC_DOUBLE:
            return "double";
        case TC_NULL:
            return "null";
        case TC_VOID:
            return "void";
        case TC_UNKNOWN:
            return "<unknown>";
        case TC_ERROR:
        default:
            return "<error>";
        }
    }
}
