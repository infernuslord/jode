/* IntegerType Copyright (C) 1999 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
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

    public Object getDefaultValue() {
	return new Integer(0);
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
    
    public Class getTypeClass() {
        switch (((IntegerType)getHint()).possTypes) {
        case IT_Z:
	    return Boolean.TYPE;
        case IT_C:
	    return Character.TYPE;
        case IT_B:
	    return Byte.TYPE;
        case IT_S:
	    return Short.TYPE;
        case IT_I:
        default:
	    return Integer.TYPE;
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
	if (Decompiler.isTypeDebugging) {
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
