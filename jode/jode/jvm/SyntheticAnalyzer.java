/* SyntheticAnalyzer Copyright (C) 1999 Jochen Hoenicke.
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

package jode.jvm;
import jode.GlobalOptions;
import jode.bytecode.BasicBlocks;
import jode.bytecode.Block;
import jode.bytecode.ClassInfo;
import jode.bytecode.FieldInfo;
import jode.bytecode.Handler;
import jode.bytecode.Instruction;
import jode.bytecode.MethodInfo;
import jode.bytecode.Opcodes;
import jode.bytecode.Reference;
import jode.bytecode.TypeSignature;
import jode.type.Type;
import jode.type.MethodType;

import java.lang.reflect.Modifier;

///#def COLLECTIONS java.util
import java.util.Iterator;
///#enddef

public class SyntheticAnalyzer implements Opcodes {
    public final static int UNKNOWN = 0;
    public final static int GETCLASS = 1;
    public final static int ACCESSGETFIELD = 2;
    public final static int ACCESSPUTFIELD = 3;
    public final static int ACCESSMETHOD = 4;
    public final static int ACCESSGETSTATIC = 5;
    public final static int ACCESSPUTSTATIC = 6;
    public final static int ACCESSSTATICMETHOD = 7;
    public final static int ACCESSCONSTRUCTOR = 8;
    
    int kind = UNKNOWN;

    int unifyParam;
    Reference reference;
    ClassInfo classInfo;
    MethodInfo method;

    public SyntheticAnalyzer(ClassInfo classInfo, MethodInfo method, 
			     boolean checkName) {
	this.classInfo = classInfo;
	this.method = method;
	if (method.getBasicBlocks() == null)
	    return;
	if (!checkName || method.getName().equals("class$"))
	    if (checkGetClass())
		return;
	if (!checkName || method.getName().startsWith("access$"))
	    if (checkAccess())
		return;
	if (method.getName().equals("<init>"))
	    if (checkConstructorAccess())
		return;
    }

    public int getKind() {
	return kind;
    }

    public Reference getReference() {
	return reference;
    }

    /**
     * Gets the index of the dummy parameter for an ACCESSCONSTRUCTOR.
     * Normally the 1 but for inner classes it may be 2.
     */
    public int getUnifyParam() {
	return unifyParam;
    }

    private static final int[] getClassOpcodes = {
	opc_aload, opc_invokestatic, opc_areturn, 
	opc_astore, opc_new, opc_dup, opc_aload, 
	opc_invokevirtual, opc_invokespecial, opc_athrow
    };
    private static final Reference[] getClassRefs = {
	null, Reference.getReference("Ljava/lang/Class;", "forName",
				     "(Ljava/lang/String;)Ljava/lang/Class;"),
	null, null, null, null, null,
	Reference.getReference("Ljava/lang/Throwable;", "getMessage",
			       "()Ljava/lang/String;"),
	Reference.getReference("Ljava/lang/NoClassDefFoundError;", "<init>",
			       "(Ljava/lang/String;)V"), null
    };


    boolean checkGetClass() {
	if (!method.isStatic()
	    || !(method.getType()
		 .equals("(Ljava/lang/String;)Ljava/lang/Class;")))
	    return false;
	
	BasicBlocks bb = method.getBasicBlocks();

	Block[] blocks = bb.getBlocks();
	Block startBlock = bb.getStartBlock();
	Handler[] excHandlers = bb.getExceptionHandlers();
	if (startBlock == null
	    || startBlock.getInstructions().size() != 3
	    || excHandlers.length != 1
	    || excHandlers[0].getStart() != startBlock
	    || excHandlers[0].getEnd() != startBlock
	    || !"java.lang.ClassNotFoundException"
	    .equals(excHandlers[0].getType()))
	    return false;

        for (int i=0; i< 3; i++) {
	    Instruction instr = 
		(Instruction) startBlock.getInstructions().get(i);
	    if (instr.getOpcode() != getClassOpcodes[i])
		return false;
	    if (getClassRefs[i] != null
		&& !getClassRefs[i].equals(instr.getReference()))
		return false;
	    if (i == 0 && instr.getLocalSlot() != 0)
		return false;
	}

	Block catchBlock = excHandlers[0].getCatcher();
	if (catchBlock.getInstructions().size() != 7)
	    return false;
	int excSlot = -1;
	for (int i=0; i< 7; i++) {
	    Instruction instr = (Instruction) 
		catchBlock.getInstructions().get(i);
	    if (instr.getOpcode() != getClassOpcodes[3+i])
		return false;
	    if (getClassRefs[3+i] != null
		&& !getClassRefs[3+i].equals(instr.getReference()))
		return false;
	    if (i == 0)
		excSlot = instr.getLocalSlot();
	    if (i == 1 && !instr.getClazzType().equals
		("Ljava/lang/NoClassDefFoundError;"))
		return false;
	    if (i == 3 && instr.getLocalSlot() != excSlot)
		return false;
	}
	this.kind = GETCLASS;
	return true;
    }

    private final int modifierMask = (Modifier.PRIVATE | Modifier.PROTECTED | 
				      Modifier.PUBLIC | Modifier.STATIC);

    public boolean checkStaticAccess() {
	BasicBlocks bb = method.getBasicBlocks();
	Handler[] excHandlers = bb.getExceptionHandlers();
	if (excHandlers != null && excHandlers.length != 0)
	    return false;
	Block[] blocks = bb.getBlocks();
	Block startBlock = bb.getStartBlock();
	if (startBlock == null)
	    return false;
	Block[] succBlocks = startBlock.getSuccs();
	if (succBlocks.length > 1 || 
	    (succBlocks.length == 1 && succBlocks[0] != null))
	    return false;
	Iterator iter = startBlock.getInstructions().iterator();
	if (!iter.hasNext())
	    return false;
	Instruction instr = (Instruction) iter.next();
	if (instr.getOpcode() == opc_getstatic) {
	    Reference ref = instr.getReference();
	    String refClazz = ref.getClazz().substring(1);
	    if (!(refClazz.substring(0, refClazz.length()-1)
		  .equals(classInfo.getName().replace('.','/'))))
		return false;
	    FieldInfo refField
		= classInfo.findField(ref.getName(), ref.getType());
	    if ((refField.getModifiers() & modifierMask) != 
		(Modifier.PRIVATE | Modifier.STATIC))
		return false;
	    if (!iter.hasNext())
		return false;
	    instr = (Instruction) iter.next();
	    if (instr.getOpcode() < opc_ireturn
		|| instr.getOpcode() > opc_areturn)
		return false;
	    /* For valid bytecode the type matches automatically */
	    reference = ref;
	    kind = ACCESSGETSTATIC;
	    return true;
	}
	int params = 0, slot = 0;
	while (instr.getOpcode() >= opc_iload
	       && instr.getOpcode() <= opc_aload
	       && instr.getLocalSlot() == slot) {
	    params++;
	    slot += (instr.getOpcode() == opc_lload 
		     || instr.getOpcode() == opc_dload) ? 2 : 1;
	    if (!iter.hasNext())
		return false;
	    instr = (Instruction) iter.next();
	}
	if (instr.getOpcode() == opc_putstatic) {
	    if (params != 1)
		return false;
	    /* For valid bytecode the type of param matches automatically */
	    Reference ref = instr.getReference();
	    String refClazz = ref.getClazz().substring(1);
	    if (!(refClazz.substring(0, refClazz.length()-1)
		  .equals(classInfo.getName().replace('.','/'))))
		return false;
	    FieldInfo refField
		= classInfo.findField(ref.getName(), ref.getType());
	    if ((refField.getModifiers() & modifierMask) != 
		(Modifier.PRIVATE | Modifier.STATIC))
		return false;
	    if (!iter.hasNext())
		return false;
	    instr = (Instruction) iter.next();
	    if (instr.getOpcode() != opc_return)
		return false;
	    reference = ref;
	    kind = ACCESSPUTSTATIC;
	    return true;
	}
	if (instr.getOpcode() == opc_invokestatic) {
	    Reference ref = instr.getReference();
	    String refClazz = ref.getClazz().substring(1);
	    if (!(refClazz.substring(0, refClazz.length()-1)
		  .equals(classInfo.getName().replace('.','/'))))
		return false;
	    MethodInfo refMethod
		= classInfo.findMethod(ref.getName(), ref.getType());
	    MethodType refType = Type.tMethod(ref.getType());
	    if ((refMethod.getModifiers() & modifierMask) != 
		(Modifier.PRIVATE | Modifier.STATIC)
		|| refType.getParameterTypes().length != params)
		return false;
	    if (!iter.hasNext())
		return false;
	    instr = (Instruction) iter.next();
	    if (refType.getReturnType() == Type.tVoid) {
		if (iter.hasNext())
		    return false;
	    } else {
		if (!iter.hasNext())
		    return false;
		instr = (Instruction) iter.next();
		if (instr.getOpcode() < opc_ireturn
		    || instr.getOpcode() > opc_areturn)
		    return false;
	    }

	    /* For valid bytecode the types matches automatically */
	    reference = ref;
	    kind = ACCESSSTATICMETHOD;
	    return true;
	}
	return false;
    }

    public boolean checkAccess() {
	if (method.isStatic()) {
	    if (checkStaticAccess())
		return true;
	}

	BasicBlocks bb = method.getBasicBlocks();
	Handler[] excHandlers = bb.getExceptionHandlers();
	if (excHandlers != null && excHandlers.length != 0)
	    return false;
	Block[] blocks = bb.getBlocks();
	Block startBlock = bb.getStartBlock();
	if (startBlock == null)
	    return false;
	Block[] succBlocks = startBlock.getSuccs();
	if (succBlocks.length > 1 || 
	    (succBlocks.length == 1 && succBlocks[0] != null))
	    return false;
	Iterator iter = startBlock.getInstructions().iterator();

	if (!iter.hasNext())
	    return false;
	Instruction instr = (Instruction) iter.next();
	if (instr.getOpcode() != opc_aload || instr.getLocalSlot() != 0)
	    return false;
	if (!iter.hasNext())
	    return false;
	instr = (Instruction) iter.next();

	if (instr.getOpcode() == opc_getfield) {
	    Reference ref = instr.getReference();
	    String refClazz = ref.getClazz().substring(1);
	    if (!(refClazz.substring(0, refClazz.length()-1)
		  .equals(classInfo.getName().replace('.','/'))))
		return false;
	    FieldInfo refField
		= classInfo.findField(ref.getName(), ref.getType());
	    if ((refField.getModifiers() & modifierMask) != Modifier.PRIVATE)
		return false;
	    if (!iter.hasNext())
		return false;
	    instr = (Instruction) iter.next();
	    if (instr.getOpcode() < opc_ireturn
		|| instr.getOpcode() > opc_areturn)
		return false;
	    /* For valid bytecode the type matches automatically */
	    reference = ref;
	    kind = ACCESSGETFIELD;
	    return true;
	}
	int params = 0, slot = 1;
	while (instr.getOpcode() >= opc_iload
	       && instr.getOpcode() <= opc_aload
	       && instr.getLocalSlot() == slot) {
	    params++;
	    slot += (instr.getOpcode() == opc_lload 
		     || instr.getOpcode() == opc_dload) ? 2 : 1;
	    if (!iter.hasNext())
		return false;
	    instr = (Instruction) iter.next();
	}
	if (instr.getOpcode() == opc_putfield) {
	    if (params != 1)
		return false;
	    /* For valid bytecode the type of param matches automatically */
	    Reference ref = instr.getReference();
	    String refClazz = ref.getClazz().substring(1);
	    if (!(refClazz.substring(0, refClazz.length()-1)
		  .equals(classInfo.getName().replace('.','/'))))
		return false;
	    FieldInfo refField
		= classInfo.findField(ref.getName(), ref.getType());
	    if ((refField.getModifiers() & modifierMask) != Modifier.PRIVATE)
		return false;
	    if (iter.hasNext())
		return false;
	    reference = ref;
	    kind = ACCESSPUTFIELD;
	    return true;
	}
	if (instr.getOpcode() == opc_invokespecial) {
	    Reference ref = instr.getReference();
	    String refClazz = ref.getClazz().substring(1);
	    if (!(refClazz.substring(0, refClazz.length()-1)
		  .equals(classInfo.getName().replace('.','/'))))
		return false;
	    MethodInfo refMethod
		= classInfo.findMethod(ref.getName(), ref.getType());
	    MethodType refType = Type.tMethod(ref.getType());
	    if ((refMethod.getModifiers() & modifierMask) != Modifier.PRIVATE
		|| refType.getParameterTypes().length != params)
		return false;
	    if (refType.getReturnType() == Type.tVoid) {
		if (iter.hasNext())
		    return false;
	    } else {
		if (!iter.hasNext())
		    return false;
		instr = (Instruction) iter.next();
		if (instr.getOpcode() < opc_ireturn
		    || instr.getOpcode() > opc_areturn)
		    return false;
	    }

	    /* For valid bytecode the types matches automatically */
	    reference = ref;
	    kind = ACCESSMETHOD;
	    return true;
	}
	return false;
    }

    public boolean checkConstructorAccess() {
	BasicBlocks bb = method.getBasicBlocks();
	String[] paramTypes
	    = TypeSignature.getParameterTypes(method.getType());
	Handler[] excHandlers = bb.getExceptionHandlers();
	if (excHandlers != null && excHandlers.length != 0)
	    return false;
	Block startBlock = bb.getStartBlock();
	if (startBlock == null)
	    return false;
	Block[] succBlocks = startBlock.getSuccs();
	if (succBlocks.length != 1 || succBlocks[0] != null)
	    return false;
	Iterator iter = startBlock.getInstructions().iterator();
	if (!iter.hasNext())
	    return false;

	unifyParam = -1;
	Instruction instr = (Instruction) iter.next();
	int params = 0, slot = 0;
	while (instr.getOpcode() >= opc_iload
	       && instr.getOpcode() <= opc_aload) {
	    if (instr.getLocalSlot() > slot
		&& unifyParam == -1 && params > 0
		&& paramTypes[params-1].charAt(0) == 'L') {
		unifyParam = params;
		params++;
		slot++;
	    }
	    if (instr.getLocalSlot() != slot)
		return false;

	    params++;
	    slot += (instr.getOpcode() == opc_lload 
		     || instr.getOpcode() == opc_dload) ? 2 : 1;
	    if (!iter.hasNext())
		return false;
	    instr = (Instruction) iter.next();
	}
	if (unifyParam == -1
	    && params > 0 && params <= paramTypes.length
	    && paramTypes[params-1].charAt(0) == 'L') {
	    unifyParam = params;
	    params++;
	    slot++;
	}
	if (instr.getOpcode() == opc_invokespecial) {
	    Reference ref = instr.getReference();
	    String refClazz = ref.getClazz().substring(1);
	    if (!(refClazz.substring(0, refClazz.length()-1)
		  .equals(classInfo.getName().replace('.','/'))))
		return false;
	    MethodInfo refMethod
		= classInfo.findMethod(ref.getName(), ref.getType());
	    MethodType refType = Type.tMethod(ref.getType());
	    if ((refMethod.getModifiers() & modifierMask) != Modifier.PRIVATE
		|| !refMethod.getName().equals("<init>")
		|| unifyParam == -1
		|| refType.getParameterTypes().length != params - 2)
		return false;	    
	    if (iter.hasNext())
		return false;
	    /* We don't check if types matches.  No problem since we only
	     * need to make sure, this constructor doesn't do anything 
	     * more than relay to the real one.
	     */
	    reference = ref;
	    kind = ACCESSCONSTRUCTOR;
	    return true;
	}
	return false;
    }
}

