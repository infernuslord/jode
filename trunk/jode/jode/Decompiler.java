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
import jode.bytecode.ClassInfo;
import jode.bytecode.SearchPath;
import jode.bytecode.InnerClassInfo;
import jode.decompiler.*;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;

public class Decompiler {
    public final static int TAB_SIZE_MASK = 0x0f;
    public final static int BRACE_AT_EOL  = 0x10;
    public final static int SUN_STYLE = 0x14;
    public final static int GNU_STYLE = 0x02;

    public static final int OPTION_LVT       = 0x0001;
    public static final int OPTION_INNER     = 0x0002;
    public static final int OPTION_ANON      = 0x0004;
    public static final int OPTION_PUSH      = 0x0008;
    public static final int OPTION_PRETTY    = 0x0010;
    public static final int OPTION_DECRYPT   = 0x0020;
    public static final int OPTION_ONETIME   = 0x0040;
    public static final int OPTION_IMMEDIATE = 0x0080;
    public static final int OPTION_VERIFY    = 0x0100;
    public static final int OPTION_CONTRAFO  = 0x0200;

    public static int options = 
	OPTION_LVT | OPTION_INNER | OPTION_ANON | 
	OPTION_DECRYPT | OPTION_VERIFY | OPTION_CONTRAFO;

    public static final String[] optionNames = {
	"lvt", "inner", "anonymous", "push", 
	"pretty", "decrypt", "onetime", "immediate",
	"verify", "contrafo"
    };

    public static int outputStyle = SUN_STYLE;

    public static void usage() {
	PrintWriter err = GlobalOptions.err;
	err.println("Version: " + GlobalOptions.version);
        err.print("use: jode [-v]"
		  +"[--cp <classpath>][--dest <destdir>]"
		  +"[--import <pkglimit> <clslimit>]");
	for (int i=0; i < optionNames.length; i++)
	    err.print("[--[no]"+optionNames[i]+"]");
	err.println("[--debug=...] class1 [class2 ...]");
	err.println("\t-v               "+
		    "be verbose (multiple times means more verbose).");
	err.println("\t--cp <classpath> "+
		    "search for classes in specified classpath.");
	err.println("\t                 "+
		    "The paths should be separated by ','.");
	err.println("\t--dest <destdir> "+
	    "write decompiled files to disk into directory destdir.");
	err.println("\t--style {sun|gnu}"+
		    " specifies indentation style");
	err.println("\t--import <pkglimit> <clslimit>");
	err.println("\t                 "+
	    "import classes used more than clslimit times");
	err.println("\t                 "+
	    "and packages with more then pkglimit used classes");
	err.println("\t--[no]inner      "+
		    "[don't] decompile inner classes.");
	err.println("\t--[no]anonymous  "+
		    "[don't] decompile anonymous classes.");
	err.println("\t--[no]contrafo   "+
		    "[don't] transform constructors of inner classes.");
	err.println("\t--[no]lvt        "+
		    "[don't] use the local variable table.");
	err.println("\t--[no]pretty     "+
		    "[don't] use `pretty' names for local variables.");
	err.println("\t--[no]push       "+
		    "[replace] PUSH instructions [with compilable code].");
	err.println("\t--[no]decrypt    "+
		    "[don't] try to decrypt encrypted strings.");
	err.println("\t--[no]onetime    "+
		    "[don't] remove locals, that are used only one time.");
	err.println("\t--[no]immediate  "+
		    "[don't] output source immediately with wrong import.");
	err.println("\t--[no]verify  "+
		    "[don't] verify code before decompiling it.");
	err.println("Debugging options, mainly used to debug this decompiler:");
	err.println("\t--debug=...      "+
		    "use --debug=help for more information.");
    }

    public static boolean skipClass(ClassInfo clazz) {
	InnerClassInfo[] outers = clazz.getOuterClasses();
	if (outers != null) {
	    if (outers[0].outer == null) {
		return ((Decompiler.options & Decompiler.OPTION_ANON) != 0);
	    } else {
		return ((Decompiler.options & Decompiler.OPTION_INNER) != 0);
	    }
	}
	return false;
    }

    public static void main(String[] params) {
        int i;
        String classPath = System.getProperty("java.class.path")
	    .replace(File.pathSeparatorChar, SearchPath.pathSeparatorChar);
	File destDir = null;
	ZipOutputStream destZip = null;
	int importPackageLimit = ImportHandler.DEFAULT_PACKAGE_LIMIT;
        int importClassLimit = ImportHandler.DEFAULT_CLASS_LIMIT;;
	GlobalOptions.err.println(GlobalOptions.copyright);
        for (i=0; i<params.length && params[i].startsWith("-"); i++) {
            if (params[i].equals("-v"))
		GlobalOptions.verboseLevel++;
		else if (params[i].equals("--dest"))
		destDir = new File(params[++i]);
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
            } else if (params[i].equals("--style")) {
		String style = params[++i];
		if (style.equals("sun"))
		    outputStyle = SUN_STYLE;
		else if (style.equals("gnu"))
		    outputStyle = GNU_STYLE;
		else {
		    GlobalOptions.err.println("Unknown style: "+style);
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
		if (params[i].startsWith("--")) {
		    boolean negated = false;
		    String optionName = params[i].substring(2);
		    if (optionName.startsWith("no")) {
			optionName = optionName.substring(2);
			negated = true;
		    }

		    int index = -1;
		    for (int j=0; j < optionNames.length; j++) {
			if (optionNames[j].startsWith(optionName)) {
			    if (optionNames[j].equals(optionName)) {
				index = j;
				break;
			    }
			    if (index == -1) {
				index = j;
			    } else {
				index = -2;
				break;
			    }
			}
		    }
		    if (index >= 0) {
			if (negated)
			    options &= ~(1<< index);
			else
			options |= 1 << index;
			continue;
		    }
		}
		if (!params[i].startsWith("-h") && !params[i].equals("--help"))
                    GlobalOptions.err.println("Unknown option: "+params[i]);
		usage();
		return;
            }
        }
        if (i == params.length) {
            usage();
            return;
        }
        
        ClassInfo.setClassPath(classPath);
	ImportHandler imports = new ImportHandler(importPackageLimit,
						  importClassLimit);
	TabbedPrintWriter writer = null;
	if (destDir == null)
	    writer = new TabbedPrintWriter(System.out, imports);
	else if (destDir.getName().endsWith(".zip")) {
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
        for (; i< params.length; i++) {
	    try {
		ClassInfo clazz;
		try {
		    clazz = ClassInfo.forName(params[i]);
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
