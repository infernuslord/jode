/* RuntimeEnvironment Copyright (C) 1999 Jochen Hoenicke.
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

package jode.jvm;
import jode.bytecode.Reference;
import jode.*;
import java.lang.reflect.*;

public class SimpleRuntimeEnvironment implements RuntimeEnvironment {

    public Object getField(Reference ref, Object obj)
	throws InterpreterException {
	Field f;
	try {
	    Class clazz = Class.forName(ref.getClazz());
	    try {
		f = clazz.getField(ref.getName());
	    } catch (NoSuchFieldException ex) {
		f = clazz.getDeclaredField(ref.getName());
	    }
	} catch (ClassNotFoundException ex) {
	    throw new InterpreterException
		(ref+": Class not found");
	} catch (NoSuchFieldException ex) {
	    throw new InterpreterException
		("Constructor "+ref+" not found");
	} catch (SecurityException ex) {
	    throw new InterpreterException
		(ref+": Security exception");
	}
	try {
	    return f.get(obj);
	} catch (IllegalAccessException ex) {
	    throw new InterpreterException
		("Field " + ref + " not accessible");
	}
    }
    public void putField(Reference ref, Object obj, Object value)
	throws InterpreterException {
	Field f;
	try {
	    Class clazz = Class.forName(ref.getClazz());
	    try {
		f = clazz.getField(ref.getName());
	    } catch (NoSuchFieldException ex) {
		f = clazz.getDeclaredField(ref.getName());
	    }
	} catch (ClassNotFoundException ex) {
	    throw new InterpreterException
		(ref+": Class not found");
	} catch (NoSuchFieldException ex) {
	    throw new InterpreterException
		("Constructor "+ref+" not found");
	} catch (SecurityException ex) {
	    throw new InterpreterException
		(ref+": Security exception");
	}
	try {
	    f.set(obj, value);
	} catch (IllegalAccessException ex) {
	    throw new InterpreterException
		("Field " + ref + " not accessible");
	}
    }
    
    public Object invokeConstructor(Reference ref, Object[] params)
	throws InterpreterException, InvocationTargetException {
	Constructor c;
	try {
	    Class clazz = Class.forName(ref.getClazz());
	    MethodType mt = (MethodType) Type.tType(ref.getType());
	    Class[] paramTypes = mt.getParameterClasses();
	    try {
		c = clazz.getConstructor(paramTypes);
	    } catch (NoSuchMethodException ex) {
		c = clazz.getDeclaredConstructor(paramTypes);
	    }
	} catch (ClassNotFoundException ex) {
	    throw new InterpreterException
		(ref+": Class not found");
	} catch (NoSuchMethodException ex) {
	    throw new InterpreterException
		("Constructor "+ref+" not found");
	} catch (SecurityException ex) {
	    throw new InterpreterException
		(ref+": Security exception");
	}
	
	try {
	    return c.newInstance(params);
	} catch (IllegalAccessException ex) {
	    throw new InterpreterException
		("Constructor " + ref + " not accessible");
	} catch (InstantiationException ex) {
	    throw new InterpreterException
		("InstantiationException in " + ref + ".");
	}
    }
    
    public Object invokeMethod(Reference ref, boolean isVirtual, 
			       Object cls, Object[] params) 
	throws InterpreterException, InvocationTargetException {
	Method m;
	if (!isVirtual && cls != null) /*XXX*/
	    throw new InterpreterException
		("Can't invoke nonvirtual Method " + ref + ".");
	MethodType mt = (MethodType) Type.tType(ref.getType());
	try {
	    Class clazz = Class.forName(ref.getClazz());
	    Class[] paramTypes = mt.getParameterClasses();
	    try {
		m = clazz.getMethod(ref.getName(), paramTypes);
	    } catch (NoSuchMethodException ex) {
		m = clazz.getDeclaredMethod(ref.getName(), paramTypes);
	    }
	} catch (ClassNotFoundException ex) {
	    throw new InterpreterException
		(ref+": Class not found");
	} catch (NoSuchMethodException ex) {
	    throw new InterpreterException
		("Method "+ref+" not found");
	} catch (SecurityException ex) {
	    throw new InterpreterException
		(ref+": Security exception");
	}
	try {
	    Type[] paramTypes = mt.getParameterTypes();
	    for (int i = 0; i< paramTypes.length; i++) {
		if (paramTypes[i] instanceof IntegerType
		    && paramTypes[i] != Type.tInt) {
		    int value = ((Integer) params[i]).intValue();
		    if (paramTypes[i] == Type.tBoolean) {
			params[i] = value != 0 ? Boolean.TRUE : Boolean.FALSE;
		    } else if (paramTypes[i] == Type.tChar) {
			params[i] = new Character((char) value);
		    } else if (paramTypes[i] == Type.tByte) {
			params[i] = new Byte((byte) value);
		    } else if (paramTypes[i] == Type.tShort) {
			params[i] = new Short((short) value);
		    } else
			throw new AssertError("Unknown integer type: "
					      +paramTypes[i]);
		}
	    }
	    return m.invoke(cls, params);
	} catch (IllegalAccessException ex) {
	    throw new InterpreterException
		("Method " + ref + " not accessible");
	}
    }
    
    public boolean instanceOf(Object obj, String className)
	throws InterpreterException {
	Class clazz;
	try {
	    clazz = Class.forName(className);
	} catch (ClassNotFoundException ex) {
	    throw new InterpreterException
		("Class "+ex.getMessage()+" not found");
	}
	return obj != null && !clazz.isInstance(obj);
    }

    public Object newArray(String type, int[] dimensions)
	throws InterpreterException, NegativeArraySizeException {
	Class clazz;
	try {
	    clazz = Class.forName(type);
	} catch (ClassNotFoundException ex) {
	    throw new InterpreterException
		("Class "+ex.getMessage()+" not found");
	}
	return Array.newInstance(clazz, dimensions);
    }

    public void enterMonitor(Object obj)
	throws InterpreterException {
	throw new InterpreterException("monitorenter not implemented");
    }
    public void exitMonitor(Object obj)
	throws InterpreterException {
	throw new InterpreterException("monitorenter not implemented");
    }
}
