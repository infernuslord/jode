/* 
 * Decompiler (c) 1998 Jochen Hoenicke
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
import java.io.*;

public class Decompiler {
    public final static String version = "0.99";
    public final static String email = "jochen@gnu.org";
    public final static String copyright = 
	"Jode Copyright 1998,1999 Jochen Hoenicke <"+email+">";
    public static boolean isVerbose = false;
    public static boolean isDebugging = false;
    public static boolean isTypeDebugging = false;
    public static boolean isFlowDebugging = false;
    public static boolean debugInOut = false;
    public static boolean debugAnalyze = false;
    public static boolean showLVT = false;
    public static boolean doChecks = false;
    public static boolean prettyLocals = false;
    public static boolean immediateOutput = false;
    public static int importPackageLimit = 3;
    public static int importClassLimit = 3;

    public static void usage() {
	System.err.println("Version: " + version);
        System.err.println("use: jode [-v][--dest <destdir>]"
			   +"[--imm][--debug][--analyze][--flow]"
			   +"[--type][--inout][--lvt][--check]"
                           +"[--import <pkglimit> <clslimit>]"
			   +"[--cp <classpath>]"
                           +" class1 [class2 ...]");
    }

    public static void main(String[] params) {
        int i;
        String classPath = System.getProperty("java.class.path");
	File destDir = null;
	System.err.println(copyright);
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
            else if (params[i].equals("--lvt"))
                showLVT = true;
            else if (params[i].equals("--check"))
                doChecks = true;
            else if (params[i].equals("--pretty"))
                prettyLocals = true;
            else if (params[i].equals("--import")) {
                importPackageLimit = Integer.parseInt(params[++i]);
                importClassLimit = Integer.parseInt(params[++i]);
            } else if (params[i].equals("--cp")) {
                classPath = params[++i];
            } else if (params[i].equals("--")) {
                i++;
                break;
            } else {
                if (!params[i].startsWith("-h"))
                    System.err.println("Unknown option: "+params[i]);
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
	    writer = new TabbedPrintWriter(System.out, "    ");
        for (; i< params.length; i++) {
	    try {
		if (destDir != null) {
		    File file = new File
			(destDir, 
			 params[i].replace('.', File.separatorChar)+".java");
		    writer = new TabbedPrintWriter
			(new FileOutputStream(file), "    ");
		}
		env.doClass(params[i], writer);
	    } catch (IOException ex) {
		System.out.println("Can't write source of "+params[i]+".");
		System.out.println("Make sure that all directories exist.");
		ex.printStackTrace();
	    }
	}
    }
}
