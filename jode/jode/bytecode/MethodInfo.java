/* MethodInfo Copyright (C) 1998-1999 Jochen Hoenicke.
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

package jode.bytecode;
import jode.type.Type;
import jode.type.MethodType;
import java.io.*;
import java.lang.reflect.Modifier;

public class MethodInfo extends BinaryInfo {

    int modifier;
    String name;
    MethodType type;

    public MethodInfo() {
    }

    public MethodInfo(String name, MethodType type, int modifier) {
	this.name = name;
	this.type = type;
	this.modifier = modifier;
    }

    public void read(ConstantPool constantPool, 
                     DataInputStream input, int howMuch) throws IOException {
	modifier   = input.readUnsignedShort();
	name = constantPool.getUTF8(input.readUnsignedShort());
        type = Type.tMethod(constantPool.getUTF8(input.readUnsignedShort()));
        readAttributes(constantPool, input, howMuch);
    }

    public String getName() {
        return name;
    }

    public MethodType getType() {
        return type;
    }

    public boolean isStatic() {
	return Modifier.isStatic(modifier);
    }

    public int getModifiers() {
        return modifier;
    }

    public String toString() {
        return "Method "+Modifier.toString(modifier)+" "+
            type.toString()+" "+name;
    }
}

