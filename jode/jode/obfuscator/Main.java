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
import java.io.PrintStream;

public class Obfuscator {
    public static boolean isVerbose = false;
    public static boolean isDebugging = false;

    public static boolean shouldStrip = true;
    public static boolean swapOrder   = false;
    public static boolean preserveSerial = true;

    public static PrintStream err = System.err;
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
        err.println("usage: jode.Obfuscator flags* [class | package]*");
        err.println("\t[-v]               "+"Verbose output");
        err.println("\t[-debug]           "+"Debugging");
        err.println("\t[-nostrip]         "+
		    "Don't strip not needed methods");
        
        err.println("\t[-cp <classpath>]  "+
                           "The class path; should contain classes.zip");
        err.println("\t[-d <directory>]   "+
		    "Destination directory for output classes");
        err.println("Preserve options: ");
        err.println("\t[-package]         "+
		    "Preserve all package members");
        err.println("\t[-protected]       "+
		    "Preserve all protected members");
        err.println("\t[-public]          "+
		    "Preserve all public members");
        err.println("\t[-preserve <name>] "+
		    "Preserve only the given name (allowed multiple times)");
	err.println("\t[-breakserial]     "+
		    "Allow the serialized form to change");
        err.println("Obfuscating options: ");
        err.println("\t[-strong]          "+
		    "Rename identifiers to random unicode identifier");
        err.println("\t[-weak]            "+
		    "Rename to random, but legal java identifier");
        err.println("\t[-unique]          "+
		    "Rename to unique legal java identifier");
        err.println("\t[-none]            "+
		    "Don't rename any method.");
        err.println("\t[-table <file>]    "+
		    "Read translation table from file");
        err.println("\t[-revtable <file>] "+
		    "Write reversed translation table to file");
        err.println("\t[-swaporder]       "+
		    "Swap the order of fields and methods.");
    }

    public static void main(String[] params) {
        int i;
        String sourcePath = System.getProperty("java.class.path");
        String destPath = ".";

        Vector preservedIdents = new Vector();

        int preserveRule = PRESERVE_NONE;
        int rename       = RENAME_WEAK;
        String table = null;
        String toTable = null;

        for (i=0; i<params.length && params[i].startsWith("-"); i++) {
            if (params[i].equals("-v"))
                isVerbose = true;
            else if (params[i].equals("-debug"))
                isDebugging = true;
            else if (params[i].equals("-nostrip"))
                shouldStrip = false;

            else if (params[i].equals("-sourcepath")
		     || params[i].equals("-classpath")
		     || params[i].equals("-cp"))
                sourcePath = params[++i];
            else if (params[i].equals("-dest")
		     || params[i].equals("-d"))
                destPath   = params[++i];

            /* Preserve options */
            else if (params[i].equals("-package"))
                preserveRule = PRESERVE_PACKAGE;
            else if (params[i].equals("-protected"))
                preserveRule = PRESERVE_PROTECTED;
            else if (params[i].equals("-public"))
                preserveRule = PRESERVE_PUBLIC;
            else if (params[i].equals("-preserve")) {
                String ident = params[++i];
                preservedIdents.addElement(ident);
            }
            else if (params[i].equals("-breakserial"))
		preserveSerial = false;

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
            else if (params[i].equals("-swaporder"))
		swapOrder = true;
            else if (params[i].equals("--")) {
                i++;
                break;
            } else {
                if (!params[i].startsWith("-h"))
                    err.println("Unknown option: "+params[i]);
                usage();
                return;
            }
        }
        if (i == params.length) {
            err.println("No package or classes specified.");
            usage();
            return;
        }

        ClassInfo.setClassPath(sourcePath);
        ClassBundle bundle = new ClassBundle();
        for (; i< params.length; i++)
            bundle.loadClasses(params[i]);

	err.println("Computing reachable / preserved settings");
        bundle.setPreserved(preserveRule, preservedIdents);

	err.println("Renaming methods");
        if (rename != RENAME_TABLE)
            bundle.buildTable(rename);
        else
            bundle.readTable(table);
        if (toTable != null)
            bundle.writeTable(toTable);

	err.println("Writing new classes");
        bundle.storeClasses(destPath);
    }
}
