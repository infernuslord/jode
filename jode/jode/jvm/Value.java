/* Value Copyright (C) 1999 Jochen Hoenicke.
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
import jode.bytecode.*;

/**
 * This class represents a stack value.
 *
 * @author Jochen Hoenicke
 */
public class Value {
    Object value;
    NewObject newObj;

    public Value() {
    }

    public void setObject(Object obj) {
	value = obj;
    }

    public Object objectValue() {
	if (newObj != null)
	    return newObj.objectValue();
	return value;
    }

    public void setInt(int i) {
	value = new Integer(i);
    }

    public int intValue() {
	return ((Integer)value).intValue();
    }

    public void setLong(long i) {
	value = new Long(i);
    }

    public long longValue() {
	return ((Long)value).longValue();
    }

    public void setFloat(float i) {
	value = new Float(i);
    }

    public float floatValue() {
	return ((Float)value).floatValue();
    }

    public void setDouble(double i) {
	value = new Double(i);
    }

    public double doubleValue() {
	return ((Double)value).doubleValue();
    }

    public void setNewObject(NewObject n) {
	newObj = n;
    }

    public NewObject getNewObject() {
	return newObj;
    }

    public void setValue(Value val) {
	value = val.value;
	newObj = val.newObj;
    }

    public String toString() {
	return newObj != null ? newObj.toString() : ""+value;
    }
}

