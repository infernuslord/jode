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
import jode.GlobalOptions;
import jode.bytecode.ClassInfo;
import jode.bytecode.Reference;
import java.io.*;
import java.util.zip.ZipOutputStream;

///#ifdef JDK12
///import java.util.Collection;
///import java.util.Iterator;
///import java.util.Set;
///import java.util.HashSet;
///import java.util.Map;
///import java.util.TreeMap;
///import java.util.WeakHashMap;
///#else
import jode.util.Collection;
import jode.util.Iterator;
import jode.util.Set;
import jode.util.HashSet;
import jode.util.Map;
import jode.util.TreeMap;
import jode.util.HashMap;
///#endif

public class ClassBundle {

    ModifierMatcher preserveRule = null;
    PackageIdentifier basePackage;
    /**
     * the identifiers that must be analyzed.
     */
    Set toAnalyze = new HashSet();

    public ClassBundle() {
	basePackage = new PackageIdentifier(this, null, "", false);
	basePackage.setReachable();
	basePackage.setPreserved();
    }

///#ifdef JDK12
///    private static final Map aliasesHash = new WeakHashMap();
///#else
    private static final Map aliasesHash = new HashMap();
///#endif

    public Reference getReferenceAlias(Reference ref) {
	Reference alias = (Reference) aliasesHash.get(ref);
	if (alias == null) {
	    Identifier ident = getIdentifier(ref);
	    String newType = getTypeAlias(ref.getType());
	    if (ident == null)
		alias = Reference.getReference
		    (ref.getClazz(), ref.getName(), newType);
	    else 
		alias = Reference.getReference
		    ("L"+ident.getParent().getFullAlias().replace('.','/')+';',
		     ident.getAlias(), newType);
	    aliasesHash.put(ref, alias);
	}
	return alias;
    }

    public String getTypeAlias(String typeSig) {
	String alias = (String) aliasesHash.get(typeSig);
	if (alias == null) { 
	    StringBuffer newSig = new StringBuffer();
	    int index = 0, nextindex;
	    while ((nextindex = typeSig.indexOf('L', index)) != -1) {
		newSig.append(typeSig.substring(index, nextindex+1));
		index = typeSig.indexOf(';', nextindex);
		String typeAlias = basePackage.findAlias
		    (typeSig.substring(nextindex+1, index).replace('/','.'));
		newSig.append(typeAlias.replace('.', '/'));
	    }
	    alias = newSig.append(typeSig.substring(index))
		.toString().intern();
	    aliasesHash.put(typeSig, alias);
	}
	return alias;
    }

    public ClassIdentifier getClassIdentifier(String name) {
	ClassIdentifier ident
	    = (ClassIdentifier) basePackage.getIdentifier(name);
	return ident;
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
	System.err.println("Loading: "+wildcard);
	basePackage.loadMatchingClasses(new WildCard(wildcard));
    }

    public void reachableIdentifier(String fqn, boolean isVirtual) {
	basePackage.reachableIdentifier(fqn, isVirtual);
    }

    public void setPreserved(ModifierMatcher preserveRule, 
			     Collection fullqualifiednames) {
	this.preserveRule = preserveRule;

	basePackage.applyPreserveRule(preserveRule);
	for (Iterator i = fullqualifiednames.iterator(); i.hasNext(); ) {
	    basePackage.preserveMatchingIdentifier
		(new WildCard((String) i.next()));
	}
	analyze();
    }

    public void analyzeIdentifier(Identifier ident) {
	if (ident == null)
	    throw new NullPointerException();
	toAnalyze.add(ident);
    }

    public void analyze() {
	while(!toAnalyze.isEmpty()) {
	    Identifier ident = (Identifier) toAnalyze.iterator().next();
	    toAnalyze.remove(ident);
	    ident.analyze();
	}
    }
    
    public void buildTable(Renamer renameRule) {
	basePackage.buildTable(renameRule);
    }

    public void readTable(String filename) {
	try {
	    TranslationTable table = new TranslationTable();
	    InputStream input = new FileInputStream(filename);
	    table.load(input);
	    input.close();
	    basePackage.readTable(table);
	} catch (java.io.IOException ex) {
	    GlobalOptions.err.println("Can't read rename table "+filename);
	    ex.printStackTrace(GlobalOptions.err);
	}
    }

    public void writeTable(String filename) {
	TranslationTable table = new TranslationTable();
	basePackage.writeTable(table);
	try {
	    OutputStream out = new FileOutputStream(filename);
	    table.store(out);
	    out.close();
	} catch (java.io.IOException ex) {
	    GlobalOptions.err.println("Can't write rename table "+filename);
	    ex.printStackTrace(GlobalOptions.err);
	}
    }

    public void doTransformations() {
	basePackage.doTransformations();
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
		GlobalOptions.err.println
		    ("Can't write zip file: "+destination);
		ex.printStackTrace(GlobalOptions.err);
	    }
	} else {
	    File directory = new File(destination);
	    if (!directory.exists()) {
		GlobalOptions.err.println("Destination directory "
				       +directory.getPath()
				       +" doesn't exists.");
		return;
	    }
	    basePackage.storeClasses(new File(destination));
	}
    }
}
