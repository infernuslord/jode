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
import java.io.*;
import java.util.*;
import java.util.zip.ZipOutputStream;

public class ClassBundle {

    int preserveRule;
    PackageIdentifier basePackage;

    public ClassBundle() {
	basePackage = new PackageIdentifier(this, null, "", false);
	basePackage.setReachable();
	basePackage.setPreserved();
    }

    public String getTypeAlias(String typeSig) {
	StringBuffer newSig = new StringBuffer();
	int index = 0, nextindex;
	while ((nextindex = typeSig.indexOf('L', index)) != -1) {
	    newSig.append(typeSig.substring(index, nextindex+1));
	    index = typeSig.indexOf(';', nextindex);
	    String alias = basePackage.findAlias
		(typeSig.substring(nextindex+1, index).replace('/','.'));
	    newSig.append(alias.replace('.', '/'));
	}
	return newSig.append(typeSig.substring(index)).toString();
    }

    public Identifier getIdentifier(String name) {
	return basePackage.getIdentifier(name);
    }

    public void loadClasses(String packageOrClass) {
	basePackage.loadClasses(packageOrClass);
    }

    public void reachableIdentifier(String fqn, boolean isVirtual) {
	basePackage.reachableIdentifier(fqn, isVirtual);
    }

    public void setPreserved(int preserveRule, Vector fullqualifiednames) {
	this.preserveRule = preserveRule;

	basePackage.applyPreserveRule(preserveRule);
	Enumeration enum = fullqualifiednames.elements();
	while (enum.hasMoreElements()) {
	    basePackage.preserveIdentifier((String) enum.nextElement());
	}
    }

    public void buildTable(int renameRule) {
	basePackage.buildTable(renameRule);
    }

    public void readTable(String filename) {
	Properties prop = new Properties();
	try {
	    InputStream input = new FileInputStream(filename);
	    prop.load(input);
	    input.close();
	} catch (java.io.IOException ex) {
	    Obfuscator.err.println("Can't read rename table "+filename);
	    ex.printStackTrace();
	}
	basePackage.readTable(prop);
    }

    public void writeTable(String filename) {
	Properties prop = new Properties();
	basePackage.writeTable(prop);
	try {
	    OutputStream out = new FileOutputStream(filename);
	    prop.save(out, "Reverse renaming table");
	    out.close();
	} catch (java.io.IOException ex) {
	    Obfuscator.err.println("Can't write rename table "+filename);
	    ex.printStackTrace();
	}
    }

    public void storeClasses(String destination) {
	if (destination.endsWith(".jar") ||
	    destination.endsWith(".zip")) {
	    try {
		ZipOutputStream zip = new ZipOutputStream
		    (new FileOutputStream(destination));
		basePackage.storeClasses(zip);
		zip.close();
	    } catch (IOException ex) {
		System.err.println("Can't write zip file: "+destination);
		ex.printStackTrace();
	    }
	} else {
	    File directory = new File(destination);
	    if (!directory.exists()) {
		Obfuscator.err.println("Destination directory "
				       +directory.getPath()
				       +" doesn't exists.");
		return;
	    }
	    basePackage.storeClasses(new File(destination));
	}
    }
}

