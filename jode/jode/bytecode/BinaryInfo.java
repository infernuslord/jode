/* jode.bytecode.BinaryInfo Copyright (C) 1997-1998 Jochen Hoenicke.
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
import java.io.*;

/**
 *
 * @author Jochen Hoenicke
 */
public class BinaryInfo {
    public static final int HIERARCHY       = 0x01;
    public static final int FIELDS          = 0x02;
    public static final int METHODS         = 0x04;
    public static final int CONSTANTS       = 0x08;
    public static final int ALL_ATTRIBUTES  = 0x10;
    public static final int FULLINFO        = 0xff;

    private int status = 0;

    protected AttributeInfo[] attributes;

    public AttributeInfo findAttribute(String name) {
        for (int i=0; i< attributes.length; i++)
            if (attributes[i].getName().equals(name))
                return attributes[i];
        return null;
    }

    public AttributeInfo[] getAttributes() {
	return attributes;
    }

    protected void skipAttributes(DataInputStream input) throws IOException {
        int count = input.readUnsignedShort();
        for (int i=0; i< count; i++) {
            input.readUnsignedShort();  // the name index
            long length = input.readInt();
	    while (length > 0) {
		long skipped = input.skip(length);
		if (skipped == 0)
		    throw new EOFException("Can't skip. EOF?");
		length -= skipped;
	    }
        }
    }

    protected void readAttributes(ConstantPool constantPool,
                                  DataInputStream input, 
                                  int howMuch) throws IOException {
        if ((howMuch & ALL_ATTRIBUTES) != 0) {
            int count = input.readUnsignedShort();
            attributes = new AttributeInfo[count];
            for (int i=0; i< count; i++) {
                attributes[i] = new AttributeInfo(); 
                attributes[i].read(constantPool, input, howMuch);
            }
	} else
            skipAttributes(input);
    }
}

