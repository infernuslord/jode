/* jode.bytecode.GrowableConstantPool Copyright (C) 1998 Jochen Hoenicke.
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
import java.util.Hashtable;

/**
 * This class represent a constant pool, where new constants can be added to.
 *
 * @author Jochen Hoenicke
 */
public class GrowableConstantPool extends ConstantPool {
    Hashtable entryToIndex = new Hashtable(); 

    public GrowableConstantPool () {
	count = 1;
	tags = new int[128];
	indices1 = new int[128];
	indices2 = new int[128];  
	constants = new Object[128];
    }

    public final void grow(int wantedSize) {
	if (tags.length < wantedSize) {
	    int newSize = Math.max(tags.length*2, wantedSize);
	    int[] tmpints = new int[newSize];
	    System.arraycopy(tags, 0, tmpints, 0, count);
	    tags = tmpints;
	    tmpints = new int[newSize];
	    System.arraycopy(indices1, 0, tmpints, 0, count);
	    indices1 = tmpints;
	    tmpints = new int[newSize];
	    System.arraycopy(indices2, 0, tmpints, 0, count);
	    indices2 = tmpints;
	    Object[] tmpobjs = new Object[newSize];
	    System.arraycopy(constants, 0, tmpobjs, 0, count);
	    constants = tmpobjs;
	}
    }

    int putConstant(int tag, Object constant) {
	String key = tag+"C"+constant;
	Integer index = (Integer) entryToIndex.get(key);
	if (index != null)
	    return index.intValue();
	int newIndex = count;
	grow(count+(tag == DOUBLE || tag == LONG ? 2 : 1));
	tags[newIndex] = tag;
	constants[newIndex] = constant;
	entryToIndex.put(key, new Integer(newIndex));
	count++;
	if (tag == DOUBLE || tag == LONG)
	    count++;
	return newIndex;
    }

    int putIndexed(int tag, int index1, int index2) {
	String key = tag+"I"+index1+","+index2;
	Integer index = (Integer) entryToIndex.get(key);
	if (index != null)
	    return index.intValue();
	grow(count+1);
	tags[count] = tag;
	indices1[count] = index1;
	indices2[count] = index2;
	entryToIndex.put(key, new Integer(count));
	return count++;
    }

    public final int putUTF(String utf) {
	return putConstant(UTF8, utf);
    }    

    public int putClassRef(String name) {
	return putIndexed(CLASS, putUTF(name.replace('.','/')), 0);
    }

    public int putRef(int tag, String[] names) {
	int classIndex = putClassRef(names[0]);
	int nameIndex  = putUTF(names[1]);
	int typeIndex  = putUTF(names[2]);
	int nameTypeIndex = putIndexed(NAMEANDTYPE, nameIndex, typeIndex);
	return putIndexed(tag, classIndex, nameTypeIndex);
    }

    public int copyConstant(ConstantPool cp, int index) 
	throws ClassFormatException {
	if (cp.tags[index] == STRING)
	    return putIndexed(STRING, 
			      putUTF(cp.getUTF8(cp.indices1[index])), 0);
	else
	    return putConstant(cp.tags[index], cp.constants[index]);
    }

    public void write(DataOutputStream stream) 
	throws IOException {
	stream.writeShort(count);
	for (int i=1; i< count; i++) {
	    int tag = tags[i];
            stream.writeByte(tag);
            switch (tag) {
	    case CLASS:
		stream.writeShort(indices1[i]);
		break;
	    case FIELDREF:
	    case METHODREF:
	    case INTERFACEMETHODREF:
		stream.writeShort(indices1[i]);
		stream.writeShort(indices2[i]);
		break;
	    case STRING:
		stream.writeShort(indices1[i]);
		break;
	    case INTEGER:
		stream.writeInt(((Integer)constants[i]).intValue());
		break;
	    case FLOAT:
		stream.writeFloat(((Float)constants[i]).floatValue());
		break;
	    case LONG:
		stream.writeLong(((Long)constants[i]).longValue());
		i++;
		break;
	    case DOUBLE:
		stream.writeDouble(((Double)constants[i]).doubleValue());
		i++;
		break;
	    case NAMEANDTYPE:
		stream.writeShort(indices1[i]);
		stream.writeShort(indices2[i]);
		break;
	    case UTF8:
		stream.writeUTF((String)constants[i]);
		break;
	    default:
		throw new ClassFormatException("unknown constant tag");
            }
	}
    }
}
