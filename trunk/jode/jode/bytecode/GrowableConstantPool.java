/* GrowableConstantPool Copyright (C) 1999 Jochen Hoenicke.
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
	String key = "" + (char)tag + constant;
	Integer index = (Integer) entryToIndex.get(key);
	if (index != null)
	    return index.intValue();
	int newIndex = count;
	grow(count + 1);
	tags[newIndex] = tag;
	constants[newIndex] = constant;
	entryToIndex.put(key, new Integer(newIndex));
	count++;
	return newIndex;
    }

    int putLongConstant(int tag, Object constant) {
	String key = "" + (char)tag + constant;
	Integer index = (Integer) entryToIndex.get(key);
	if (index != null)
	    return index.intValue();
	int newIndex = count;
	grow(count + 2);
	tags[newIndex] = tag;
	tags[newIndex+1] = -tag;
	constants[newIndex] = constant;
	entryToIndex.put(key, new Integer(newIndex));
	count += 2;
	return newIndex;
    }

    int putIndexed(String key, int tag, int index1, int index2) {
	Integer indexObj = (Integer) entryToIndex.get(key);
	if (indexObj != null) {
	    /* Maybe this was a reserved, but not filled entry */
	    int index = indexObj.intValue();
	    indices1[index] = index1;
	    indices2[index] = index2;
	    return index;
	}
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

    public int putClassName(String name) {
	name = name.replace('.','/');
	return putIndexed(""+(char) CLASS + name,
			  CLASS, putUTF(name), 0);
    }

    public int putClassType(String name) {
	if (name.charAt(0) == 'L')
	    name = name.substring(1, name.length()-1);
	return putIndexed(""+(char) CLASS + name,
			  CLASS, putUTF(name), 0);
    }

    public int putRef(int tag, Reference ref) {
	String className = ref.getClazz();
	String typeSig = ref.getType();
	String nameAndType = ref.getName() + "/" + typeSig;

	int classIndex = putClassType(className);
	int nameIndex  = putUTF(ref.getName());
	int typeIndex  = putUTF(typeSig);
	int nameTypeIndex = putIndexed("" + (char) NAMEANDTYPE + nameAndType,
				       NAMEANDTYPE, nameIndex, typeIndex);
	return putIndexed("" + (char)tag + className + "/" + nameAndType,
			  tag, classIndex, nameTypeIndex);
    }

    /**
     * Puts a constant into this constant pool
     * @param c the constant, must be of type 
     *    Integer, Long, Float, Double or String
     * @return the index into the pool of this constant.
     */
    public int putConstant(Object c) {
	if (c instanceof String) {
	    return putIndexed("" + (char) STRING + c,
			      STRING, putUTF((String) c), 0);
	} else {
	    int tag;
	    if (c instanceof Integer)
		tag = INTEGER;
	    else if (c instanceof Float)
		tag = FLOAT;
	    else
		throw new IllegalArgumentException
		    ("illegal constant " + c + " of type: " + c.getClass());
	    return putConstant(tag, c);
        }
    }

    /**
     * Puts a constant into this constant pool
     * @param c the constant, must be of type 
     *    Integer, Long, Float, Double or String
     * @return the index into the pool of this constant.
     */
    public int putLongConstant(Object c) {
	int tag;
	if (c instanceof Long)
	    tag = LONG;
	else if (c instanceof Double)
	    tag = DOUBLE;
	else
	    throw new IllegalArgumentException
		("illegal long constant " + c + " of type: " + c.getClass());
	return putLongConstant(tag, c);
    }

    /**
     * Reserve an entry in this constant pool for a constant (for ldc).
     * @param c the constant, must be of type 
     *    Integer, Long, Float, Double or String
     * @return the reserved index into the pool of this constant.
     */
    public int reserveConstant(Object c) {
	if (c instanceof String) {
	    return putIndexed("" + (char)STRING + c, 
			      STRING, -1, 0);
	} else {
	    return putConstant(c);
        }
    }

    /**
     * Reserve an entry in this constant pool for a constant (for ldc).
     * @param c the constant, must be of type 
     *    Integer, Long, Float, Double or String
     * @return the reserved index into the pool of this constant.
     */
    public int reserveLongConstant(Object c) {
	return putLongConstant(c);
    }

    public int copyConstant(ConstantPool cp, int index) 
	throws ClassFormatException {
	return putConstant(cp.getConstant(index));
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
