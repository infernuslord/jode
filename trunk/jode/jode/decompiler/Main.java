/* Main Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.bytecode.ClassInfo;
import jode.bytecode.ClassPath;
import jode.GlobalOptions;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;

import gnu.getopt.LongOpt;
import gnu.getopt.Getopt;


public class Main extends Options {
    private static final int OPTION_START=0x10000;
    private static final int OPTION_END  =0x20000;

    private static final LongOpt[] longOptions = new LongOpt[] {
	new LongOpt("cp", LongOpt.REQUIRED_ARGUMENT, null, 'c'),
	new LongOpt("classpath", LongOpt.REQUIRED_ARGUMENT, null, 'c'),
	new LongOpt("dest", LongOpt.REQUIRED_ARGUMENT, null, 'd'),
	new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
	new LongOpt("version", LongOpt.NO_ARGUMENT, null, 'V'),
	new LongOpt("verbose", LongOpt.OPTIONAL_ARGUMENT, null, 'v'),
	new LongOpt("debug", LongOpt.OPTIONAL_ARGUMENT, null, 'D'),
	new LongOpt("import", LongOpt.REQUIRED_ARGUMENT, null, 'i'),
	new LongOpt("style", LongOpt.REQUIRED_ARGUMENT, null, 's'),
	new LongOpt("lvt", LongOpt.OPTIONAL_ARGUMENT, null, 
		    OPTION_START+0),
	new LongOpt("inner", LongOpt.OPTIONAL_ARGUMENT, null, 
		    OPTION_START+1),
	new LongOpt("anonymous", LongOpt.OPTIONAL_ARGUMENT, null, 
		    OPTION_START+2),
	new LongOpt("push", LongOpt.OPTIONAL_ARGUMENT, null, 
		    OPTION_START+3),
	new LongOpt("pretty", LongOpt.OPTIONAL_ARGUMENT, null, 
		    OPTION_START+4),
	new LongOpt("decrypt", LongOpt.OPTIONAL_ARGUMENT, null, 
		    OPTION_START+5),
	new LongOpt("onetime", LongOpt.OPTIONAL_ARGUMENT, null, 
		    OPTION_START+6),
	new LongOpt("immediate", LongOpt.OPTIONAL_ARGUMENT, null, 
		    OPTION_START+7),
	new LongOpt("verify", LongOpt.OPTIONAL_ARGUMENT, null, 
		    OPTION_START+8),
	new LongOpt("contrafo", LongOpt.OPTIONAL_ARGUMENT, null, 
		    OPTION_START+9)
    };

    public static void usage() {
	PrintWriter err = GlobalOptions.err;
	err.println("Version: " + GlobalOptions.version);
        err.println("Usage: java jode.decompiler.Main [OPTIONS]... [CLASSES]...");
	err.println("  -h, --help           "+
		    "show this information.");
	err.println("  -V, --version        "+
		    "output version information and exit.");
	err.println("  -v, --verbose        "+
		    "be verbose (multiple times means more verbose).");
	err.println("  -c, --classpath <path> "+
		    "search for classes in specified classpath.");
	err.println("                       "+
		    "The directories should be separated by ','.");
	err.println("  -d, --dest <dir>     "+
		    "write decompiled files to disk into directory destdir.");
	err.println("  -s, --style {sun|gnu}  "+
		    "specify indentation style");
	err.println("  -i, --import <pkglimit>,<clslimit>");
	err.println("                       "+
		    "import classes used more than clslimit times");
	err.println("                       "+
		    "and packages with more then pkglimit used classes.");
	err.println("                       "+
		    "Limit 0 means, never import, default is 0,1.");

	err.println("The following options can be turned on or off with `yes' or `no' argument.");
	err.println("      --inner          "+
		    "decompile inner classes (default).");
	err.println("      --anonymous      "+
		    "decompile anonymous classes (default).");
	err.println("      --contrafo       "+
		    "transform constructors of inner classes (default).");
	err.println("      --lvt            "+
		    "use the local variable table (default).");
	err.println("      --pretty         "+
		    "use `pretty' names for local variables.");
	err.println("      --push           "+
		    "allow PUSH instructions in output.");
	err.println("      --decrypt        "+
		    "decrypt encrypted strings (default).");
	err.println("      --onetime        "+
		    "remove locals, that are used only one time.");
	err.println("      --immediate      "+
		    "output source immediately (may produce buggy code).");
	err.println("      --verify         "+
		    "verify code before decompiling it.");
	err.println("Debugging options, mainly used to debug this decompiler:");
	err.println("  -D, --debug=...      "+
		    "use --debug=help for more information.");
    }

    public static boolean handleOption(int option, int longind, String arg) {
	if (arg == null)
	    options ^= 1 << option;
	else if ("yes".startsWith(arg) || arg.equals("on"))
	    options |= 1 << option;
	else if ("no".startsWith(arg) || arg.equals("off"))
	    options &= ~(1 << option);
	else {
	    GlobalOptions.err.println
		("jode.decompiler.Main: option --"+longOptions[longind].getName()
		 +" takes one of `yes', `no', `on', `off' as parameter");
	    return false;
	}
	return true;
    }

    public static void main(String[] params) {
	if (params.length == 0) {
	    usage();
	    return;
	}

	ClassPath classPath;
        String classPathStr = System.getProperty("java.class.path")
	    .replace(File.pathSeparatorChar, ClassPath.altPathSeparatorChar);
	String destDir = null;

	int importPackageLimit = ImportHandler.DEFAULT_PACKAGE_LIMIT;
        int importClassLimit = ImportHandler.DEFAULT_CLASS_LIMIT;;

	GlobalOptions.err.println(GlobalOptions.copyright);

	boolean errorInParams = false;
	Getopt g = new Getopt("jode.decompiler.Main", params, "hVvc:d:D:i:s:",
			      longOptions, true);
	for (int opt = g.getopt(); opt != -1; opt = g.getopt()) {
	    switch(opt) {
	    case 0:
		break;
	    case 'h':
		usage();
		errorInParams = true;
		break;
	    case 'V':
		GlobalOptions.err.println(GlobalOptions.version);
		break;
	    case 'c':
		classPathStr = g.getOptarg();
		break;
	    case 'd':
		destDir = g.getOptarg();
		break;
	    case 'v': {
		String arg = g.getOptarg();
		if (arg == null)
		    GlobalOptions.verboseLevel++;
		else {
		    try {
			GlobalOptions.verboseLevel = Integer.parseInt(arg);
		    } catch (NumberFormatException ex) {
			GlobalOptions.err.println
			    ("jode.decompiler.Main: Argument `"
			     +arg+"' to --verbose must be numeric:");
			errorInParams = true;
		    }
		}
		break;
	    }
	    case 'D': {
		String arg = g.getOptarg();
		if (arg == null)
		    arg = "help";
		errorInParams |= !GlobalOptions.setDebugging(arg);
		break;
	    }
	    case 's': {
		String arg = g.getOptarg();
		if ("sun".startsWith(arg))
		    outputStyle = SUN_STYLE;
		else if ("gnu".startsWith(arg))
		    outputStyle = GNU_STYLE;
		else {
		    GlobalOptions.err.println
			("jode.decompiler.Main: Unknown style `"+arg+"'.");
		    errorInParams = true;
		}
		break;
	    }
	    case 'i': {
		String arg = g.getOptarg();
		int comma = arg.indexOf(',');
		try {
		    int packLimit = Integer.parseInt(arg.substring(0, comma));
		    if (packLimit == 0)
			packLimit = Integer.MAX_VALUE;
		    if (packLimit < 0)
			throw new IllegalArgumentException();
		    int clazzLimit = Integer.parseInt(arg.substring(comma+1));
		    if (clazzLimit == 0)
			clazzLimit = Integer.MAX_VALUE;
		    if (clazzLimit < 0)
			throw new IllegalArgumentException();

		    importPackageLimit = packLimit;
		    importClassLimit = clazzLimit;
			
		} catch (RuntimeException ex) {
		    GlobalOptions.err.println
			("jode.decompiler.Main: Invalid argument for -i option.");
		    errorInParams = true;
		}
		break;
	    }
	    default:
		if (opt >= OPTION_START && opt <= OPTION_END) {
		    errorInParams |= !handleOption(opt-OPTION_START, 
						   g.getLongind(), 
						   g.getOptarg());
		} else
		    errorInParams = true;
		break;
	    }
	}
	if (errorInParams)
	    return;
	classPath = new ClassPath(classPathStr);
        ClassInfo.setClassPath(classPath);
	ImportHandler imports = new ImportHandler(importPackageLimit,
						  importClassLimit);

	ZipOutputStream destZip = null;
	TabbedPrintWriter writer = null;
	if (destDir == null)
	    writer = new TabbedPrintWriter(System.out, imports);
	else if (destDir.toLowerCase().endsWith(".zip")
		 || destDir.toLowerCase().endsWith(".jar")) {
	    try {
		destZip = new ZipOutputStream(new FileOutputStream(destDir));
	    } catch (IOException ex) {
		GlobalOptions.err.println("Can't open zip file "+destDir);
		ex.printStackTrace(GlobalOptions.err);
		return;
	    }
	    writer = new TabbedPrintWriter(new BufferedOutputStream(destZip), 
					   imports, false);
	}
        for (int i= g.getOptind(); i< params.length; i++) {
	    try {
		ClassInfo clazz;
		try {
		    clazz = classPath.getClassInfo(params[i]);
		} catch (IllegalArgumentException ex) {
		    GlobalOptions.err.println
			("`"+params[i]+"' is not a class name");
		    continue;
		}
		if (skipClass(clazz))
		    continue;

		String filename = 
		    params[i].replace('.', File.separatorChar)+".java";
		if (destZip != null) {
		    writer.flush();
		    destZip.putNextEntry(new ZipEntry(filename));
		} else if (destDir != null) {
		    File file = new File (destDir, filename);
		    File directory = new File(file.getParent());
		    if (!directory.exists() && !directory.mkdirs()) {
			GlobalOptions.err.println
			    ("Could not create directory " 
			     + directory.getPath() + ", check permissions.");
		    }
		    writer = new TabbedPrintWriter
			(new BufferedOutputStream(new FileOutputStream(file)),
			 imports, false);
		}

		GlobalOptions.err.println(params[i]);
		
		ClassAnalyzer clazzAna = new ClassAnalyzer(clazz, imports);
		clazzAna.dumpJavaFile(writer);
		
		if (destZip != null) {
		    writer.flush();
		    destZip.closeEntry();
		} else if (destDir != null)
		    writer.close();
		/* Now is a good time to clean up */
		System.gc();
	    } catch (IOException ex) {
		GlobalOptions.err.println
		    ("Can't write source of "+params[i]+".");
		GlobalOptions.err.println("Check the permissions.");
		ex.printStackTrace(GlobalOptions.err);
	    }
	}
	if (destZip != null) {
	    try {
		destZip.close();
	    } catch (IOException ex) {
		GlobalOptions.err.println("Can't close Zipfile");
		ex.printStackTrace(GlobalOptions.err);
	    }
	}
    }
}
