/* 
 * ClassIdentifier (c) 1998 Jochen Hoenicke
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

public class ClassIdentifier extends Identifier {
    ClassBundle bundle;
    PackageIdentifier pack;
    String name;
    ClassInfo info;

    int preserveRule;
    Identifier[] identifiers;
    Vector knownSubClasses = new Vector();
    Vector virtualReachables = new Vector();

    public ClassIdentifier(ClassBundle bundle, PackageIdentifier pack, 
			   String name, ClassInfo info) {
	super(name);
	this.bundle = bundle;
	this.pack = pack;
	this.name = name;
	this.info = info;

	preserveRule = bundle.preserveRule;
	if ((preserveRule & (info.getModifiers() ^ Modifier.PRIVATE)) != 0) {
	    setReachable();
	    setPreserved();
	}
    }

    public void addSubClass(ClassIdentifier ci) {
	knownSubClasses.addElement(ci);
	Enumeration enum = virtualReachables.elements();
	while (enum.hasMoreElements()) {
	    String[] method = (String[]) enum.nextElement();
	    ci.reachableIdentifier(method[0], method[1], true);
	}
    }

    public void preserveIdentifier(String name, String typeSig) {
	for (int i=0; i< identifiers.length; i++) {
	    if (identifiers[i].getName().equals(name)
		&& (typeSig == null
		    || identifiers[i] .getType().equals(typeSig)))
		identifiers[i].setPreserved();
	}
    }

    public void reachableIdentifier(String name, String typeSig,
				    boolean isVirtual) {
	for (int i=0; i < identifiers.length; i++) {
	    if (identifiers[i].getName().equals(name)
		&& (typeSig == null
		    || identifiers[i] .getType().equals(typeSig)))
		identifiers[i].setReachable();
	}
	if (isVirtual) {
	    Enumeration enum = knownSubClasses.elements();
	    while (enum.hasMoreElements())
		((ClassIdentifier)enum.nextElement())
		    .reachableIdentifier(name, typeSig, isVirtual);
	    virtualReachables.addElement
		(new String[] { name, typeSig });
	}
    }

    public void chainIdentifier(Identifier ident) {
	String name = ident.getName();
	String typeSig = ident.getType();
	for (int i=0; i< identifiers.length; i++) {
	    if (identifiers[i].getName().equals(ident.getName())
		&& (identifiers[i].getType().equals(typeSig)))
		ident.addShadow(identifiers[i]);
	}
    }

    /**
     * Preserve all fields, that are necessary, to serialize
     * a compatible class.
     */
    public void preserveSerializable() {
	/*XXX*/
	/* add a field serializableVersionUID if not existent */
    }

    public void initSuperClasses(ClassInfo superclass) {
	while (superclass != null) {
	    if (superclass.getName().equals("java.lang.Serializable"))
		preserveSerializable();
	    
	    ClassIdentifier superident = (ClassIdentifier)
		bundle.getIdentifier(superclass.getName());
	    if (superident != null) {
		for (int i=0; i < superident.identifiers.length; i++)
		    if (superident.identifiers[i] 
			instanceof MethodIdentifier) {
			MethodIdentifier mid = (MethodIdentifier) 
			    superident.identifiers[i];
			// all virtual methods in superclass must be chained.
			int modif = mid.info.getModifiers();
			if (((Modifier.PRIVATE 
			      | Modifier.STATIC
			      | Modifier.FINAL) & modif) == 0
			    && !(mid.getName().equals("<init>"))) {
			    // chain the preserved/same name lists.
			    chainIdentifier(superident.identifiers[i]);
			}
		    }
	    } else {
		// all methods and fields in superclass are preserved!
		MethodInfo[] topmethods = superclass.getMethods();
		FieldInfo[] topfields  = superclass.getFields();
		for (int i=0; i< topmethods.length; i++) {
		    // all virtual methods in superclass may be
		    // virtually reachable
		    int modif = topmethods[i].getModifiers();
		    if (((Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL)
			 & modif) == 0
			&& !topmethods[i].getName().equals("<init>")) {
			reachableIdentifier
			    (topmethods[i].getName(), 
			     topmethods[i].getType().getTypeSignature(),
			     true);
			preserveIdentifier
			    (topmethods[i].getName(), 
			     topmethods[i].getType().getTypeSignature());
		    }
		}
	    }
	    ClassInfo[] ifaces = superclass.getInterfaces();
	    for (int i=0; i < ifaces.length; i++)
		initSuperClasses(ifaces[i]);
	    superclass = superclass.getSuperclass();
	}
    }

    public void setSingleReachable() {
	info.loadInfo(info.FULLINFO);

	if (Obfuscator.isVerbose)
	    Obfuscator.err.println(this);

	FieldInfo[] finfos   = info.getFields();
	MethodInfo[] minfos  = info.getMethods();
	identifiers = new Identifier[finfos.length + minfos.length];
	for (int i=0; i< finfos.length; i++) {
	    identifiers[i] = new FieldIdentifier(this, finfos[i]);
	    if ((preserveRule & (finfos[i].getModifiers() ^ Modifier.PRIVATE))
		!= 0) {
		identifiers[i].setReachable();
		identifiers[i].setPreserved();
	    }
	}
	for (int i=0; i< minfos.length; i++) {
	    identifiers[finfos.length + i]
		= new MethodIdentifier(this, minfos[i]);
	    if ((preserveRule & (minfos[i].getModifiers() ^ Modifier.PRIVATE))
		!= 0) {
		identifiers[i].setReachable();
		identifiers[i].setPreserved();
	    }
	    if (identifiers[i].getName().equals("<init>"))
		identifiers[i].setPreserved();
	}

	ClassInfo[] ifaces = info.getInterfaces();
	for (int i=0; i < ifaces.length; i++) {
	    ClassIdentifier ifaceident = (ClassIdentifier)
		bundle.getIdentifier(ifaces[i].getName());
	    if (ifaceident != null) {
		ifaceident.setReachable();
		ifaceident.addSubClass(this);
	    }
	    initSuperClasses(ifaces[i]);
	}

	if (info.getSuperclass() != null) {
	    ClassIdentifier superident = (ClassIdentifier)
		bundle.getIdentifier(info.getSuperclass().getName());
	    if (superident != null) {
		superident.setReachable();
		superident.addSubClass(this);
	    }
	    initSuperClasses(info.getSuperclass());
	}
	preserveIdentifier("<clinit>", "()V");
    }

    public void strip() {
	int newLength = 0;
	for (int i=0; i < identifiers.length; i++) {
	    if (identifiers[i].isReachable())
		newLength++;
	    else {
		if (Obfuscator.isDebugging)
		    Obfuscator.err.println(identifiers[i].toString()
					   + " is stripped");
	    }
	}
	Identifier[] newIdents = new Identifier[newLength];
	int index = 0;
	for (int i=0; i < identifiers.length; i++) {
	    if (identifiers[i].isReachable())
		newIdents[index++] = identifiers[i];
	}
	identifiers = newIdents;
    }

    public void buildTable(int renameRule) {
	super.buildTable(renameRule);
	for (int i=0; i < identifiers.length; i++)
	    identifiers[i].buildTable(renameRule);
    }

    public void writeTable(PrintWriter out) throws IOException {
	if (getName() != getAlias())
	    out.println("" + getFullAlias() + " = " + getName());
	for (int i=0; i < identifiers.length; i++)
	    identifiers[i].writeTable(out);
    }

    public void storeClass(OutputStream out) throws IOException {
	/*XXX*/
    }

    /**
     * @return the full qualified name, excluding trailing dot.
     */
    public String getFullName() {
	return pack.getFullName() + getName();
    }

    /**
     * @return the full qualified alias, excluding trailing dot.
     */
    public String getFullAlias() {
	return pack.getFullAlias() + getAlias();
    }

    public String getName() {
	return name;
    }

    public String getType() {
	return "Ljava/lang/Class;";
    }

    public String toString() {
	return "ClassIdentifier "+getFullName();
    }

    public static boolean containsField(ClassInfo clazz, String newAlias) {
	FieldInfo[] finfos = clazz.getFields();
	for (int i=0; i< finfos.length; i++) {
	    if (finfos[i].getName().equals(newAlias))
		return true;
	}
	
	ClassInfo[] ifaces = clazz.getInterfaces();
	for (int i=0; i < ifaces.length; i++) {
	    if (containsField(ifaces[i], newAlias))
		return true;
	}

	if (clazz.getSuperclass() != null) {
	    if (containsField(clazz.getSuperclass(), newAlias))
		return true;
	}
	return false;
    }

    public boolean containsField(String newAlias) {
	for (int i=0; i< identifiers.length; i++) {
	    if (identifiers[i] instanceof FieldIdentifier
		&& identifiers[i].getAlias().equals(newAlias))
		return true;
	}

	ClassInfo[] ifaces = info.getInterfaces();
	for (int i=0; i < ifaces.length; i++) {
	    ClassIdentifier ifaceident = (ClassIdentifier)
		bundle.getIdentifier(ifaces[i].getName());
	    if (ifaceident != null) {
		if (ifaceident.containsField(newAlias))
		    return true;
	    } else {
		if (containsField(ifaces[i], newAlias))
		    return true;
	    }
	}

	if (info.getSuperclass() != null) {
	    ClassIdentifier superident = (ClassIdentifier)
		bundle.getIdentifier(info.getSuperclass().getName());
	    if (superident != null) {
		if (superident.containsField(newAlias))
		    return true;
	    } else {
		if (containsField(info.getSuperclass(), newAlias))
		    return true;
	    }
	}
	return false;
    }

    public static boolean containsMethod(ClassInfo clazz, 
					 String newAlias, String paramType) {
	MethodInfo[] minfos = clazz.getMethods();
	for (int i=0; i< minfos.length; i++) {
	    if (minfos[i].getName().equals(newAlias)
		&& minfos[i].getType().getTypeSignature()
		.startsWith(paramType))
		return true;
	}
	
	ClassInfo[] ifaces = clazz.getInterfaces();
	for (int i=0; i < ifaces.length; i++) {
	    if (containsMethod(ifaces[i], newAlias, paramType))
		return true;
	}

	if (clazz.getSuperclass() != null) {
	    if (containsMethod(clazz.getSuperclass(), newAlias, paramType))
		return true;
	}
	return false;
    }

    public boolean containsMethod(String newAlias, String paramType) {
	for (int i=0; i< identifiers.length; i++) {
	    if (identifiers[i] instanceof MethodIdentifier
		&& identifiers[i].getAlias().equals(newAlias)
		&& identifiers[i].getType().startsWith(paramType))
		return true;
	}
	
	ClassInfo[] ifaces = info.getInterfaces();
	for (int i=0; i < ifaces.length; i++) {
	    ClassIdentifier ifaceident = (ClassIdentifier)
		bundle.getIdentifier(ifaces[i].getName());
	    if (ifaceident != null) {
		if (ifaceident.containsMethod(newAlias, paramType))
		    return true;
	    } else {
		if (containsMethod(ifaces[i], newAlias, paramType))
		    return true;
	    }
	}

	if (info.getSuperclass() != null) {
	    ClassIdentifier superident = (ClassIdentifier)
		bundle.getIdentifier(info.getSuperclass().getName());
	    if (superident != null) {
		if (superident.containsMethod(newAlias, paramType))
		    return true;
	    } else {
		if (containsMethod(info.getSuperclass(), newAlias, paramType))
		    return true;
	    }
	}
	return false;
    }

    public boolean conflicting(String newAlias) {
	return pack.contains(newAlias);
    }
}
