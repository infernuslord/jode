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

        if (top.getTypeCode() != 9 && top.getTypeCode() != 10)
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

	if (bottom.getTypeCode() != 10 || top.getTypeCode() != 10) 
	    return tError;

	if (bottom == top)
	    return bottom;
	
	ClassDeclaration c1 = new ClassDeclaration(bottom.getClassName());
	ClassDeclaration c2 = new ClassDeclaration(top.getClassName());
	
	try {
	    if (c1.getClassDefinition(env).superClassOf(env, c2) ||
 		c1.getClassDefinition(env).implementedBy(env, c2))
		return new ClassRangeType(bottom, top);
	} catch (ClassNotFound ex) {
	}
	return tError;
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

	if ((t1.getTypeCode() != 9 && t1.getTypeCode() != 10) ||
	    (t2.getTypeCode() != 9 && t2.getTypeCode() != 10))
	    return tError;

	if (t1 == MyType.tObject)
	    return t2;
	if (t2 == MyType.tObject)
	    return t1;

	if (t1.getTypeCode() == 9 && t2.getTypeCode() == 9) 
	    return tArray(getSpecializedType(t1.getElementType(),
					     t2.getElementType()));

	if (t1.getTypeCode() != 10 || t2.getTypeCode() != 10)
	    return tError;

	/* Now we have two classes or interfaces.  The result should
	 * be the object that is the the child of both objects resp
	 * implements both interfaces.  
	 *
	 * I currently only handle the simple case where one of the
	 * two objects implements the other or is a child of it.
	 *
	 * Forget the following setences, java tells us if the local
	 * is an interface or an object.
	 *
	 * There are really complicated cases that are currently
	 * ignored: imaging, c1 and c2 are both disjunct interfaces
	 * and there are some object which implements them both.
	 * There is no way for us to guess which.
	 *
	 * What can we do about this?  We probably need something more 
	 * powerful than a simple class range.
	 * But maybe this isn't needed at all.  How should someone
	 * use an object which implements two interfaces in a local 
	 * variable without casting?  The information which object 
	 * to use must be somewhere in the method.
	 *
	 * But think of this code fragment:
	 *
	 * class Foo implements i1, i2 { ... }
	 *
	 * class Bar {
	 *    Foo getFoo() { ... }
	 *    void someFunction() {
	 *        while ((Foo foo = getFoo()) != null) {
	 *           foo.interface1Method();
	 *           foo.interface2Method();
	 *        }
         *    }
	 * }
	 *
	 * Since the while condition is moved to the bottom of 
	 * the loop, the type information of foo is only available
	 * <em>after</em> the two interface methods are called.
	 * The current code would produce tError.  
         */
	
	ClassDeclaration c1 = new ClassDeclaration(t1.getClassName());
	ClassDeclaration c2 = new ClassDeclaration(t2.getClassName());
	
	try {
	    if (c1.getClassDefinition(env).superClassOf(env, c2))
		return t2;
	    if (c2.getClassDefinition(env).superClassOf(env, c1))
		return t1;
	    if (c1.getClassDefinition(env).implementedBy(env, c2))
		return t2;
	    if (c2.getClassDefinition(env).implementedBy(env, c1))
		return t1;
	} catch (ClassNotFound ex) {
	}
	return tError;
    }

    /**
     * Returns the generalized type of t1 and t2, e.g
     *  tObject, tString -> tObject
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
		return t2;
	    else
		return t1;
	}
	if ((t1.getTypeCode() != 9 && t1.getTypeCode() != 10) ||
	    (t1.getTypeCode() != 9 && t1.getTypeCode() != 10))
	    return tError;

	if (t1 == MyType.tObject)
	    return t1;
	if (t2 == MyType.tObject)
	    return t2;

	if (t1.getTypeCode() == 9 && t2.getTypeCode() == 9) 
	    return tArray(getGeneralizedType(t1.getElementType(),
					     t2.getElementType()));

	if (t1.getTypeCode() != 10 || t2.getTypeCode() != 10)
	    return tError;

	/* This code is not always correct:
	 * We don't want a real super type in all cases, but maybe only
	 * an interface which both objects implement. Think of this:
	 *
	 * interface I;
	 * class C1 implements I;
	 * class C2 implements I;
	 *
	 *  {
	 *     I var;
	 *     if (cond)
	 *       var = getC1();
	 *     else
	 *       var = getC2();
	 *     return var.interfaceMethod();
	 *  }
	 *
	 * The current code would first assign the type object to
	 * var and then produce a type error when interfaceMethod
	 * is called.
	 *
	 * Now we have proved that we need some better concept for
	 * types.  (Maybe a set of types for the upper and lower
	 * bound)
	 */
	
	ClassDeclaration c1 = new ClassDeclaration(t1.getClassName());
	ClassDeclaration c2 = new ClassDeclaration(t2.getClassName());
	
	try {
	    /* if one of the two types is an interface which
	     * is implemented by the other type the interface
	     * is the result.
	     */
	    if (c1.getClassDefinition(env).implementedBy(env, c2))	
		return t1;
	    if (c2.getClassDefinition(env).implementedBy(env, c1))
		return t2;

	    ClassDefinition c = c1.getClassDefinition(env);
	    while(c != null && !c.superClassOf(env, c2)) {
		c = c.getSuperClass(env).getClassDefinition(env);
	    }
	    if (c != null)
		return tClass(c.getName());
	} catch (ClassNotFound ex) {
	}
	return tObject;
    }
	    
    public Type getIntersection(ClassRangeType type)
    {
	Type bottom = getSpecializedType(bottomType, type.bottomType);
	Type top    = getGeneralizedType(topType, type.topType);

	Type newType = createRangeType(bottom,top);
        if (newType == tError)
            System.err.println("intersecting "+ this +" and "+ type + 
                               " to <" + bottom + "-" + top + 
                               "> to <error>");
        return newType;
    }

    public boolean intersects(ClassRangeType type)
    {
	return getIntersection(type) != tError;
    }

    public String typeString(String string, boolean flag1, boolean flag2)
    {
// 	if (verbose??)
	    return "<"+bottomType+"-"+topType+">" + string;
// 	else
// 	    return bottomType.typeString(string, flag1, flag2);
    }

//     public String toString()
//     {
// 	    return "<"+bottomType+"-"+topType+">";
//     }
}
