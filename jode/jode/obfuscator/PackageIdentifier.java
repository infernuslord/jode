/* PackageIdentifier Copyright (C) 1999 Jochen Hoenicke.
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
import jode.Obfuscator;
import jode.bytecode.ClassInfo;
import jode.bytecode.FieldInfo;
import jode.bytecode.MethodInfo;
import java.lang.reflect.Modifier;
import java.util.*;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class PackageIdentifier extends Identifier {
    ClassBundle bundle;
    PackageIdentifier parent;
    String name;

    boolean loadOnDemand;
    Hashtable loadedClasses;

    public PackageIdentifier(ClassBundle bundle, 
			     PackageIdentifier parent,
			     String name, boolean loadOnDemand) {
	super(name);
	this.bundle = bundle;
	this.parent = parent;
	this.name = name;
	this.loadOnDemand = loadOnDemand;
	this.loadedClasses = new Hashtable();
	if (loadOnDemand && !Obfuscator.shouldStrip) {
	    // Load all classes and packages now, so they don't get stripped
	    Vector v = new Vector();
	    Enumeration enum = 
		ClassInfo.getClassesAndPackages(parent.getFullName()
						+getName());
	    while (enum.hasMoreElements()) {
		//insert sorted and remove double elements;
		String subclazz = (String)enum.nextElement();
		for (int i=0; ; i++) {
		    if (i == v.size()) {
			v.addElement(subclazz);
			break;
		    }
		    int compare = subclazz.compareTo((String)v.elementAt(i));
		    if (compare < 0) {
			v.insertElementAt(subclazz, i);
			break;
		    } else if (compare == 0)
			break;
		}
	    }
	    enum = v.elements();
	    while (enum.hasMoreElements()) {
		String subclazz = (String) enum.nextElement();
		String fullname = getFullName() + subclazz;
		if (ClassInfo.isPackage(fullname)) {
		    Identifier ident = new PackageIdentifier
			(bundle, this, subclazz, true);
		    loadedClasses.put(subclazz, ident);
		} else {
		    Identifier ident = new ClassIdentifier
			(bundle, this, subclazz, ClassInfo.forName(fullname));
		    loadedClasses.put(subclazz, ident);
		    ((ClassIdentifier) ident).initClass();
		}
	    }		
	}
    }

    public Identifier getIdentifier(String name) {
	if (loadOnDemand) {
	    Identifier ident = loadClass(name);
	    if (ident != null && bundle.preserveRule != -1)
		ident.applyPreserveRule(bundle.preserveRule);
	}

	int index = name.indexOf('.');
	if (index == -1)
	    return (Identifier) loadedClasses.get(name);
	else {
	    PackageIdentifier pack = (PackageIdentifier) 
		loadedClasses.get(name.substring(0, index));
	    if (pack != null)
		return pack.getIdentifier(name.substring(index+1));
	    else
		return null;
	}
    }

    public Identifier loadClass(String name) {
	int index = name.indexOf('.');
	if (index == -1) {
	    Identifier ident = (Identifier) loadedClasses.get(name);
	    if (ident == null) {
		String fullname = getFullName() + name;
		if (ClassInfo.isPackage(fullname)) {
		    ident = new PackageIdentifier(bundle, this, name, true);
		    loadedClasses.put(name, ident);
		} else if (!ClassInfo.exists(fullname)) {
		    System.err.println("Warning: Can't find class "+fullname);
		} else {
		    ident = new ClassIdentifier(bundle, this, name, 
						ClassInfo.forName(fullname));
		    loadedClasses.put(name, ident);
		    ((ClassIdentifier) ident).initClass();
		}
	    }
	    return ident;
	} else {
	    String subpack = name.substring(0, index);
	    PackageIdentifier pack = 
		(PackageIdentifier) loadedClasses.get(subpack);
	    if (pack == null) {
		pack = new PackageIdentifier(bundle, this, 
					     subpack, loadOnDemand);
		loadedClasses.put(subpack, pack);
	    }
	    
	    if (pack != null)
		return pack.getIdentifier(name.substring(index+1));
	    else
		return null;
	}
    }

    public Identifier loadClasses(String packageOrClass) {
	int index = packageOrClass.indexOf('.');
	if (index == -1) {
	    return loadClass(packageOrClass);
	} else {
	    String subpack = packageOrClass.substring(0, index);
	    PackageIdentifier pack = (PackageIdentifier)
		loadedClasses.get(subpack);
	    if (pack == null) {
		pack = new PackageIdentifier(bundle, this, 
					     subpack, loadOnDemand);
		loadedClasses.put(subpack, pack);
	    }
	    return pack.loadClasses(packageOrClass.substring(index+1));
	}
    }

    public void applyPreserveRule(int preserveRule) {
	Enumeration enum = loadedClasses.elements();
	while(enum.hasMoreElements())
	    ((Identifier) enum.nextElement()).applyPreserveRule(preserveRule);
    }

    public void reachableIdentifier(String fqn, boolean isVirtual) {
	int index = fqn.indexOf('.');
	String component = index == -1 ? fqn : fqn.substring(0, index);
	Identifier ident = getIdentifier(component);
	if (ident == null)
	    return;

	if (index == -1) {
	    ident.setReachable();
	    return;
	}

	if (ident instanceof PackageIdentifier)
	    ((PackageIdentifier) ident).reachableIdentifier
		(fqn.substring(index+1), isVirtual);
	else {
	    String method = fqn.substring(index+1);
	    index = method.indexOf('.');
	    if (index == -1) {
		((ClassIdentifier) ident).reachableIdentifier
		    (method, null, isVirtual);
	    } else {
		((ClassIdentifier) ident).reachableIdentifier
		    (method.substring(0, index), method.substring(index+1), 
		     isVirtual);
	    }
	}
    }

    public void preserveIdentifier(String fqn) {
	int index = fqn.indexOf('.');
	String component = index == -1 ? fqn : fqn.substring(0, index);
	Identifier ident = getIdentifier(component);
	if (ident == null)
	    return;
	ident.setReachable();
	ident.setPreserved();
	if (index == -1)
	    return;

	if (ident instanceof PackageIdentifier)
	    ((PackageIdentifier) ident).preserveIdentifier
		(fqn.substring(index+1));
	else {
	    String method = fqn.substring(index+1);
	    index = method.indexOf('.');
	    if (index == -1) {
		((ClassIdentifier) ident).reachableIdentifier
		    (method, null, false);
		((ClassIdentifier) ident).preserveIdentifier
		    (method, null);
	    } else {
		((ClassIdentifier) ident).reachableIdentifier
		    (method.substring(0, index), method.substring(index+1), 
		     false);
		((ClassIdentifier) ident).preserveIdentifier
		    (method.substring(0, index), method.substring(index+1));
	    }
	}
    }

    /**
     * @return the full qualified name, including trailing dot.
     */
    public String getFullName() {
	if (parent != null)
	    return parent.getFullName() + getName() + ".";
	else
	    return "";
    }

    /**
     * @return the full qualified alias, including trailing dot.
     */
    public String getFullAlias() {
	if (parent != null)
	    return parent.getFullAlias() + getAlias() + ".";
	else
	    return "";
    }

    public String findAlias(String className) {
	int index = className.indexOf('.');
	if (index == -1) {
	    Identifier ident = getIdentifier(className);
	    if (ident != null)
		return ident.getFullAlias();
	} else {
	    Identifier pack = getIdentifier(className.substring(0, index));
	    if (pack != null)
		return ((PackageIdentifier)pack)
		    .findAlias(className.substring(index+1));
	}
	return className;
    }

    public void buildTable(int renameRule) {
	super.buildTable(renameRule);
	Enumeration enum = loadedClasses.elements();
	while (enum.hasMoreElements()) {
	    Identifier ident = (Identifier) enum.nextElement();
	    if (ident instanceof ClassIdentifier) {
		((ClassIdentifier) ident).buildTable(renameRule);
	    } else
		((PackageIdentifier) ident).buildTable(renameRule);
	}
    }

    public void readTable(Hashtable table) {
	if (parent != null)
	    setAlias((String) table.get(parent.getFullName() + getName()));
	Enumeration enum = loadedClasses.elements();
	while (enum.hasMoreElements()) {
	    Identifier ident = (Identifier) enum.nextElement();
	    if (!Obfuscator.shouldStrip || ident.isReachable())
		ident.readTable(table);
	}
    }

    public void writeTable(Hashtable table) {
	if (parent != null)
	    table.put(parent.getFullAlias() + getAlias(), getName());
	Enumeration enum = loadedClasses.elements();
	while (enum.hasMoreElements()) {
	    Identifier ident = (Identifier) enum.nextElement();
	    if (!Obfuscator.shouldStrip || ident.isReachable())
		ident.writeTable(table);
	}
    }

    public Identifier getParent() {
	return parent;
    }

    public String getName() {
	return name;
    }

    public String getType() {
	return "package";
    }

    public void storeClasses(ZipOutputStream zip) {
	Enumeration enum = loadedClasses.elements();
	while (enum.hasMoreElements()) {
	    Identifier ident = (Identifier) enum.nextElement();
	    if (Obfuscator.shouldStrip && !ident.isReachable()) {
		if (Obfuscator.isDebugging)
		    Obfuscator.err.println("Class/Package "
					   + ident.getFullName()
					   + " is not reachable");
		continue;
	    }
	    if (ident instanceof PackageIdentifier)
		((PackageIdentifier) ident)
		    .storeClasses(zip);
	    else {
		try {
		    String filename = ident.getFullAlias().replace('.','/')
			+ ".class";
		    zip.putNextEntry(new ZipEntry(filename));
		    DataOutputStream out = new DataOutputStream(zip);
		    ((ClassIdentifier) ident).storeClass(out);
		    out.flush();
		    zip.closeEntry();
		} catch (java.io.IOException ex) {
		    Obfuscator.err.println("Can't write Class "
					   + ident.getName());
		    ex.printStackTrace(Obfuscator.err);
		}
	    }
	}
    }

    public void storeClasses(File destination) {
	File newDest = (parent == null) ? destination 
	    : new File(destination, getAlias());
	if (!newDest.exists() && !newDest.mkdir()) {
	    Obfuscator.err.println("Could not create directory "
				   +newDest.getPath()+", check permissions.");
	}
	Enumeration enum = loadedClasses.elements();
	while (enum.hasMoreElements()) {
	    Identifier ident = (Identifier) enum.nextElement();
	    if (Obfuscator.shouldStrip && !ident.isReachable()) {
		if (Obfuscator.isDebugging)
		    Obfuscator.err.println("Class/Package "
					   + ident.getFullName()
					   + " is not reachable");
		continue;
	    }
	    if (ident instanceof PackageIdentifier)
		((PackageIdentifier) ident)
		    .storeClasses(newDest);
	    else {
		try {
		    File file = new File(newDest, ident.getAlias()+".class");
// 		    if (file.exists()) {
// 			Obfuscator.err.println
// 			    ("Refuse to overwrite existing class file "
// 			     +file.getPath()+".  Remove it first.");
// 			return;
// 		    }
		    DataOutputStream out = new DataOutputStream
			(new FileOutputStream(file));
		    ((ClassIdentifier) ident).storeClass(out);
		    out.close();
		} catch (java.io.IOException ex) {
		    Obfuscator.err.println("Can't write Class "
					   + ident.getName());
		    ex.printStackTrace(Obfuscator.err);
		}
	    }
	}
    }

    public String toString() {
	return (parent == null) ? "base package" 
	    : parent.getFullName()+getName();
    }

    public boolean contains(String newAlias) {
	Enumeration enum = loadedClasses.elements();
	while (enum.hasMoreElements()) {
	    Identifier ident = (Identifier)enum.nextElement();
	    if ((!Obfuscator.shouldStrip || ident.isReachable())
		&& ident.getAlias().equals(newAlias))
		return true;
	}
	return false;
    }

    public boolean conflicting(String newAlias, boolean strong) {
	return parent.contains(newAlias);
    }
}
