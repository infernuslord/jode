package jode.jvm;
import jode.bytecode.*;
import jode.type.*;
import jode.AssertError;
import jode.Decompiler;
import java.util.BitSet;
import java.util.Stack;

public class CodeVerifier implements Opcodes {
    ClassInfo ci;
    MethodInfo mi;
    BytecodeInfo bi;

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
    }

    public VerifyInfo initInfo() {
	VerifyInfo info = new VerifyInfo();
	Type[] paramTypes = mi.getType().getParameterTypes();
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
	if (t1.equals(t2))
	    return true;
	if (t1.getTypeCode() == Type.TC_INTEGER
	    && t2.getTypeCode() == Type.TC_INTEGER)
	    return true;
	if (t1 == Type.tUObject)
	    return (t2.getTypeCode() == Type.TC_CLASS
		    || t2.getTypeCode() == Type.TC_ARRAY
		    || t2.getTypeCode() == -Type.TC_CLASS);
	if (t2 == Type.tUObject)
	    return (t1.getTypeCode() == Type.TC_CLASS
		    || t1.getTypeCode() == Type.TC_ARRAY
		    || t1.getTypeCode() == -Type.TC_CLASS);
	if ((t1.getTypeCode() == Type.TC_CLASS
	     || t1.getTypeCode() == Type.TC_ARRAY)
	    && (t2.getTypeCode() == Type.TC_CLASS
		|| t2.getTypeCode() == Type.TC_ARRAY)) {
	    if (t1.getTypeCode() == Type.TC_ARRAY) {
		if (t2.getTypeCode() == Type.TC_CLASS)
		    return (t2 == Type.tObject);
		Type e1 = ((ArrayType)t1).getElementType();
		Type e2 = ((ArrayType)t2).getElementType();
		if ((e1.getTypeCode() == Type.TC_CLASS
		     || e1.getTypeCode() == Type.TC_ARRAY
		     || e1 == Type.tUObject)
		    && (e2.getTypeCode() == Type.TC_CLASS
			|| e2.getTypeCode() == Type.TC_ARRAY
			|| e2 == Type.tUObject))
		    return isOfType(e1, e2);
		return false;
	    } else {
		if (t2.getTypeCode() == Type.TC_ARRAY)
		    return false;
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
	if (t1.getTypeCode() == Type.TC_INTEGER
	    && t2.getTypeCode() == Type.TC_INTEGER)
	    return t1;
	if (t1 == Type.tUObject)
	    return (t2.getTypeCode() == Type.TC_CLASS
		    || t2.getTypeCode() == Type.TC_ARRAY
		    || t2.getTypeCode() == -Type.TC_CLASS) ? t2 : Type.tError;
	if (t2 == Type.tUObject)
	    return (t1.getTypeCode() == Type.TC_CLASS
		    || t1.getTypeCode() == Type.TC_ARRAY
		    || t1.getTypeCode() == -Type.TC_CLASS) ? t1 : Type.tError;
	if ((t1.getTypeCode() == Type.TC_CLASS
	     || t1.getTypeCode() == Type.TC_ARRAY)
	    && (t2.getTypeCode() == Type.TC_CLASS
		|| t2.getTypeCode() == Type.TC_ARRAY)) {
	    if (t1.getTypeCode() == Type.TC_ARRAY) {
		if (t2.getTypeCode() == Type.TC_CLASS)
		    return Type.tObject;
		Type e1 = ((ArrayType)t1).getElementType();
		Type e2 = ((ArrayType)t2).getElementType();
		if ((e1.getTypeCode() == Type.TC_CLASS
		     || e1.getTypeCode() == Type.TC_ARRAY
		     || e1 == Type.tUObject)
		    && (e2.getTypeCode() == Type.TC_CLASS
			|| e2.getTypeCode() == Type.TC_ARRAY
			|| e2 == Type.tUObject))
		    return Type.tArray(mergeType(e1, e2));
		return Type.tObject;
	    } else {
		if (t2.getTypeCode() == Type.TC_ARRAY)
		    return Type.tObject;
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
	if (instr.tmpInfo == null) {
	    instr.tmpInfo = info;
	    return true;
	}
	boolean changed = false;
	VerifyInfo oldInfo = (VerifyInfo) instr.tmpInfo;
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
	return changed;
    }


    Type[] types = { Type.tInt, Type.tLong, Type.tFloat, Type.tDouble,
		     Type.tUObject, Type.tByte, Type.tChar, Type.tShort };

    public VerifyInfo modelEffect(Instruction instr, VerifyInfo prevInfo) 
	throws VerifyException {
	int jsrLength = 
	    prevInfo.jsrTargets != null ? prevInfo.jsrTargets.length : 0;
	VerifyInfo result = (VerifyInfo) prevInfo.clone();
	switch (instr.opcode) {
	case opc_nop:
	case opc_goto:
	    break;
	case opc_ldc: {
	    Type type;
	    if (instr.objData == null)
		type = Type.tUObject;
	    else if (instr.objData instanceof Integer)
		type = Type.tInt;
	    else if (instr.objData instanceof Float)
		type = Type.tFloat;
	    else
		type = Type.tString;
	    result.push(type);
	    break;
	}
	case opc_ldc2_w: {
	    Type type;
	    if (instr.objData instanceof Long)
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
		&& (!result.jsrLocals[jsrLength-1].get(instr.localSlot)
		    || ((instr.opcode & 0x1) == 0
			&& !result.jsrLocals[jsrLength-1]
			.get(instr.localSlot+1)))) {
		result.jsrLocals = (BitSet[]) result.jsrLocals.clone();
		result.jsrLocals[jsrLength-1]
		    = (BitSet) result.jsrLocals[jsrLength-1].clone();
		result.jsrLocals[jsrLength-1].set(instr.localSlot);
		if ((instr.opcode & 0x1) == 0)
		    result.jsrLocals[jsrLength-1].set(instr.localSlot + 1);
	    }
	    if ((instr.opcode & 0x1) == 0
		&& result.locals[instr.localSlot+1] != Type.tVoid)
		throw new VerifyException(instr.getDescription());
	    Type type = result.locals[instr.localSlot];
	    if (!isOfType(type, types[instr.opcode - opc_iload]))
		throw new VerifyException(instr.getDescription());
	    result.push(type);
	    if ((instr.opcode & 0x1) == 0)
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
			  Type.tArray(types[instr.opcode - opc_iaload]))
		&& (instr.opcode != opc_baload
		    || !isOfType(arrType, Type.tArray(Type.tBoolean))))
		throw new VerifyException(instr.getDescription());
	    
	    Type elemType = (arrType == Type.tUObject ? Type.tUObject
			     :((ArrayType)arrType).getElementType());
	    result.push(elemType);
	    if (((1 << instr.opcode - opc_iaload) & 0xa) != 0)
		result.push(Type.tVoid);
	    break;
	}
	case opc_istore: case opc_lstore: 
	case opc_fstore: case opc_dstore: case opc_astore: {
	    if (jsrLength > 0
		&& (!result.jsrLocals[jsrLength-1].get(instr.localSlot)
		    || ((instr.opcode & 0x1) != 0
			&& !result.jsrLocals[jsrLength-1]
			.get(instr.localSlot+1)))) {
		result.jsrLocals = (BitSet[]) result.jsrLocals.clone();
		result.jsrLocals[jsrLength-1]
		    = (BitSet) result.jsrLocals[jsrLength-1].clone();
		result.jsrLocals[jsrLength-1].set(instr.localSlot);
		if ((instr.opcode & 0x1) != 0)
		    result.jsrLocals[jsrLength-1].set(instr.localSlot + 1);
	    }
	    if ((instr.opcode & 0x1) != 0
		&& result.pop() != Type.tVoid)
		throw new VerifyException(instr.getDescription());
	    Type type = result.pop();
	    if (instr.opcode != opc_astore
		|| !(type instanceof ReturnAddressType))
		if (!isOfType(type, types[instr.opcode - opc_istore]))
		    throw new VerifyException(instr.getDescription());
	    result.locals[instr.localSlot] = type;
	    if ((instr.opcode & 0x1) != 0)
		result.locals[instr.localSlot+1] = Type.tVoid;
	    break;
	}
	case opc_iastore: case opc_lastore:
	case opc_fastore: case opc_dastore: case opc_aastore:
	case opc_bastore: case opc_castore: case opc_sastore: {
	    if (((1 << instr.opcode - opc_iastore) & 0xa) != 0
		&& result.pop() != Type.tVoid)
		throw new VerifyException(instr.getDescription());
	    Type type = result.pop();
	    if (!isOfType(result.pop(), Type.tInt))
		throw new VerifyException(instr.getDescription());
	    Type arrType = result.pop();
	    if (!isOfType(arrType, 
			  Type.tArray(types[instr.opcode - opc_iastore]))
		&& (instr.opcode != opc_bastore 
		    || !isOfType(arrType, Type.tArray(Type.tBoolean))))
		throw new VerifyException(instr.getDescription());
	    Type elemType = instr.opcode >= opc_bastore ? Type.tInt
		: types[instr.opcode - opc_iastore];
	    if (!isOfType(type, elemType))
		throw new VerifyException(instr.getDescription());
	    break;
	}
	case opc_pop: case opc_pop2: {
	    int count = instr.opcode - (opc_pop-1);
	    result.need(count);
	    result.stackHeight -= count;
	    break;
	}
	case opc_dup: case opc_dup_x1: case opc_dup_x2: {
	    int depth = instr.opcode - opc_dup;
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
	    int depth = instr.opcode - opc_dup2;
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
	    Type type = types[(instr.opcode - opc_iadd) & 3];
	    if ((instr.opcode & 1) != 0
		&& result.pop() != Type.tVoid)
		throw new VerifyException(instr.getDescription());
	    if (!isOfType(result.pop(), type))
		throw new VerifyException(instr.getDescription());
	    if ((instr.opcode & 1) != 0) {
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
	    Type type = types[(instr.opcode - opc_ineg) & 3];
	    if ((instr.opcode & 1) != 0) {
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
	    
	    if ((instr.opcode & 1) != 0) {
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
	    if ((instr.opcode & 1) != 0
		&& result.pop() != Type.tVoid)
		throw new VerifyException(instr.getDescription());
	    if (!isOfType(result.pop(),
			  types[instr.opcode & 1]))
		throw new VerifyException(instr.getDescription());
	    if ((instr.opcode & 1) != 0) {
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
	    if (!isOfType(result.locals[instr.localSlot], Type.tInt))
		throw new VerifyException(instr.getDescription());
	    break;
        case opc_i2l: case opc_i2f: case opc_i2d:
        case opc_l2i: case opc_l2f: case opc_l2d:
        case opc_f2i: case opc_f2l: case opc_f2d:
        case opc_d2i: case opc_d2l: case opc_d2f: {
            int from = (instr.opcode-opc_i2l)/3;
            int to   = (instr.opcode-opc_i2l)%3;
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
	    if (((1 << instr.opcode - opc_ireturn) & 0xa) != 0
		&& result.pop() != Type.tVoid)
		throw new VerifyException(instr.getDescription());
	    Type type = result.pop();
	    if (!isOfType(type, types[instr.opcode - opc_ireturn])
		|| !isOfType(type, mi.getType().getReturnType()))
		throw new VerifyException(instr.getDescription());
	    break;
	}
	case opc_jsr: {
	    result.stack[result.stackHeight++]
		= new ReturnAddressType(instr.succs[0]);
	    result.jsrTargets = new Instruction[jsrLength+1];
	    result.jsrLocals = new BitSet[jsrLength+1];
	    if (jsrLength > 0) {
		for (int i=0; i< prevInfo.jsrTargets.length; i++)
		    if (prevInfo.jsrTargets[i] == instr.succs[0])
			throw new VerifyException(instr.getDescription()+
						  " is recursive");
		System.arraycopy(prevInfo.jsrTargets, 0, 
				 result.jsrTargets, 0, jsrLength);
		System.arraycopy(prevInfo.jsrLocals, 0, 
				 result.jsrLocals, 0, jsrLength);
	    }
	    result.jsrTargets[jsrLength] = instr.succs[0];
	    result.jsrLocals[jsrLength] = new BitSet();
	    break;
	}
	case opc_return:
	    if (mi.getType().getReturnType() != Type.tVoid)
		throw new VerifyException(instr.getDescription());
	    break;

	case opc_getstatic: {
	    Reference ref = (Reference) instr.objData;
	    Type type = Type.tType(ref.getType());
	    result.push(type);
	    if (type.stackSize() == 2)
		result.push(Type.tVoid);
	    break;
	}
	case opc_getfield: {
	    Reference ref = (Reference) instr.objData;
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
	    Reference ref = (Reference) instr.objData;
	    Type type = Type.tType(ref.getType());
	    if (type.stackSize() == 2
		&& result.pop() != Type.tVoid)
		throw new VerifyException(instr.getDescription());
	    if (!isOfType(result.pop(), type))
		throw new VerifyException(instr.getDescription());
	    break;
	}
	case opc_putfield: {
	    Reference ref = (Reference) instr.objData;
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
	    Reference ref = (Reference) instr.objData;
	    MethodType mt = (MethodType) Type.tType(ref.getType());
	    Type[] paramTypes = mt.getParameterTypes();
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
	    } else if (instr.opcode != opc_invokestatic) {
		Type classType = Type.tType(ref.getClazz());
		if (!isOfType(result.pop(), classType))
		    throw new VerifyException(instr.getDescription());
	    }
	    Type type = mt.getReturnType();
	    if (type != Type.tVoid) {
		result.push(type);
		if (type.stackSize() == 2)
		    result.push(Type.tVoid);
	    }
	    break;
	}
	case opc_new: {
	    String clName = (String) instr.objData;
	    ClassInfo ci = ClassInfo.forName
		(clName.substring(1, clName.length()-1).replace('/','.'));
	    result.stack[result.stackHeight++] = 
		new UninitializedClassType(ci, false);
	    break;
	}
	case opc_arraylength: {
	    if (result.pop().getTypeCode()
		!= Type.TC_ARRAY)
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
	    Type classType = Type.tType((String) instr.objData);
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
	    int dimension = instr.intData;
	    for (int i=dimension - 1; i >= 0; i--)
		if (!isOfType(result.pop(), Type.tInt))
		    throw new VerifyException(instr.getDescription());
	    Type classType = Type.tType((String) instr.objData);
	    result.push(classType);
	    break;
	}
	default:
	    throw new AssertError("Invalid opcode "+instr.opcode);
	}
	return result;
    }
    
    public void doVerify() throws VerifyException {
	Stack instrStack = new Stack();
	
	bi.getFirstInstr().tmpInfo = initInfo();
	instrStack.push(bi.getFirstInstr());
	Handler[] handlers = bi.getExceptionHandlers();
	while (!instrStack.isEmpty()) {
	    Instruction instr = (Instruction) instrStack.pop();
	    if (!instr.alwaysJumps && instr.nextByAddr == null)
		throw new VerifyException("Flow can fall off end of method");

	    VerifyInfo prevInfo = (VerifyInfo) instr.tmpInfo;
	    if (instr.opcode == opc_ret) {
		if (prevInfo.jsrTargets == null
		    || !(prevInfo.locals[instr.localSlot] 
			 instanceof ReturnAddressType))
		    throw new VerifyException(instr.getDescription());
		int jsrLength = prevInfo.jsrTargets.length - 1;
		Instruction jsrTarget = 
		    ((ReturnAddressType) 
		     prevInfo.locals[instr.localSlot]).jsrTarget;
		if (jsrTarget != prevInfo.jsrTargets[jsrLength])
		    throw new VerifyException(instr.getDescription());
		VerifyInfo jsrTargetInfo = (VerifyInfo) jsrTarget.tmpInfo;
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
		BitSet usedLocals = prevInfo.jsrLocals[jsrLength];
		for (int i=0; i < jsrTarget.preds.length; i++) {
		    Instruction jsrInstr = jsrTarget.preds[i];
		    VerifyInfo jsrInfo = (VerifyInfo) jsrInstr.tmpInfo;
		    if (jsrInfo == null)
			continue;
		    VerifyInfo afterJsrInfo = (VerifyInfo) jsrInfo.clone();
		    for (int j = 0; j < bi.getMaxLocals(); j++) {
			if (usedLocals.get(j))
			    afterJsrInfo.locals[j] = prevInfo.locals[j];
		    }
		    if (mergeInfo(jsrInstr.nextByAddr, afterJsrInfo))
			instrStack.push(jsrInstr.nextByAddr);
		}
	    } else {
		VerifyInfo info = modelEffect(instr, prevInfo);
		if (!instr.alwaysJumps)
		    if (mergeInfo(instr.nextByAddr, info))
			instrStack.push(instr.nextByAddr);
		if (instr.opcode == opc_jsr) {
		    VerifyInfo targetInfo = 
			(VerifyInfo) instr.succs[0].tmpInfo;
		    if (targetInfo != null && targetInfo.retInstr != null) {
			VerifyInfo afterJsrInfo
			    = (VerifyInfo) prevInfo.clone();
			VerifyInfo retInfo
			    = (VerifyInfo) targetInfo.retInstr.tmpInfo;
			BitSet usedLocals
			    = retInfo.jsrLocals[retInfo.jsrLocals.length-1];
			for (int j = 0; j < bi.getMaxLocals(); j++) {
			    if (usedLocals.get(j))
				afterJsrInfo.locals[j] = retInfo.locals[j];
			}
			if (mergeInfo(instr.nextByAddr, afterJsrInfo))
			    instrStack.push(instr.nextByAddr);
		    }
		}
		if (instr.succs != null) {
		    for (int i=0; i< instr.succs.length; i++) {
			if (instr.succs[i].addr < instr.addr) {
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
			if (mergeInfo(instr.succs[i],
				      (VerifyInfo) info.clone()))
			    instrStack.push(instr.succs[i]);
		    }
		}
		for (int i=0; i<handlers.length; i++) {
		    if (handlers[i].start.addr <= instr.addr
			&& handlers[i].end.addr >= instr.addr) {
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

	for (Instruction instr = bi.getFirstInstr(); instr != null;
	     instr = instr.nextByAddr)
	    instr.tmpInfo = null;
    }


    public void verify() throws VerifyException {
	try {
	    doVerify();
	} catch (VerifyException ex) {
	    for (Instruction instr = bi.getFirstInstr(); instr != null;
		 instr = instr.nextByAddr) {

		VerifyInfo info = (VerifyInfo) instr.tmpInfo;
		if (info != null)
		    System.err.println(info.toString());
		System.err.println(instr.getDescription());

		instr.tmpInfo = null;
	    }
	    throw ex;
	}
    }
}
