/* BinaryInfo Copyright (C) 1998-1999 Jochen Hoenicke.
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
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import jode.util.SimpleMap;

///#def COLLECTIONS java.util
import java.util.Map;
import java.util.Collections;
import java.util.Iterator;
///#enddef


/**
 * <p>Represents a container for user specified attributes.</p>
 *
 * <p>Java bytecode is extensible: Classes, Methods and Fields may
 * have any number of attributes.  Every attribute has a name and some
 * unformatted data.</p>
 *
 * <p>There are some predefined attributes, even the Code of a Method
 * is an attribute.  These predefined attributes are all handled by
 * this package as appropriate.  This methods are only useful for non
 * standard attributes.</p>
 *
 * <p>One application of this attributes are installation classes.
 * These classes have a special attribute containing a zip of the
 * files that should be installed.  There are other possible uses,
 * e.g.  putting native machine code for some architectures into the
 * class.</p>
 *
 * @author Jochen Hoenicke 
 */
public class BinaryInfo {
    private Map unknownAttributes = null;

    void skipAttributes(DataInputStream input) throws IOException {
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

    int getKnownAttributeCount() {
	return 0;
    }

    void readAttribute(String name, int length,
		       ConstantPool constantPool,
		       DataInputStream input, 
		       int howMuch) throws IOException {
	byte[] data = new byte[length];
	input.readFully(data);
	if (howMuch >= ClassInfo.ALL) {
	    if (unknownAttributes == null)
		unknownAttributes = new SimpleMap();
	    unknownAttributes.put(name, data);
	}
    }

    static class ConstrainedInputStream extends FilterInputStream {
	int length;

	public ConstrainedInputStream(int attrLength, InputStream input) {
	    super(input);
	    length = attrLength;
	}

	public int read() throws IOException {
	    if (length > 0) {
		int data = super.read();
		length--;
		return data;
	    }
	    throw new EOFException();
	}

	public int read(byte[] b, int off, int len) throws IOException {
	    if (length < len) {
		len = length;
	    }
	    if (len == 0)
		return -1;
	    int count = super.read(b, off, len);
	    length -= count;
	    return count;
	}

	public int read(byte[] b) throws IOException {
	    return read(b, 0, b.length);
	}

	public long skip(long count) throws IOException {
	    if (length < count) {
		count = length;
	    }
	    count = super.skip(count);
	    length -= (int) count;
	    return count;
	}

	public void skipRemaining() throws IOException {
	    while (length > 0) {
		int skipped = (int) skip(length);
		if (skipped == 0)
		    throw new EOFException();
		length -= skipped;
	    }
	}
    }

    void readAttributes(ConstantPool constantPool,
			DataInputStream input, 
			int howMuch) throws IOException {
	int count = input.readUnsignedShort();
	unknownAttributes = null;
	for (int i=0; i< count; i++) {
	    String attrName = 
		constantPool.getUTF8(input.readUnsignedShort());
	    final int attrLength = input.readInt();
	    ConstrainedInputStream constrInput = 
		    new ConstrainedInputStream(attrLength, input);
	    readAttribute(attrName, attrLength, 
			  constantPool, new DataInputStream(constrInput),
			  howMuch);
	    constrInput.skipRemaining();
	}
    }

    void drop(int keep) {
	if (keep < ClassInfo.ALL)
	    unknownAttributes = null;
    }

    void prepareAttributes(GrowableConstantPool gcp) {
	if (unknownAttributes == null)
	    return;
	Iterator i = unknownAttributes.keySet().iterator();
	while (i.hasNext())
	    gcp.putUTF8((String) i.next());
    }

    void writeKnownAttributes
	(GrowableConstantPool constantPool, 
	 DataOutputStream output) throws IOException {
    }

    void writeAttributes
	(GrowableConstantPool constantPool, 
	 DataOutputStream output) throws IOException {
	int count = getKnownAttributeCount();
	if (unknownAttributes != null)
	    count += unknownAttributes.size();
	output.writeShort(count);
	writeKnownAttributes(constantPool, output);
	if (unknownAttributes != null) {
	    Iterator i = unknownAttributes.entrySet().iterator();
	    while (i.hasNext()) {
		Map.Entry e = (Map.Entry) i.next();
		String name = (String) e.getKey();
		byte[] data = (byte[]) e.getValue();
		output.writeShort(constantPool.putUTF8(name));
		output.writeInt(data.length);
		output.write(data);
	    }
	}
    }

    int getAttributeSize() {
	int size = 2; /* attribute count */
	if (unknownAttributes != null) {
	    Iterator i = unknownAttributes.values().iterator();
	    while (i.hasNext())
		size += 2 + 4 + ((byte[]) i.next()).length;
	}
	return size;
    }
    
    /**
     * Finds a non standard attribute with the given name.
     * @param name the name of the attribute.
     * @return the contents of the attribute, null if not found.
     */
    public byte[] findAttribute(String name) {
	if (unknownAttributes != null)
	    return (byte[]) unknownAttributes.get(name);
	return null;
    }

    /**
     * Gets all non standard attributes
     */
    public Iterator getAttributes() {
	if (unknownAttributes != null)
	    return unknownAttributes.values().iterator();
	return Collections.EMPTY_SET.iterator();
    }

    /**
     * Adds a new non standard attribute or replaces an old one with the
     * same name.
     * @param name the name of the attribute.
     * @param contents the new contens.
     */
    public void setAttribute(String name, byte[] contents) {
	if (unknownAttributes == null)
	    unknownAttributes = new SimpleMap();
	unknownAttributes.put(name, contents);
    }

    /**
     * Removes a new non standard attribute.
     * @param name the name of the attribute.
     * @return the old contents of the attribute.
     */
    public byte[] removeAttribute(String name) {
	if (unknownAttributes != null)
	    return (byte[]) unknownAttributes.remove(name);
	return null;
    }

    /**
     * Removes all non standard attribute.
     */
    public void removeAllAttributes() {
	unknownAttributes = null;
    }
}


