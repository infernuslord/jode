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
 * explicit. <p>
 *
 *
 * Think of this global type hierarchie:
 * <pre>
 *
 *          tUnknown
 *          /   |   \
 *         /    |    \
 *  tObject  boolean  int
 *    /  \             |
 *   /  tArray       short
 * other               |
 * classes            byte
 * </pre>
 *
 * int implements the "interface" tBoolByte.  boolean and byte
 * implement the "interface" tBoolByte which extends tBoolInt.
 *
 * The type tBoolInt is <tBoolInt, tUnknown>, the type tBoolByte is
 * <tBoolByte, tUnknown> (hard coded in <code>getTop</code>).  The
 * type tUInt is <int, byte>.
 *
 * Note that tUnknown is no valid type, so we can replace
 * <tUnknown,  byte>  with <int, byte>
 * <tUnknown, tArray> with <tObject, tArray>
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
    public static final int TC_BOOLBYTE = 105;
    public static final int TC_BOOLINT  = 106;

    protected static JodeEnvironment env;

    public static final Hashtable classHash = new Hashtable();
    public static final Hashtable arrayHash = new Hashtable();    

    public static final Type tBoolean = new Type(TC_BOOLEAN);
    public static final Type tByte    = new Type(TC_BYTE);
    public static final Type tChar    = new Type(TC_CHAR);
    public static final Type tShort   = new Type(TC_SHORT);
    public static final Type tInt     = new Type(TC_INT);
    public static final Type tLong    = new Type(TC_LONG);
    public static final Type tFloat   = new Type(TC_FLOAT);
    public static final Type tDouble  = new Type(TC_DOUBLE);
    public static final Type tVoid    = new Type(TC_VOID);
    public static final Type tError   = new Type(TC_ERROR);
    public static final Type tUnknown = new Type(TC_UNKNOWN);
    public static final Type tUInt    = tRange(tInt, tByte);
    public static final Type tBoolInt = new Type(TC_BOOLINT);
    public static final Type tBoolByte= new Type(TC_BOOLBYTE);
    public static final Type tObject  = tClass("java.lang.Object");
    public static final Type tUObject = tRange(tObject, tUnknown);
    public static final Type tString  = tClass("java.lang.String");
    public static final Type tStringBuffer = tClass("java.lang.StringBuffer");

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

    public static final Type tType(Class clazz) {
        if (clazz.isArray())
            return tArray(tType(clazz.getComponentType()));
        if (clazz.isPrimitive()) {
            return clazz == Boolean.TYPE   ? tBoolean 
                :  clazz == Byte.TYPE      ? tByte 
                :  clazz == Character.TYPE ? tChar 
                :  clazz == Short.TYPE     ? tShort 
                :  clazz == Integer.TYPE   ? tInt 
                :  clazz == Float.TYPE     ? tFloat 
                :  clazz == Long.TYPE      ? tLong 
                :  clazz == Double.TYPE    ? tDouble 
                :  clazz == Void.TYPE      ? tVoid 
                :  tError;
        }
        return new ClassInterfacesType(clazz);
    }

    public static final Type tClass(String clazzname) {
        clazzname = clazzname.replace(java.io.File.separatorChar, '.');
        Object result = classHash.get(clazzname);
        if (result == null) {
            result = new ClassInterfacesType(clazzname);
            classHash.put(clazzname, result);
        }
        return (Type) result;
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

    public static final Type tRange(Type bottom, Type top) {
        if (bottom.typecode == TC_RANGE
            || top.typecode == TC_RANGE)
            throw new AssertError("tRange("+bottom+","+top+")");
        return new RangeType(bottom, top);
    }

    public static Type tSuperType(Type type) {
        if (type.getTop() == tUnknown) {
            if (type == tBoolInt || type == tBoolByte)
                return tBoolInt;
            if (type.getBottom().typecode == TC_CLASS)
                return tUObject;
        }
        return type.getTop().createRangeType(tUnknown);
    }

    public static Type tSubType(Type type) {
        return tUnknown.createRangeType(type.getBottom());
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

    public Type getBottom() {
        return this;
    }

    public Type getTop() {
        return (this == tBoolByte || this == tBoolInt) ? tUnknown : this;
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

    /**
     * Returns the common sub type of this and type.
     * @param type the other type.
     * @return the common sub type.
     */
    public Type getSpecializedType(Type type) {
        /*  tError  , x       -> tError
         *  tUnknown, x       -> x
         *  x       , x       -> x
         *  boolean , boolint -> boolean
         *  byte    , boolint -> byte
         *  boolean , boolbyte -> boolean
         *  byte    , boolbyte -> byte
         *  short   , boolbyte -> byte
         *  int     , boolbyte -> byte
         *  byte    , short   -> byte
         *  boolint , short   -> short
         *  boolint , boolbyte -> boolbyte
         *  boolbyte, short   -> tError
         */
        return (this == tError || type == tError) ? tError
            :  (this == type || type == tUnknown) ? this
            :  (this == tUnknown)                 ? type

            :  (typecode == TC_BOOLEAN) ? 
            /* TC_BOOLEAN is only compatible to TC_BOOLINT / TC_BOOLBYTE */
            ((type == tBoolInt || type == tBoolByte) ? this : tError)

            :  (typecode <= TC_INT)
            /* TC_BYTE, ..., TC_INT are compatible to higher
             * types and TC_BOOLINT, TC_BYTE is compatible to TC_BOOLBYTE
             */
            ?   ((  type.typecode == TC_BOOLEAN) ? tError
                 : (type.typecode <= typecode)   ? type
                 : (type.typecode <= TC_INT
                    || type.typecode == TC_BOOLINT) ? this 
                 : (type == tBoolByte) ? tByte
                 : tError)

            :  (typecode == TC_BOOLINT)
            /* TC_BOOLEAN,...,TC_INT all implement TC_BOOLINT
             */
            ?   ( (type.typecode <= TC_INT 
                   || type.typecode == TC_BOOLBYTE) ? type : tError )

            :  (typecode == TC_BOOLBYTE)
            /* TC_BOOLEAN, TC_BYTE implement TC_BOOLBYTE;
             * TC_BYTE extend TC_SHORT, TC_INT.
             * TC_BOOLBYTE extends TC_BOOLINT.
             */
            ?   ( (type.typecode <= TC_BYTE) ? type
                  : (type.typecode <= TC_INT) ? tByte
                  : (type == tBoolInt) ? this : tError )

            : tError;
    }

    /**
     * Returns the common super type of this and type.
     * @param type the other type.
     * @return the common super type.
     */
    public Type getGeneralizedType(Type type) {
        /* Note that type can't be boolint/boolbyte  (set getBottom) */
        /*  tError  , x        -> tError
         *  tUnknown, x        -> x
         *  x       , x        -> x
         *  byte    , short    -> short
         */

        return (this == tError || type == tError) ? tError
            :  (this == type || type == tUnknown) ? this
            :  (this == tUnknown)                 ? type

            :  (typecode >= TC_BYTE && typecode <= TC_INT)
            /* TC_BYTE, ..., TC_INT are compatible to higher
             */
            ?     ((type.typecode < TC_BYTE) ? tError
                   : (type.typecode <= typecode) ? this
                   : (type.typecode <= TC_INT) ? type : tError)
            
            :     tError;
    }

    /**
     * Create the type corresponding to the range from bottomType to this.
     * @param bottomType the start point of the range
     * @return the range type, or tError if not possible.
     */
    public Type createRangeType(Type bottomType) {
        /* Note that this can't be tBoolByte or tBoolInt */
        /*  x       , tError   -> tError
         *  object  , tUnknown -> <object, tUnknown>
         *  boolean , tUnknown -> boolean
         *  int     , tUnknown -> <int, byte>
         *  boolint , tUnknown -> boolint
         *  x       , x        -> x
         *  tUnknown, boolean  -> boolean
         *  tUnknown, short    -> <int, short>
         *  short   , byte     -> <short, byte>
         *  byte    , short    -> tError
         *  tUnknown, float    -> float
         */

        return (this == tError || bottomType == tError) ? tError
            :  (this == bottomType)                     ? this

            :  (this == tUnknown) 
            ?      ((bottomType == tBoolInt
                     || bottomType == tBoolByte
                     || bottomType == tBoolean
                     || bottomType == tByte   ) ? bottomType
                    :(bottomType.typecode <= TC_INT) 
                    ?   tRange(bottomType, tByte)
                    :   tRange(bottomType, this))

            :  (this == tBoolean)
            ?      ((bottomType == tBoolInt 
                     || bottomType == tBoolByte
                     || bottomType == tUnknown) ? this : tError)
            
            :  (typecode <= TC_INT) 
            /*  tUnknown, short    -> <int, short>
             *  short   , byte     -> <short, byte>
             *  byte    , short    -> tError
             *  boolint , short    -> <int, short>
             *  boolbyte, byte     -> byte
             *  boolbyte, short    -> tError
             */
            ?      ((bottomType.typecode < typecode) 
                    ?    tError

                    : (bottomType.typecode <= TC_INT)
                    ?    tRange(bottomType, this)

                    : (bottomType.typecode == TC_BOOLBYTE
                       && this == tByte)
                    ?    tByte

                    : (bottomType.typecode == TC_BOOLINT
                       || bottomType.typecode == TC_UNKNOWN)
                    ?    (this == tInt) ? tInt : tRange(tInt, this)

                    :    tError)
            : (bottomType.typecode == TC_UNKNOWN) ? this
            :      tError;
    }

    /**
     * Intersect this type with another type and return the new type.
     * @param type the other type.
     * @return the intersection, or tError, if a type conflict happens.
     */
    public final Type intersection(Type type) {

        Type top = getTop().getGeneralizedType(type.getTop());
        Type bottom = getBottom().getSpecializedType(type.getBottom());
        Type result = top.createRangeType(bottom);

        if (result == tError) {
            boolean oldTypeDebugging = Decompiler.isTypeDebugging;
            Decompiler.isTypeDebugging = true;
            System.err.println("intersecting "+ this +" and "+ type
                               + " to <" + bottom + "," + top + ">"
                               + " to <error>");
            Decompiler.isTypeDebugging = oldTypeDebugging;
            if (oldTypeDebugging)
                throw new AssertError("type error");
        } else if (Decompiler.isTypeDebugging) {
            if (this.equals(type)) {
//                 System.err.println("intersecting identical: "+this);
//                 Thread.dumpStack();
            } else
                System.err.println("intersecting "+ this +" and "+ type + 
                                   " to " + result);

	}	    
        return result;
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
    public final boolean isOfType(Type type) {
        return (getTop().getGeneralizedType(type.getTop()).createRangeType
                (getBottom().getSpecializedType(type.getBottom())) != tError);
//         return (getSpecializedType(type).equals(type));
    }

    /**
     * Marks this type as used, so that the class is imported.
     */
    public void useType() {
        /* No action needed for simple types */
    }

    public String toString() {
        switch (typecode) {
        case TC_BOOLINT:
            if (Decompiler.isTypeDebugging)
                return "<bool or int>";
            /* fall through */
        case TC_BOOLBYTE:
            if (Decompiler.isTypeDebugging)
                return "<bool or byte>";
            /* fall through */
        case TC_BOOLEAN:
            return "boolean";
        case TC_BYTE:
            return "byte";
        case TC_CHAR:
            return "char";
        case TC_SHORT:
            return "short";
        case TC_INT:
            return "int";
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
