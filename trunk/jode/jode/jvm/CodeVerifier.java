/* CodeVerifier Copyright (C) 1999 Jochen Hoenicke.
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
import jode.AssertError;
import jode.GlobalOptions;
import jode.bytecode.BytecodeInfo;
import jode.bytecode.ClassInfo;
import jode.bytecode.Handler;
import jode.bytecode.Instruction;
import jode.bytecode.MethodInfo;
import jode.bytecode.Opcodes;
import jode.bytecode.Reference;
import jode.type.ArrayType;
import jode.type.ClassInterfacesType;
import jode.type.MethodType;
import jode.type.Type;

import java.util.BitSet;
import java.util.Stack;

public class CodeVerifier implements Opcodes {
    ClassInfo ci;
    MethodInfo mi;
    BytecodeInfo bi;

    MethodType mt;

    class UninitializedClassType extends Type {
	ClassInfo classType;
	boolean maySuper;
	public UninitializedClassType(ClassInfo clazz, boolean maySuper) {
	    super(-TC_CLASS);
	    this.classType = clazz;
	    this.maySuper = maySuper;
	}
	public boolean equals(Object o) {
	    return o instanceof UninitializedClassType
		&& ((UninitializedClassType) o).classType.equals(classType);
	}
	public String toString() {
	    StringBuffer result = new StringBuffer("new ").append(classType);
	    if (maySuper)
		result.append("(maySuper)");
	    return result.toString();
	}
    }

    class ReturnAddressType extends Type {
	Instruction jsrTarget;

	public ReturnAddressType(Instruction instr) {
	    super(-TC_METHOD);
	    jsrTarget = instr;
	}
	public boolean equals(Object o) {
	    return o instanceof ReturnAddressType
		&& ((ReturnAddressType) o).jsrTarget == jsrTarget;
	}
	public String toString() {
	    return "returnAddress "+jsrTarget;
	}
    }

    
    /**
     * JLS 4.9.6: Verifying code that contains a finally clause:
     *  - Each instruction keeps track of the list of jsr targets.
     *  - For each instruction and each jsr needed to reach that instruction
     *    a bit vector is maintained of all local vars accessed or modified.
     */

    class VerifyInfo implements Cloneable {
	Type[] stack = new Type[bi.getMaxStack()];
	Type[] locals = new Type[bi.getMaxLocals()];
	Instruction[] jsrTargets = null;
	BitSet[] jsrLocals = null;
	int stackHeight = 0;
	int maxHeight = 0;
	/* If this is a jsr target, this field contains the single 
	 * allowed ret instruction.
	 */
	Instruction retInstr = null;

	public Object clone() {
	    try {
		VerifyInfo result = (VerifyInfo) super.clone();
		result.stack = (Type[]) stack.clone();
		result.locals = (Type[]) locals.clone();
		return result;
	    } catch(CloneNotSupportedException ex) {
		throw new AssertError("Clone not supported?");
	    }
	}

	public final void reserve(int count) throws VerifyException {
	    if (stackHeight + count > maxHeight) {
		maxHeight = stackHeight + count;
		if (maxHeight > stack.length)
		    throw new VerifyException("stack overflow");
	    }
	}
	
	public final void need(int count) throws VerifyException {
	    if (stackHeight < count)
		throw new VerifyException("stack underflow");
	}
	
	public final void push(Type type) throws VerifyException {
	    reserve(1);
	    stack[stackHeight++] = type; 
	}
	
	public final Type pop() throws VerifyException {
	    need(1);
	    return stack[--stackHeight];
	}
	
	public String toString() {
	    StringBuffer result = new StringBuffer("locals:[");
	    String comma = "";
	    for (int i=0; i<locals.length; i++) {
		result.append(comma).append(i).append(':');
		if (locals[i] == Type.tError)
		    result.append("-");
		else
		    result.append(locals[i]);
		comma = ",";
	    }
	    result.append("], stack:[");
	    comma = "";
	    for (int i=0; i<stackHeight; i++) {
		result.append(comma).append(stack[i]);
		comma = ",";
	    }
	    if (jsrTargets != null) {
		result.append("], jsrs:[");
		comma = "";
		for (int i=0; i<jsrTargets.length; i++) {
		    result.append(comma).append(jsrTargets[i])
			.append(jsrLocals[i]);
		    comma = ",";
		}
	    }
	    return result.append("]").toString();
	}
    }


    public CodeVerifier(ClassInfo ci, MethodInfo mi, BytecodeInfo bi) {
	this.ci = ci;
	this.mi = mi;
	this.bi = bi;
	this.mt = Type.tMethod(mi.getType());
    }

    public VerifyInfo initInfo() {
	VerifyInfo info = new VerifyInfo();
	Type[] paramTypes = mt.getParameterTypes();
	int slot = 0;
	if (!mi.isStatic()) {
	    if (mi.getName().equals("<init>"))
		info.locals[slot++] = 
		    new UninitializedClassType(ci, true);
	    else
		info.locals[slot++] = Type.tClass(ci);
	}
	for (int i=0; i< paramTypes.length; i++) {
	    info.locals[slot++] = paramTypes[i];
	    if (paramTypes[i].stackSize() == 2)
		info.locals[slot++] = Type.tVoid;
	}
	while (slot < bi.getMaxLocals())
	    info.locals[slot++] = Type.tError;
	return info;
    }

    public boolean isOfType(Type t1, Type t2) {
	if ((GlobalOptions.debuggingFlags
	     & GlobalOptions.DEBUG_VERIFIER) != 0)
	    GlobalOptions.err.println("isOfType("+t1+","+t2+")");
	if (t1.equals(t2))
	    return true;
	if (t2.getTypeCode() == Type.TC_UNKNOWN)
	    return true;
	if (t1.getTypeCode() == Type.TC_INTEGER
	    && t2.getTypeCode() == Type.TC_INTEGER)
	    return true;
	if (t1.equals(Type.tUObject))
	    return (t2.getTypeCode() == Type.TC_CLASS
		    || t2.getTypeCode() == Type.TC_ARRAY
		    || t2.getTypeCode() == -Type.TC_CLASS);
	if (t2.equals(Type.tUObject))
	    return (t1.getTypeCode() == Type.TC_CLASS
		    || t1.getTypeCode() == Type.TC_ARRAY
		    || t1.getTypeCode() == -Type.TC_CLASS);
	if (t1.getTypeCode() == Type.TC_ARRAY) {
	    if (t2.getTypeCode() == Type.TC_CLASS)
		return (((ClassInterfacesType)t2).getClazz()
			== ClassInfo.javaLangObject);
	    else if (t2.getTypeCode() == Type.TC_ARRAY) {
		Type e1 = ((ArrayType)t1).getElementType();
		Type e2 = ((ArrayType)t2).getElementType();
		if (e2.getTypeCode() == Type.TC_UNKNOWN)
		    return true;

		/* Note that short[] is not compatible to int[],
		 * therefore this extra check.
		 */
		if ((e1.getTypeCode() == Type.TC_CLASS
		     || e1.getTypeCode() == Type.TC_ARRAY
		     || e1.equals(Type.tUObject))
		    && (e2.getTypeCode() == Type.TC_CLASS
			|| e2.getTypeCode() == Type.TC_ARRAY
			|| e2.equals(Type.tUObject)))
		    return isOfType(e1, e2);
	    }
	    return false;
	}
	if (t1.getTypeCode() == Type.TC_CLASS) {
	    if (t2.getTypeCode() == Type.TC_CLASS) {
		ClassInfo c1 = ((ClassInterfacesType) t1).getClazz();
		ClassInfo c2 = ((ClassInterfacesType) t2).getClazz();
		return c2.superClassOf(c1);
	    }
	}
	return false;
    }

    public Type mergeType(Type t1, Type t2) {
	if (t1.equals(t2))
	    return t1;
	if (t1.getTypeCode() == Type.TC_UNKNOWN)
	    return t2;
	if (t2.getTypeCode() == Type.TC_UNKNOWN)
	    return t1;
	if (t1.getTypeCode() == Type.TC_INTEGER
	    && t2.getTypeCode() == Type.TC_INTEGER)
	    return t1;
	if (t1.equals(Type.tUObject))
	    return (t2.getTypeCode() == Type.TC_CLASS
		    || t2.getTypeCode() == Type.TC_ARRAY
		    || t2.getTypeCode() == -Type.TC_CLASS) ? t2 : Type.tError;
	if (t2.equals(Type.tUObject))
	    return (t1.getTypeCode() == Type.TC_CLASS
		    || t1.getTypeCode() == Type.TC_ARRAY
		    || t1.getTypeCode() == -Type.TC_CLASS) ? t1 : Type.tError;
	if (t1.getTypeCode() == Type.TC_ARRAY) {
	    if (t2.getTypeCode() == Type.TC_CLASS)
		return Type.tObject;
	    else if (t2.getTypeCode() == Type.TC_ARRAY) {
		Type e1 = ((ArrayType)t1).getElementType();
		Type e2 = ((ArrayType)t2).getElementType();

		if (e1.getTypeCode() == Type.TC_UNKNOWN)
		    return t2;
		if (e2.getTypeCode() == Type.TC_UNKNOWN)
		    return t1;
		/* Note that short[] is not compatible to int[],
		 * therefore this extra check.
		 */
		if ((e1.getTypeCode() == Type.TC_CLASS
		     || e1.getTypeCode() == Type.TC_ARRAY
		     || e1.equals(Type.tUObject))
		    && (e2.getTypeCode() == Type.TC_CLASS
			|| e2.getTypeCode() == Type.TC_ARRAY
			|| e2.equals(Type.tUObject)))
		    return Type.tArray(mergeType(e1, e2));
		return Type.tObject;
	    }
	    return Type.tError;
	}
	if (t1.getTypeCode() == Type.TC_CLASS) {
	    if (t2.getTypeCode() == Type.TC_ARRAY)
		return Type.tObject;
	    if (t2.getTypeCode() == Type.TC_CLASS) {
		ClassInfo c1 = ((ClassInterfacesType) t1).getClazz();
		ClassInfo c2 = ((ClassInterfacesType) t2).getClazz();
		if (c1.superClassOf(c2))
		    return t1;
		if (c2.superClassOf(c1))
		    return t2;
		do {
		    c1 = c1.getSuperclass();
		} while (!c1.superClassOf(c2));
		return Type.tClass(c1);
	    }
	}
	return Type.tError;
    }

    public boolean mergeInfo(Instruction instr, VerifyInfo info) 
	throws VerifyException {
	if (instr.getTmpInfo() == null) {
	    instr.setTmpInfo(info);
	    return true;
	}
	boolean changed = false;
	VerifyInfo oldInfo = (VerifyInfo) instr.getTmpInfo();
	if (oldInfo.stackHeight != info.stackHeight)
	    throw new VerifyException("Stack height differ at: "
				      + instr.getDescription());
	for (int i=0; i < oldInfo.stackHeight; i++) {
	    Type newType = mergeType(oldInfo.stack[i], info.stack[i]);
	    if (!newType.equals(oldInfo.stack[i])) {
		if (newType == Type.tError)
		    throw new VerifyException("Type error while merging: "
					      + oldInfo.stack[i]
					      + " and " + info.stack[i]);
		changed = true;
		oldInfo.stack[i] = newType;
	    }
	}
	for (int i=0; i < bi.getMaxLocals(); i++) {
	    Type newType = mergeType(oldInfo.locals[i], info.locals[i]);
	    if (!newType.equals(oldInfo.locals[i])) {
		changed = true;
		oldInfo.locals[i] = newType;
	    }
	}
	if (oldInfo.jsrTargets != null) {
	    int jsrDepth;
	    if (info.jsrTargets == null)
		jsrDepth = 0;
	    else {
		jsrDepth = info.jsrTargets.length;
		int infoPtr = 0;
	    oldInfo_loop:
		for (int oldInfoPtr=0; 
		     oldInfoPtr < oldInfo.jsrTargets.length; oldInfoPtr++) {
		    for (int i=infoPtr; i< jsrDepth; i++) {
			if (oldInfo.jsrTargets[oldInfoPtr]
			    == info.jsrTargets[i]) {
			    System.arraycopy(info.jsrTargets, i,
					     info.jsrTargets, infoPtr,
					     jsrDepth - i);
			    jsrDepth -= (i - infoPtr);
			    infoPtr++;
			    continue oldInfo_loop;
			}
		    }
		}
		jsrDepth = infoPtr;
	    }
	    if (jsrDepth != oldInfo.jsrTargets.length) {
		if (jsrDepth == 0)
		    oldInfo.jsrTargets = null;
		else {
		    oldInfo.jsrTargets = new Instruction[jsrDepth];
		    System.arraycopy(info.jsrTargets, 0, 
				     oldInfo.jsrTargets, 0, jsrDepth);
		}
		changed = true;
	    }
	}
	return changed;
    }


    Type[] types = { Type.tInt, Type.tLong, Type.tFloat, Type.tDouble,
		     Type.tUObject, Type.tByte, Type.tChar, Type.tShort };

    public VerifyInfo modelEffect(Instruction instr, VerifyInfo prevInfo) 
	throws VerifyException {
	int jsrLength = 
	    prevInfo.jsrTargets != null ? prevInfo.jsrTargets.length : 0;
	VerifyInfo result = (VerifyInfo) prevInfo.clone();
	switch (instr.getOpcode()) {
	case opc_nop:
	case opc_goto:
	    break;
	case opc_ldc: {
	    Type type;
	    Object constant = instr.getConstant();
	    if (constant == null)
		type = Type.tUObject;
	    else if (constant instanceof Integer)
		type = Type.tInt;
	    else if (constant instanceof Float)
		type = Type.tFloat;
	    else
		type = Type.tString;
	    result.push(type);
	    break;
	}
	case opc_ldc2_w: {
	    Type type;
	    Object constant = instr.getConstant();
	    if (constant instanceof Long)
		type = Type.tLong;
	    else
		type = Type.tDouble;
	    result.push(type);
	    result.push(Type.tVoid);
	    break;
	}
	case opc_iload: 
	case opc_lload: 
	case opc_fload: 
	case opc_dload:
	case opc_aload: {
	    if (jsrLength > 0
		&& (!result.jsrLocals[jsrLength-1].get(instr.getLocalSlot())
		    || ((instr.getOpcode() & 0x1) == 0
			&& !result.jsrLocals[jsrLength-1]
			.get(instr.getLocalSlot()+1)))) {
		result.jsrLocals = (BitSet[]) result.jsrLocals.clone();
		result.jsrLocals[jsrLength-1]
		    = (BitSet) result.jsrLocals[jsrLength-1].clone();
		result.jsrLocals[jsrLength-1].set(instr.getLocalSlot());
		if ((instr.getOpcode() & 0x1) == 0)
		    result.jsrLocals[jsrLength-1].set(instr.getLocalSlot() + 1);
	    }
	    if ((instr.getOpcode() & 0x1) == 0
		&& result.locals[instr.getLocalSlot()+1] != Type.tVoid)
		throw new VerifyException(instr.getDescription());
	    Type type = result.locals[instr.getLocalSlot()];
	    if (!isOfType(type, types[instr.getOpcode() - opc_iload]))
		throw new VerifyException(instr.getDescription());
	    result.push(type);
	    if ((instr.getOpcode() & 0x1) == 0)
		result.push(Type.tVoid);
	    break;
	}
	case opc_iaload: case opc_laload: 
	case opc_faload: case opc_daload: case opc_aaload:
	case opc_baload: case opc_caload: case opc_saload: {
	    if (!isOfType(result.pop(), Type.tInt))
		throw new VerifyException(instr.getDescription());
	    Type arrType = result.pop(); 
	    if (!isOfType(arrType, 
			  Type.tArray(types[instr.getOpcode() - opc_iaload]))
		&& (instr.getOpcode() != opc_baload
		    || !isOfType(arrType, Type.tArray(Type.tBoolean))))
		throw new VerifyException(instr.getDescription());
	    
	    Type elemType = (arrType.equals(Type.tUObject) 
			     ? types[instr.getOpcode() - opc_iaload]
			     :((ArrayType)arrType).getElementType());
	    result.push(elemType);
	    if (((1 << instr.getOpcode() - opc_iaload) & 0xa) != 0)
		result.push(Type.tVoid);
	    break;
	}
	case opc_istore: case opc_lstore: 
	case opc_fstore: case opc_dstore: case opc_astore: {
	    if (jsrLength > 0
		&& (!result.jsrLocals[jsrLength-1].get(instr.getLocalSlot())
		    || ((instr.getOpcode() & 0x1) != 0
			&& !result.jsrLocals[jsrLength-1]
			.get(instr.getLocalSlot()+1)))) {
		result.jsrLocals = (BitSet[]) result.jsrLocals.clone();
		result.jsrLocals[jsrLength-1]
		    = (BitSet) result.jsrLocals[jsrLength-1].clone();
		result.jsrLocals[jsrLength-1].set(instr.getLocalSlot());
		if ((instr.getOpcode() & 0x1) != 0)
		    result.jsrLocals[jsrLength-1].set(instr.getLocalSlot() + 1);
	    }
	    if ((instr.getOpcode() & 0x1) != 0
		&& result.pop() != Type.tVoid)
		throw new VerifyException(instr.getDescription());
	    Type type = result.pop();
	    if (instr.getOpcode() != opc_astore
		|| !(type instanceof ReturnAddressType))
		if (!isOfType(type, types[instr.getOpcode() - opc_istore]))
		    throw new VerifyException(instr.getDescription());
	    result.locals[instr.getLocalSlot()] = type;
	    if ((instr.getOpcode() & 0x1) != 0)
		result.locals[instr.getLocalSlot()+1] = Type.tVoid;
	    break;
	}
	case opc_iastore: case opc_lastore:
	case opc_fastore: case opc_dastore: case opc_aastore:
	case opc_bastore: case opc_castore: case opc_sastore: {
	    if (((1 << instr.getOpcode() - opc_iastore) & 0xa) != 0
		&& result.pop() != Type.tVoid)
		throw new VerifyException(instr.getDescription());
	    Type type = result.pop();
	    if (!isOfType(result.pop(), Type.tInt))
		throw new VerifyException(instr.getDescription());
	    Type arrType = result.pop();
	    if (!isOfType(arrType, 
			  Type.tArray(types[instr.getOpcode() - opc_iastore]))
		&& (instr.getOpcode() != opc_bastore 
		    || !isOfType(arrType, Type.tArray(Type.tBoolean))))
		throw new VerifyException(instr.getDescription());
	    Type elemType = instr.getOpcode() >= opc_bastore ? Type.tInt
		: types[instr.getOpcode() - opc_iastore];
	    if (!isOfType(type, elemType))
		throw new VerifyException(instr.getDescription());
	    break;
	}
	case opc_pop: case opc_pop2: {
	    int count = instr.getOpcode() - (opc_pop-1);
	    result.need(count);
	    result.stackHeight -= count;
	    break;
	}
	case opc_dup: case opc_dup_x1: case opc_dup_x2: {
	    int depth = instr.getOpcode() - opc_dup;
	    result.reserve(1);
	    result.need(depth+1);
	    if (result.stack[result.stackHeight-1] == Type.tVoid)
		throw new VerifyException(instr.getDescription());
	    
	    int stackdepth = result.stackHeight - (depth + 1);
	    if (result.stack[stackdepth] == Type.tVoid)
		throw new VerifyException(instr.getDescription()
					  + " on long or double");
	    for (int i=result.stackHeight; i > stackdepth; i--)
		result.stack[i] = result.stack[i-1];
	    result.stack[stackdepth] = result.stack[result.stackHeight++];
	    break;
	}
	case opc_dup2: case opc_dup2_x1: case opc_dup2_x2: {
	    int depth = instr.getOpcode() - opc_dup2;
	    result.reserve(2);
	    result.need(depth+2);
	    if (result.stack[result.stackHeight-2] == Type.tVoid)
		throw new VerifyException(instr.getDescription()
					  + " on misaligned long or double");
	    int stacktop = result.stackHeight;
	    int stackdepth = stacktop - (depth + 2);
	    if (result.stack[stackdepth] == Type.tVoid)
		throw new VerifyException(instr.getDescription()
					  + " on long or double");
	    for (int i=stacktop; i > stackdepth; i--)
		result.stack[i+1] = result.stack[i-1];
	    result.stack[stackdepth+1] = result.stack[stacktop+1];
	    result.stack[stackdepth] = result.stack[stacktop];
	    result.stackHeight+=2;
	    break;
	}
	case opc_swap: {
	    result.need(2);
	    if (result.stack[result.stackHeight-2] == Type.tVoid
		|| result.stack[result.stackHeight-1] == Type.tVoid)
		throw new VerifyException(instr.getDescription()
					  + " on misaligned long or double");
	    Type tmp = result.stack[result.stackHeight-1];
	    result.stack[result.stackHeight-1] = 
		result.stack[result.stackHeight-2];
	    result.stack[result.stackHeight-2] = tmp;
	    break;
	}
        case opc_iadd: case opc_ladd: case opc_fadd: case opc_dadd:
        case opc_isub: case opc_lsub: case opc_fsub: case opc_dsub:
        case opc_imul: case opc_lmul: case opc_fmul: case opc_dmul:
        case opc_idiv: case opc_ldiv: case opc_fdiv: case opc_ddiv:
        case opc_irem: case opc_lrem: case opc_frem: case opc_drem: {
	    Type type = types[(instr.getOpcode() - opc_iadd) & 3];
	    if ((instr.getOpcode() & 1) != 0
		&& result.pop() != Type.tVoid)
		throw new VerifyException(instr.getDescription());
	    if (!isOfType(result.pop(), type))
		throw new VerifyException(instr.getDescription());
	    if ((instr.getOpcode() & 1) != 0) {
		result.need(2);
		if (result.stack[result.stackHeight-1] != Type.tVoid
		    || !isOfType(result.stack[result.stackHeight-2], type))
		    throw new VerifyException(instr.getDescription());
	    } else {
		result.need(1);
		if (!isOfType(result.stack[result.stackHeight-1], type))
		    throw new VerifyException(instr.getDescription());
	    }
	    break;
	}
        case opc_ineg: case opc_lneg: case opc_fneg: case opc_dneg: {
	    Type type = types[(instr.getOpcode() - opc_ineg) & 3];
	    if ((instr.getOpcode() & 1) != 0) {
		result.need(2);
		if (result.stack[result.stackHeight-1] != Type.tVoid
		    || !isOfType(result.stack[result.stackHeight-2], type))
		    throw new VerifyException(instr.getDescription());
	    } else {
		result.need(1);
		if (!isOfType(result.stack[result.stackHeight-1], type))
		    throw new VerifyException(instr.getDescription());
	    }
	    break;
	}
        case opc_ishl: case opc_lshl:
        case opc_ishr: case opc_lshr:
        case opc_iushr: case opc_lushr:
	    if (!isOfType(result.pop(), Type.tInt))
		throw new VerifyException(instr.getDescription());
	    
	    if ((instr.getOpcode() & 1) != 0) {
		result.need(2);
		if (result.stack[result.stackHeight-1] != Type.tVoid ||
		    !isOfType(result.stack[result.stackHeight-2],Type.tLong))
		    throw new VerifyException(instr.getDescription());
	    } else {
		result.need(1);
		if (!isOfType(result.stack[result.stackHeight-1],Type.tInt))
		    throw new VerifyException(instr.getDescription());
	    }
	    break;

        case opc_iand: case opc_land:
        case opc_ior : case opc_lor :
        case opc_ixor: case opc_lxor:
	    if ((instr.getOpcode() & 1) != 0
		&& result.pop() != Type.tVoid)
		throw new VerifyException(instr.getDescription());
	    if (!isOfType(result.pop(),
			  types[instr.getOpcode() & 1]))
		throw new VerifyException(instr.getDescription());
	    if ((instr.getOpcode() & 1) != 0) {
		result.need(2);
		if (result.stack[result.stackHeight-1] != Type.tVoid
		    || !isOfType(result.stack[result.stackHeight-2],
				 Type.tLong))
		    throw new VerifyException(instr.getDescription());
	    } else {
		result.need(1);
		if (!isOfType(result.stack[result.stackHeight-1], 
			      Type.tInt))
		    throw new VerifyException(instr.getDescription());
	    }
	    break;

	case opc_iinc:
	    if (!isOfType(result.locals[instr.getLocalSlot()], Type.tInt))
		throw new VerifyException(instr.getDescription());
	    break;
        case opc_i2l: case opc_i2f: case opc_i2d:
        case opc_l2i: case opc_l2f: case opc_l2d:
        case opc_f2i: case opc_f2l: case opc_f2d:
        case opc_d2i: case opc_d2l: case opc_d2f: {
            int from = (instr.getOpcode()-opc_i2l)/3;
            int to   = (instr.getOpcode()-opc_i2l)%3;
            if (to >= from)
                to++;
	    if ((from & 1) != 0
		&& result.pop() != Type.tVoid)
		throw new VerifyException(instr.getDescription());
	    if (!isOfType(result.pop(), types[from]))
		throw new VerifyException(instr.getDescription());
		
	    result.push(types[to]);
	    if ((to & 1) != 0)
		result.push(Type.tVoid);
	    break;
	}
        case opc_i2b: case opc_i2c: case opc_i2s:
	    result.need(1);
	    if (!isOfType(result.stack[result.stackHeight-1], Type.tInt))
		throw new VerifyException(instr.getDescription());
	    break;

	case opc_lcmp:
	    if (result.pop() != Type.tVoid)
		throw new VerifyException(instr.getDescription());
	    if (result.pop() != Type.tLong)
		throw new VerifyException(instr.getDescription());
	    if (result.pop() != Type.tVoid)
		throw new VerifyException(instr.getDescription());
	    if (result.pop() != Type.tLong)
		throw new VerifyException(instr.getDescription());
	    result.push(Type.tInt);
	    break;
	case opc_dcmpl: case opc_dcmpg:
	    if (result.pop() != Type.tVoid)
		throw new VerifyException(instr.getDescription());
	    if (result.pop() != Type.tDouble)
		throw new VerifyException(instr.getDescription());
	    if (result.pop() != Type.tVoid)
		throw new VerifyException(instr.getDescription());
	    if (result.pop() != Type.tDouble)
		throw new VerifyException(instr.getDescription());
	    result.push(Type.tInt);
	    break;
	case opc_fcmpl: case opc_fcmpg:
	    if (result.pop() != Type.tFloat)
		throw new VerifyException(instr.getDescription());
	    if (result.pop() != Type.tFloat)
		throw new VerifyException(instr.getDescription());
	    result.push(Type.tInt);
	    break;

	case opc_ifeq: case opc_ifne: 
	case opc_iflt: case opc_ifge: 
	case opc_ifgt: case opc_ifle:
	case opc_tableswitch:
	case opc_lookupswitch:
	    if (!isOfType(result.pop(), Type.tInt))
		throw new VerifyException(instr.getDescription());
	    break;

	case opc_if_icmpeq: case opc_if_icmpne:
	case opc_if_icmplt: case opc_if_icmpge: 
	case opc_if_icmpgt: case opc_if_icmple: 
	    if (!isOfType(result.pop(), Type.tInt))
		throw new VerifyException(instr.getDescription());
	    if (!isOfType(result.pop(), Type.tInt))
		throw new VerifyException(instr.getDescription());
	    break;
	case opc_if_acmpeq: case opc_if_acmpne:
	    if (!isOfType(result.pop(), Type.tUObject))
		throw new VerifyException(instr.getDescription());
	    if (!isOfType(result.pop(), Type.tUObject))
		throw new VerifyException(instr.getDescription());
	    break;
	case opc_ifnull: case opc_ifnonnull:
	    if (!isOfType(result.pop(), Type.tUObject))
		throw new VerifyException(instr.getDescription());
	    break;

	case opc_ireturn: case opc_lreturn: 
	case opc_freturn: case opc_dreturn: case opc_areturn: {
	    if (((1 << instr.getOpcode() - opc_ireturn) & 0xa) != 0
		&& result.pop() != Type.tVoid)
		throw new VerifyException(instr.getDescription());
	    Type type = result.pop();
	    if (!isOfType(type, types[instr.getOpcode() - opc_ireturn])
		|| !isOfType(type, mt.getReturnType()))
		throw new VerifyException(instr.getDescription());
	    break;
	}
	case opc_jsr: {
	    result.stack[result.stackHeight++]
		= new ReturnAddressType(instr.getSuccs()[0]);
	    result.jsrTargets = new Instruction[jsrLength+1];
	    result.jsrLocals = new BitSet[jsrLength+1];
	    if (jsrLength > 0) {
		for (int i=0; i< prevInfo.jsrTargets.length; i++)
		    if (prevInfo.jsrTargets[i] == instr.getSuccs()[0])
			throw new VerifyException(instr.getDescription()+
						  " is recursive");
		System.arraycopy(prevInfo.jsrTargets, 0, 
				 result.jsrTargets, 0, jsrLength);
		System.arraycopy(prevInfo.jsrLocals, 0, 
				 result.jsrLocals, 0, jsrLength);
	    }
	    result.jsrTargets[jsrLength] = instr.getSuccs()[0];
	    result.jsrLocals[jsrLength] = new BitSet();
	    break;
	}
	case opc_return:
	    if (mt.getReturnType() != Type.tVoid)
		throw new VerifyException(instr.getDescription());
	    break;

	case opc_getstatic: {
	    Reference ref = instr.getReference();
	    Type type = Type.tType(ref.getType());
	    result.push(type);
	    if (type.stackSize() == 2)
		result.push(Type.tVoid);
	    break;
	}
	case opc_getfield: {
	    Reference ref = instr.getReference();
	    Type classType = Type.tType(ref.getClazz());
	    if (!isOfType(result.pop(), classType))
		throw new VerifyException(instr.getDescription());
	    Type type = Type.tType(ref.getType());
	    result.push(type);
	    if (type.stackSize() == 2)
		result.push(Type.tVoid);
	    break;
	}
	case opc_putstatic: {
	    Reference ref = instr.getReference();
	    Type type = Type.tType(ref.getType());
	    if (type.stackSize() == 2
		&& result.pop() != Type.tVoid)
		throw new VerifyException(instr.getDescription());
	    if (!isOfType(result.pop(), type))
		throw new VerifyException(instr.getDescription());
	    break;
	}
	case opc_putfield: {
	    Reference ref = instr.getReference();
	    Type type = Type.tType(ref.getType());
	    if (type.stackSize() == 2
		&& result.pop() != Type.tVoid)
		throw new VerifyException(instr.getDescription());
	    if (!isOfType(result.pop(), type))
		throw new VerifyException(instr.getDescription());
	    Type classType = Type.tType(ref.getClazz());
	    if (!isOfType(result.pop(), classType))
		throw new VerifyException(instr.getDescription());
	    break;
	}
	case opc_invokevirtual:
	case opc_invokespecial:
	case opc_invokestatic :
	case opc_invokeinterface: {
	    Reference ref = instr.getReference();
	    MethodType refmt = (MethodType) Type.tType(ref.getType());
	    Type[] paramTypes = refmt.getParameterTypes();
	    for (int i=paramTypes.length - 1; i >= 0; i--) {
		if (paramTypes[i].stackSize() == 2
		    && result.pop() != Type.tVoid)
		    throw new VerifyException(instr.getDescription());
		if (!isOfType(result.pop(), 
			      paramTypes[i]))
		    throw new VerifyException(instr.getDescription());
	    }
	    if (ref.getName().equals("<init>")) {
	        Type clazz = result.pop();
		if (!(clazz instanceof UninitializedClassType))
		    throw new VerifyException(instr.getDescription());
		UninitializedClassType uct = (UninitializedClassType) clazz;
		String refClazz = ref.getClazz();
		if (refClazz.charAt(0) != 'L')
		    throw new VerifyException(instr.getDescription());
		refClazz = refClazz.substring(1, refClazz.length()-1)
		    .replace('/','.');
		if (!uct.classType.getName().equals(refClazz)
		    && (!uct.maySuper || !(uct.classType.getSuperclass()
					   .getName().equals(refClazz))))
		    throw new VerifyException(instr.getDescription());
		Type newType = Type.tClass(uct.classType);
		for (int i=0; i< result.stackHeight; i++)
		    if (result.stack[i] == clazz)
			result.stack[i] = newType;
		for (int i=0; i< result.locals.length; i++)
		    if (result.locals[i] == clazz)
			result.locals[i] = newType;
	    } else if (instr.getOpcode() != opc_invokestatic) {
		Type classType = Type.tType(ref.getClazz());
		if (!isOfType(result.pop(), classType))
		    throw new VerifyException(instr.getDescription());
	    }
	    Type type = refmt.getReturnType();
	    if (type != Type.tVoid) {
		result.push(type);
		if (type.stackSize() == 2)
		    result.push(Type.tVoid);
	    }
	    break;
	}
	case opc_new: {
	    String clName = instr.getClazzType();
	    ClassInfo ci = ClassInfo.forName
		(clName.substring(1, clName.length()-1).replace('/','.'));
	    result.stack[result.stackHeight++] = 
		new UninitializedClassType(ci, false);
	    break;
	}
	case opc_arraylength: {
	    if (!isOfType(result.pop(), Type.tArray(Type.tUnknown)))
		throw new VerifyException(instr.getDescription());
	    result.push(Type.tInt);
	    break;
	}
	case opc_athrow: {
	    if (!isOfType(result.pop(), 
			  Type.tClass("java.lang.Throwable")))
		throw new VerifyException(instr.getDescription());
	    break;
	}
	case opc_checkcast: {
	    Type classType = Type.tType(instr.getClazzType());
	    if (!isOfType(result.pop(), Type.tUObject))
		throw new VerifyException(instr.getDescription());
	    result.push(classType);
	    break;
	}
	case opc_instanceof: {
	    if (!isOfType(result.pop(), Type.tObject))
		throw new VerifyException(instr.getDescription());
	    result.push(Type.tInt);
	    break;
	}
	case opc_monitorenter:
	case opc_monitorexit:
	    if (!isOfType(result.pop(), Type.tObject))
		throw new VerifyException(instr.getDescription());
	    break;
	case opc_multianewarray: {
	    int dimension = instr.getIntData();
	    for (int i=dimension - 1; i >= 0; i--)
		if (!isOfType(result.pop(), Type.tInt))
		    throw new VerifyException(instr.getDescription());
	    Type classType = Type.tType(instr.getClazzType());
	    result.push(classType);
	    break;
	}
	default:
	    throw new AssertError("Invalid opcode "+instr.getOpcode());
	}
	return result;
    }
    
    public void doVerify() throws VerifyException {
	Stack instrStack = new Stack();
	
	bi.getFirstInstr().setTmpInfo(initInfo());
	instrStack.push(bi.getFirstInstr());
	Handler[] handlers = bi.getExceptionHandlers();
	while (!instrStack.isEmpty()) {
	    Instruction instr = (Instruction) instrStack.pop();
	    if (!instr.doesAlwaysJump() && instr.getNextByAddr() == null)
		throw new VerifyException("Flow can fall off end of method");

	    VerifyInfo prevInfo = (VerifyInfo) instr.getTmpInfo();
	    if (instr.getOpcode() == opc_ret) {
		if (prevInfo.jsrTargets == null
		    || !(prevInfo.locals[instr.getLocalSlot()] 
			 instanceof ReturnAddressType))
		    throw new VerifyException(instr.getDescription());
		int jsrLength = prevInfo.jsrTargets.length - 1;
		Instruction jsrTarget = 
		    ((ReturnAddressType) 
		     prevInfo.locals[instr.getLocalSlot()]).jsrTarget;
		while (jsrTarget != prevInfo.jsrTargets[jsrLength])
		    if (--jsrLength < 0) 
			throw new VerifyException(instr.getDescription());
		VerifyInfo jsrTargetInfo = (VerifyInfo) jsrTarget.getTmpInfo();
		if (jsrTargetInfo.retInstr == null)
		    jsrTargetInfo.retInstr = instr;
		else if (jsrTargetInfo.retInstr != instr)
		    throw new VerifyException
			("JsrTarget has more than one ret: "
			 + jsrTarget.getDescription());
		Instruction[] nextTargets;
		BitSet[] nextLocals;
		if (jsrLength > 0) {
		    nextTargets = new Instruction[jsrLength];
		    nextLocals = new BitSet[jsrLength];
		    System.arraycopy(prevInfo.jsrTargets, 0, 
				     nextTargets, 0, jsrLength);
		    System.arraycopy(prevInfo.jsrLocals, 0, 
				     nextLocals, 0, jsrLength);
		} else {
		    nextTargets = null;
		    nextLocals = null;
		}
		for (int i=0; i < jsrTarget.getPreds().length; i++) {
		    Instruction jsrInstr = jsrTarget.getPreds()[i];
		    if (jsrInstr.getTmpInfo() != null)
			instrStack.push(jsrInstr);
		}
	    } else {
		VerifyInfo info = modelEffect(instr, prevInfo);
		if (!instr.doesAlwaysJump())
		    if (mergeInfo(instr.getNextByAddr(), info))
			instrStack.push(instr.getNextByAddr());
		if (instr.getOpcode() == opc_jsr) {
		    VerifyInfo targetInfo = 
			(VerifyInfo) instr.getSuccs()[0].getTmpInfo();
		    if (targetInfo != null && targetInfo.retInstr != null) {
			VerifyInfo afterJsrInfo
			    = (VerifyInfo) prevInfo.clone();
			VerifyInfo retInfo
			    = (VerifyInfo) targetInfo.retInstr.getTmpInfo();
			BitSet usedLocals
			    = retInfo.jsrLocals[retInfo.jsrLocals.length-1];
			for (int j = 0; j < bi.getMaxLocals(); j++) {
			    if (usedLocals.get(j))
				afterJsrInfo.locals[j] = retInfo.locals[j];
			}
			if (mergeInfo(instr.getNextByAddr(), afterJsrInfo))
			    instrStack.push(instr.getNextByAddr());
		    }
		}
		if (instr.getSuccs() != null) {
		    for (int i=0; i< instr.getSuccs().length; i++) {
			if (instr.getSuccs()[i].getAddr() < instr.getAddr()) {
			    /* This is a backwards branch */
			    for (int j = 0; j < prevInfo.locals.length; j++) {
				if (prevInfo.locals[j] 
				    instanceof UninitializedClassType)
				    throw new VerifyException
					("Uninitialized local in back-branch");
			    }
			    for (int j = 0; j < prevInfo.stackHeight; j++) {
				if (prevInfo.stack[j] 
				    instanceof UninitializedClassType)
				    throw new VerifyException
					("Uninitialized stack in back-branch");
			    }
			}
			if (mergeInfo(instr.getSuccs()[i],
				      (VerifyInfo) info.clone()))
			    instrStack.push(instr.getSuccs()[i]);
		    }
		}
		for (int i=0; i<handlers.length; i++) {
		    if (handlers[i].start.getAddr() <= instr.getAddr()
			&& handlers[i].end.getAddr() >= instr.getAddr()) {
			for (int j = 0; j < prevInfo.locals.length; j++) {
			    if (prevInfo.locals[j] 
				instanceof UninitializedClassType)
				throw new VerifyException
				    ("Uninitialized local in try block");
			}
			VerifyInfo excInfo = (VerifyInfo) prevInfo.clone();
			excInfo.stackHeight = 1;
			if (handlers[i].type != null)
			    excInfo.stack[0] = Type.tClass(handlers[i].type);
			else
			    excInfo.stack[0] = 
				Type.tClass("java.lang.Throwable");
			if (mergeInfo(handlers[i].catcher, excInfo))
			    instrStack.push(handlers[i].catcher);
		    }
		}
	    }
	}

	if ((GlobalOptions.debuggingFlags
	     & GlobalOptions.DEBUG_VERIFIER) != 0) {
	    for (Instruction instr = bi.getFirstInstr(); instr != null;
		 instr = instr.getNextByAddr()) {

		VerifyInfo info = (VerifyInfo) instr.getTmpInfo();
		if (info != null)
		    GlobalOptions.err.println(info.toString());
		GlobalOptions.err.println(instr.getDescription());

	    }
	}
	for (Instruction instr = bi.getFirstInstr(); instr != null;
	     instr = instr.getNextByAddr())
	    instr.setTmpInfo(null);
    }


    public void verify() throws VerifyException {
	try {
	    doVerify();
	} catch (VerifyException ex) {
	    for (Instruction instr = bi.getFirstInstr(); instr != null;
		 instr = instr.getNextByAddr()) {

		VerifyInfo info = (VerifyInfo) instr.getTmpInfo();
		if (info != null)
		    GlobalOptions.err.println(info.toString());
		GlobalOptions.err.println(instr.getDescription());

		instr.setTmpInfo(null);
	    }
	    throw ex;
	}
    }
}
