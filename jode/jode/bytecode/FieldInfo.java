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
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
///#def COLLECTIONEXTRA java.lang
import java.lang.Comparable;
///#enddef

public final class FieldInfo extends BinaryInfo implements Comparable {
    int modifier;
    String name;
    String typeSig;

    Object constant;
    boolean syntheticFlag;
    boolean deprecatedFlag;

    public FieldInfo() {
    }

    public FieldInfo(String name, String typeSig, int modifier) {
	this.name = name;
	this.typeSig = typeSig;
	this.modifier = modifier;
    }

    void readAttribute(String name, int length,
		       ConstantPool cp,
		       DataInputStream input, 
		       int howMuch) throws IOException {
	if (howMuch >= ClassInfo.DECLARATIONS
	    && name.equals("ConstantValue")) {
	    if (length != 2)
		throw new ClassFormatException
		    ("ConstantValue attribute has wrong length");
	    int index = input.readUnsignedShort();
	    constant = cp.getConstant(index);
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
	modifier = input.readUnsignedShort();
	name = constantPool.getUTF8(input.readUnsignedShort());
	typeSig = constantPool.getUTF8(input.readUnsignedShort());
        readAttributes(constantPool, input, howMuch);
    }

    void reserveSmallConstants(GrowableConstantPool gcp) {
    }

    void prepareWriting(GrowableConstantPool gcp) {
	gcp.putUTF8(name);
	gcp.putUTF8(typeSig);
	if (constant != null) {
	    gcp.putUTF8("ConstantValue");
	    if (typeSig.charAt(0) == 'J' || typeSig.charAt(0) == 'D')
		gcp.putLongConstant(constant);
	    else
		gcp.putConstant(constant);
	}
	if (syntheticFlag)
	    gcp.putUTF8("Synthetic");
	if (deprecatedFlag)
	    gcp.putUTF8("Deprecated");
	prepareAttributes(gcp);
    }

    protected int getKnownAttributeCount() {
	int count = 0;
	if (constant != null)
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
	if (constant != null) {
	    output.writeShort(gcp.putUTF8("ConstantValue"));
	    output.writeInt(2);
	    int index;
	    if (typeSig.charAt(0) == 'J'
		|| typeSig.charAt(0) == 'D')
		index = gcp.putLongConstant(constant);
	    else
		index = gcp.putConstant(constant);
	    output.writeShort(index);
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
	    constant = null;
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
    
    public boolean isSynthetic() {
	return syntheticFlag;
    }

    public boolean isDeprecated() {
	return deprecatedFlag;
    }

    public Object getConstant() {
	return constant;
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

    public void setConstant(Object newConstant) {
	constant = newConstant;
    }

    /** 
     * Compares two FieldInfo objects for field order.  The field
     * order is as follows: First the static class intializer followed
     * by constructor with type signature sorted lexicographic.  Then
     * all other fields sorted lexicographically by name.  If two
     * fields have the same name, they are sorted by type signature,
     * though that can only happen for obfuscated code.
     *
     * @return a positive number if this field follows the other in
     * field order, a negative number if it preceeds the
     * other, and 0 if they are equal.  
     * @exception ClassCastException if other is not a ClassInfo.  */
    public int compareTo(Object other) {
	FieldInfo fi = (FieldInfo) other;
	int result = name.compareTo(fi.name);
	if (result == 0)
	    result = typeSig.compareTo(fi.typeSig);
	return result;
    }

    public String toString() {
        return "Field "+Modifier.toString(modifier)+" "+
            typeSig+" "+name;
    }
}

