package jode.bytecode;
import jode.AssertError;

/**
 * This class contains some static methods to handle type signatures.
 */
public class TypeSignature {
    /**
     * This is a private method for generating the signature of a
     * given type.  
     */
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

    /**
     * Generate the signature for the given Class.
     * @param clazz a java.lang.Class, this may also be a primitive or
     * array type.
     * @return the type signature (see section 4.3.2 Field Descriptors
     * of the JVM specification)
     */
    public static String getSignature(Class clazz) {
	return appendSignature(new StringBuffer(), clazz).toString();
    }
 
    /**
     * Generate a method signature.
     * @param paramT the java.lang.Class of the parameter types of the method.
     * @param returnT the java.lang.Class of the return type of the method.
     * @return the method signature (see section 4.3.3 Method Descriptors
     * of the JVM specification)
     */
    public static String getSignature(Class paramT[], Class returnT) {
	StringBuffer sig = new StringBuffer("(");
	for (int i=0; i< paramT.length; i++)
	    appendSignature(sig, paramT[i]);
	return appendSignature(sig.append(')'), returnT).toString();
    }

    /**
     * Check if the given type is a two slot type.
     */
    private static boolean usingTwoSlots(char type) {
	return "JD".indexOf(type) >= 0;
    }

    /**
     * Returns the number of words, an object of the given simple type
     * signature takes.  
     */
    public static int getTypeSize(String typeSig) {
	return usingTwoSlots(typeSig.charAt(0)) ? 2 : 1;
    }

    public static String getElementType(String typeSig) {
	if (typeSig.charAt(0) != '[')
	    throw new IllegalArgumentException();
	return typeSig.substring(1);
    }

    public static ClassInfo getClassInfo(String typeSig) {
	if (typeSig.charAt(0) != 'L')
	    throw new IllegalArgumentException();
	return ClassInfo.forName
	    (typeSig.substring(1, typeSig.length()-1).replace('/', '.'));
    }

    public static int skipType(String methodTypeSig, int position) {
	char c = methodTypeSig.charAt(position++);
	while (c == '[')
	    c = methodTypeSig.charAt(position++);
	if (c == 'L')
	    return methodTypeSig.indexOf(';', position) + 1;
	return position;
    }
    
    /**
     * Returns the number of words, the arguments for the given method
     * type signature takes.  
     */
    public static int getArgumentSize(String methodTypeSig) {
	int nargs = 0;
	int i = 1;
	for (;;) {
	    char c = methodTypeSig.charAt(i);
	    if (c == ')')
		return nargs;
	    i = skipType(methodTypeSig, i);
	    if (usingTwoSlots(c))
		nargs += 2;
	    else 
		nargs++;
	}
    }

    /**
     * Returns the number of words, an object of the given simple type
     * signature takes.  
     */
    public static int getReturnSize(String methodTypeSig) {
	int length = methodTypeSig.length();
	if (methodTypeSig.charAt(length - 2) == ')') {
	    // This is a single character return type.
	    char returnType = methodTypeSig.charAt(length - 1);
	    return returnType == 'V' ? 0 
		: usingTwoSlots(returnType) ? 2 : 1;
	} else
	    // All multi character return types take one parameter
	    return 1;
    }

    /**
     * Returns the number of words, an object of the given simple type
     * signature takes.  
     */
    public static String[] getParameterTypes(String methodTypeSig) {
	int pos = 1;
	int count = 0;
	while (methodTypeSig.charAt(pos) != ')') {
	    pos = skipType(methodTypeSig, pos);
	    count++;
	}
	String[] params = new String[count];
	pos = 1;
	for (int i = 0; i < count; i++) {
	    int start = pos;
	    pos = skipType(methodTypeSig, pos);
	    params[i] = methodTypeSig.substring(start, pos);
	}
	return params;
    }

    /**
     * Returns the number of words, an object of the given simple type
     * signature takes.  
     */
    public static String getReturnType(String methodTypeSig) {
	return methodTypeSig.substring(methodTypeSig.lastIndexOf(')')+1);
    }

    private static void checkClassName(String clName) 
	throws IllegalArgumentException 
    {
	boolean start = true;
	for (int i=0; i< clName.length(); i++) {
	    char c = clName.charAt(i);
	    if (c == '/')
		start = true;
	    else if (start && Character.isJavaIdentifierStart(c)) 
		start = false;
	    else if ((start && false /*XXX*/)
		     || !Character.isJavaIdentifierPart(c))
		throw new IllegalArgumentException("Illegal java class name: "
						   + clName);
	}
    }

    /**
     * Check if there is a valid simple type signature starting at index
     * in string typesig.
     * @return the index at which the type signature ends.
     * @exception IllegalArgumentException if there was an illegal character.
     * @exception StringIndexOutOfBoundsException if the typesig ended early.
     */
    private static int checkTypeSig(String typesig, int index) {
	char c = typesig.charAt(index++);
	while (c == '[')
	    c = typesig.charAt(index++);
	if (c == 'L') {
	    int end = typesig.indexOf(';', index);
	    // next instruction throws StringIndexOutOfBounds, if no ; exists.
	    checkClassName(typesig.substring(index+1, end));
	    index = end + 1;
	} else {
	    if ("ZBSCIJFD".indexOf(c) == -1)
		throw new IllegalArgumentException("Type sig error: "+typesig);
	}
	return index;
    }

    public static void checkTypeSig(String typesig) 
	throws IllegalArgumentException
    {
	try {
	    if (checkTypeSig(typesig, 0) != typesig.length())
		throw new IllegalArgumentException
		    ("Type sig too long: "+typesig);
	} catch (StringIndexOutOfBoundsException ex) {
	    throw new IllegalArgumentException
		("Incomplete type sig: "+typesig);
	}
    }

    public static void checkMethodTypeSig(String typesig) 
	throws IllegalArgumentException
    {
	try {
	    if (typesig.charAt(0) != '(')
		throw new IllegalArgumentException
		    ("No method signature: "+typesig);
	    int i = 1;
	    while (typesig.charAt(i) != ')')
		i = checkTypeSig(typesig, i);
	    // skip closing parenthesis.
	    i++;
	    if (typesig.charAt(i) == 'V')
		// accept void return type.
		i++;
	    else
		i = checkTypeSig(typesig, i);
	    if (i != typesig.length())
		throw new IllegalArgumentException
		    ("Type sig too long: "+typesig);
	} catch (StringIndexOutOfBoundsException ex) {
	    throw new IllegalArgumentException
		("Incomplete type sig: "+typesig);
	}
    }
}

