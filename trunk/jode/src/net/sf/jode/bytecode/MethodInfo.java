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

package net.sf.jode.bytecode;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
///#def COLLECTIONEXTRA java.lang
import java.lang.Comparable;
///#enddef

public final class MethodInfo extends BinaryInfo implements Comparable {
    int modifier;
    String name;
    String typeSig;

    BasicBlocks basicblocks;
    String[] exceptions;
    boolean syntheticFlag;
    boolean deprecatedFlag;

    public MethodInfo() {
    }

    public MethodInfo(String name, String typeSig, int modifier) {
	this.name = name;
	this.typeSig = typeSig;
	this.modifier = modifier;
    }

    void readAttribute(String name, int length, ConstantPool cp,
		       DataInputStream input, int howMuch) throws IOException {
	if (howMuch >= ClassInfo.NODEBUG && name.equals("Code")) {
	    basicblocks = new BasicBlocks(this);
	    basicblocks.read(cp, input, howMuch);
	} else if (howMuch >= ClassInfo.DECLARATIONS
		   && name.equals("Exceptions")) {
	    int count = input.readUnsignedShort();
	    exceptions = new String[count];
	    for (int i = 0; i < count; i++)
		exceptions[i] = cp.getClassName(input.readUnsignedShort());
	    if (length != 2 * (count + 1))
		throw new ClassFormatException
		    ("Exceptions attribute has wrong length");
	} else if (name.equals("Synthetic")) {
	    syntheticFlag = true;
	    if (length != 0)
		throw new ClassFormatException
		    ("Synthetic attribute has wrong length");
	} else if (name.equals("Deprecated")) {
	    deprecatedFlag = true;
	    if (length != 0)
		throw new ClassFormatException
		    ("Deprecated attribute has wrong length");
	} else
	    super.readAttribute(name, length, cp, input, howMuch);
    }

    void read(ConstantPool constantPool, 
	      DataInputStream input, int howMuch) throws IOException {
	modifier   = input.readUnsignedShort();
	name = constantPool.getUTF8(input.readUnsignedShort());
        typeSig = constantPool.getUTF8(input.readUnsignedShort());
        readAttributes(constantPool, input, howMuch);
    }

    void reserveSmallConstants(GrowableConstantPool gcp) {
	if (basicblocks != null)
	    basicblocks.reserveSmallConstants(gcp);
    }

    void prepareWriting(GrowableConstantPool gcp) {
	gcp.putUTF8(name);
	gcp.putUTF8(typeSig);
	if (basicblocks != null) {
	    gcp.putUTF8("Code");
	    basicblocks.prepareWriting(gcp);
	}
	if (exceptions != null) {
	    gcp.putUTF8("Exceptions");
	    for (int i = 0; i < exceptions.length; i++)
		gcp.putClassName(exceptions[i]);
	}
	if (syntheticFlag)
	    gcp.putUTF8("Synthetic");
	if (deprecatedFlag)
	    gcp.putUTF8("Deprecated");
	prepareAttributes(gcp);
    }

    int getKnownAttributeCount() {
	int count = 0;
	if (basicblocks != null)
	    count++;
	if (exceptions != null)
	    count++;
	if (syntheticFlag)
	    count++;
	if (deprecatedFlag)
	    count++;
	return count;
    }

    void writeKnownAttributes(GrowableConstantPool gcp,
			      DataOutputStream output) 
	throws IOException {
	if (basicblocks != null) {
	    output.writeShort(gcp.putUTF8("Code"));
	    basicblocks.write(gcp, output);
	}
	if (exceptions != null) {
	    int count = exceptions.length;
	    output.writeShort(gcp.putUTF8("Exceptions"));
	    output.writeInt(2 + count * 2);
	    output.writeShort(count);
	    for (int i = 0; i < count; i++)
		output.writeShort(gcp.putClassName(exceptions[i]));
	}
	if (syntheticFlag) {
	    output.writeShort(gcp.putUTF8("Synthetic"));
	    output.writeInt(0);
	}
	if (deprecatedFlag) {
	    output.writeShort(gcp.putUTF8("Deprecated"));
	    output.writeInt(0);
	}
    }

    void write(GrowableConstantPool constantPool, 
		      DataOutputStream output) throws IOException {
	output.writeShort(modifier);
	output.writeShort(constantPool.putUTF8(name));
	output.writeShort(constantPool.putUTF8(typeSig));
        writeAttributes(constantPool, output);
    }

    void drop(int keep) {
	if (keep < ClassInfo.DECLARATIONS)
	    exceptions = null;
	if (keep < ClassInfo.NODEBUG)
	    basicblocks = null;
	else
	    basicblocks.drop(keep);
	super.drop(keep);
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return typeSig;
    }

    public int getModifiers() {
        return modifier;
    }

    public boolean isConstructor() {
	return name.charAt(0) == '<';
    }
    
    public boolean isStatic() {
	return Modifier.isStatic(modifier);
    }

    public boolean isSynthetic() {
	return syntheticFlag;
    }

    public boolean isDeprecated() {
	return deprecatedFlag;
    }

    public BasicBlocks getBasicBlocks() {
	return basicblocks;
    }

    public String[] getExceptions() {
	return exceptions;
    }
    
    public void setName(String newName) {
        name = newName;
    }
    
    public void setType(String newType) {
        typeSig = newType;
    }

    public void setModifiers(int newModifier) {
	modifier = newModifier;
    }

    public void setSynthetic(boolean flag) {
	syntheticFlag = flag;
    }

    public void setDeprecated(boolean flag) {
	deprecatedFlag = flag;
    }

    public void setBasicBlocks(BasicBlocks newBasicblocks) {
	basicblocks = newBasicblocks;
    }

    public void setExceptions(String[] newExceptions) {
	exceptions = newExceptions;
    }

    /** 
     * Compares two MethodInfo objects for method order.  The method
     * order is as follows: First the static class intializer followed
     * by constructor with type signature sorted lexicographic.  Then
     * all other methods sorted lexicographically by name.  If two
     * methods have the same name, they are sorted by type signature.
     *
     * @return a positive number if this method follows the other in
     * method order, a negative number if it preceeds the
     * other, and 0 if they are equal.  
     * @exception ClassCastException if other is not a ClassInfo.  
     */
    public int compareTo(Object other) {
	MethodInfo mi = (MethodInfo) other;
	/* Normally constructors should automatically sort themself to
	 * the beginning, but if method name starts with a digit, the
	 * order would be destroyed.
	 *
	 * The JVM explicitly forbids methods starting with digits,
	 * nonetheless some obfuscators break this rule.
	 *
	 * But note that <clinit> comes lexicographically before <init>.
	 */
	if (name.charAt(0) != mi.name.charAt(0)) {
	    if (name.charAt(0) == '<')
		return -1;
	    if (mi.name.charAt(0) == '<')
		return 1;
	}
	int result = name.compareTo(mi.name);
	if (result == 0)
	    result = typeSig.compareTo(mi.typeSig);
	return result;
    }

    public String toString() {
        return "Method "+Modifier.toString(modifier)+" "+
            typeSig + " " + name;
    }
}
