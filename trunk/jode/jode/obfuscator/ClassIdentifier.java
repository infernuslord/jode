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
import jode.bytecode.*;
import java.lang.reflect.Modifier;
import java.util.*;
import java.io.*;

public class ClassIdentifier extends Identifier {
    ClassBundle bundle;
    PackageIdentifier pack;
    String name;
    ClassInfo info;
    boolean willStrip;

    int preserveRule;
    int fieldCount;
    /* The first fieldCount are of type FieldIdentifier, the remaining
     * are MethodIdentifier
     */
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
		    || identifiers[i].getType().equals(typeSig)))
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
		for (int i=superident.fieldCount;
		     i < superident.identifiers.length; i++) {
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
	super.setSingleReachable();
	if (Obfuscator.isVerbose)
	    Obfuscator.err.println("Reachable: "+this);
	reachableIdentifier("<clinit>", "()V", false);
    }

    public void initClass() {
	info.loadInfo(info.FULLINFO);

	FieldInfo[] finfos   = info.getFields();
	MethodInfo[] minfos  = info.getMethods();
	fieldCount = finfos.length;
	identifiers = new Identifier[finfos.length + minfos.length];
	for (int i=0; i< fieldCount; i++) {
	    identifiers[i] = new FieldIdentifier(this, finfos[i]);
	    if ((preserveRule & (finfos[i].getModifiers() ^ Modifier.PRIVATE))
		!= 0) {
		identifiers[i].setReachable();
		identifiers[i].setPreserved();
	    }
	}
	for (int i=0; i< minfos.length; i++) {
	    identifiers[fieldCount + i]
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
		ifaceident.addSubClass(this);
	    }
	    initSuperClasses(ifaces[i]);
	}

	if (info.getSuperclass() != null) {
	    ClassIdentifier superident = (ClassIdentifier)
		bundle.getIdentifier(info.getSuperclass().getName());
	    if (superident != null) {
		superident.addSubClass(this);
	    }
	    initSuperClasses(info.getSuperclass());
	}
	preserveIdentifier("<clinit>", "()V");
	preserveIdentifier("<init>", null);
    }

    public void strip() {
	willStrip = true;
	if (Obfuscator.isDebugging) {
	    for (int i=0; i < identifiers.length; i++) {
		if (!identifiers[i].isReachable())
		    Obfuscator.err.println(identifiers[i].toString()
					   + " is stripped");
	    }
	}
    }

    public void buildTable(int renameRule) {
	super.buildTable(renameRule);
	for (int i=0; i < identifiers.length; i++)
	    if (!willStrip || identifiers[i].isReachable())
		identifiers[i].buildTable(renameRule);
    }

    public void writeTable(PrintWriter out) throws IOException {
	if (getName() != getAlias())
	    out.println("" + getFullAlias() + " = " + getName());
	for (int i=0; i < identifiers.length; i++)
	    if (!willStrip || identifiers[i].isReachable())
		identifiers[i].writeTable(out);
    }

    public void addIfaces(Vector result, ClassInfo[] ifaces) {
	for (int i=0; i < ifaces.length; i++) {
	    ClassIdentifier ifaceident = (ClassIdentifier)
		bundle.getIdentifier(ifaces[i].getName());
	    if (ifaceident != null) {
		if (ifaceident.isReachable())
		    result.addElement(ifaceident.getFullAlias());
		else
		    addIfaces(result, ifaceident.info.getInterfaces());
	    } else
		result.addElement(ifaces[i].getName());
	}
    }

    /**
     * Generates the new super class and interfaces, removing super
     * classes and interfaces that are not reachable.
     * @return an array of class names (full qualified, dot separated)
     * where the first entry is the super class (may be null) and the
     * other entries are the interfaces.
     */
    public String[] getSuperIfaces() {
	/* First generate Ifaces and superclass infos */
	Vector newSuperIfaces = new Vector();
	addIfaces(newSuperIfaces, info.getInterfaces());
	String superName = null;
	ClassInfo superClass = info.getSuperclass();
	while (superClass != null) {
	    ClassIdentifier superident = (ClassIdentifier)
		bundle.getIdentifier(superClass.getName());
	    if (superident != null) {
		if (superident.isReachable()) {
		    superName = superident.getFullAlias();
		    break;
		} else {
		    addIfaces(newSuperIfaces, superClass.getInterfaces());
		    superClass = superClass.getSuperclass();
		}
	    } else {
		superName = superClass.getName();
		break;
	    }
	}
	newSuperIfaces.insertElementAt(superName, 0);
	String[] result = new String[newSuperIfaces.size()];
	newSuperIfaces.copyInto(result);
	return result;
    }

    public void storeClass(DataOutputStream out) throws IOException {
	GrowableConstantPool gcp = new GrowableConstantPool();

	for (int i=fieldCount; i < identifiers.length; i++)
	    if (!willStrip || identifiers[i].isReachable())
		((MethodIdentifier)identifiers[i]).reserveSmallConstants(gcp);

	int[] hierarchyInts;
	{
	    String[] hierarchy = getSuperIfaces();
	    hierarchyInts = new int[hierarchy.length];   
	    for (int i=0; i< hierarchy.length; i++) {
		if (hierarchy[i] != null)
		    hierarchyInts[i] = gcp.putClassRef(hierarchy[i]);
		else
		    hierarchyInts[i] = 0;
	    }
	    hierarchy = null;
	}
	int nameIndex = gcp.putClassRef(getFullAlias());

	int fields = 0;
	int methods = 0;

	for (int i=0; i<fieldCount; i++) {
	    if (!willStrip || identifiers[i].isReachable()) {
		((FieldIdentifier)identifiers[i]).fillConstantPool(gcp);
		fields++;
	    }
	}
	for (int i=fieldCount; i < identifiers.length; i++) {
	    if (!willStrip || identifiers[i].isReachable()) {
		((MethodIdentifier)identifiers[i]).fillConstantPool(gcp);
		methods++;
	    }
	}

	out.writeInt(0xcafebabe);
	out.writeShort(3);
	out.writeShort(45);
	gcp.write(out);

	out.writeShort(info.getModifiers());
	out.writeShort(nameIndex);
	out.writeShort(hierarchyInts[0]);
	out.writeShort(hierarchyInts.length-1);
	for (int i=1; i<hierarchyInts.length; i++)
	    out.writeShort(hierarchyInts[i]);
	
	out.writeShort(fields);
	for (int i=0; i<fieldCount; i++)
	    if (!willStrip || identifiers[i].isReachable())
		((FieldIdentifier)identifiers[i]).write(out);
	out.writeShort(methods);
	for (int i=fieldCount; i < identifiers.length; i++)
	    if (!willStrip || identifiers[i].isReachable())
		((MethodIdentifier)identifiers[i]).write(out);

	out.writeShort(0); // no attributes;
    }

    public Identifier getParent() {
	return pack;
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

    public Identifier getIdentifier(String fieldName, String typeSig) {
	for (int i=0; i < identifiers.length; i++) {
	    if (identifiers[i].getName().equals(fieldName)
		&& identifiers[i].getType().startsWith(typeSig))
		return identifiers[i];
	}
	
	ClassInfo[] ifaces = info.getInterfaces();
	for (int i=0; i < ifaces.length; i++) {
	    ClassIdentifier ifaceident = (ClassIdentifier)
		bundle.getIdentifier(ifaces[i].getName());
	    if (ifaceident != null) {
		Identifier ident
		    = ifaceident.getIdentifier(fieldName, typeSig);
		if (ident != null)
		    return ident;
	    }
	}

	if (info.getSuperclass() != null) {
	    ClassIdentifier superident = (ClassIdentifier)
		bundle.getIdentifier(info.getSuperclass().getName());
	    if (superident != null) {
		Identifier ident 
		    = superident.getIdentifier(fieldName, typeSig);
		if (ident != null)
		    return ident;
	    }
	}
	return null;
    }

    public static boolean containFieldAlias(ClassInfo clazz, 
					    String fieldName, String typeSig) {
	FieldInfo[] finfos = clazz.getFields();
	for (int i=0; i< finfos.length; i++) {
	    if (finfos[i].getName().equals(fieldName)
		&& finfos[i].getType().getTypeSignature().startsWith(typeSig))
		return true;
	}
	
	ClassInfo[] ifaces = clazz.getInterfaces();
	for (int i=0; i < ifaces.length; i++) {
	    if (containFieldAlias(ifaces[i], fieldName, typeSig))
		return true;
	}

	if (clazz.getSuperclass() != null) {
	    if (containFieldAlias(clazz.getSuperclass(),
				  fieldName, typeSig))
		return true;
	}
	return false;
    }

    public boolean containFieldAlias(String fieldName, String typeSig) {
	for (int i=0; i < fieldCount; i++) {
	    if ((!willStrip || identifiers[i].isReachable())
		&& identifiers[i].getAlias().equals(fieldName)
		&& identifiers[i].getType().startsWith(typeSig))
		return true;
	}
	
	ClassInfo[] ifaces = info.getInterfaces();
	for (int i=0; i < ifaces.length; i++) {
	    ClassIdentifier ifaceident = (ClassIdentifier)
		bundle.getIdentifier(ifaces[i].getName());
	    if (ifaceident != null) {
		if (ifaceident.containFieldAlias(fieldName, typeSig))
		    return true;
	    } else {
		if (containFieldAlias(ifaces[i], fieldName, typeSig))
		    return true;
	    }
	}

	if (info.getSuperclass() != null) {
	    ClassIdentifier superident = (ClassIdentifier)
		bundle.getIdentifier(info.getSuperclass().getName());
	    if (superident != null) {
		if (superident.containFieldAlias(fieldName, typeSig))
		    return true;
	    } else {
		if (containFieldAlias(info.getSuperclass(), 
				      fieldName, typeSig))
		    return true;
	    }
	}
	return false;
    }

    public static Object getMethod(ClassInfo clazz, 
				    String methodName, String paramType) {
	MethodInfo[] minfos = clazz.getMethods();
	for (int i=0; i< minfos.length; i++) {
	    if (minfos[i].getName().equals(methodName)
		&& minfos[i].getType().getTypeSignature()
		.startsWith(paramType))
		return minfos[i];
	}
	
	ClassInfo[] ifaces = clazz.getInterfaces();
	for (int i=0; i < ifaces.length; i++) {
	    Object result = getMethod(ifaces[i], methodName, paramType);
	    if (result != null)
		return result;
	}

	if (clazz.getSuperclass() != null) {
	    Object result = getMethod(clazz.getSuperclass(),
				      methodName, paramType);
	    if (result != null)
		return result;
	}
	return null;
    }

    public Object getMethod(String methodName, String paramType) {
	for (int i=fieldCount; i< identifiers.length; i++) {
	    if ((!willStrip || identifiers[i].isReachable())
		&& identifiers[i].getAlias().equals(methodName)
		&& identifiers[i].getType().startsWith(paramType))
		return identifiers[i];
	}
	
	ClassInfo[] ifaces = info.getInterfaces();
	for (int i=0; i < ifaces.length; i++) {
	    ClassIdentifier ifaceident = (ClassIdentifier)
		bundle.getIdentifier(ifaces[i].getName());
	    if (ifaceident != null) {
		Object result = ifaceident.getMethod(methodName, paramType);
		if (result != null)
		    return result;
	    } else {
		Object result = getMethod(ifaces[i], methodName, paramType);
		if (result != null)
		    return result;
	    }
	}

	if (info.getSuperclass() != null) {
	    ClassIdentifier superident = (ClassIdentifier)
		bundle.getIdentifier(info.getSuperclass().getName());
	    if (superident != null) {
		Object result = superident.getMethod(methodName, paramType);
		if (result != null)
		    return result;
	    } else {
		Object result = getMethod(info.getSuperclass(), 
					  methodName, paramType);
		if (result != null)
		    return result;
	    }
	}
	return null;
    }

    public boolean conflicting(String newAlias) {
	return pack.contains(newAlias);
    }
}

