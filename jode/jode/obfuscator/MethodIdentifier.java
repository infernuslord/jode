/* MethodIdentifier Copyright (C) 1999 Jochen Hoenicke.
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
import java.lang.reflect.Modifier;
import jode.Obfuscator;
import jode.bytecode.*;
import jode.Type;
import java.io.*;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Hashtable;
///#ifdef JDK12
///import java.lang.ref.SoftReference;
///#endif

public class MethodIdentifier extends Identifier implements Opcodes {
    ClassIdentifier clazz;
    MethodInfo info;
    /**
     * The exceptions that can be thrown by this method
     */
    String[] exceptions;

///#ifdef JDK12
///    /**
///     * The byte code for this method, or null if there isn't any.
///     */
///    SoftReference byteCodeRef;
///    /**
///     * The code analyzer of this method, or null if there isn't any.
///     */
///    SoftReference codeAnalyzerRef;
///#else
    /**
     * The byte code for this method, or null if there isn't any.
     */
    BytecodeInfo byteCode;
    /**
     * The code analyzer of this method, or null if there isn't any.
     */
    CodeAnalyzer codeAnalyzer;
///#endif

    public BytecodeInfo getBytecode() {
///#ifdef JDK12
///	if (byteCodeRef != null && byteCodeRef.get() != null) 
///	    return (BytecodeInfo) byteCodeRef.get();
///	BytecodeInfo byteCode = null;
///#else
	if (byteCode != null) 
	    return byteCode;
///#endif
        AttributeInfo codeattr = info.findAttribute("Code");
	try {
	    if (codeattr != null) {
		DataInputStream stream = new DataInputStream
		    (new ByteArrayInputStream(codeattr.getContents()));
		byteCode = new BytecodeInfo();
		byteCode.read(clazz.info.getConstantPool(), stream);
///#ifdef JDK12
///		byteCodeRef = new SoftReference(byteCode);
///#endif
	    }
	} catch (IOException ex) {
	    ex.printStackTrace(Obfuscator.err);
	}
	return byteCode;
    }

    public CodeAnalyzer getCodeAnalyzer() {
///#ifdef JDK12
///	if (codeAnalyzerRef != null && codeAnalyzerRef.get() != null) 
///	    return (CodeAnalyzer) codeAnalyzerRef.get();
///	CodeAnalyzer codeAnalyzer = null;
///#else
	if (codeAnalyzer != null)
	    return codeAnalyzer;
///#endif

	BytecodeInfo code = getBytecode();
	if (code != null) {
	    codeAnalyzer = new ConstantAnalyzer(code, this);
///#ifdef JDK12
///	    codeAnalyzerRef = new SoftReference(codeAnalyzer);
///#endif
	}
	return codeAnalyzer;
    }

    public MethodIdentifier(ClassIdentifier clazz, MethodInfo info) {
	super(info.getName());
	this.clazz = clazz;
	this.info  = info;
        AttributeInfo exceptionsattr = info.findAttribute("Exceptions");
	try {
	    if (exceptionsattr != null)
		readExceptions(exceptionsattr);
	} catch (IOException ex) {
	    ex.printStackTrace(Obfuscator.err);
	}
    }

    public void applyPreserveRule(int preserveRule) {
	if ((preserveRule & (info.getModifiers() ^ Modifier.PRIVATE)) != 0) {
	    setReachable();
	    setPreserved();
	}
    }

    public void readExceptions(AttributeInfo exceptionsattr) 
	throws IOException {
	byte[] content = exceptionsattr.getContents();
	DataInputStream input = new DataInputStream
	    (new ByteArrayInputStream(content));
	ConstantPool cp = clazz.info.getConstantPool();
	
	int count = input.readUnsignedShort();
	exceptions = new String[count];
	for (int i=0; i< count; i++) {
	    exceptions[i] 
		= cp.getClassName(input.readUnsignedShort()).replace('/','.');
	}
    }

    public void setSingleReachable() {
	super.setSingleReachable();
	clazz.bundle.analyzeIdentifier(this);
    }

    public void analyze() {
	if (Obfuscator.isDebugging)
	    Obfuscator.err.println("Analyze: "+this);

	String type = getType();
	int index = type.indexOf('L');
	while (index != -1) {
	    int end = type.indexOf(';', index);
	    clazz.bundle.reachableIdentifier(type.substring(index+1, end)
					     , false);
	    index = type.indexOf('L', end);
	}

	if (exceptions != null) {
	    for (int i=0; i< exceptions.length; i++)
		clazz.bundle.reachableIdentifier(exceptions[i], false);
	}

	if (getCodeAnalyzer() != null)
	    getCodeAnalyzer().analyzeCode();
    }

    public void readTable(Hashtable table) {
	setAlias((String) table.get(getFullName() + "." + getType()));
    }

    public void writeTable(Hashtable table) {
	table.put(getFullAlias()
		  + "." + clazz.bundle.getTypeAlias(getType()), getName());
    }

    public Identifier getParent() {
	return clazz;
    }

    public String getFullName() {
	return clazz.getFullName() + "." + getName();
    }

    public String getFullAlias() {
	return clazz.getFullAlias() + "." + getAlias();
    }

    public String getName() {
	return info.getName();
    }

    public String getType() {
	return info.getType().getTypeSignature();
    }

    public boolean conflicting(String newAlias, boolean strong) {
	String paramType = getType();
	if (!strong) {
	    paramType = paramType.substring(0, paramType.indexOf(')')+1);
	}
	if (clazz.getMethod(newAlias, paramType) != null)
	    return true;

	Enumeration enum = clazz.knownSubClasses.elements();
	while (enum.hasMoreElements()) {
	    ClassIdentifier ci = (ClassIdentifier) enum.nextElement();
	    if (ci.hasMethod(newAlias, paramType))
		return true;
	}
	return false;
    }

    public String toString() {
	return "MethodIdentifier "+getFullName()+"."+getType();
    }


    int nameIndex;
    int descriptorIndex;
    int codeIndex;
    BytecodeInfo strippedBytecode;
    byte[] code;
    int exceptionsIndex;
    int[] excIndices;

    /**
     * This method does the code transformation.  This include
     * <ul><li>new slot distribution for locals</li>
     *     <li>obfuscating transformation of flow</li>
     *     <li>renaming field, method and class references</li>
     * </ul>
     */
    public void doCodeTransformations(GrowableConstantPool gcp) {
	if (getCodeAnalyzer() != null) {
	    strippedBytecode = getCodeAnalyzer().stripCode();
//  	    strippedBytecode.dumpCode(Obfuscator.err);
	    /* XXX This should be in a if (Obfuscator.distributeLocals) */
	    LocalOptimizer localOpt = new LocalOptimizer(strippedBytecode);
	    localOpt.calcLocalInfo();
	    localOpt.stripLocals();
	    localOpt.distributeLocals();
//  	    if (Obfuscator.isDebugging)
//  		localOpt.dumpLocals();
//  	    strippedBytecode.dumpCode(Obfuscator.err);
	    
	    RemovePopAnalyzer remPop = 
		new RemovePopAnalyzer(strippedBytecode, this);
	    remPop.stripCode();
//  	    strippedBytecode.dumpCode(Obfuscator.err);

	    for (Instruction instr = strippedBytecode.getFirstInstr(); 
		 instr != null; instr = instr.nextByAddr) {
		switch (instr.opcode) {
		case opc_invokespecial:
		case opc_invokestatic:
		case opc_invokeinterface:
		case opc_invokevirtual: {
		    Reference ref = (Reference) instr.objData;
		    ClassIdentifier ci = (ClassIdentifier)
			clazz.bundle.getIdentifier(ref.getClazz());
		    String newType = clazz.bundle.getTypeAlias(ref.getType());
		    
		    if (ci != null) {
			MethodIdentifier mi = (MethodIdentifier) 
			    ci.getIdentifier(ref.getName(), ref.getType());
			instr.objData = new Reference
			    (ci.getFullAlias(), mi.getAlias(), newType);
		    } else 
			instr.objData = new Reference
			    (ref.getClazz(), ref.getName(), newType);
		    break;
		}
		case opc_putstatic:
		case opc_putfield:
		case opc_getstatic:
		case opc_getfield: {
		    Reference ref = (Reference) instr.objData;
		    String newType = clazz.bundle.getTypeAlias(ref.getType());
		    ClassIdentifier ci = (ClassIdentifier)
			clazz.bundle.getIdentifier(ref.getClazz());
		    if (ci != null) {
			FieldIdentifier fi = (FieldIdentifier) 
			    ci.getIdentifier(ref.getName(), ref.getType());
			instr.objData = new Reference
			    (ci.getFullAlias(), fi.getAlias(), newType);
		    } else 
			instr.objData = new Reference
			    (ref.getClazz(), ref.getName(), newType);

		    break;
		}
		case opc_new:
		case opc_anewarray:
		case opc_checkcast:
		case opc_instanceof:
		case opc_multianewarray: {
		    String clName = (String) instr.objData;
		    if (clName.charAt(0) == '[') {
			clName = clazz.bundle.getTypeAlias(clName);
		    } else {
			ClassIdentifier ci = (ClassIdentifier) 
			    clazz.bundle.getIdentifier(clName);
			if (ci != null)
			    clName = ci.getFullAlias();
		    }
		    instr.objData = clName;
		    break;
		}
		}
	    }
	
	    Handler[] handlers = strippedBytecode.getExceptionHandlers();
	    for (int i=0; i< handlers.length; i++) {
		if (handlers[i].type != null) {
		    ClassIdentifier ci = (ClassIdentifier) 
			clazz.bundle.getIdentifier(handlers[i].type);
		    if (ci != null)
		    handlers[i].type = ci.getFullAlias();
		}
	    }

	    strippedBytecode.prepareWriting(gcp);
	}
    }

    public void fillConstantPool(GrowableConstantPool gcp) {
	nameIndex = gcp.putUTF(getAlias());
	descriptorIndex = gcp.putUTF(clazz.bundle.getTypeAlias(getType()));

	codeIndex = 0;
        if (strippedBytecode != null) {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    DataOutputStream output = new DataOutputStream(baos);
	    try {
		strippedBytecode.writeCode(gcp, clazz.bundle, output);
		output.close();
		code = baos.toByteArray();
		codeIndex = gcp.putUTF("Code");
	    } catch (IOException ex) {
		code = null;
	    }
	    strippedBytecode = null;
	}
	if (exceptions != null) {
	    exceptionsIndex = gcp.putUTF("Exceptions");
	    excIndices = new int[exceptions.length];
	    for (int i=0; i< exceptions.length; i++) {
		ClassIdentifier ci = (ClassIdentifier) 
		    clazz.bundle.getIdentifier(exceptions[i]);
		if (ci != null)
		    excIndices[i] = gcp.putClassRef(ci.getFullAlias());
		else
		    excIndices[i] = gcp.putClassRef(exceptions[i]);
	    }
	    exceptions = null;
	}
    }

    public void write(DataOutputStream out) throws IOException {
	out.writeShort(info.getModifiers());
	out.writeShort(nameIndex);
	out.writeShort(descriptorIndex);
	int attrCount = 0;
	if (code != null)
	    attrCount++;
	if (excIndices != null)
	    attrCount++;
	out.writeShort(attrCount);
	if (code != null) {
	    out.writeShort(codeIndex);
	    out.writeInt(code.length);
	    out.write(code);
	    code = null;
	}
	if (excIndices != null) {
	    out.writeShort(exceptionsIndex);
	    out.writeInt(excIndices.length*2+2);
	    out.writeShort(excIndices.length);
	    for (int i=0; i< excIndices.length; i++)
		out.writeShort(excIndices[i]);
	    excIndices = null;
	}
    }
}
