/* 
 * PackageIdentifier (c) 1998 Jochen Hoenicke
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
import jode.bytecode.FieldInfo;
import jode.bytecode.MethodInfo;
import java.lang.reflect.Modifier;
import java.util.*;
import java.io.*;

public class PackageIdentifier extends Identifier {
    ClassBundle bundle;
    PackageIdentifier parent;
    String name;

    boolean loadOnDemand;
    Hashtable loadedClasses;

    public PackageIdentifier(ClassBundle bundle, 
			     PackageIdentifier parent,
			     String name, boolean loadOnDemand) {
	super(name);
	this.bundle = bundle;
	this.parent = parent;
	this.name = name;
	this.loadOnDemand = loadOnDemand;
	this.loadedClasses = new Hashtable();
    }

    public Identifier getIdentifier(String name) {
	if (loadOnDemand)
	    return loadClass(name);

	int index = name.indexOf('.');
	if (index == -1)
	    return (Identifier) loadedClasses.get(name);
	else {
	    PackageIdentifier pack = (PackageIdentifier) 
		loadedClasses.get(name.substring(0, index));
	    if (pack != null)
		return pack.getIdentifier(name.substring(index+1));
	    else
		return null;
	}
    }

    public Identifier loadClass(String name) {
	int index = name.indexOf('.');
	if (index == -1) {
	    Identifier ident = (Identifier) loadedClasses.get(name);
	    if (ident == null) {
		String fullname = getFullName() + name;
		if (ClassInfo.isPackage(fullname)) {
		    ident = new PackageIdentifier(bundle, this, name, true);
		} else if (!ClassInfo.exists(fullname)) {
		    throw new IllegalArgumentException
			("Can't find class"+fullname);
		} else {
		    ident = new ClassIdentifier(bundle, this, name, 
						ClassInfo.forName(fullname));
		}
		loadedClasses.put(name, ident);
	    }
	    return ident;
	} else {
	    String subpack = name.substring(0, index);
	    PackageIdentifier pack = 
		(PackageIdentifier) loadedClasses.get(subpack);
	    if (pack == null) {
		pack = new PackageIdentifier(bundle, this, 
					     subpack, loadOnDemand);
		loadedClasses.put(subpack, pack);
	    }
	    
	    if (pack != null)
		return pack.getIdentifier(name.substring(index+1));
	    else
		return null;
	}
    }

    public Identifier loadClasses(String packageOrClass) {
	int index = packageOrClass.indexOf('.');
	if (index == -1) {
	    return loadClass(packageOrClass);
	} else {
	    String subpack = packageOrClass.substring(0, index);
	    PackageIdentifier pack = (PackageIdentifier)
		loadedClasses.get(subpack);
	    if (pack == null) {
		pack = new PackageIdentifier(bundle, this, 
					     subpack, loadOnDemand);
		loadedClasses.put(subpack, pack);
	    }
	    return pack.loadClasses(packageOrClass.substring(index+1));
	}
    }

    public void reachableIdentifier(String fqn, boolean isVirtual) {
	int index = fqn.indexOf('.');
	String component = index == -1 ? fqn : fqn.substring(0, index);
	Identifier ident = getIdentifier(component);
	if (ident == null)
	    return;

	ident.setReachable();
	if (index == -1)
	    return;

	if (ident instanceof PackageIdentifier)
	    ((PackageIdentifier) ident).reachableIdentifier
		(fqn.substring(index+1), isVirtual);
	else {
	    String method = fqn.substring(index+1);
	    index = method.indexOf('.');
	    if (index == -1) {
		((ClassIdentifier) ident).reachableIdentifier
		    (method, null, isVirtual);
	    } else {
		((ClassIdentifier) ident).reachableIdentifier
		    (method.substring(0, index), method.substring(index+1), 
		     isVirtual);
	    }
	}
    }

    public void preserveIdentifier(String fqn) {
	int index = fqn.indexOf('.');
	String component = index == -1 ? fqn : fqn.substring(0, index);
	Identifier ident = getIdentifier(component);
	if (ident == null)
	    return;
	ident.setReachable();
	ident.setPreserved();
	if (index == -1)
	    return;

	if (ident instanceof PackageIdentifier)
	    ((PackageIdentifier) ident).preserveIdentifier
		(fqn.substring(index+1));
	else {
	    String method = fqn.substring(index+1);
	    index = method.indexOf('.');
	    if (index == -1) {
		((ClassIdentifier) ident).reachableIdentifier
		    (method, null, false);
		((ClassIdentifier) ident).preserveIdentifier
		    (method, null);
	    } else {
		((ClassIdentifier) ident).reachableIdentifier
		    (method.substring(0, index), method.substring(index+1), 
		     false);
		((ClassIdentifier) ident).preserveIdentifier
		    (method.substring(0, index), method.substring(index+1));
	    }
	}
    }

    /**
     * @return the full qualified name, including trailing dot.
     */
    public String getFullName() {
	if (parent != null)
	    return parent.getFullName() + getName() + ".";
	else
	    return "";
    }

    /**
     * @return the full qualified alias, including trailing dot.
     */
    public String getFullAlias() {
	if (parent != null)
	    return parent.getFullAlias() + getAlias() + ".";
	else
	    return "";
    }

    public String findAlias(String className) {
	int index = className.indexOf('.');
	if (index == -1) {
	    Identifier ident = getIdentifier(className);
	    if (ident != null)
		return ident.getFullAlias();
	} else {
	    Identifier pack = getIdentifier(className.substring(0, index));
	    if (pack != null)
		return ((PackageIdentifier)pack)
		    .findAlias(className.substring(index+1));
	}
	return className;
    }

    public void strip() {
	Hashtable ht = new Hashtable();
	Enumeration enum = loadedClasses.elements();
	while (enum.hasMoreElements()) {
	    Identifier ident = (Identifier) enum.nextElement();
	    if (ident.isReachable()) {
		ht.put(ident.getName(), ident);
		if (ident instanceof ClassIdentifier) {
		    ((ClassIdentifier) ident).strip();
		} else
		    ((PackageIdentifier) ident).strip();
	    } else {
		if (Obfuscator.isDebugging)
		    Obfuscator.err.println("Class/Package "
					   + ident.getFullName()
					   + " is not reachable");
	    }
	}
	loadedClasses = ht;
    }

    public void buildTable(int renameRule) {
	super.buildTable(renameRule);
	Enumeration enum = loadedClasses.elements();
	while (enum.hasMoreElements()) {
	    Identifier ident = (Identifier) enum.nextElement();
	    if (ident instanceof ClassIdentifier) {
		((ClassIdentifier) ident).buildTable(renameRule);
	    } else
		((PackageIdentifier) ident).buildTable(renameRule);
	}
    }

    public void writeTable(PrintWriter out) throws IOException {
	if (parent != null && getName() != getAlias())
	    out.println("" + parent.getFullAlias() + getAlias()
			+ " = " + getName());
	Enumeration enum = loadedClasses.elements();
	while (enum.hasMoreElements()) {
	    ((Identifier)enum.nextElement()).writeTable(out);
	}
    }

    public String getName() {
	return name;
    }

    public String getType() {
	return "package";
    }

    public void storeClasses(File destination) {
	File newDest = (parent == null) ? destination 
	    : new File(destination, getName());
	Enumeration enum = loadedClasses.elements();
	while (enum.hasMoreElements()) {
	    Identifier ident = (Identifier) enum.nextElement();
	    if (ident instanceof PackageIdentifier)
		((PackageIdentifier) ident)
		    .storeClasses(newDest);
	    else {
		try {
		    ((ClassIdentifier) ident).storeClass(null);
		} catch (java.io.IOException ex) {
		    Obfuscator.err.println("Can't write Class "
					   + ident.getName());
		    ex.printStackTrace();
		}
	    }
	}
    }

    public String toString() {
	return (parent == null) ? "base package" 
	    : parent.getFullName()+getName();
    }

    public boolean contains(String newAlias) {
	Enumeration enum = loadedClasses.elements();
	while (enum.hasMoreElements()) {
	    if (((Identifier)enum.nextElement()).getAlias().equals(newAlias))
		return true;
	}
	return false;
    }

    public boolean conflicting(String newAlias) {
	return parent.contains(newAlias);
    }
}
