/* ImportHandler Copyright (C) 1998-1999 Jochen Hoenicke.
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

package jode.decompiler;
import jode.GlobalOptions;
import jode.bytecode.ClassInfo;
import jode.type.*;

import java.io.IOException;
import java.util.*;

public class ImportHandler {
    Hashtable imports;
    /* Classes that doesn't need to be qualified. */
    Hashtable cachedClassNames = null;
    ClassAnalyzer main;
    String className;
    String pkg;

    int importPackageLimit;
    int importClassLimit;

    public ImportHandler() {
	this(3,3);
    }
    
    public ImportHandler(int packageLimit, int classLimit) {
	importPackageLimit = packageLimit;
	importClassLimit = classLimit;
    }

    /**
     * Checks if the className conflicts with a class imported from
     * another package and must be fully qualified therefore.
     * The imports must should have been cleaned up before.
     * <p>
     * Known Bug: If a class, local, field or method with the same
     * name as the package of className exists, using the fully
     * qualified name is no solution.  This sometimes can't be fixed
     * at all (except by renaming the package).  It happens only in
     * ambigous contexts, namely static field/method access.
     * @param name The full qualified class name.
     * @return true if this className must be printed fully qualified.  
     */
    private boolean conflictsImport(String name) {
        int pkgdelim = name.lastIndexOf('.');
        if (pkgdelim != -1) {
            String pkgName = name.substring(0, pkgdelim);
            /* All classes in this package doesn't conflict */
            if (pkgName.equals(pkg))
                return false;

            // name without package, but _including_ leading dot.
            name = name.substring(pkgdelim); 

            if (pkg.length() != 0) {
                if (ClassInfo.exists(pkg+name))
                    return true;
            }

            Enumeration enum = imports.keys();
            while (enum.hasMoreElements()) {
                String importName = (String) enum.nextElement();
                if (importName.endsWith(".*")) {
                    /* strip the "*" */
                    importName = importName.substring
                        (0, importName.length()-2);
                    if (!importName.equals(pkgName)) {
                        if (ClassInfo.exists(importName))
                            return true;
                    }
                }
            }
        }
        return false;
    }

    private void cleanUpImports() {
        Integer dummyVote = new Integer(Integer.MAX_VALUE);
        Hashtable newImports = new Hashtable();
        Vector classImports = new Vector();
        Enumeration enum = imports.keys();
        while (enum.hasMoreElements()) {
            String importName = (String) enum.nextElement();
            Integer vote = (Integer) imports.get(importName);
            if (!importName.endsWith(".*")) {
                if (vote.intValue() < importClassLimit)
                    continue;
                int delim = importName.lastIndexOf(".");
                Integer pkgvote = (Integer)
                    imports.get(importName.substring(0, delim)+".*");
                if (pkgvote.intValue() >= importPackageLimit)
                    continue;

                /* This is a single Class import, that is not
                 * superseeded by a package import.  Mark it for
                 * importation, but don't put it in newImports, yet.  
                 */
                classImports.addElement(importName);
            } else {
                if (vote.intValue() < importPackageLimit)
                    continue;
                newImports.put(importName, dummyVote);
            }
        }

        imports = newImports;
        cachedClassNames = new Hashtable();
        /* Now check if the class import conflict with any of the
         * package imports.
         */
        enum = classImports.elements();
        while (enum.hasMoreElements()) {
            /* If there are more than one single class imports with
             * the same name, exactly the first (in hash order) will
             * be imported. */
            String className = (String) enum.nextElement();
            if (!conflictsImport(className)) {
                imports.put(className, dummyVote);
                String name = 
                    className.substring(className.lastIndexOf('.')+1);
                cachedClassNames.put(className, name);
            }
        }
    }

    public void dumpHeader(TabbedPrintWriter writer) 
         throws java.io.IOException
    {
        writer.println("/* "+ className 
		       + " - Decompiled by JoDe (Jochen's Decompiler)");
	writer.println(" * Send comments or bug reports to "
		       + GlobalOptions.email);
	writer.println(" */");
        if (pkg.length() != 0)
            writer.println("package "+pkg+";");

        cleanUpImports();
        Enumeration enum = imports.keys();
        while (enum.hasMoreElements()) {
            String pkgName = (String)enum.nextElement();
            if (!pkgName.equals("java.lang.*"))
                writer.println("import "+pkgName+";");
        }
        writer.println("");
    }

    public void error(String message) {
        GlobalOptions.err.println(message);
    }

    public void init(String className) {
        imports = new Hashtable();
        /* java.lang is always imported */
        imports.put("java.lang.*", new Integer(Integer.MAX_VALUE));

        int pkgdelim = className.lastIndexOf('.');
        pkg = (pkgdelim == -1)? "" : className.substring(0, pkgdelim);
        this.className = (pkgdelim == -1) ? className
            : className.substring(pkgdelim+1);
    }

    /* Marks the clazz as used, so that it will be imported if used often
     * enough.
     */
    public void useClass(ClassInfo clazz) {
	String name = clazz.getName();
        int pkgdelim = name.lastIndexOf('.');
        if (pkgdelim != -1) {
            String pkgName = name.substring(0, pkgdelim);
            if (pkgName.equals(pkg))
                return;

            Integer pkgVote = (Integer) imports.get(pkgName+".*");
            if (pkgVote != null 
                && pkgVote.intValue() >= importPackageLimit)
                return;

            Integer i = (Integer) imports.get(name);
            if (i == null) {
                /* This class wasn't imported before.  Mark the whole package
                 * as used once more. */

                pkgVote = (pkgVote == null)
                    ? new Integer(1): new Integer(pkgVote.intValue()+1);
                imports.put(pkgName+".*", pkgVote);
                i = new Integer(1);

            } else {
                if (i.intValue() >= importClassLimit)
                    return;
                i = new Integer(i.intValue()+1);
            }
            imports.put(name, i);
        }
    }

    public final void useType(Type type) {
	if (type instanceof ArrayType)
	    useType(((ArrayType) type).getElementType());
	else if (type instanceof ClassInterfacesType)
	    useClass(((ClassInterfacesType) type).getClassInfo());
    }

    /**
     * Check if clazz is imported and maybe remove package delimiter from
     * full qualified class name.
     * <p>
     * Known Bug 1: If this is called before the imports are cleaned up,
     * (that is only for debugging messages), the result is unpredictable.
     * <p>
     * Known Bug 2: It is not checked if the class name conflicts with
     * a local variable, field or method name.  This is very unlikely
     * since the java standard has different naming convention for those
     * names. (But maybe an intelligent obfuscator may use this fact.)
     * This can only happen with static fields or static methods.
     * @return a legal string representation of clazz.  
     */
    public String getClassString(ClassInfo clazz) {
	String name = clazz.getName();
        if (cachedClassNames == null)
            /* We are not yet clean, return the full name */
            return name;

        /* First look in our cache. */
        String cached = (String) cachedClassNames.get(name);
        if (cached != null)
            return cached;

        int pkgdelim = name.lastIndexOf('.');
        if (pkgdelim != -1) {
                
            String pkgName = name.substring(0, pkgdelim);

            Integer i;
            if (pkgName.equals(pkg)
                || (imports.get(pkgName+".*") != null
                    && !conflictsImport(name))) {
                String result = name.substring(pkgdelim+1);
                cachedClassNames.put(name, result);
                return result;
            }
        }
        cachedClassNames.put(name, name);
        return name;
    }

    public String getTypeString(Type type) {
	if (type instanceof ArrayType)
	    return getTypeString(((ArrayType) type).getElementType()) + "[]";
	else if (type instanceof ClassInterfacesType)
	    return getClassString(((ClassInterfacesType) type).getClassInfo());
	else if (type instanceof NullType)
	    return "Object";
	else
	    return type.toString();
    }

    protected int loadFileFlags()
    {
        return 1;
    }
}


