package jode;
import sun.tools.java.Constants;
import sun.tools.java.Type;
import sun.tools.java.Identifier;
import java.util.Hashtable;

/**
 * This is my own type class.  It differs from sun.tools.java.Type, in
 * that it maintains a type range.  This type range may be implicit or
 * explicit. <p>
 *
 * The type tInt (which is the same as Type.tInt) is a good example
 * for the implicit range &lt;tInt -- tBoolean&gt;.  Most other
 * standard types stand for the range consisting only of themselve.
 * The explicit form is the class range type <p>
 *
 * The main operation on a type range is the intersection.  To do this
 * on class ranges we need two more operations: specialization and
 * generalization. <p>
 *
 * specialization chooses the startpoint of two intervals which
 * lies in the open range &lt;sp -- null&gt;, where sp is the startpoint
 * of the other interval,  or returns tError on failure.<p>
 *
 * generalization chooses the endpoint of two intervals which lies in
 * the open range &lt;null -- ep&gt;, where ep is the endpoint of
 * the other interval, or returns tError on failure.<p>
 */
public class MyType extends Type {
    static Hashtable superclasses = new Hashtable();

    protected static JodeEnvironment env;

    public static final Type tStringBuffer = 
	Type.tClass(idJavaLangStringBuffer);
    public static final Type tUnknown = new ClassRangeType(null, null);
    public static final Type tUInt    = tInt;
    public static final Type tUIndex  = tInt;
    public static final Type tUObject = new ClassRangeType(tObject, null);

    public static Type tSuperType(Type type) {
	int typeCode = type.getTypeCode();
	if (typeCode == 9 || typeCode == 10 || typeCode == 103) 
	    return new ClassRangeType(tObject, type);
	else
	    return type;
    }

    public static Type tSubType(Type type) {
	int typeCode = type.getTypeCode();
	if (typeCode == 9 || typeCode == 10 || typeCode == 103) 
	    return new ClassRangeType(type, null);
	else
	    return type;
    }
	
    public static Type tClassOrArray(Identifier ident) {
        if (ident.toString().charAt(0) == '[')
            return MyType.tType(ident.toString());
        else
            return MyType.tClass(ident);
    }

    public static void setEnvironment(JodeEnvironment e) {
	env = e;
    }

    protected MyType(int i, String str) {
        super (i, str);
    }

    public int stackSize()
    {
        return 1;
    }

    public String typeString(String var, boolean flag1, boolean flag2)
    {
        String typeStr;
        switch (typeCode) {
        case 100: typeStr="unknown"; break;
        default:
            throw new RuntimeException("Wrong typeCode "+typeCode);
        }
        if (var.length() > 0)
            return typeStr+" "+var;
        return typeStr;
    }

    /**
     * Find the intersection of two types
     * @param t1 the first type.
     * @param t2 the second type.
     * @return the intersection, or tError, if a type conflict happens.
     */
    public static Type intersection(Type t1, Type t2) {
// 	System.err.println("intersecting "+ t1 +" and "+ t2);
	/* Trivial cases first.
	 */
	if (t1 == t2 || t2 == tUnknown)
	    return t1;
	if (t1 == tUnknown)
	    return t2;

	/* This is the integer case
	 * tBoolean = 0 ,..., tInt = 4
	 * return the smaller type code.
	 */
        if (t1.getTypeCode() <= 4 && t2.getTypeCode() <= 4) {
	    if (t1.getTypeCode() < t2.getTypeCode())
		return t1;
	    else
		return t2;
	}

	/* If this is an array or a class convert to class range.
	 */
	if (t1.getTypeCode() == 9 || t1.getTypeCode() == 10)
	    t1 = new ClassRangeType(t1,t1);

	if (t2.getTypeCode() == 9 || t2.getTypeCode() == 10)
	    t2 = new ClassRangeType(t2,t2);

	/* Now it must be a class range type, or we have lost!
	 */
	if (t1.getTypeCode() != 103 || t2.getTypeCode() != 103) {
            System.err.println("intersecting "+ t1 +" and "+ t2 + 
                               " to <error>");
	    return tError;
        }

	return ((ClassRangeType)t1).getIntersection((ClassRangeType)t2);
    }


    /**
     * @deprecated renamed to intersection.
     */
    public static Type commonType(Type t1, Type t2) {
	return intersection(t1, t2);
    }

    /**
     * Check if t1 and &lt;unknown -- t2&rt; are not disjunct.
     * @param t1  the type that should be checked
     * @param t2  a simple type; this mustn't be a range type.
     * @return true if this is the case.
     */
    public static boolean isOfType(Type t1, Type t2) {
        return (ClassRangeType.getGeneralizedType(t1,t2) == t2 &&
                ClassRangeType.getSpecializedType(t1,t2) == t2);
    }
}
