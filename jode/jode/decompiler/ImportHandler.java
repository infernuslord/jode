/* 
 * JodeEnvironment (c) 1998 Jochen Hoenicke
 *
 * You may distribute under the terms of the GNU General Public License.
 *
 * IN NO EVENT SHALL JOCHEN HOENICKE BE LIABLE TO ANY PARTY FOR DIRECT,
 * INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT OF
 * THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF JOCHEN HOENICKE 
 * HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * JOCHEN HOENICKE SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS"
 * BASIS, AND JOCHEN HOENICKE HAS NO OBLIGATION TO PROVIDE MAINTENANCE,
 * SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * $Id$
 */

package jode;
import java.util.*;
import jode.bytecode.ClassHierarchy;
import gnu.bytecode.ClassType;

public class JodeEnvironment {
    Hashtable imports;
    /* Classes that doesn't need to be qualified. */
    Hashtable cachedClassNames = null;
    ClassAnalyzer main;
    String className;
    String pkg;

    SearchPath classPath;

    JodeEnvironment(String path) {
        classPath = new SearchPath(path);
        ClassHierarchy.setClassPath(classPath);
	Type.setEnvironment(this);
        imports = new Hashtable();
        /* java.lang is always imported */
        imports.put("java.lang.*", new Integer(Integer.MAX_VALUE));
    }

    public gnu.bytecode.ClassType getClassType(String clazzName) {
        try {
            return gnu.bytecode.ClassFileInput.readClassType
                (classPath.getFile(clazzName.replace('.', '/')
                                   +".class"));
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Class not found.");
        }
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
                if (classPath.exists((pkg+name).replace('.', '/')
                                     + ".class"))
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
                        if (classPath.exists(importName.replace('.', '/')
                                             + ".class"))
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
                if (vote.intValue() < Decompiler.importClassLimit)
                    continue;
                int delim = importName.lastIndexOf(".");
                Integer pkgvote = (Integer)
                    imports.get(importName.substring(0, delim)+".*");
                if (pkgvote.intValue() >= Decompiler.importPackageLimit)
                    continue;

                /* This is a single Class import, that is not
                 * superseeded by a package import.  Mark it for
                 * importation, but don't put it in newImports, yet.  
                 */
                classImports.addElement(importName);
            } else {
                if (vote.intValue() < Decompiler.importPackageLimit)
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

    private void dumpHeader(TabbedPrintWriter writer) 
         throws java.io.IOException
    {
        writer.println("/* "+ className + " - Decompiled by JoDe (Jochen's Decompiler)\n * Send comments or bug reports to Jochen Hoenicke <jochenh@bigfoot.com>\n */");
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
        System.err.println(message);
    }

    public void doClass(String className) 
    {
        ClassHierarchy clazz;
        try {
            clazz = ClassHierarchy.forName(className);
        } catch (IllegalArgumentException ex) {
            System.err.println("`"+className+"' is not a class name");
            return;
        }

        System.err.println(className);
        
        int pkgdelim = className.lastIndexOf('.');
        pkg = (pkgdelim == -1)? "" : className.substring(0, pkgdelim);
        this.className = (pkgdelim == -1) ? className
            : className.substring(pkgdelim+1);

        main = new ClassAnalyzer(null, clazz, this);
        main.analyze();

        TabbedPrintWriter writer = 
            new TabbedPrintWriter(System.out, "    ");
        try {
            dumpHeader(writer);
            main.dumpSource(writer);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    /* Marks the clazz as used, so that it will be imported if used often
     * enough.
     */
    public void useClass(String name) {
        int pkgdelim = name.lastIndexOf('.');
        if (pkgdelim != -1) {
            String pkgName = name.substring(0, pkgdelim);
            if (pkgName.equals(pkg))
                return;

            Integer pkgVote = (Integer) imports.get(pkgName+".*");
            if (pkgVote != null 
                && pkgVote.intValue() >= Decompiler.importPackageLimit)
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
                if (i.intValue() >= Decompiler.importClassLimit)
                    return;
                i = new Integer(i.intValue()+1);
            }
            imports.put(name, i);
        }
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
     * names. (But maybe a intelligent obfuscator may use this fact.)
     * This can only happen with static fields or static methods.
     * @return a legal string representation of clazz.  
     */
    public String classString(String name) {
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

    protected int loadFileFlags()
    {
        return 1;
    }
}
