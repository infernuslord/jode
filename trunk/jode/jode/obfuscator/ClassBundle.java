/* 
 * ClassBundle (c) 1998 Jochen Hoenicke
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
package jode.obfuscator;
import jode.Obfuscator;
import jode.bytecode.ClassInfo;
import java.util.*;

public class ClassBundle {

    Hashtable loadedClasses = new Hashtable();

    public ClassReachability getLoadedClass(String name) {
	return (ClassReachability) loadedClasses.get(name);
    }

    public void loadClass(ClassInfo clazz) {
	if (loadedClasses.get(clazz.getName()) != null) {
	    System.err.println("warning: ignoring double class: "
			       + clazz.getName());
	    return;
	}
	loadedClasses.put(clazz.getName(), 
			  new ClassReachability(this, clazz));
    }

    public void loadClasses(String packageOrClass) {
	if (ClassInfo.isPackage(packageOrClass)) {
	    Enumeration enum = ClassInfo.getClasses(packageOrClass);
	    while (enum.hasMoreElements())
		loadClass((ClassInfo)enum.nextElement());
	} else
	    loadClass(ClassInfo.forName(packageOrClass));
    }

    public void markPreserved(int preserveRule,
			      Vector classnames, Vector methodnames) {
	Enumeration enum = loadedClasses.elements();
	while (enum.hasMoreElements()) {
	    ((ClassReachability) enum.nextElement())
		.postInitialize();
	}
	if (preserveRule != Obfuscator.PRESERVE_NONE) {
	    enum = loadedClasses.elements();
	    while (enum.hasMoreElements()) {
		((ClassReachability) enum.nextElement())
		    .doPreserveRule(preserveRule);
	    }
	}
	/*XXX*/
    }

    public void strip() {
    }

    public void buildTable(int renameRule) {
    }

    public void readTable(String filename) {
    }

    public void writeTable(String filename) {
    }

    public void storeClasses(String destination) {
    }
}
