/* ClassIdentifier Copyright (C) 1999 Jochen Hoenicke.
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

package jode.obfuscator;
import jode.GlobalOptions;
import jode.bytecode.*;
///#ifdef JDK12
///import java.util.Comparator;
///import java.util.Collection;
///import java.util.Collections;
///import java.util.Arrays;
///import java.util.Iterator;
///import java.util.List;
///import java.util.LinkedList;
///import java.util.Map;
///#else
import jode.util.Comparator;
import jode.util.Collection;
import jode.util.Collections;
import jode.util.Arrays;
import jode.util.Iterator;
import jode.util.List;
import jode.util.LinkedList;
import jode.util.Map;
///#endif

import java.lang.reflect.Modifier;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.OutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;

public class ClassIdentifier extends Identifier {
    PackageIdentifier pack;
    String name;
    ClassInfo info;
    boolean willStrip;
    String superName;
    String[] ifaceNames;

    int fieldCount;
    /* The first fieldCount are of type FieldIdentifier, the remaining
     * are MethodIdentifier
     */
    Identifier[] identifiers;
    List knownSubClasses = new LinkedList();
    List virtualReachables = new LinkedList();

    public ClassIdentifier(PackageIdentifier pack, 
			   String name, ClassInfo info) {
	super(name);
	this.pack = pack;
	this.name = name;
	this.info = info;
    }

    public void addSubClass(ClassIdentifier ci) {
	knownSubClasses.add(ci);
	for(Iterator i = virtualReachables.iterator(); i.hasNext(); ) {
	    String[] method = (String[]) i.next();
	    ci.reachableIdentifier(method[0], method[1], true);
	}
    }

    public void preserveMatchingIdentifier(WildCard wildcard) {
	String fullName = getFullName() + ".";
	for (int i=0; i< identifiers.length; i++) {
	    if (wildcard.matches(fullName + identifiers[i].getName())
		|| wildcard.matches(fullName + identifiers[i].getName()
				    + "." +identifiers[i].getType())) {
		if (GlobalOptions.verboseLevel > 0)
		    GlobalOptions.err.println("Preserving "+identifiers[i]);
		setPreserved();
		identifiers[i].setPreserved();
		identifiers[i].setReachable();
	    }		
	}
    }

    public void preserveIdentifier(String name, String typeSig) {
	for (int i=0; i< identifiers.length; i++) {
	    if (name.equals(identifiers[i].getName())
		&& typeSig.equals(identifiers[i].getType()))
		identifiers[i].setPreserved();
	}
    }

    public void reachableIdentifier(String name, String typeSig,
				    boolean isVirtual) {
	boolean found = false;
	for (int i=0; i< identifiers.length; i++) {
	    if (name.equals(identifiers[i].getName())
		&& typeSig.equals(identifiers[i].getType())) {
		identifiers[i].setReachable();
		found = true;
	    }
	}
	if (!found) {
	    /*XXXXXXXX super reachableIdentifier */
	} /*ELSE*/
	if (isVirtual) {
	    for (Iterator i = knownSubClasses.iterator(); i.hasNext(); )
		((ClassIdentifier)i.next())
		    .reachableIdentifier(name, typeSig, false);
	    virtualReachables.add(new String[] { name, typeSig });
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
     * This is partly taken from the classpath project.
     */
    public long calcSerialVersionUID() {
	final MessageDigest md;
	try {
	    md = MessageDigest.getInstance("SHA");
	} catch (NoSuchAlgorithmException ex) {
	    ex.printStackTrace();
	    GlobalOptions.err.println("Can't calculate serialVersionUID");
	    return 0L;
	}
	OutputStream digest = new OutputStream() {

	    public void write(int b) {
		md.update((byte) b);
	    }

	    public void write(byte[] data, int offset, int length) {
		md.update(data, offset, length);
	    }
	};
	DataOutputStream out = new DataOutputStream(digest);
	try {
	    out.writeUTF(info.getName());
	    
	    int modifiers = info.getModifiers();
	    // just look at interesting bits
	    modifiers = modifiers & ( Modifier.ABSTRACT | Modifier.FINAL
				      | Modifier.INTERFACE | Modifier.PUBLIC );
	    out.writeInt(modifiers);
	    
	    ClassInfo[] interfaces
		= (ClassInfo[]) info.getInterfaces().clone();
	    Arrays.sort(interfaces, new Comparator() {
		public int compare( Object o1, Object o2 ) {
		    return ((ClassInfo)o1).getName()
			.compareTo(((ClassInfo)o2).getName());
		}
	    });
	    for( int i=0; i < interfaces.length; i++ ) {
		out.writeUTF(interfaces[i].getName());
	    }
	    
	    
	    Comparator identCmp = new Comparator() {
		public int compare(Object o1, Object o2)  {
		    Identifier i1 = (Identifier)o1;
		    Identifier i2 = (Identifier)o2;
		    boolean special1 = (i1.equals("<init>")
					|| i1.equals("<clinit>"));
		    boolean special2 = (i2.equals("<init>")
					|| i2.equals("<clinit>"));
		    // Put constructors at the beginning
		    if (special1 != special2) {
			return special1 ? -1 : 1;
		    }

		    int comp = i1.getName().compareTo(i2.getName());
		    if (comp != 0) {
			return comp;
		    } else {
			return i1.getType().compareTo(i2.getType());
		    }
		}
	    };

	    List idents = Arrays.asList((Object[]) identifiers.clone());
	    List fields = idents.subList(0, fieldCount);
	    List methods = idents.subList(fieldCount, idents.size());
	    Collections.sort(fields, identCmp);
	    Collections.sort(methods, identCmp);
	    
	    for (Iterator i = fields.iterator(); i.hasNext();) {
		FieldIdentifier field = (FieldIdentifier) i.next();
		modifiers = field.info.getModifiers();
		if ((modifiers & Modifier.PRIVATE) != 0
		    && (modifiers & (Modifier.STATIC 
				     | Modifier.TRANSIENT)) != 0)
		    continue;
		
		out.writeUTF(field.getName());
		out.writeInt(modifiers);
		out.writeUTF(field.getType());
	    }
	    for(Iterator i = methods.iterator(); i.hasNext(); ) {
		MethodIdentifier method = (MethodIdentifier) i.next();
		modifiers = method.info.getModifiers();
		if( Modifier.isPrivate(modifiers))
		    continue;
		
		out.writeUTF(method.getName());
		out.writeInt(modifiers);
		
		// the replacement of '/' with '.' was needed to make computed
		// SUID's agree with those computed by JDK
		out.writeUTF(method.getType().replace('/', '.'));
	    }
	    
	    out.close();

	    byte[] sha = md.digest();
	    long result = 0;
	    for (int i=0; i < 8; i++) {
		result += (long)(sha[i] & 0xFF) << (8 * i);
	    }
	    return result;
	} catch (IOException ex) {
	    ex.printStackTrace();
	    GlobalOptions.err.println("Can't calculate serialVersionUID");
	    return 0L;
	}
    }

    /**
     * Preserve all fields, that are necessary, to serialize
     * a compatible class.
     */
    public void preserveSerializable() {
	preserveIdentifier("writeObject", "(Ljava.io.ObjectOutputStream)V");
	preserveIdentifier("readObject", "(Ljava.io.ObjectOutputStream)V");
	if ((Main.options & Main.OPTION_PRESERVESERIAL) != 0) {
	    setPreserved();
	    boolean hasSerialUID = false;
	    for (int i=0; i< fieldCount; i++) {
		if ("serialVersionUID".equals(identifiers[i].getName())
		    && "J".equals(identifiers[i].getType())) {
		    identifiers[i].setReachable();
		    identifiers[i].setPreserved();
		    hasSerialUID = true;
		    break;
		}
	    }
	    if (!hasSerialUID) {
		/* add a field serializableVersionUID if not existent */
		long serialVersion = calcSerialVersionUID();
		Identifier[] newIdents = new Identifier[identifiers.length+1];
		System.arraycopy(identifiers, 0, newIdents, 0, fieldCount);
		System.arraycopy(identifiers, fieldCount, 
				 newIdents, fieldCount + 1, 
				 identifiers.length - fieldCount);
		FieldInfo UIDField = new FieldInfo
		    (info, "serialVersionUID", "J", 
		     Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL);
		UIDField.setConstant(new Long(serialVersion));
		FieldIdentifier fident = new FieldIdentifier(this, UIDField);
		fident.setPreserved();
		fident.setReachable();
		newIdents[fieldCount++] = fident;
		identifiers = newIdents;
	    }
	    for (int i=0; i < fieldCount; i++) {
		FieldIdentifier ident = (FieldIdentifier) identifiers[i];
		if ((ident.info.getModifiers() 
		     & (Modifier.TRANSIENT | Modifier.STATIC)) == 0) {
		    ident.setPreserved();
		    ident.setNotConstant();
		}
		/* XXX - only preserve them if writeObject not existent
		 * or if writeObject calls defaultWriteObject, and similar
		 * for readObject
		 */
	    }
	}
    }

    /**
     * Marks the package as preserved, too.
     */
    protected void setSinglePreserved() {
	pack.setPreserved();
    }

    public void setSingleReachable() {
	super.setSingleReachable();
	Main.getClassBundle().analyzeIdentifier(this);
    }
    
    public void analyzeSuperClasses(ClassInfo superclass) {
	while (superclass != null) {
	    if (superclass.getName().equals("java.io.Serializable"))
		preserveSerializable();
	    
	    ClassIdentifier superident = Main.getClassBundle()
		.getClassIdentifier(superclass.getName());
	    if (superident != null) {
		superident.addSubClass(this);
	    } else {
		// all virtual methods in superclass are reachable now!
		MethodInfo[] topmethods = superclass.getMethods();
		for (int i=0; i< topmethods.length; i++) {
		    int modif = topmethods[i].getModifiers();
		    if (((Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL)
			 & modif) == 0
			&& !topmethods[i].getName().equals("<init>")) {
			reachableIdentifier
			    (topmethods[i].getName(), topmethods[i].getType(),
			     true);
		    }
		}
	    }
	    ClassInfo[] ifaces = superclass.getInterfaces();
	    for (int i=0; i < ifaces.length; i++)
		analyzeSuperClasses(ifaces[i]);
	    superclass = superclass.getSuperclass();
	}
    }

    public void analyze() {
	if (GlobalOptions.verboseLevel > 0)
	    GlobalOptions.err.println("Reachable: "+this);

	ClassInfo[] ifaces = info.getInterfaces();
	for (int i=0; i < ifaces.length; i++)
	    analyzeSuperClasses(ifaces[i]);
	analyzeSuperClasses(info.getSuperclass());
    }

    public void initSuperClasses(ClassInfo superclass) {
	while (superclass != null) {
	    if (superclass.getName().equals("java.lang.Serializable"))
		preserveSerializable();
	    
	    ClassIdentifier superident = Main.getClassBundle()
		.getClassIdentifier(superclass.getName());
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
		for (int i=0; i< topmethods.length; i++) {
		    // all virtual methods in superclass may be
		    // virtually reachable
		    int modif = topmethods[i].getModifiers();
		    if (((Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL)
			 & modif) == 0
			&& !topmethods[i].getName().equals("<init>")) {
			preserveIdentifier
			    (topmethods[i].getName(), topmethods[i].getType());
		    }
		}
	    }
	    ClassInfo[] ifaces = superclass.getInterfaces();
	    for (int i=0; i < ifaces.length; i++)
		initSuperClasses(ifaces[i]);
	    superclass = superclass.getSuperclass();
	}
    }

    public void initClass() {
	info.loadInfo(info.FULLINFO);

	FieldInfo[] finfos   = info.getFields();
	MethodInfo[] minfos  = info.getMethods();
	if (Main.swapOrder) {
	    Random rand = new Random();
	    Collections.shuffle(Arrays.asList(finfos), rand);
	    Collections.shuffle(Arrays.asList(minfos), rand);
	}
	fieldCount = finfos.length;
	identifiers = new Identifier[finfos.length + minfos.length];
	for (int i=0; i< fieldCount; i++) {
	    identifiers[i] = new FieldIdentifier(this, finfos[i]);
	}
	for (int i=0; i< minfos.length; i++) {
	    identifiers[fieldCount + i]
		= new MethodIdentifier(this, minfos[i]);
	    if (identifiers[fieldCount + i].getName().equals("<clinit>")) {
		/* If there is a static initializer, it is automatically
		 * reachable (even if this class wouldn't be otherwise).
		 */
		identifiers[fieldCount + i].setPreserved();
		identifiers[fieldCount + i].setReachable();
	    } else if (identifiers[fieldCount + i].getName().equals("<init>"))
		identifiers[fieldCount + i].setPreserved();
	}

	// preserve / chain inherited methods and fields.
	ClassInfo[] ifaces = info.getInterfaces();
	ifaceNames = new String[ifaces.length];
	for (int i=0; i < ifaces.length; i++) {
	    ifaceNames[i] = ifaces[i].getName();
	    ClassIdentifier ifaceident = Main.getClassBundle()
		.getClassIdentifier(ifaceNames[i]);
	    initSuperClasses(ifaces[i]);
	}

	if (info.getSuperclass() != null) {
	    superName = info.getSuperclass().getName();
	    ClassIdentifier superident = Main.getClassBundle()
		.getClassIdentifier(superName);
	    initSuperClasses(info.getSuperclass());
	}

	if ((Main.stripping & Main.STRIP_SOURCE) != 0) {
	    info.setSourceFile(null);
	}
	if ((Main.stripping & Main.STRIP_INNERINFO) != 0) {
	    info.setInnerClasses(new InnerClassInfo[0]);
	    info.setOuterClasses(new InnerClassInfo[0]);
	    info.setExtraClasses(new InnerClassInfo[0]);
	}
	// load inner classes
	InnerClassInfo[] innerClasses = info.getInnerClasses();
	InnerClassInfo[] outerClasses = info.getOuterClasses();
	InnerClassInfo[] extraClasses = info.getExtraClasses();
	if (outerClasses != null) {
	    for (int i=0; i < outerClasses.length; i++) {
		if (outerClasses[i].outer != null) {
		    Main.getClassBundle()
			.getClassIdentifier(outerClasses[i].outer);
		}
	    }
	}
	if (innerClasses != null) {
	    for (int i=0; i < innerClasses.length; i++) {
		Main.getClassBundle()
		    .getClassIdentifier(innerClasses[i].inner);
	    }
	}
	if (extraClasses != null) {
	    for (int i=0; i < extraClasses.length; i++) {
		Main.getClassBundle()
		    .getClassIdentifier(extraClasses[i].inner);
		if (extraClasses[i].outer != null)
		    Main.getClassBundle()
			.getClassIdentifier(extraClasses[i].outer);
	    }
	}
    }

    public void buildTable(Renamer renameRule) {
	super.buildTable(renameRule);
	for (int i=0; i < identifiers.length; i++)
	    if ((Main.stripping & Main.STRIP_UNREACH) == 0
		|| identifiers[i].isReachable())
		identifiers[i].buildTable(renameRule);
    }

    public void readTable(Map table) {
	super.readTable(table);
	for (int i=0; i < identifiers.length; i++)
	    if ((Main.stripping & Main.STRIP_UNREACH) == 0
		|| identifiers[i].isReachable())
		identifiers[i].readTable(table);
    }

    public void writeTable(Map table) {
	super.writeTable(table);
	for (int i=0; i < identifiers.length; i++)
	    if ((Main.stripping & Main.STRIP_UNREACH) == 0
		|| identifiers[i].isReachable())
		identifiers[i].writeTable(table);
    }

    /**
     * Add the ClassInfo objects of the interfaces of ancestor.  But if
     * an interface of ancestor is not reachable it will add its interfaces
     * instead.
     * @param result The Collection where the interfaces should be added to.
     * @param ancestor The ancestor whose interfaces should be added.
     */
    public void addIfaces(Collection result, ClassIdentifier ancestor) {
	String[] ifaces = ancestor.ifaceNames;
	ClassInfo[] ifaceInfos = ancestor.info.getInterfaces();
	for (int i=0; i < ifaces.length; i++) {
	    ClassIdentifier ifaceident
		= Main.getClassBundle().getClassIdentifier(ifaces[i]);
	    if (ifaceident != null && !ifaceident.isReachable())
		addIfaces(result, ifaceident);
	    else
		result.add(ifaceInfos[i]);
	}
    }

    /**
     * Generates the new super class and interfaces, removing super
     * classes and interfaces that are not reachable.
     * @return an array of class names (full qualified, dot separated)
     * where the first entry is the super class (may be null) and the
     * other entries are the interfaces.
     */
    public void transformSuperIfaces() {
	if ((Main.stripping & Main.STRIP_UNREACH) == 0)
	    return;

	Collection newIfaces = new LinkedList();
	ClassIdentifier ancestor = this;
	while(true) {
	    addIfaces(newIfaces, ancestor);
	    ClassIdentifier superident 
		= Main.getClassBundle().getClassIdentifier(ancestor.superName);
	    if (superident == null || superident.isReachable())
		break;
	    ancestor = superident;
	}
	ClassInfo superInfo = ancestor.info.getSuperclass();
	ClassInfo[] ifaces = (ClassInfo[]) 
	    newIfaces.toArray(new ClassInfo[newIfaces.size()]);
	info.setSuperclass(superInfo);
	info.setInterfaces(ifaces);
    }

    public void transformInnerClasses() {
	InnerClassInfo[] outerClasses = info.getOuterClasses();
	if (outerClasses != null) {
	    int newOuterCount = outerClasses.length;
	    if ((Main.stripping & Main.STRIP_UNREACH) != 0) {
		for (int i=0; i < outerClasses.length; i++) {
		    if (outerClasses[i].outer != null) {
			ClassIdentifier outerIdent = Main.getClassBundle()
			    .getClassIdentifier(outerClasses[i].outer);
			if (outerIdent != null && !outerIdent.isReachable())
			    newOuterCount--;
		    }
		}
	    }
	    if (newOuterCount == 0) {
		info.setOuterClasses(null);
	    } else {
		InnerClassInfo[] newOuters = new InnerClassInfo[newOuterCount];
		int pos = 0;
		String lastClass = getFullAlias();
		for (int i=0; i<outerClasses.length; i++) {
		    ClassIdentifier outerIdent = outerClasses[i].outer != null
			? (Main.getClassBundle()
			   .getClassIdentifier(outerClasses[i].outer))
			: null;
		    
		    if (outerIdent != null && !outerIdent.isReachable())
			continue;
		    
		    String inner = lastClass;
		    String outer = outerIdent == null
			? outerClasses[i].outer
			: outerIdent.getFullAlias();
		    String name = outerClasses[i].name == null ? null
			: ((outer != null && inner.startsWith(outer+"$")) 
			   ? inner.substring(outer.length()+1)
			   : inner.substring(inner.lastIndexOf('.')+1));

		    newOuters[pos++] = new InnerClassInfo
			(inner, outer, name, outerClasses[i].modifiers);
		    lastClass = outer;
		}
		info.setOuterClasses(newOuters);
	    }
	}

	InnerClassInfo[] innerClasses = info.getInnerClasses();
	if (innerClasses != null) {
	    int newInnerCount = innerClasses.length;
	    if ((Main.stripping & Main.STRIP_UNREACH) != 0) {
		for (int i=0; i < innerClasses.length; i++) {
		    ClassIdentifier innerIdent = Main.getClassBundle()
			.getClassIdentifier(innerClasses[i].inner);
		    if (innerIdent != null && !innerIdent.isReachable())
			newInnerCount--;
		}
	    }
	    if (newInnerCount == 0) {
		info.setInnerClasses(null);
	    } else {
		InnerClassInfo[] newInners = new InnerClassInfo[newInnerCount];
		int pos = 0;
		for (int i=0; i<innerClasses.length; i++) {
		    ClassIdentifier innerIdent = Main.getClassBundle()
			.getClassIdentifier(innerClasses[i].inner);
		    if (innerIdent != null 
			&& (Main.stripping & Main.STRIP_UNREACH) != 0
			&& !innerIdent.isReachable())
			continue;
		    
		    String inner = innerIdent == null
			? innerClasses[i].inner
			: innerIdent.getFullAlias();
		    String outer = getFullAlias();
		    String name = innerClasses[i].name == null ? null
			: ((outer != null && inner.startsWith(outer+"$")) 
			   ? inner.substring(outer.length()+1)
			   : inner.substring(inner.lastIndexOf('.')+1));
		    
		    newInners[pos++] = new InnerClassInfo
			(inner, outer, name, innerClasses[i].modifiers);
		}
		info.setInnerClasses(newInners);
	    }
	}

	InnerClassInfo[] extraClasses = info.getExtraClasses();
	if (extraClasses != null) {
	    int newExtraCount = extraClasses.length;
	    if ((Main.stripping & Main.STRIP_UNREACH) != 0) {
		for (int i=0; i < extraClasses.length; i++) {
		    ClassIdentifier outerIdent = extraClasses[i].outer != null
			? (Main.getClassBundle()
			   .getClassIdentifier(extraClasses[i].outer))
			: null;
		    ClassIdentifier innerIdent = Main.getClassBundle()
			.getClassIdentifier(extraClasses[i].inner);
		    if ((outerIdent != null && !outerIdent.isReachable())
			|| (innerIdent != null && !innerIdent.isReachable()))
			newExtraCount--;
		}
	    }

	    if (newExtraCount == 0) {
		info.setExtraClasses(null);
	    } else {
		InnerClassInfo[] newExtras = newExtraCount > 0 
		    ? new InnerClassInfo[newExtraCount] : null;

		int pos = 0;
		for (int i=0; i<extraClasses.length; i++) {
		    ClassIdentifier outerIdent = extraClasses[i].outer != null
			? (Main.getClassBundle()
			   .getClassIdentifier(extraClasses[i].outer))
			: null;
		    ClassIdentifier innerIdent = Main.getClassBundle()
			.getClassIdentifier(extraClasses[i].inner);
		    
		    if (innerIdent != null && !innerIdent.isReachable())
			continue;
		    if (outerIdent != null && !outerIdent.isReachable())
			continue;

		    String inner = innerIdent == null
			? extraClasses[i].inner
			: innerIdent.getFullAlias();
		    String outer = outerIdent == null
			? extraClasses[i].outer
			: outerIdent.getFullAlias();
		
		    String name = extraClasses[i].name == null ? null
			: ((outer != null && inner.startsWith(outer+"$")) 
			   ? inner.substring(outer.length()+1)
			   : inner.substring(inner.lastIndexOf('.')+1));

		    newExtras[pos++] = new InnerClassInfo
			(inner, outer, name, extraClasses[i].modifiers);
		}
		info.setExtraClasses(newExtras);
	    }
	}
    }

    public void doTransformations() {
	if (GlobalOptions.verboseLevel > 0)
	    GlobalOptions.err.println("Transforming "+this);
	info.setName(getFullAlias());
	transformSuperIfaces();
	transformInnerClasses();

	int newFieldCount = 0, newMethodCount = 0;
	if ((Main.stripping & Main.STRIP_UNREACH) != 0) {
	    for (int i=0; i < fieldCount; i++)
		if (identifiers[i].isReachable())
		    newFieldCount++;
	    for (int i=fieldCount; i < identifiers.length; i++)
		if (identifiers[i].isReachable())
		    newMethodCount++;
	} else {
	    newFieldCount = fieldCount;
	    newMethodCount = identifiers.length - fieldCount;
	}

	FieldInfo[] newFields = new FieldInfo[newFieldCount];
	MethodInfo[] newMethods = new MethodInfo[newMethodCount];
	newFieldCount = newMethodCount = 0;

	for (int i=0; i < fieldCount; i++) {
	    if ((Main.stripping & Main.STRIP_UNREACH) == 0
		|| identifiers[i].isReachable()) {
		((FieldIdentifier)identifiers[i]).doTransformations();
		newFields[newFieldCount++]
		    = ((FieldIdentifier)identifiers[i]).info;
	    }
	}
	for (int i=fieldCount; i < identifiers.length; i++) {
	    if ((Main.stripping & Main.STRIP_UNREACH) == 0
		|| identifiers[i].isReachable()) {
		((MethodIdentifier)identifiers[i]).doTransformations();
		newMethods[newMethodCount++]
		    = ((MethodIdentifier)identifiers[i]).info;
	    }
	}

	info.setFields(newFields);
	info.setMethods(newMethods);
    }
    
    public void storeClass(DataOutputStream out) throws IOException {
	if (GlobalOptions.verboseLevel > 0)
	    GlobalOptions.err.println("Writing "+this);
	info.write(out);
	info = null;
	identifiers = null;
    }

    public Identifier getParent() {
	return pack;
    }

    /**
     * @return the full qualified name, excluding trailing dot.
     */
    public String getFullName() {
	if (pack.parent == null)
	    return getName();
	else 
	    return pack.getFullName() + "." + getName();
    }

    /**
     * @return the full qualified alias, excluding trailing dot.
     */
    public String getFullAlias() {
	if (pack.parent == null)
	    return getAlias();
	else 
	    return pack.getFullAlias() + "." + getAlias();
    }

    public String getName() {
	return name;
    }

    public String getType() {
	return "Ljava/lang/Class;";
    }

    public Iterator getChilds() {
	return Arrays.asList(identifiers).iterator();
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
	
	for (int i=0; i < ifaceNames.length; i++) {
	    ClassIdentifier ifaceident = Main.getClassBundle()
		.getClassIdentifier(ifaceNames[i]);
	    if (ifaceident != null) {
		Identifier ident
		    = ifaceident.getIdentifier(fieldName, typeSig);
		if (ident != null)
		    return ident;
	    }
	}

	if (superName != null) {
	    ClassIdentifier superident = Main.getClassBundle()
		.getClassIdentifier(superName);
	    if (superident != null) {
		Identifier ident 
		    = superident.getIdentifier(fieldName, typeSig);
		if (ident != null)
		    return ident;
	    }
	}
	return null;
    }

    public static boolean containsField
	(ClassInfo clazz, String name, String type, ModifierMatcher modMatch) {
	FieldInfo[] finfos = clazz.getFields();
	for (int i=0; i< finfos.length; i++) {
	    if (finfos[i].getName().equals(name)
		&& finfos[i].getType().startsWith(type)
		&& modMatch.matches(finfos[i].getModifiers()))
		return true;
	}
	
	ClassInfo[] ifaces = clazz.getInterfaces();
	for (int i=0; i < ifaces.length; i++) {
	    if (containsField(ifaces[i], name, type, modMatch))
		return true;
	}

	if (clazz.getSuperclass() != null) {
	    if (containsField(clazz.getSuperclass(),
			      name, type, modMatch))
		return true;
	}
	return false;
    }

    public static boolean containsMethod
	(ClassInfo clazz, String name, String type, ModifierMatcher modMatch) {
	MethodInfo[] minfos = clazz.getMethods();
	for (int i=0; i< minfos.length; i++) {
	    if (minfos[i].getName().equals(name)
		&& minfos[i].getType().startsWith(type)
		&& modMatch.matches(minfos[i].getModifiers()))
		return true;
	}
	
	ClassInfo[] ifaces = clazz.getInterfaces();
	for (int i=0; i < ifaces.length; i++) {
	    if (containsMethod(ifaces[i], name, type, modMatch))
		return true;
	}

	if (clazz.getSuperclass() != null) {
	    if (containsMethod(clazz.getSuperclass(),
			       name, type, modMatch))
		return true;
	}
	return false;
    }

    public boolean containsFieldAliasDirectly(String fieldName, String typeSig,
					      ModifierMatcher matcher) {
	for (int i=0; i < fieldCount; i++) {
	    if (((Main.stripping & Main.STRIP_UNREACH) == 0
		 || identifiers[i].isReachable())
		&& identifiers[i].wasAliased()
		&& identifiers[i].getAlias().equals(fieldName)
		&& identifiers[i].getType().startsWith(typeSig)
		&& matcher.matches(identifiers[i]))
		return true;
	}
	return false;
    }

    public boolean containsMethodAliasDirectly(String methodName, 
					       String paramType,
					       ModifierMatcher matcher) {
	for (int i=fieldCount; i< identifiers.length; i++) {
	    if (((Main.stripping & Main.STRIP_UNREACH) == 0
		 || identifiers[i].isReachable())
		&& identifiers[i].wasAliased()
		&& identifiers[i].getAlias().equals(methodName)
		&& identifiers[i].getType().startsWith(paramType)
		&& matcher.matches(identifiers[i]))
		return true;
	}
	return false;
    }

    public boolean containsFieldAlias(String fieldName, String typeSig, 
				      ModifierMatcher matcher) {
	if (containsFieldAliasDirectly(fieldName, typeSig, matcher))
	    return true;

	ModifierMatcher packMatcher = matcher.forceAccess(0, true);
	ClassInfo[] ifaces = info.getInterfaces();
	for (int i=0; i < ifaces.length; i++) {
	    ClassIdentifier ifaceident = Main.getClassBundle()
		.getClassIdentifier(ifaces[i].getName());
	    if (ifaceident != null) {
		if (ifaceident.containsFieldAlias(fieldName, typeSig, 
						  packMatcher))
		    return true;
	    } else {
		if (containsField(ifaces[i], fieldName, typeSig,
				  packMatcher))
		    return true;
	    }
	}

	if (info.getSuperclass() != null) {
	    ClassIdentifier superident = Main.getClassBundle()
		.getClassIdentifier(info.getSuperclass().getName());
	    if (superident != null) {
		if (superident.containsFieldAlias(fieldName, typeSig, 
						  packMatcher))
		    return true;
	    } else {
		if (containsField(info.getSuperclass(), 
				  fieldName, typeSig, packMatcher))
		    return true;
	    }
	}
	return false;
    }

    public boolean containsMethodAlias(String methodName, String typeSig,
				       ModifierMatcher matcher) {
	if (containsMethodAliasDirectly(methodName,typeSig, matcher))
	    return true;
	
	ModifierMatcher packMatcher = matcher.forceAccess(0, true);
	ClassInfo[] ifaces = info.getInterfaces();
	for (int i=0; i < ifaces.length; i++) {
	    ClassIdentifier ifaceident = Main.getClassBundle()
		.getClassIdentifier(ifaces[i].getName());
	    if (ifaceident != null) {
		if (ifaceident.containsMethodAlias(methodName, typeSig, 
						   packMatcher))
		    return true;
	    } else {
		if (containsMethod(ifaces[i], methodName, typeSig, 
				   packMatcher))
		    return true;
	    }
	}

	if (info.getSuperclass() != null) {
	    ClassIdentifier superident = Main.getClassBundle()
		.getClassIdentifier(info.getSuperclass().getName());
	    if (superident != null) {
		if (superident.containsMethodAlias(methodName, typeSig, 
						   packMatcher))
		    return true;
	    } else {
		if (containsMethod(info.getSuperclass(), 
				   methodName, typeSig, packMatcher))
		    return true;
	    }
	}
	return false;
    }

    public boolean fieldConflicts(FieldIdentifier field, String newAlias) {
	String typeSig = (Main.options & Main.OPTION_STRONGOVERLOAD) != 0
	    ? field.getType() : "";

	/* Fields are special: They are not overriden but hidden.  We
	 * must only take care, that the reference of every
	 * getfield/putfield opcode points to the exact class, afterwards
	 * we can use doubled name as much as we want.
	 */


	ModifierMatcher mm = ModifierMatcher.allowAll;
	if (containsFieldAliasDirectly(newAlias, typeSig, mm))
	    return true;


//  	boolean isPublic = (field.info.getModifier() 
//  			    & (Modifier.PROTECTED | Modifier.PUBLIC)) == 0;
//  	if (field.info.getModifier() & Modifier.PRIVATE != 0) {
//  	    for (Iterator i = clazz.knownSubClasses.iterator(); 
//  		 i.hasNext(); ) {
//  		ClassIdentifier ci = (ClassIdentifier) i.next();
//  		if ((isPublic || ci.getParent() == getParent())
//  		    && ci.containsFieldAliasDirectly(newAlias, typeSig, mm))
//  		    return true;
//  	    }
//  	}
	return false;
    }

    public boolean methodConflicts(MethodIdentifier method, String newAlias) {
	String paramType = method.getType();
	if ((Main.options & Main.OPTION_STRONGOVERLOAD) == 0)
	    paramType = paramType.substring(0, paramType.indexOf(')')+1);

	ModifierMatcher matcher = ModifierMatcher.allowAll;
	if (containsMethodAlias(newAlias, paramType, matcher))
	    return true;

	ModifierMatcher packMatcher = matcher.forceAccess(0, true);
	if (packMatcher.matches(method)) {
	    for (Iterator i = knownSubClasses.iterator(); i.hasNext(); ) {
		ClassIdentifier ci = (ClassIdentifier) i.next();
		if (ci.containsMethodAliasDirectly(newAlias, paramType, 
						   matcher))
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
		&& minfos[i].getType().startsWith(paramType))
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
	    if (((Main.stripping & Main.STRIP_UNREACH) == 0
		 || identifiers[i].isReachable())
		&& identifiers[i].getAlias().equals(methodName)
		&& identifiers[i].getType().startsWith(paramType))
		return identifiers[i];
	}
	ClassInfo[] ifaces = info.getInterfaces();
	for (int i=0; i < ifaces.length; i++) {
	    ClassIdentifier ifaceident = Main.getClassBundle()
		.getClassIdentifier(ifaces[i].getName());
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
	    ClassIdentifier superident = Main.getClassBundle()
		.getClassIdentifier(info.getSuperclass().getName());
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
	return pack.contains(newAlias, this);
    }
}
