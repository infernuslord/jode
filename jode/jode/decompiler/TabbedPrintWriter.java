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

package jode;
import java.io.*;

public class TabbedPrintWriter {
    boolean atbol;
    String tabstr;
    StringBuffer indent;
    PrintWriter pw;

    public TabbedPrintWriter (OutputStream os, String tabstr) {
	pw = new PrintWriter(os);
	this.tabstr=tabstr;
	indent = new StringBuffer();
	atbol = true;
    }

    public void tab() {
	indent.append(tabstr);
    }

    public void untab() {
	indent.setLength(indent.length()-tabstr.length());
    }

    public void println(String str) throws java.io.IOException {
	if (atbol) {
	    pw.print(indent);
	}
	pw.println(str);
        pw.flush();
	atbol = true;
    }

    public void print(String str) throws java.io.IOException {
	if (atbol) {
	    pw.print(indent);
	}
	pw.print(str);
	atbol = false;
    }
}
