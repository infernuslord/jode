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

public class JodeEnvironment {
    Hashtable imports = new Hashtable();
    ClassAnalyzer main;
    String className;
    String pkg;

    SearchPath classPath;

    JodeEnvironment() {
	Type.setEnvironment(this);
        classPath = new SearchPath(System.getProperty("java.class.path"));
    }

    public java.io.InputStream getClassStream(Class clazz) 
        throws java.io.IOException {
        return classPath.getFile(clazz.getName().
                                 replace('.', java.io.File.separatorChar)
                                 +".class");
        
    }

    public void dumpHeader(TabbedPrintWriter writer) 
         throws java.io.IOException
    {
        writer.println("/* "+ className + " - Decompiled by JoDe (Jochen's Decompiler)\n * Send comments or bug reports to Jochen Hoenicke <jochenh@bigfoot.com>\n */");
        if (pkg.length() != 0)
            writer.println("package "+pkg+";");

        Enumeration enum = imports.keys();
        while (enum.hasMoreElements()) {
            String importName = (String) enum.nextElement();
            Integer vote = (Integer) imports.get(importName);
            if (vote.intValue() >=
                (importName.endsWith(".*")
                 ? Decompiler.importPackageLimit
                 : Decompiler.importClassLimit))
                writer.println("import "+importName+";");
        }
        writer.println("");
    }

    public void error(String message) {
        System.err.println(message);
    }

    public void doClass(String className) 
    {
        Class clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException ex) {
            System.err.println("Class `"+className+"' not found");
            return;
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
    public void useClass(Class clazz) {
        String name = clazz.getName();
        int pkgdelim = name.lastIndexOf('.');
        if (pkgdelim != -1) {
            String pkgName = name.substring(0, pkgdelim);
            if (pkgName.equals(pkg)
                || pkgName.equals("java.lang"))
                return;
            Integer i = (Integer) imports.get(pkgName+".*");
            i = (i == null)? new Integer(1): new Integer(i.intValue()+1);
            imports.put(pkgName+".*", i);
            if (i.intValue() >= Decompiler.importPackageLimit)
                return;
            
            i = (Integer) imports.get(name);
            i = (i == null)? new Integer(1): new Integer(i.intValue()+1);
            imports.put(name, i);
        }
    }

    /**
     * Check if clazz is imported and maybe remove package delimiter from
     * full qualified class name.
     * <p>
     * Known Bug: If the same class name is in more than one imported package
     * the name should be qualified, but isn't.
     * @return a legal string representation of clazz.  
     */
    public String classString(Class clazz) {
        String name = clazz.getName();
        int pkgdelim = name.lastIndexOf('.');
        if (pkgdelim != -1) {
            String pkgName = name.substring(0, pkgdelim);

            Integer i;
            if (pkgName.equals(pkg) 
                || pkgName.equals("java.lang")
                || ( (i = (Integer)imports.get(pkgName+".*")) != null
                     && i.intValue() >= Decompiler.importPackageLimit )
                || ( (i = (Integer)imports.get(name)) != null
                     && i.intValue() >= Decompiler.importClassLimit )) {
                return  name.substring(pkgdelim+1);
            }
        }
        return name;
    }

    protected int loadFileFlags()
    {
        return 1;
    }
}
