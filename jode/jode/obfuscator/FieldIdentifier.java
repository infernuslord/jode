/* FieldIdentifier Copyright (C) 1999 Jochen Hoenicke.
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

package jode.obfuscator;
import java.lang.reflect.Modifier;
import jode.bytecode.*;
import java.io.*;
import java.util.*;

public class FieldIdentifier extends Identifier{
    FieldInfo info;
    ClassIdentifier clazz;
    /**
     * This field tells if the value is not constant.  It is initially
     * set to false, and if a write to that field is found, it is set
     * to true.
     */
    private boolean notConstant;
    private Object constant;

    /**
     * The FieldChangeListener that should be notified if a 
     * write to this field is found.
     */
    private Vector fieldListeners = new Vector();

    public FieldIdentifier(ClassIdentifier clazz, FieldInfo info) {
	super(info.getName());
	this.info = info;
	this.clazz = clazz;
	this.constant = info.getConstant();
    }

    public void applyPreserveRule(int preserveRule) {
	if ((preserveRule & (info.getModifiers() ^ Modifier.PRIVATE)) != 0) {
	    setReachable();
	    setPreserved();
	}
    }

    public void setSingleReachable() {
	super.setSingleReachable();
	clazz.bundle.analyzeIdentifier(this);
    }
    
    public void analyze() {
	String type = getType();
	int index = type.indexOf('L');
	if (index != -1) {
	    int end = type.indexOf(';', index);
	    clazz.bundle.reachableIdentifier(type.substring(index+1, end),
					     false);
	}
    }

    public Identifier getParent() {
	return clazz;
    }

    public String getFullName() {
	return clazz.getFullName() + "." + getName();
    }

    public String getFullAlias() {
	return clazz.getFullAlias() + "." + getAlias();
    }

    public String getName() {
	return info.getName();
    }

    public String getType() {
	return info.getType().getTypeSignature();
    }

    public boolean isNotConstant() {
	return notConstant;
    }
    
    public Object getConstant() {
	return constant;
    }
    
    public void addFieldListener(Identifier ident) {
	if (!fieldListeners.contains(ident))
	    fieldListeners.addElement(ident);
    }

    public void setNotConstant() {
	if (notConstant)
	    return;

	notConstant = true;
	Enumeration enum = fieldListeners.elements();
	while (enum.hasMoreElements())
	    clazz.bundle.analyzeIdentifier((Identifier) enum.nextElement());
	fieldListeners = null;
    }

    public String toString() {
	return "FieldIdentifier "+getFullName()+"."+getType();
    }

    public void readTable(Hashtable table) {
	String alias = (String) table.get(getFullName() + "." + getType());
	if (alias == null)
	    alias = (String) table.get(getFullName());
	if (alias != null)
	    setAlias(alias);
    }

    public void writeTable(Hashtable table) {
	table.put(getFullAlias()
		  + "." + clazz.bundle.getTypeAlias(getType()), getName());
    }

    public boolean conflicting(String newAlias, boolean strong) {
	String typeSig = strong ? getType() : "";
	if (clazz.containFieldAlias(newAlias, typeSig))
	    return true;

	Enumeration enum = clazz.knownSubClasses.elements();
	while (enum.hasMoreElements()) {
	    ClassIdentifier ci = (ClassIdentifier) enum.nextElement();
	    if (ci.containsFieldAliasDirectly(newAlias, typeSig))
		return true;
	}
	return false;
    }

    int nameIndex;
    int descriptorIndex;
    int constvalIndex;
    int constvalcontentIndex;

    public void fillConstantPool(GrowableConstantPool gcp) 
	throws ClassFormatException {
	nameIndex = gcp.putUTF(getAlias());
	descriptorIndex = gcp.putUTF(clazz.bundle.getTypeAlias(getType()));
	constvalIndex = 0;
        AttributeInfo attribute = info.findAttribute("ConstantValue");
	if (attribute != null) {
	    byte[] contents = attribute.getContents();
	    if (contents.length != 2)
		throw new ClassFormatError("ConstantValue attribute"
					   + " has wrong length");
	    int index = (contents[0] & 0xff) << 8 | (contents[1] & 0xff);
	    constvalIndex = gcp.putUTF("ConstantValue");
	    constvalcontentIndex = 
		gcp.copyConstant(clazz.info.getConstantPool(), index);
	}
    }

    public void write(DataOutputStream out) throws IOException {
	out.writeShort(info.getModifiers());
	out.writeShort(nameIndex);
	out.writeShort(descriptorIndex);
	if (constvalIndex != 0) {
	    out.writeShort(1); // number of Attributes
	    out.writeShort(constvalIndex);
	    out.writeInt(2);   // length of Attribute
	    out.writeShort(constvalcontentIndex);
	} else
	    out.writeShort(0);
    }
}
