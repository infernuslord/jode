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
import java.lang.reflect.InvocationTargetException;

/**
 * This interface is used by the Interpreter to actually modify objects,
 * invoke methods, etc. <br>
 *
 * The objects used in this runtime environment need not to be of the
 * real class, but could also be of some wrapper class.  The only
 * exception are arrays, which must be arrays (but not necessarily of
 * the real element type). <br>
 * 
 * @author Jochen Hoenicke */
public interface RuntimeEnvironment {
    /**
     * Get the value of a field member.
     * @param fieldref the Reference of the field.
     * @param obj the object of which the field should be taken, null
     * if the field is static.
     * @return the field value.  Primitive types are wrapped to 
     * Object.
     * @exception InterpreterException if the field does not exists, the
     * object is not supported etc.
     */
    public Object getField(Reference fieldref, Object obj)
	throws InterpreterException;

    /**
     * Set the value of a field member.
     * @param fieldref the Reference of the field.
     * @param obj the object of which the field should be taken, null
     * if the field is static.
     * @param value the field value.  Primitive types are wrapped to 
     * Object.
     * @exception InterpreterException if the field does not exists, the
     * object is not supported etc.
     */
    public void putField(Reference fieldref, Object obj, Object value)
	throws InterpreterException;


    /**
     * Invoke a method.
     * @param methodRef the reference to the method.
     * @param isVirtual true, iff the call is virtual
     * @param cls the object on which the method should be called, null
     * if the method is static.
     * @param params the params of the method.  Primitive types are
     * wrapped to Object.
     * @return the return value of the method.  Primitive types are
     * wrapped to Object,  void type is ignored, may be null.
     * @exception InterpreterException if the field does not exists, the
     * object is not supported etc.  */
    public Object invokeMethod(Reference methodRef, boolean isVirtual, 
			       Object cls, Object[] params)
	throws InterpreterException, InvocationTargetException;
    public Object invokeConstructor(Reference methodRef, Object[] params)
	throws InterpreterException, InvocationTargetException;

    public boolean instanceOf(Object obj, String className)
	throws InterpreterException;

    public Object newArray(String type, int[] dimensions)
	throws InterpreterException;

    public void enterMonitor(Object obj)
	throws InterpreterException;
    public void exitMonitor(Object obj)
	throws InterpreterException;
}


