/* Obfuscator Copyright (C) 1998-1999 Jochen Hoenicke.
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

package jode;
import jode.bytecode.ClassInfo;
import jode.obfuscator.*;
import java.util.Vector;
import java.lang.reflect.Modifier;
import java.io.PrintStream;

public class Obfuscator {
    public static boolean shouldStrip = true;
    public static boolean swapOrder   = false;
    public static boolean preserveSerial = true;

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

    public static void usage() {
	PrintStream err = GlobalOptions.err;
        err.println("usage: jode.Obfuscator flags* [class | package]*");
        err.println("\t-v                "+
		    "Verbose output (allowed multiple times).");
        err.println("\t--nostrip         "+
		    "Don't strip not needed methods");
        
        err.println("\t--cp <classpath>  "+
                           "The class path; should contain classes.zip");
        err.println("\t-d,--dest <directory>  "+
		    "Destination directory for output classes");
        err.println("Preserve options: ");
        err.println("\t--package         "+
		    "Preserve all package members");
        err.println("\t--protected       "+
		    "Preserve all protected members");
        err.println("\t--public          "+
		    "Preserve all public members");
        err.println("\t--preserve <name> "+
		    "Preserve only the given name (allowed multiple times)");
	err.println("\t--breakserial     "+
		    "Allow the serialized form to change");
        err.println("Obfuscating options: ");
        err.println("\t--rename={strong|weak|unique|none} "+
		    "Rename identifiers with given scheme");
        err.println("\t--table <file>    "+
		    "Read (some) translation table from file");
        err.println("\t--revtable <file> "+
		    "Write reversed translation table to file");
        err.println("\t--swaporder       "+
		    "Swap the order of fields and methods.");
	err.println("\t--debug=...       "+
		    "use --debug=help for more information.");
    }

    public static int parseRenameOption(String option) {
	if (option.equals("strong"))
	    return RENAME_STRONG;
	else if (option.equals("weak"))
	    return RENAME_WEAK;
	else if (option.equals("unique"))
	    return RENAME_UNIQUE;
	else if (option.equals("none"))
	    return RENAME_NONE;
	GlobalOptions.err.println("Incorrect value for --rename option: "
				  + option);
	usage();
	System.exit(0);
	return 0;
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
                GlobalOptions.verboseLevel++;
            else if (params[i].startsWith("--debug")) {
		String flags;
		if (params[i].startsWith("--debug=")) {
		    flags = params[i].substring(8);
		} else if (params[i].length() != 7) {
		    usage();
		    return;
		} else {
		    flags = params[++i];
		}
		GlobalOptions.setDebugging(flags);
            } else if (params[i].equals("--nostrip"))
                shouldStrip = false;

            else if (params[i].equals("--sourcepath")
		     || params[i].equals("--classpath")
		     || params[i].equals("--cp"))
                sourcePath = params[++i];
            else if (params[i].equals("--dest")
		     || params[i].equals("-d"))
                destPath   = params[++i];

            /* Preserve options */
            else if (params[i].equals("--package"))
                preserveRule = PRESERVE_PACKAGE;
            else if (params[i].equals("--protected"))
                preserveRule = PRESERVE_PROTECTED;
            else if (params[i].equals("--public"))
                preserveRule = PRESERVE_PUBLIC;
            else if (params[i].equals("--preserve")) {
                String ident = params[++i];
                preservedIdents.addElement(ident);
            }
            else if (params[i].equals("--breakserial"))
		preserveSerial = false;

            /* Obfuscate options */
            else if (params[i].equals("--rename"))
		rename = parseRenameOption(params[++i]);
	    else if (params[i].startsWith("--rename="))
		rename = parseRenameOption(params[i].substring(9));
            else if (params[i].equals("--table")) {
                table  = params[++i];
            }
            else if (params[i].equals("--revtable")) {
                toTable = params[++i];
            } 
            else if (params[i].equals("--swaporder"))
		swapOrder = true;
            else if (params[i].equals("--")) {
                i++;
                break;
            } else {
                if (!params[i].equals("-h") && !params[i].equals("--help"))
                    GlobalOptions.err.println("Unknown option: "+params[i]);
                usage();
                return;
            }
        }
        if (i == params.length) {
            GlobalOptions.err.println("No package or classes specified.");
            usage();
            return;
        }

	GlobalOptions.err.println("Loading classes");
        ClassInfo.setClassPath(sourcePath);
        ClassBundle bundle = new ClassBundle();
        for (; i< params.length; i++)
            bundle.loadClasses(params[i]);

	GlobalOptions.err.println("Computing reachable / preserved settings");
        bundle.setPreserved(preserveRule, preservedIdents);

	GlobalOptions.err.println("Renaming methods");
	if (table != null)
            bundle.readTable(table);
	bundle.buildTable(rename);
        if (toTable != null)
            bundle.writeTable(toTable);

	GlobalOptions.err.println("Transforming the classes");
	bundle.doTransformations();
	GlobalOptions.err.println("Writing new classes");
        bundle.storeClasses(destPath);
    }
}
