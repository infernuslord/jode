package jode;
import sun.tools.java.*;

/**
 * The LocalInfo represents a local variable of a method.
 * The problem is that two different local variables may use the same
 * slot.  The current strategie is to make the range of a local variable 
 * as small as possible.
 *
 * There may be more than one LocalInfo for a single local variable,
 * because the algorithm begins with totally disjunct variables and
 * then unifies locals.  One of the local is then a shadow object which
 * calls the member functions of the other local.
 */
public class LocalInfo {
    private static int serialnr = 0;
    private int slot;
    private Identifier name;
    private Type type;
    private LocalInfo shadow;

    /**
     * Create a new local info.  The name will be an identifier
     * of the form local_x__yyy, where x is the slot number and
     * yyy a unique number.
     * @param slot  The slot of this variable.
     */
    public LocalInfo(int slot) {
        name = null;
        type = MyType.tUnknown;
        this.slot = slot;
    }

    /**
     * Combines the LocalInfo with another.  This will make this
     * a shadow object to the other local info.  That is all member
     * functions will use the new local info instead of data in this
     * object. <p>
     * If this is called with ourself nothing will happen.
     * @param li the local info that we want to shadow.
     */
    public void combineWith(LocalInfo li) {
        if (shadow != null)
            shadow.combineWith(li);
        li = li.getLocalInfo();
        if (this != li) {
            shadow = li;
            li.setType(type);
        }
    }

    /**
     * Get the real LocalInfo.  This may be different from the
     * current object if this is a shadow local info.
     */
    public LocalInfo getLocalInfo() {
        if (shadow != null)
            return shadow.getLocalInfo();
        return this;
    }

    /**
     * Get the name of this local.
     */
    public Identifier getName() {
        if (shadow != null) 
            return shadow.getName();
        if (name == null)
            name = Identifier.lookup("local_"+slot+"__"+serialnr++);
        return name;
    }

    /**
     * Set the name of this local.
     */
    public void setName(Identifier name) {
        if (shadow != null) 
            shadow.setName(name);
        else
            this.name = name;
    }

    /**
     * Get the type of this local.
     */
    public Type getType() {
        if (shadow != null) 
            return shadow.getType();
        return type;
    }

    /**
     * Sets a new information about the type of this local.  
     * The type of the local is may be made more specific by this call.
     * @param  The new type information to be set.
     * @return The new type of the local.
     */
    public Type setType(Type newType) {
        if (shadow != null) 
            return shadow.setType(newType);
        this.type = MyType.intersection(this.type, newType);
        if (this.type == MyType.tError)
            System.err.println("Type error in "+getName());
        return this.type;
    }

    public boolean isShadow() {
	return (shadow != null);
    }
}

