/* FieldInfo Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.Type;
import java.io.*;
import java.lang.reflect.Modifier;

public class FieldInfo extends BinaryInfo {
    int modifier;
    String name;
    Type type;
    Object constant;

    public FieldInfo() {
    }

    public FieldInfo(String name, Type type, int modifier) {
	this.name = name;
	this.type = type;
	this.modifier = modifier;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public int getModifiers() {
        return modifier;
    }

    public Object getConstant() {
        return constant;
    }

    public void read(ConstantPool constantPool, 
                     DataInputStream input, int howMuch) throws IOException {
	modifier = input.readUnsignedShort();
	name = constantPool.getUTF8(input.readUnsignedShort());
	type = Type.tType(constantPool.getUTF8(input.readUnsignedShort()));

        readAttributes(constantPool, input, howMuch);

	if ((howMuch & ClassInfo.ALL_ATTRIBUTES) != 0) {
	    AttributeInfo attribute = findAttribute("ConstantValue");
	    if (attribute != null) {
		byte[] contents = attribute.getContents();
		if (contents.length != 2)
		    throw new ClassFormatException("ConstantValue attribute"
						   + " has wrong length");
		int index = (contents[0] & 0xff) << 8 | (contents[1] & 0xff);
		constant = constantPool.getConstant(index);
	    }
	}
    }

    public String toString() {
        return "Field "+Modifier.toString(modifier)+" "+
            type.toString()+" "+name;
    }
}
