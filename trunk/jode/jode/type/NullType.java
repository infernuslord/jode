package jode;
public class NullType extends ClassInterfacesType {
    public NullType() {
	super(null, null);
	typecode = TC_NULL;
    }

    public Type createRangeType(ClassInterfacesType bottomType) {
	return tRange(bottomType, this);
    }

    /**
     * Returns the generalized type of this and type.  We have two
     * classes and multiple interfaces.  The result should be the
     * object that is the the super class of both objects and all
     * interfaces, that one class or interface of each type 
     * implements.  */
    public Type getGeneralizedType(Type type) {
	if (type.typecode == TC_RANGE)
	    type = ((RangeType) type).getTop();
	return type;
    }
    /**
     * Returns the specialized type of this and type.
     * We have two classes and multiple interfaces.  The result 
     * should be the object that extends both objects
     * and the union of all interfaces.
     */
    public Type getSpecializedType(Type type) {
	if (type.typecode == TC_RANGE)
	    type = ((RangeType) type).getBottom();
	return type;
    }

    /**
     * Marks this type as used, so that the class is imported.
     */
    public void useType() {
    }

    public String toString() {
	return "/*NULL*/" + env.classString("java.lang.Object");
    }

    public boolean equals(Object o) {
	return o == this;
    }

    public Type getHint() {
	return tNull;
    }

    /**
     * Intersect this type with another type and return the new type.
     * @param type the other type.
     * @return the intersection, or tError, if a type conflict happens.
     */
    public Type intersection(Type type) {
	throw new AssertError("NULL.intersection");
    }
}

