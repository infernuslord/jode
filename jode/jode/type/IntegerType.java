/* 
 * IntegerType (c) 1998 Jochen Hoenicke
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

/**
 * This is a type class for 16 bit integral types.  There are seven
 * different types, namely <code>int, char, short, byte, boolean,
 * const short, const byte</code> abbreviated <code>I, C, S, B, Z, cS,
 * cB</code>.  <code>cB</code> and <code>cS</code> specify constant
 * ints whose value is in byte resp. short range.  They may be
 * converted to B resp. S, but sometimes need an explicit cast.
 *
 * @author Jochen Hoenicke */
public class IntegerType extends Type {

    public static final int IT_I  = 0x01;
    public static final int IT_C  = 0x02;
    public static final int IT_S  = 0x04;
    public static final int IT_B  = 0x08;
    public static final int IT_Z  = 0x10;
    public static final int IT_cS = 0x20;
    public static final int IT_cB = 0x40;
    private static final int NUM_TYPES = 7;

    private static final int[] subTypes = { 
	/*I*/ IT_I|IT_C|IT_S|IT_B/*|IT_cS|IT_cB*/, /*C*/ IT_C, 
	/*S*/ IT_S|IT_B/*|IT_cS|IT_cB*/, /*B*/ IT_B/*|IT_cB*/, 
	/*Z*/ IT_Z, 
	/*cS*/IT_cS|IT_cB, /*cB*/IT_cB
    };
    private static final int[] superTypes = { 
	/*I*/ IT_I, /*C*/ IT_I|IT_C, 
	/*S*/ IT_I|IT_S, /*B*/ IT_I|IT_S|IT_B, 
	/*Z*/ IT_Z,
	/*cS*/IT_I|IT_C|IT_S|IT_cS, /*cB*/IT_I|IT_C|IT_S|IT_B|IT_cS|IT_cB
    };
    private static final Type[] simpleTypes = {
	new IntegerType(IT_I), new IntegerType(IT_C), 
	new IntegerType(IT_S), new IntegerType(IT_B), 
	new IntegerType(IT_Z),
	new IntegerType(IT_cS), new IntegerType(IT_cB)
    };
    private static final String[] typeNames = {
	"I","C","S","B","Z","cS","cB"
    };
	

    int possTypes;
    int strongHint = 0;
    int weakHint = 0;

    /**
     * Create a new type with the given type.
     */
    public IntegerType(int types) {
	super(TC_INTEGER);
	possTypes = types;
    }

    public Type getHint() {
	int hint = ((possTypes & IT_Z) != 0 ? IT_Z
		    : strongHint != 0 ? strongHint
		    : weakHint != 0 ? weakHint
		    : possTypes);
	int i = 0;
	for (int it = 0x1; (it & hint) == 0; it <<= 1)
	    i++;
	return simpleTypes[i];
    }

    private int getSubTypes() {
	int result = 0;
	for (int i=0; i < NUM_TYPES; i++) {
	    if (((1<<i) & possTypes) != 0)
		result |= subTypes[i];
	}
	return result;
    }
    private int getSuperTypes() {
	int result = 0;
	for (int i=0; i < NUM_TYPES; i++) {
	    if (((1<<i) & possTypes) != 0)
		result |= superTypes[i];
	}
	return result;
    }

    public Type getSubType() {
	return new IntegerType(getSubTypes());
    }

