/* ClassBundle Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.bytecode.Reference;
import java.io.*;
import java.util.*;
import java.util.zip.ZipOutputStream;

public class ClassBundle {

    int preserveRule;
    PackageIdentifier basePackage;
    /**
     * the identifiers that must be analyzed.
     */
///#ifdef JDK12
///    Set toAnalyze = new HashSet();
///#else
    Stack toAnalyze = new Stack();
///#endif

    public ClassBundle() {
	basePackage = new PackageIdentifier(this, null, "", false);
	basePackage.setReachable();
	basePackage.setPreserved();
    }

    public String getTypeAlias(String typeSig) {
	StringBuffer newSig = new StringBuffer();
	int index = 0, nextindex;
	while ((nextindex = typeSig.indexOf('L', index)) != -1) {
	    newSig.append(typeSig.substring(index, nextindex+1));
	    index = typeSig.indexOf(';', nextindex);
	    String alias = basePackage.findAlias
		(typeSig.substring(nextindex+1, index).replace('/','.'));
	    newSig.append(alias.replace('.', '/'));
	}
	return newSig.append(typeSig.substring(index)).toString();
    }

    public ClassIdentifier getClassIdentifier(String name) {
	return (ClassIdentifier) basePackage.getIdentifier(name);
    }

    public Identifier getIdentifier(Reference ref) {
	String clName = ref.getClazz();
	if (clName.charAt(0) == '[')
	    /* Can't represent arrays */
	    return null;
	ClassIdentifier ident =
	    getClassIdentifier(clName.substring(1, clName.length()-1)
			       .replace('/','.'));
	if (ident == null)
	    return null;
	return ident.getIdentifier(ref.getName(), ref.getType());
    }

    public void loadClasses(String wildcard) {
	basePackage.loadMatchingClasses(new WildCard(wildcard));
    }

    public void reachableIdentifier(String fqn, boolean isVirtual) {
	basePackage.reachableIdentifier(fqn, isVirtual);
    }

    public void setPreserved(int preserveRule, Vector fullqualifiednames) {
	this.preserveRule = preserveRule;

	basePackage.applyPreserveRule(preserveRule);
	Enumeration enum = fullqualifiednames.elements();
	while (enum.hasMoreElements()) {
	    basePackage.preserveMatchingIdentifier
		(new WildCard((String) enum.nextElement()));
	}
	analyze();
    }

///#ifdef JDK12
///    public void analyzeIdentifier(Identifier i) {
///	if (!toAnalyze.contains(i))
///	    toAnalyze.add(i);
///    }
///
///    public void analyze() {
///	while(!toAnalyze.isEmpty()) {
///	    Identifier ident = (Identifier) toAnalyze.iterator().next();
///	    toAnalyze.remove(ident);
///	    ident.analyze();
///	}
///    }
///#else
    public void analyzeIdentifier(Identifier i) {
	if (!toAnalyze.contains(i))
	    toAnalyze.addElement(i);
    }

    public void analyze() {
	while (!toAnalyze.isEmpty())
	    ((Identifier) toAnalyze.pop()).analyze();
    }
///#endif
    
    public void buildTable(int renameRule) {
	basePackage.buildTable(renameRule);
    }

    public void readTable(String filename) {
	Properties prop = new Properties();
	try {
	    InputStream input = new FileInputStream(filename);
	    prop.load(input);
	    input.close();
	} catch (java.io.IOException ex) {
	    Obfuscator.err.println("Can't read rename table "+filename);
	    ex.printStackTrace(Obfuscator.err);
	}
	basePackage.readTable(prop);
    }

    public void writeTable(String filename) {
	Properties prop = new Properties();
	basePackage.writeTable(prop);
	try {
	    OutputStream out = new FileOutputStream(filename);
	    prop.save(out, "Reverse renaming table");
	    out.close();
	} catch (java.io.IOException ex) {
	    Obfuscator.err.println("Can't write rename table "+filename);
	    ex.printStackTrace(Obfuscator.err);
	}
    }

    public void storeClasses(String destination) {
	if (destination.endsWith(".jar") ||
	    destination.endsWith(".zip")) {
	    try {
		ZipOutputStream zip = new ZipOutputStream
		    (new FileOutputStream(destination));
		basePackage.storeClasses(zip);
		zip.close();
	    } catch (IOException ex) {
		System.err.println("Can't write zip file: "+destination);
		ex.printStackTrace(Obfuscator.err);
	    }
	} else {
	    File directory = new File(destination);
	    if (!directory.exists()) {
		Obfuscator.err.println("Destination directory "
				       +directory.getPath()
				       +" doesn't exists.");
		return;
	    }
	    basePackage.storeClasses(new File(destination));
	}
    }
}
