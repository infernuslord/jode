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

public interface RuntimeEnvironment {
    public Object getField(Reference fieldref, Object obj)
	throws InterpreterException;
    public void putField(Reference fieldref, Object obj, Object value)
	throws InterpreterException;
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
