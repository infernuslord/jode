/* 
 * Obfuscator (c) 1998 Jochen Hoenicke
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
import jode.bytecode.ClassInfo;
import jode.obfuscator.*;
import java.util.Vector;
import java.lang.reflect.Modifier;

public class Obfuscator {
    public static boolean isVerbose = false;
    public static boolean isDebugging = false;

    public static final int PRESERVE_NONE = 0;
    public static final int PRESERVE_PUBLIC = Modifier.PUBLIC;
    public static final int PRESERVE_PROTECTED =
	PRESERVE_PUBLIC | Modifier.PROTECTED;
    public static final int PRESERVE_PACKAGE = 
	PRESERVE_PROTECTED | Modifier.PRIVATE; //XXX

    public static final int RENAME_STRONG = 0;
    public static final int RENAME_WEAK   = 1;
    public static final int RENAME_UNIQUE = 2;
    public static final int RENAME_NONE   = 3;
    public static final int RENAME_TABLE  = 4;

    public static void usage() {
        Decompiler.err.println("usage: jode.Obfuscator flags* [class | package]*");
        Decompiler.err.println("\t[-v]               "+"Verbose output");
        Decompiler.err.println("\t[-debug]           "+"Debugging");
        Decompiler.err.println("\t[-nostrip]         "+
                           "Don't strip not needed methods");
        
        
        Decompiler.err.println("\t[-sourcepath]      "+
                           "Colon-separated list of source-file directory");
        Decompiler.err.println("\t[-d <directory>]   "+
                           "Destination directory for output classes");
        Decompiler.err.println("Preserve options: ");
        Decompiler.err.println("\t[-package]         "+
                           "Preserve all package members");
        Decompiler.err.println("\t[-protected]       "+
                           "Preserve all protected members");
        Decompiler.err.println("\t[-public]          "+
                           "Preserve all public members");
        Decompiler.err.println("\t[-class <name>]    "+
                           "Preserve only the given class (allowed multiple times");
        Decompiler.err.println("\t[-method <name>]   "+
                           "Preserve only the given metod (allowed multiple times");
        Decompiler.err.println("Obfuscating options: ");
        Decompiler.err.println("\t[-strong]          "+
                           "Rename identifiers to random number/letters");
        Decompiler.err.println("\t[-weak]            "+
                           "Rename to random, but legal java identifier");
        Decompiler.err.println("\t[-unique]          "+
                           "Rename to unique legal java identifier");
        Decompiler.err.println("\t[-none]            "+
                           "Don't rename any method.");
        Decompiler.err.println("\t[-table <file>]    "+
                           "Read translation table from file");
        Decompiler.err.println("\t[-revtable <file>] "+
                           "Write reversed translation table to file");
    }

    public static void main(String[] params) {
        int i;
        String sourcePath = System.getProperty("java.class.path");
        String destPath = ".";

        Vector classnames  = new Vector();
        Vector methodnames = new Vector();

        boolean strip = true;
        int preserve = PRESERVE_NONE;
        int rename   = RENAME_WEAK;
        String table = null;
        String toTable = null;

        for (i=0; i<params.length && params[i].startsWith("-"); i++) {
            if (params[i].equals("-v"))
                isVerbose = true;
            else if (params[i].equals("-debug"))
                isDebugging = true;
            else if (params[i].equals("-nostrip"))
                strip = false;

            else if (params[i].equals("-sourcepath"))
                sourcePath = params[++i];
            else if (params[i].equals("-classpath"))
                sourcePath = params[++i];
            else if (params[i].equals("-d"))
                destPath   = params[++i];

            /* Preserve options */
            else if (params[i].equals("-package"))
                preserve = PRESERVE_PACKAGE;
            else if (params[i].equals("-protected"))
                preserve = PRESERVE_PROTECTED;
            else if (params[i].equals("-public"))
                preserve = PRESERVE_PUBLIC;
            else if (params[i].equals("-class")) {
                String className = params[++i];
                classnames.addElement(className);
            }
            else if (params[i].equals("-method")) {
                String methodName = params[++i];
                methodnames.addElement(methodName);
            }

            /* Obfuscate options */
            else if (params[i].equals("-strong"))
                rename = RENAME_STRONG;
            else if (params[i].equals("-weak"))
                rename = RENAME_WEAK;
            else if (params[i].equals("-unique"))
                rename = RENAME_UNIQUE;
            else if (params[i].equals("-none"))
                rename = RENAME_NONE;
            else if (params[i].equals("-table")) {
                rename = RENAME_TABLE;
                table  = params[++i];
            }
            else if (params[i].equals("-revtable")) {
                toTable = params[++i];
            } 
            else if (params[i].equals("--")) {
                i++;
                break;
            } else {
                if (!params[i].startsWith("-h"))
                    Decompiler.err.println("Unknown option: "+params[i]);
                usage();
                return;
            }
        }
        if (i == params.length) {
            Decompiler.err.println("No package or classes specified.");
            usage();
            return;
        }

        ClassInfo.setClassPath(sourcePath);
        ClassBundle bundle = new ClassBundle();
        for (; i< params.length; i++)
            bundle.loadClasses(params[i]);
        bundle.markPreserved(preserve, classnames, methodnames);

        if (strip)
            bundle.strip();

        if (rename != RENAME_TABLE)
            bundle.buildTable(rename);
        else
            bundle.readTable(table);
        if (toTable != null)
            bundle.writeTable(table);

        bundle.storeClasses(destPath);
    }
}
