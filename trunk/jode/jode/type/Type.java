/* Type Copyright (C) 1998-1999 Jochen Hoenicke.
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

package jode.type;
import jode.AssertError;
import jode.GlobalOptions;
import jode.bytecode.ClassInfo;
///#ifdef JDK12
///import java.lang.ref.WeakReference;
///import java.lang.ref.ReferenceQueue;
///import java.util.Map;
///import java.util.HashMap;
///#else
import java.util.Hashtable;
///#endif

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

///#ifdef JDK12
///    private static final Map classHash = new HashMap();
///    private static final ReferenceQueue classQueue = new ReferenceQueue();
///    private static final Map arrayHash = new HashMap();    
///    private static final ReferenceQueue arrayQueue = new ReferenceQueue();
///    private static final Map methodHash = new HashMap();    
///    private static final ReferenceQueue methodQueue = new ReferenceQueue();
///#else
    private static final Hashtable classHash = new Hashtable();
    private static final Hashtable arrayHash = new Hashtable();    
    private static final Hashtable methodHash = new Hashtable();    
///#endif

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
    public static final ClassInterfacesType tObject = 
	tClass("java.lang.Object");
    public static final ReferenceType tNull = new NullType();
    public static final Type tUObject = tRange(tObject, tNull);
    public static final Type tString  = tClass("java.lang.String");
    public static final Type tStringBuffer = tClass("java.lang.StringBuffer");
    public static final Type tJavaLangClass = tClass("java.lang.Class");

    private static final StringBuffer appendSignature(StringBuffer sb,
						      Class javaType) {
	if (javaType.isPrimitive()) {
	    if (javaType == Boolean.TYPE)
		return sb.append('Z');
	    else if (javaType == Byte.TYPE)
		return sb.append('B');
	    else if (javaType == Character.TYPE)
		return sb.append('C');
	    else if (javaType == Short.TYPE)
		return sb.append('S');
	    else if (javaType == Integer.TYPE)
		return sb.append('I');
	    else if (javaType == Long.TYPE)
		return sb.append('J');
	    else if (javaType == Float.TYPE)
		return sb.append('F');
	    else if (javaType == Double.TYPE)
		return sb.append('D');
	    else if (javaType == Void.TYPE)
		return sb.append('V');
	    else
		throw new AssertError("Unknown primitive type: "+javaType);
	} else if (javaType.isArray()) {
	    return appendSignature(sb.append('['), 
				   javaType.getComponentType());
	} else {
	    return sb.append('L')
		.append(javaType.getName().replace('.','/')).append(';');
	}
    }

    public static String getSignature(Class clazz) {
	return appendSignature(new StringBuffer(), clazz).toString();
    }

    public static String getSignature(Class paramT[], Class returnT) {
	StringBuffer sig = new StringBuffer("(");
	for (int i=0; i< paramT.length; i++)
	    appendSignature(sig, paramT[i]);
	return appendSignature(sig.append(')'), returnT).toString();
    }

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
	case '(':
	    return tMethod(type);
        }
        throw new AssertError("Unknown type signature: "+type);
    }

    public static final Type tType(Class javaType) {
	return Type.tType(getSignature(javaType));
    }

    public static final ClassInterfacesType tClass(String clazzname) {
	return tClass(ClassInfo.forName(clazzname.replace('/','.')));
    }

    public static final ClassInterfacesType tClass(ClassInfo clazzinfo) {
///#ifdef JDK12
///	java.lang.ref.Reference died;
///	while ((died = classQueue.poll()) != null)
///	    classHash.values().remove(died);
///	WeakReference ref = (WeakReference) classHash.get(clazzinfo);
///        Object result = (ref == null) ? null : ref.get();
///#else
        Object result = classHash.get(clazzinfo);
///#endif
        if (result == null) {
            result = new ClassInterfacesType(clazzinfo);
///#ifdef JDK12
///            classHash.put(clazzinfo, new WeakReference(result, classQueue));
///#else
            classHash.put(clazzinfo, result);
///#endif
        }
        return (ClassInterfacesType) result;
    }

    public static final Type tArray(Type type) {
        if (type == tError) 
            return type;
///#ifdef JDK12
///	java.lang.ref.Reference died;
///	while ((died = arrayQueue.poll()) != null)
///	    arrayHash.values().remove(died);
///	WeakReference ref = (WeakReference) arrayHash.get(type);
///        Type result = (ref == null) ? null : (Type) ref.get();
///#else
        Type result = (Type) arrayHash.get(type);
///#endif
        if (result == null) {
            result = new ArrayType(type);
///#ifdef JDK12
///            arrayHash.put(type, new WeakReference(result, arrayQueue));
///#else
            arrayHash.put(type, result);
///#endif
        }
        return result;
    }

    public static MethodType tMethod(String signature) {
///#ifdef JDK12
///	java.lang.ref.Reference died;
///	while ((died = methodQueue.poll()) != null)
///	    methodHash.values().remove(died);
///	WeakReference ref = (WeakReference) methodHash.get(signature);
///        MethodType result = (ref == null) ? null : (MethodType) ref.get();
///#else
	MethodType result = (MethodType) methodHash.get(signature);
///#endif
	if (result == null) {
	    result = new MethodType(signature);
///#ifdef JDK12
///	    methodHash.put(signature, new WeakReference(result, methodQueue));
///#else
            methodHash.put(signature, result);
///#endif
        }
        return result;
    }
    public static MethodType tMethod(Class paramT[], Class returnT) {
	return tMethod(getSignature(paramT, returnT));
    }

    public static final Type tRange(ReferenceType bottom, 
				    ReferenceType top) {
        return new RangeType(bottom, top);
    }

    public static Type tSuperType(Type type) {
        return type.getSuperType();
    }

    public static Type tSubType(Type type) {
        return type.getSubType();
    }
	
    final int typecode;

    /**
     * Create a new type with the given type code.
     */
    protected Type(int tc) {
        typecode = tc;
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
            return 0;
        case TC_ERROR:
        default:
            return 1;
        case TC_DOUBLE:
        case TC_LONG:
            return 2;
        }
    }

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
	if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_TYPES) != 0)
	    GlobalOptions.err.println("intersecting "+ this +" and "+ type
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
	return this.intersection(type) != Type.tError;
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

    public Object getDefaultValue() {
        switch (typecode) {
        case TC_LONG:
            return new Long(0);
        case TC_FLOAT:
            return new Float(0);
        case TC_DOUBLE:
            return new Double(0);
        default:
            return null;
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

    public Class getTypeClass() throws ClassNotFoundException {
        switch (typecode) {
        case TC_LONG:
            return Long.TYPE;
        case TC_FLOAT:
            return Float.TYPE;
        case TC_DOUBLE:
            return Double.TYPE;
        default:
	    throw new AssertError("getTypeClass() called on illegal type");
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
