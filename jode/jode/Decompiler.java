/* Decompiler Copyright (C) 1998-1999 Jochen Hoenicke.
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
import java.io.*;
import jode.decompiler.TabbedPrintWriter;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;

public class Decompiler {
    public final static String version = "0.99";
    public final static String email = "jochen@gnu.org";
    public final static String copyright = 
	"Jode (c) 1998,1999 Jochen Hoenicke <"+email+">";
    
    public final static int TAB_SIZE_MASK = 0x0f;
    public final static int BRACE_AT_EOL  = 0x10;
    public final static int SUN_STYLE = 0x14;
    public final static int GNU_STYLE = 0x02;

    public static PrintStream err = System.err;
    public static boolean isVerbose = false;
    public static boolean isDebugging = false;
    public static boolean isTypeDebugging = false;
    public static boolean isFlowDebugging = false;
    public static boolean usePUSH = false;
    public static boolean debugInOut = false;
    public static boolean debugAnalyze = false;
    public static boolean showLVT = false;
    public static boolean useLVT = true;
    public static boolean doChecks = false;
    public static boolean prettyLocals = false;
    public static boolean immediateOutput = false;
    public static boolean highlevelTrafos = true;
    public static boolean stringDecrypting = true;
    public static boolean undoOptimizations = true;
    public static boolean removeOnetimeLocals = false;
    public static int outputStyle = SUN_STYLE;
    public static int importPackageLimit = 3;
    public static int importClassLimit = 3;

    public static void usage() {
	err.println("Version: " + version);
        err.println("use: jode [-v][--dest <destdir>]"
			   +"[--imm][--pretty]"
			   +"[--cp <classpath>]"
		           +"[--nolvt][--usepush][--nodecrypt]"
                           +"[--import <pkglimit> <clslimit>]"
		           +"[--debug][--analyze][--flow]"
			   +"[--type][--inout][--lvt][--check]"
                           +" class1 [class2 ...]");
	err.println("\t-v               "+
		    "show progress.");
	err.println("\t--dest <destdir> "+
	    "write decompiled files to disk into directory destdir.");
	err.println("\t--imm            "+
		    "output source immediately with wrong import.");
	err.println("\t--pretty         "+
		    "use `pretty' names for local variables.");
	err.println("\t--cp <classpath> "+
		    "search for classes in specified classpath.");
	err.println("\t--nolvt          "+
		    "don't use the local variable table.");
	err.println("\t--usepush        "+
		    "don't remove non compilable PUSH instrucions.");
	err.println("\t--nodecrypt      "+
		    "don't try to decrypt encrypted strings.");
	err.println("\t--style {sun|gnu}"+
		    " specifies indentation style");
	err.println("\t--import <pkglimit> <clslimit>");
	err.println("\t                 "+
	    "import classes used more than clslimit times");
	err.println("\t                 "+
	    "and packages with more then pkglimit used classes");
	err.println("Debugging options, mainly used to debug this decompiler:");
	err.println("\t--debug          "+
		    "output some debugging messages into the source code.");
	err.println("\t--analyze        "+
		    "show analyzation order of flow blocks.");
	err.println("\t--flow           "+
		    "show flow block merging.");
	err.println("\t--type           "+
		    "show how types are guessed.");
	err.println("\t--inout          "+
		    "show T1/T2 in/out-set analysis.");
	err.println("\t--lvt            "+
		    "dump the local variable table.");
	err.println("\t--check          "+
		    "do flow block sanity checks.");
    }

    public static void main(String[] params) {
        int i;
        String classPath = System.getProperty("java.class.path");
	File destDir = null;
	ZipOutputStream destZip = null;
	err.println(copyright);
        for (i=0; i<params.length && params[i].startsWith("-"); i++) {
            if (params[i].equals("-v"))
                isVerbose = true;
            else if (params[i].equals("--imm"))
                immediateOutput = true;
	    else if (params[i].equals("--dest"))
		destDir = new File(params[++i]);
            else if (params[i].equals("--debug"))
                isDebugging = true;
            else if (params[i].equals("--type"))
                isTypeDebugging = true;
            else if (params[i].equals("--analyze"))
                debugAnalyze = true;
            else if (params[i].equals("--flow"))
                isFlowDebugging = true;
            else if (params[i].equals("--inout"))
                debugInOut = true;
            else if (params[i].equals("--nolvt"))
                useLVT = false;
            else if (params[i].equals("--usepush"))
                usePUSH = true;
            else if (params[i].equals("--lvt"))
                showLVT = true;
            else if (params[i].equals("--check"))
                doChecks = true;
            else if (params[i].equals("--pretty"))
                prettyLocals = true;
            else if (params[i].equals("--nodecrypt"))
                stringDecrypting = false;
            else if (params[i].equals("--style")) {
		String style = params[++i];
		if (style.equals("sun"))
		    outputStyle = SUN_STYLE;
		else if (style.equals("gnu"))
		    outputStyle = GNU_STYLE;
		else {
		    err.println("Unknown style: "+style);
		    usage();
		    return;
		}
            } else if (params[i].equals("--import")) {
                importPackageLimit = Integer.parseInt(params[++i]);
                importClassLimit = Integer.parseInt(params[++i]);
            } else if (params[i].equals("--cp")) {
                classPath = params[++i];
            } else if (params[i].equals("--")) {
                i++;
                break;
            } else {
                if (!params[i].startsWith("-h") &&
		    !params[i].equals("--help"))
                    err.println("Unknown option: "+params[i]);
                usage();
                return;
            }
        }
        if (i == params.length) {
            usage();
            return;
        }
        JodeEnvironment env = new JodeEnvironment(classPath);
	TabbedPrintWriter writer = null;
	if (destDir == null)
	    writer = new TabbedPrintWriter(System.out);
	else if (destDir.getName().endsWith(".zip")) {
	    try {
		destZip = new ZipOutputStream(new FileOutputStream(destDir));
	    } catch (IOException ex) {
		err.println("Can't open zip file "+destDir);
		ex.printStackTrace(err);
		return;
	    }
	    writer = new TabbedPrintWriter(destZip);
	}
        for (; i< params.length; i++) {
	    try {
		String filename = 
		    params[i].replace('.', File.separatorChar)+".java";
		if (destZip != null) {
		    destZip.putNextEntry(new ZipEntry(filename));
		} else if (destDir != null) {
		    File file = new File (destDir, filename);
		    File directory = new File(file.getParent());
		    if (!directory.exists() && !directory.mkdirs()) {
			err.println("Could not create directory "
				    +directory.getPath()+", "
				    +"check permissions.");
		    }
		    writer = new TabbedPrintWriter(new FileOutputStream(file));
		}
		env.doClass(params[i], writer);
		if (destZip != null) {
		    writer.flush();
		    destZip.closeEntry();
		} else if (destDir != null)
		    writer.close();
		/* Now is a good time to clean up */
		System.gc();
	    } catch (IOException ex) {
		err.println("Can't write source of "+params[i]+".");
		err.println("Check the permissions.");
		ex.printStackTrace(err);
	    }
	}
	if (destZip != null) {
	    try {
		destZip.close();
	    } catch (IOException ex) {
		err.println("Can't close Zipfile");
		ex.printStackTrace(err);
	    }
	}
    }
}
