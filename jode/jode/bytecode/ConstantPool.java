/* ConstantPool Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.Type;

/**
 * This class represent the constant pool.
 *
 * @author Jochen Hoenicke
 */
public class ConstantPool {
    public final static int CLASS              =  7;
    public final static int FIELDREF           =  9;
    public final static int METHODREF          = 10;
    public final static int INTERFACEMETHODREF = 11;
    public final static int STRING             =  8;
    public final static int INTEGER            =  3;
    public final static int FLOAT              =  4;
    public final static int LONG               =  5;
    public final static int DOUBLE             =  6;
    public final static int NAMEANDTYPE        = 12;
    public final static int UTF8               =  1;

    int count;
    int[] tags;
    int[] indices1, indices2;

    Object[] constants;

    public ConstantPool () {
    }

    public void read(DataInputStream stream) 
	throws IOException {
	count = stream.readUnsignedShort();
        tags = new int[count];
        indices1 = new int[count];
        indices2 = new int[count];
        constants = new Object[count];

	for (int i=1; i< count; i++) {
            int tag = stream.readUnsignedByte();
            tags[i] = tag;
            switch (tag) {
	    case CLASS:
		indices1[i] = stream.readUnsignedShort();
		break;
	    case FIELDREF:
	    case METHODREF:
	    case INTERFACEMETHODREF:
		indices1[i] = stream.readUnsignedShort();
		indices2[i] = stream.readUnsignedShort();
		break;
	    case STRING:
		indices1[i] = stream.readUnsignedShort();
		break;
	    case INTEGER:
		constants[i] = new Integer(stream.readInt());
		break;
	    case FLOAT:
		constants[i] = new Float(stream.readFloat());
		break;
	    case LONG:
		constants[i] = new Long(stream.readLong());
                tags[++i] = -LONG;
		break;
	    case DOUBLE:
		constants[i] = new Double(stream.readDouble());
                tags[++i] = -DOUBLE;
		break;
	    case NAMEANDTYPE:
		indices1[i] = stream.readUnsignedShort();
		indices2[i] = stream.readUnsignedShort();
		break;
	    case UTF8:
		constants[i] = stream.readUTF().intern();
		break;
	    default:
		throw new ClassFormatException("unknown constant tag");
            }
	}
    }

    public int getTag(int i) throws ClassFormatException {
        if (i == 0)
            throw new ClassFormatException("null tag");
        return tags[i];
    }

    public String getUTF8(int i) throws ClassFormatException {
        if (i == 0)
            return null;
        if (tags[i] != UTF8)
            throw new ClassFormatException("Tag mismatch");
        return (String)constants[i];
    }

    public Reference getRef(int i) throws ClassFormatException {
        if (i == 0)
            return null;
        if (tags[i] != FIELDREF
            && tags[i] != METHODREF && tags[i] != INTERFACEMETHODREF)
            throw new ClassFormatException("Tag mismatch");
	if (constants[i] == null) {
	    int classIndex = indices1[i];
	    int nameTypeIndex = indices2[i];
	    if (tags[nameTypeIndex] != NAMEANDTYPE)
		throw new ClassFormatException("Tag mismatch");
	    constants[i] = new Reference
		(getClassName(classIndex), 
		 getUTF8(indices1[nameTypeIndex]), 
		 getUTF8(indices2[nameTypeIndex]));
	}
	return (Reference) constants[i];
    }

    public Object getConstant(int i) throws ClassFormatException {
        if (i == 0)
            throw new ClassFormatException("null constant");
        switch (tags[i]) {
        case ConstantPool.INTEGER: 
        case ConstantPool.FLOAT:
        case ConstantPool.LONG:
        case ConstantPool.DOUBLE:
            return constants[i];
        case ConstantPool.STRING: 
            return getUTF8(indices1[i]);
        }
        throw new ClassFormatException("unknown constant tag: "+tags[i]);
    }

    public String getClassName(int i) throws ClassFormatException {
        if (i == 0)
            return null;
        if (tags[i] != CLASS)
            throw new ClassFormatException("Tag mismatch");
        return getUTF8(indices1[i]).replace('/', '.');
    }

    public String toString(int i) {
	switch (tags[i]) {
        case CLASS:
            return "class "+toString(indices1[i]);
        case STRING:
            return "\""+toString(indices1[i])+"\"";
        case INTEGER:
        case FLOAT:
        case LONG:
        case DOUBLE:
        case UTF8:
            return constants[i].toString();
        case FIELDREF:
        case METHODREF:
        case INTERFACEMETHODREF:
            return "Ref: "+toString(indices1[i])+": "
                + toString(indices2[i]);
        case NAMEANDTYPE:
            return toString(indices1[i])+" "+toString(indices2[i]);
        default:
            return "unknown tag: "+tags[i];
	}
    }

    public int size() {
	return count;
    }

    public String toString() {
        StringBuffer result = new StringBuffer("[ null");
        for (int i=1; i< count; i++) {
            result.append(", ").append(toString(i));
        }
        result.append(" ]");
        return result.toString();
    }            
}
