/* 
 * TabbedPrintWriter (c) 1998 Jochen Hoenicke
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

package jode.decompiler;
import java.io.*;
import jode.Decompiler;

public class TabbedPrintWriter {
    boolean atbol;
    int indentsize;
    int currentIndent = 0;
    String indentStr = "";
    PrintWriter pw;

    public TabbedPrintWriter (OutputStream os) {
	pw = new PrintWriter(os, true);
	this.indentsize = (Decompiler.outputStyle & Decompiler.TAB_SIZE_MASK);
	atbol = true;
    }

    public TabbedPrintWriter (Writer os) {
	pw = new PrintWriter(os, true);
	this.indentsize = (Decompiler.outputStyle & Decompiler.TAB_SIZE_MASK);
	atbol = true;
    }

    /**
     * Convert the numeric indentation to a string.
     */
    protected void makeIndentStr() {
	int tabs = (currentIndent >> 3);
	// This is a very fast implementation.
	if (tabs <= 20)
	    indentStr = "\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t       "
		.substring(20 - tabs, 20 + (currentIndent&7));
	else {
	    /* the slow way */
	    StringBuffer sb = new StringBuffer(tabs+7);
	    while (tabs > 20) {
		sb.append("\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t");
		tabs -= 20;
	    }
	    sb.append("\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t       "
		      .substring(20 - tabs, tabs + (currentIndent&7)));
	    indentStr = sb.toString();
	} 
    }

    public void tab() {
	currentIndent += indentsize;
	makeIndentStr();
    }

    public void untab() {
	currentIndent -= indentsize;
	makeIndentStr();
    }

    private String newline = System.getProperty("line.separator");

//     public void write(String str) {
// 	if (atbol)
// 	    super.write(indentStr);
// 	super.write(str);
// 	atbol = (str.equals(newline));
//     }

    public void println(String str) {
	if (atbol)
	    pw.print(indentStr);
	pw.println(str);
	atbol = true;
    }

    public void println() {
	pw.println();
	atbol = true;
    }

    public void print(String str) {
	if (atbol)
	    pw.print(indentStr);
	pw.print(str);
	atbol = false;
    }

    /**
     * Print a opening brace with the current indentation style.
     * Called at the end of the line of the instance that opens the
     * brace.  It doesn't do a tab stop after opening the brace.
     */
    public void openBrace() {
	if ((Decompiler.outputStyle & Decompiler.BRACE_AT_EOL) != 0)
	    if (atbol)
		println("{");
	    else
		println(" {");
	else {
	    if (!atbol)
		println();
	    if (currentIndent > 0)
		tab();
	    println("{");
	}
    }

    public void closeBraceContinue() {
	if ((Decompiler.outputStyle & Decompiler.BRACE_AT_EOL) != 0)
	    print("} ");
	else {
	    println("}");
	    if (currentIndent > 0)
		untab();
	}
    }

    public void closeBrace() {
	if ((Decompiler.outputStyle & Decompiler.BRACE_AT_EOL) != 0)
	    println("}");
	else {
	    println("}");
	    if (currentIndent > 0)
		untab();
	}
    }

    public void flush() {
	pw.flush();
    }

    public void close() {
	pw.close();
    }
}
