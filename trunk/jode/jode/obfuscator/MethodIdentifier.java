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
import jode.GlobalOptions;
import jode.Obfuscator;
import jode.bytecode.*;
import jode.type.Type;
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

///#ifdef JDK12
///    /**
///     * The code analyzer of this method, or null if there isn't any.
///     */
///    SoftReference codeAnalyzerRef;
///#else
    /**
     * The code analyzer of this method, or null if there isn't any.
     */
    CodeAnalyzer codeAnalyzer;
///#endif

    public CodeAnalyzer getCodeAnalyzer() {
///#ifdef JDK12
///	if (codeAnalyzerRef != null && codeAnalyzerRef.get() != null) 
///	    return (CodeAnalyzer) codeAnalyzerRef.get();
///	CodeAnalyzer codeAnalyzer = null;
///#else
	if (codeAnalyzer != null)
	    return codeAnalyzer;
///#endif

	BytecodeInfo code = info.getBytecode();
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
    }

    public void applyPreserveRule(int preserveRule) {
	if ((preserveRule & (info.getModifiers() ^ Modifier.PRIVATE)) != 0) {
	    setReachable();
	    setPreserved();
	}
    }

    public void setSingleReachable() {
	super.setSingleReachable();
	clazz.bundle.analyzeIdentifier(this);
    }

    public void analyze() {
	if (GlobalOptions.verboseLevel > 1)
	    GlobalOptions.err.println("Analyze: "+this);

	String type = getType();
	int index = type.indexOf('L');
	while (index != -1) {
	    int end = type.indexOf(';', index);
	    clazz.bundle.reachableIdentifier(type.substring(index+1, end)
					     , false);
	    index = type.indexOf('L', end);
	}

	String[] exceptions = info.getExceptions();
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
	return info.getType();
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

    /**
     * This method does the code transformation.  This include
     * <ul><li>new slot distribution for locals</li>
     *     <li>obfuscating transformation of flow</li>
     *     <li>renaming field, method and class references</li>
     * </ul>
     */
    public void doTransformations() {
	info.setName(getAlias());
	info.setType(clazz.bundle.getTypeAlias(info.getType()));
	if (getCodeAnalyzer() != null) {
	    BytecodeInfo strippedBytecode = getCodeAnalyzer().stripCode();
//  	    strippedBytecode.dumpCode(GlobalOptions.err);

	    /* XXX This should be in a if (Obfuscator.distributeLocals) */
	    LocalOptimizer localOpt = new LocalOptimizer(strippedBytecode,
							 info);
	    localOpt.calcLocalInfo();
	    localOpt.stripLocals();
	    localOpt.distributeLocals();


//  	    if (GlobalOptions.verboseLevel > 4)
//  		localOpt.dumpLocals();
//  	    strippedBytecode.dumpCode(GlobalOptions.err);
	    
	    RemovePopAnalyzer remPop = 
		new RemovePopAnalyzer(strippedBytecode, this);
	    remPop.stripCode();
//  	    strippedBytecode.dumpCode(GlobalOptions.err);

	    for (Instruction instr = strippedBytecode.getFirstInstr(); 
		 instr != null; instr = instr.nextByAddr) {
		switch (instr.opcode) {
		case opc_invokespecial:
		case opc_invokestatic:
		case opc_invokeinterface:
		case opc_invokevirtual: {
		    Reference ref = (Reference) instr.objData;
		    MethodIdentifier mi = (MethodIdentifier)
			clazz.bundle.getIdentifier(ref);
		    String newType = clazz.bundle.getTypeAlias(ref.getType());
		    if (mi != null)
			instr.objData = new Reference
			    ("L"+mi.clazz.getFullAlias().replace('.','/')+';',
			     mi.getAlias(), newType);
		    else 
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
		    FieldIdentifier fi = (FieldIdentifier) 
			clazz.bundle.getIdentifier(ref);
		    if (fi != null) {
			instr.objData = new Reference
			    ("L"+fi.clazz.getFullAlias().replace('.','/')+';',
			     fi.getAlias(), newType);
		    } else 
			instr.objData = new Reference
			    (ref.getClazz(), ref.getName(), newType);

		    break;
		}
		case opc_new:
		case opc_checkcast:
		case opc_instanceof:
		case opc_multianewarray: {
		    String clName = (String) instr.objData;
		    clName = clazz.bundle.getTypeAlias(clName);
		    instr.objData = clName;
		    break;
		}
		}
	    }
	
	    Handler[] handlers = strippedBytecode.getExceptionHandlers();
	    for (int i=0; i< handlers.length; i++) {
		if (handlers[i].type != null) {
		    ClassIdentifier ci =
			clazz.bundle.getClassIdentifier(handlers[i].type);
		    if (ci != null)
		    handlers[i].type = ci.getFullAlias();
		}
	    }
	    info.setBytecode(strippedBytecode);
	}
    }
}
