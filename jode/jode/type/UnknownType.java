package jode;
import sun.tools.java.Type;
import java.util.Hashtable;

public class UnknownType extends Type {
    static Hashtable subclasses = new Hashtable();

    public static Type tUnknown = new UnknownType(100, "x");
    public static Type tUInt    = new UnknownType(101, "i");
    public static Type tUIndex  = new UnknownType(104, "[");
    public static Type tUObject = new UnknownType(102, "*");
    public static Type tSubClass(Type type) {
        Type subtype = (Type) subclasses.get(type);
        if (subtype == null) {
            subtype = new UnknownSubType(type);
            subclasses.put(type, subtype);
        }
        return subtype;
    }

    protected UnknownType(int i, String str) {
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
        case 100: typeStr="<unknown>"; break;
        case 101: typeStr="<int>"; break; /*XXX*/
        case 102: typeStr="<Object>"; break;
        case 104: typeStr="<arrindex>"; break; /*XXX*/
        default:
            throw new RuntimeException("Wrong typeCode "+typeCode);
        }
        if (var.length() > 0)
            return typeStr+" "+var;
        return typeStr;
    }

    public static Type commonType(Type t1, Type t2) {
        if (t1 == t2 || t2 == tUnknown)
            return t1;

        switch (t1.getTypeCode()) {
        case  0: /* boolean*/
        case  1: /* byte   */
        case  2: /* char   */
        case  3: /* short  */
        case  4: /* int    */
            if (t2.getTypeCode() <= 4) {
                if (t2.getTypeCode() > t1.getTypeCode())
                    return t2;
                else
                    return t1;
            }
            if (t2 == tUInt || t2 == tUIndex)
                return t1;
            break;

        case  5: /* long   */
        case  6: /* float  */
        case  7: /* double */
        case  8: /* null?  */
        case 11: /* void   */
        case 12: /* method */
        case 13: /* error  */
            break;

        case  9: /* array  */
            if (t2 == tUObject)
                return t1;
            if (t2.getTypeCode() == 9) /* array, array case */
                return tArray(commonType(t1.getElementType(), 
                                         t2.getElementType()));
            break;

        case 10: /* class  */
            if (t2 == tUObject)
                return t1;
            if (t2.getTypeCode() == 103) {
                /* find suitable subclass of t2 */
                return t2; /*XXX*/
            }
            if (t2.getTypeCode() == 10) {
                return t1; /*XXX*/
            }
            break;

        case 100: /* unknown */
            return t2;

        case 101: /* unknown int */
            if ((t2.getTypeCode() >= 0 && t2.getTypeCode() <= 4) || 
                t2 == tUIndex)
                return t2;
            break;
        case 104: /* unknown index */
            if (t2.getTypeCode() >= 1 && t2.getTypeCode() <= 4)
                return t2;
            if (t2 == tUInt)
                return t1;
            break;

        case 102: /* unknown object */
            if (t2.getTypeCode() == 9 || t2.getTypeCode() == 10 || 
                t2.getTypeCode() == 103)
                return t2;
            break;
            
        case 103: /* unknown super class */
            if (t2.getTypeCode() == 10 || t2.getTypeCode() == 103)
                return t2; /*XXX*/
            if (t2 == tUObject)
                return t1;
            break;

        default:
            throw new AssertError("Wrong typeCode "+t1.getTypeCode());
        }
        return tError;
    }

    /**
     * Check if t1 is a t2.
     * @return true if t1 is a more specific type than t2, e.g.
     *    if t2 is a superclass of t1
     */
    public static boolean isOfType(Type t1, Type t2) {
        if ((t1 == t2 || t2 == tUnknown) && t1 != tError)
            return true;

        switch (t1.getTypeCode()) {
        case  0: /* boolean*/
        case  1: /* byte   */
        case  2: /* char   */
        case  3: /* short  */
        case  4: /* int    */

            /* JavaC thinks, that this is okay. */
            if (t2.getTypeCode() >= 0 && t2.getTypeCode() <=4)
                return true;

            /* fallthrough */
        case 104: /* unknown index */
            if (t2 == tUInt || t2 == tUIndex)
                return true;
            break;

        case  5: /* long   */
        case  6: /* float  */
        case  7: /* double */
        case  8: /* null?  */
        case 11: /* void   */
        case 12: /* method */
        case 13: /* error  */
        case 100: /* unknown */
        case 101: /* unknown int */
        case 102: /* unknown object */
            break;

        case  9: /* array  */
            if (t2 == tUObject)
                return true;
            if (t2.getTypeCode() == 9) /* array,array case */
                return isOfType(t1.getElementType(), t2.getElementType());
            break;

        case 10: /* class  */
            if (t2 == tUObject)
                return true;
            if (t2.getTypeCode() == 103)
                /* Always true because t2 may be an Object XXX I think not*/
                return true;
            if (t2.getTypeCode() == 10)
                /* true if t2 is a superclass of t1 */
                return true; /*XXX*/
            break;
            
        case 103: /* unknown super class */
            if (t2.getTypeCode() == 103)
                /* Always true because t2 may be an Object XXX I think not*/
                return true;

            if (t2.getTypeCode() == 10) {
                /* true if t2 is a real super class 
                   (or interface) of t1.getElementType() */
                return true; /*XXX*/
            }
            if (t2 == tUObject)
                return true;
            break;

        default:
            throw new AssertError("Wrong typeCode "+t1.getTypeCode());
        }
        return false;
    }
}