    public Type getSuperType() {
	return new IntegerType(getSuperTypes());
    }

//      /**
//       * Returns the common sub type of this and type.
//       * @param type the other type.
//       * @return the common sub type.
//       */
//      public Type getSpecializedType(Type type) {
//          /*  tError  , x        -> tError
//           *  x       , tUnknown -> x
//           *  Integer , Integer  -> possTypes1 & possTypes2 == 0 ? tError
//           */
//  	if (type == tUnknown)
//  	    return this;
//  	if (type.typecode != TC_INTEGER)
//  	    return tError;
//  	IntegerType other = (IntegerType) type;
//  	int mergeTypes = possTypes & other.possTypes;
//  	/* HINTING XXX */
//  	if (mergeTypes == possTypes)
//  	    return this;
//  	if (mergeTypes == other.possTypes)
//  	    return other;
//  	if (mergeTypes == 0)
//  	    return tError;
//  	return new IntegerType(mergeTypes);
//      }

//      /**
//       * Returns the common super type of this and type.
//       * @param type the other type.
//       * @return the common super type.
//       */
//      public Type getGeneralizedType(Type type) {
//  	return getSpecializedType(type);
//      }

//      /**
//       * Create the type corresponding to the range from bottomType to this.
//       * @param bottomType the start point of the range
//       * @return the range type, or tError if not possible.
//       */
//      public Type intersection(Type bottomType) {
//          /* Note that this can't be tBoolByte or tBoolInt */
//          /*  x       , tError   -> tError
//           *  object  , tUnknown -> <object, tUnknown>
//           *  boolean , tUnknown -> boolean
//           *  int     , tUnknown -> <int, byte>
//           *  boolint , tUnknown -> boolint
//           *  tUnknown, x        -> getSuperTypes(x)
//           *  tUnknown, short    -> <int, short>
//           *  short   , byte     -> <short, byte>
//           *  byte    , short    -> tError
//           *  tUnknown, float    -> float
//           */
//  	if (bottomType == tUnknown)
//  	    return this;
//  	else if (bottomType.typecode != TC_INTEGER)
//  	    return tError;
//  	/*XXX HINTING*/

//  	int result = ((IntegerType)bottomType).possTypes & possTypes;
//  	return new IntegerType(result);
//      }

    /**
     * Checks if this type represents a valid type instead of a list
     * of minimum types.
     */
    public boolean isValidType() {
	return true;
    }

    /**
     * Check if this and &lt;unknown -- type&rt; are not disjunct.
     * @param type  a simple type; this mustn't be a range type.
     * @return true if this is the case.
     */
    public boolean isOfType(Type type) {
	return (type.typecode == TC_INTEGER
		&& (((IntegerType)type).possTypes & possTypes) != 0);
    }

    public String getDefaultName() {
        switch (((IntegerType)getHint()).possTypes) {
        case IT_Z:
            return "bool";
        case IT_C:
            return "c";
        case IT_B:
        case IT_S:
        case IT_I:
            return "i";
        default:
	    throw new jode.AssertError("Local can't be of constant type!");
        }
    }

    public String getTypeSignature() {
        switch (((IntegerType)getHint()).possTypes) {
        case IT_Z:
            return "Z";
        case IT_C:
            return "C";
        case IT_B:
            return "B";
        case IT_S:
            return "S";
        case IT_I:
        default:
            return "I";
        }
    }
    
    public String toString() {
        switch (possTypes) {
        case IT_Z:
            return "boolean";
        case IT_C:
            return "char";
        case IT_B:
            return "byte";
        case IT_S:
            return "short";
        case IT_I:
            return "int";
	default:
	    StringBuffer sb = new StringBuffer("{");
	    String comma = "";
	    for (int i=0; i< NUM_TYPES; i++)
		if (((1<<i) & possTypes) != 0) {
		    sb.append(comma).append(typeNames[i]);
		    comma = ",";
		}
	    sb.append("}");
	    return sb.toString();
        }
    }

    /**
     * Intersect this type with another type and return the new type.
     * @param type the other type.
     * @return the intersection, or tError, if a type conflict happens.
     */
    public Type intersection(Type type) {
	if (type == tError)
	    return type;
	if (type == tUnknown)
	    return this;

	int mergeTypes;
	if (type.typecode != TC_INTEGER)
	    mergeTypes = 0;
	else {
	    IntegerType other = (IntegerType) type;
	    mergeTypes = possTypes & other.possTypes;
	    /* HINTING XXX */
	    if (mergeTypes == possTypes)
		return this;
	    if (mergeTypes == other.possTypes)
		return other;
	}
	Type result = mergeTypes == 0 ? tError : new IntegerType(mergeTypes);
	if (Decompiler.isTypeDebugging || result == tError) {
	    Decompiler.err.println("intersecting "+ this +" and "+ type + 
                                   " to " + result);
	}	    
	return result;
    }

    public boolean equals(Object o) {
        if (o == this) 
            return true;
        if (o instanceof IntegerType)
	    return ((IntegerType)o).possTypes == possTypes;
        return false;
    }
}
