/* ConstantAnalyzer Copyright (C) 1999 Jochen Hoenicke.
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

import jode.AssertError;
import jode.GlobalOptions;
import jode.type.MethodType;
import jode.type.Type;
import jode.bytecode.*;
import jode.jvm.InterpreterException;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;
import java.util.BitSet;

///#ifdef JDK12
///import java.util.Arrays;
///import java.util.Collection;
///import java.util.HashSet;
///import java.util.Set;
///import java.util.HashMap;
///import java.util.Map;
///import java.util.Iterator;
///#else
import jode.util.Arrays;
import jode.util.Collection;
import jode.util.HashSet;
import jode.util.Set;
import jode.util.HashMap;
import jode.util.Map;
import jode.util.Iterator;
///#endif

/**
 * Analyze the code, assuming every field that is not yet written to
 * is constant.  This may imply that some code is dead code.
 *
 * @author Jochen Hoenicke
 */
public class ConstantAnalyzer implements Opcodes, CodeAnalyzer {
    boolean working;
    Map constInfos;

    BytecodeInfo bytecode;
    Identifier listener;

    private static ConstantRuntimeEnvironment runtime
	= new ConstantRuntimeEnvironment();

    private final static int CMP_EQ = 0;
    private final static int CMP_NE = 1;
    private final static int CMP_LT = 2;
    private final static int CMP_GE = 3;
    private final static int CMP_GT = 4;
    private final static int CMP_LE = 5;
    private final static int CMP_GREATER_MASK
	= (1 << CMP_GT)|(1 << CMP_GE)|(1 << CMP_NE);
    private final static int CMP_LESS_MASK
	= (1 << CMP_LT)|(1 << CMP_LE)|(1 << CMP_NE);
    private final static int CMP_EQUAL_MASK
	= (1 << CMP_GE)|(1 << CMP_LE)|(1 << CMP_EQ);

    final static int REACHABLE     = 0x1;
    final static int CONSTANT      = 0x2;
    final static int CONSTANTFLOW  = 0x4;

    private interface ConstantListener {
	public void constantChanged();
    }

    private static class JSRTargetInfo implements Cloneable {
	Instruction jsrTarget;
	BitSet usedLocals;
	StackLocalInfo retInfo = null;

	public JSRTargetInfo(Instruction target) {
	    jsrTarget = target;
	    usedLocals = new BitSet();
	}

	public JSRTargetInfo copy() {
	    try {
		JSRTargetInfo result = (JSRTargetInfo) clone();
		result.usedLocals = (BitSet) usedLocals.clone();
		return result;
	    } catch (CloneNotSupportedException ex) {
		throw new IncompatibleClassChangeError(ex.getMessage());
	    }
	}

	public void merge(JSRTargetInfo o) {
	    BitSet used = (BitSet) usedLocals.clone();
	    used.or(o.usedLocals);
	    if (retInfo != null) {
		if (o.retInfo != null && o.retInfo != retInfo)
		    throw new AssertError("retInfos differ");
  		o.retInfo = retInfo;
		retInfo.enqueue();
	    }
	}

	public String toString() {
	    StringBuffer sb = new StringBuffer(String.valueOf(jsrTarget));
	    if (retInfo != null)
		sb.append("->").append(retInfo.instr);
	    return sb.append(usedLocals).toString();
	}
    }

    private static class ConstValue implements ConstantListener {
	public final static Object VOLATILE = new Object();
	/**
	 * The constant value, VOLATILE if value is not constant.
	 * This may also be an instance of JSRTargetInfo
	 */
	Object value;
	/**
	 * The number of slots this value takes on the stack.
	 */
	int stackSize;
	/**
	 * The constant listeners, that want to be informed if this is
	 * no longer constant.
	 */
	Set listeners;
	
	public ConstValue(Object constant) {
	    value = constant;
	    stackSize = (constant instanceof Double
			 || constant instanceof Long) ? 2 : 1;
	    listeners = new HashSet();
	}
	
	public ConstValue(ConstValue constant) {
	    value = constant.value;
	    stackSize = constant.stackSize;
	    listeners = new HashSet();
	    constant.addConstantListener(this);
	}
	
	public ConstValue(int stackSize) {
	    this.value = VOLATILE;
	    this.stackSize = stackSize;
	}
	
	public ConstValue copy() {
	    return (value == VOLATILE) ? this
		: new ConstValue(this);
	}
	
	public void addConstantListener(ConstantListener l) {
	    listeners.add(l);
	}
	
	public void removeConstantListener(ConstantListener l) {
	    listeners.remove(l);
	}
	
	public void fireChanged() {
	    value = VOLATILE;
	    for (Iterator i = listeners.iterator(); i.hasNext(); )
		((ConstantListener) i.next()).constantChanged();
	    listeners = null;
	}
	
	public void constantChanged() {
	    if (value != VOLATILE)
		fireChanged();
	}
	
	/**
	 * Merge the other value into this value.
	 */
	public void merge(ConstValue other) {
	    if (this == other)
		return;

	    if (value == other.value
		|| (value != null && value.equals(other.value))) {
		if (value != VOLATILE) {
//  		    other.addConstantListener(this);
		    this.addConstantListener(other);
		}
		return;
	    }

	    if (value instanceof JSRTargetInfo
		&& other.value instanceof JSRTargetInfo) {
		((JSRTargetInfo) value).merge((JSRTargetInfo) other.value);
		return;
	    }
	    
	    if (value != VOLATILE)
		fireChanged();
//  	    if (other.value != VOLATILE)
//  		other.fireChanged();
	}

	public String toString() {
	    return value == VOLATILE ? "vol("+stackSize+")" : ""+value;
	}
    }

    private static class StackLocalInfo implements ConstantListener {
	ConstValue[] stack;
	ConstValue[] locals;
	Instruction instr;

	/**
	 * The queue that should be notified, if the constant values of
	 * this instruction changes.  We put ourself on this queue in that
	 * case.
	 */
	Collection notifyQueue;

	public ConstValue copy(ConstValue value) {
	    return (value == null) ? null : value.copy();
	}

	private StackLocalInfo(ConstValue[] stack, 
			       ConstValue[] locals,
			       Collection notifyQueue) {
	    this.stack = stack;
	    this.locals = locals;
	    this.notifyQueue = notifyQueue;
	}
    
	public StackLocalInfo(int numLocals, 
			      boolean isStatic, String methodTypeSig,
			      Collection notifyQueue) {
	    
	    MethodType mt = Type.tMethod(methodTypeSig);
	    locals = new ConstValue[numLocals];
	    stack = new ConstValue[0];
	    this.notifyQueue = notifyQueue;
	    int slot = 0;
	    if (!isStatic)
		locals[slot++] = new ConstValue(1);
	    for (int i=0; i< mt.getParameterTypes().length; i++) {
		int stackSize = mt.getParameterTypes()[i].stackSize();
		locals[slot] = unknownValue[stackSize-1];
		slot += stackSize;
	    }
	}

	public final void enqueue() {
	    notifyQueue.add(this);
	}
    
	public void constantChanged() {
	    enqueue();
	}
    
	public StackLocalInfo poppush(int pops, ConstValue push) {
	    ConstValue[] newStack 
		= new ConstValue[stack.length - pops + push.stackSize];
	    ConstValue[] newLocals = (ConstValue[]) locals.clone();
	    System.arraycopy(stack, 0, newStack, 0, stack.length-pops);
	    newStack[stack.length-pops] = push.copy();
	    return new StackLocalInfo(newStack, newLocals, notifyQueue);
	}

	public StackLocalInfo pop(int pops) {
	    ConstValue[] newStack 
		= new ConstValue[stack.length - pops];
	    ConstValue[] newLocals = (ConstValue[]) locals.clone();
	    System.arraycopy(stack, 0, newStack, 0, stack.length-pops);
	    return new StackLocalInfo(newStack, newLocals, notifyQueue);
	}

	public StackLocalInfo dup(int count, int depth) {
	    ConstValue[] newStack 
		= new ConstValue[stack.length + count];
	    ConstValue[] newLocals = (ConstValue[]) locals.clone();
	    if (depth == 0)
		System.arraycopy(stack, 0, newStack, 0, stack.length);
	    else {
		int pos = stack.length - count - depth;
		System.arraycopy(stack, 0, newStack, 0, pos);
		for (int i=0; i < count; i++)
		    newStack[pos++] = copy(stack[stack.length-count + i]);
		for (int i=0; i < depth; i++)
		    newStack[pos++] = copy(stack[stack.length-count-depth + i]);
	    }
	    for (int i=0; i < count; i++)
		newStack[stack.length+i] = copy(stack[stack.length-count + i]);
	    return new StackLocalInfo(newStack, newLocals, notifyQueue);
	}

	public StackLocalInfo swap() {
	    ConstValue[] newStack 
		= new ConstValue[stack.length];
	    ConstValue[] newLocals = (ConstValue[]) locals.clone();
	    System.arraycopy(stack, 0, newStack, 0, stack.length - 2);
	    newStack[stack.length-2] = stack[stack.length-1].copy();
	    newStack[stack.length-1] = stack[stack.length-2].copy();
	    return new StackLocalInfo(newStack, newLocals, notifyQueue);
	}

	public StackLocalInfo copy() {
	    ConstValue[] newStack = (ConstValue[]) stack.clone();
	    ConstValue[] newLocals = (ConstValue[]) locals.clone();
	    return new StackLocalInfo(newStack, newLocals, notifyQueue);
	}

	public ConstValue getLocal(int slot) {
	    return locals[slot];
	}

	public ConstValue getStack(int depth) {
	    return stack[stack.length - depth];
	}

	public StackLocalInfo setLocal(int slot, ConstValue value) {
	    locals[slot] = value;
	    if (value != null && value.stackSize == 2)
		locals[slot+1] = null;
	    for (int i=0; i< locals.length; i++) {
		if (locals[i] != null
		    && locals[i].value instanceof JSRTargetInfo) {
		    locals[i] = locals[i].copy();
		    locals[i].value = ((JSRTargetInfo) locals[i].value).copy();
		    ((JSRTargetInfo)locals[i].value).usedLocals.set(slot);
		}
	    }
	    for (int i=0; i< stack.length; i++) {
		if (stack[i] != null
		    && stack[i].value instanceof JSRTargetInfo) {
		    locals[i] = locals[i].copy();
		    locals[i].value = ((JSRTargetInfo)locals[i].value).copy();
		    ((JSRTargetInfo)locals[i].value).usedLocals.set(slot);
		}
	    }
	    return new StackLocalInfo(stack, locals, notifyQueue);
	}
	
	public void merge(StackLocalInfo other) {
	    for (int i=0; i < locals.length; i++) {
		if (locals[i] != null) {
		    if (other.locals[i] == null) {
			locals[i] = null;
			enqueue();
		    } else {
			locals[i].merge(other.locals[i]);
		    }
		}
	    }
	    if (stack.length != other.stack.length)
		throw new jode.AssertError("stack length differs");
	    for (int i=0; i < stack.length; i++) {
		if ((other.stack[i] == null) != (stack[i] == null))
		    throw new jode.AssertError("stack types differ");
		else if (stack[i] != null)
		    stack[i].merge(other.stack[i]);
	    }
	}

	public String toString() {
	    return "Locals: "+Arrays.asList(locals)
		+"Stack: "+Arrays.asList(stack)+ "Instr: "+instr;
	}
    }

    private static class ConstantInfo implements ConstantListener {
	ConstantInfo() {
	    this(REACHABLE, null);
	}

	ConstantInfo(int flags) {
	    this(flags, null);
	}

	ConstantInfo(int flags, Object constant) {
	    this.flags = flags;
	    this.constant = constant;
	}
	
	int flags;
	/**
	 * The constant, may be an Instruction for CONSTANTFLOW.
	 */
	Object constant;

	public void constantChanged() {
	    constant = null;
	    flags &= ~(CONSTANT | CONSTANTFLOW);
	}
    }

    private static ConstValue[] unknownValue = {
	new ConstValue(1), new ConstValue(2)
    };

    private static ConstantInfo unknownConstInfo = new ConstantInfo();

    public ConstantAnalyzer() {
    }

    public void mergeInfo(Instruction instr, 
			  StackLocalInfo info) {
	if (instr.tmpInfo == null) {
	    instr.tmpInfo = info;
	    info.instr = instr;
	    info.enqueue();
	} else
	    ((StackLocalInfo)instr.tmpInfo).merge(info);
    }

    public Identifier canonizeReference(Instruction instr) {
	Reference ref = (Reference) instr.objData;
	Identifier ident = Main.getClassBundle().getIdentifier(ref);
	String clName = ref.getClazz();
	String realClazzName;
	if (ident != null) {
	    ClassIdentifier clazz = (ClassIdentifier)ident.getParent();
	    realClazzName = "L" + (clazz.getFullName()
				   .replace('.', '/')) + ";";
	} else {
	    /* We have to look at the ClassInfo's instead, to
	     * point to the right method.
	     */
	    ClassInfo clazz;
	    if (clName.charAt(0) == '[') {
		/* Arrays don't define new methods (well clone(),
		 * but that can be ignored).
		 */
		clazz = ClassInfo.javaLangObject;
	    } else {
		clazz = ClassInfo.forName
		    (clName.substring(1, clName.length()-1)
		     .replace('/','.'));
	    }
	    while (clazz != null
		   && clazz.findMethod(ref.getName(), 
				       ref.getType()) == null)
		clazz = clazz.getSuperclass();

	    if (clazz == null) {
		GlobalOptions.err.println("WARNING: Can't find reference: "
					  +ref);
		realClazzName = clName;
	    } else
		realClazzName = "L" + clazz.getName().replace('.', '/') + ";";
	}
	if (!realClazzName.equals(ref.getClazz())) {
	    ref = Reference.getReference(realClazzName, 
					 ref.getName(), ref.getType());
	    instr.objData = ref;
	}
	return ident;
    }

    public void handleReference(Reference ref, boolean isVirtual) {
	String clName = ref.getClazz();
	/* Don't have to reach array methods */
	if (clName.charAt(0) != '[') {
	    clName = clName.substring(1, clName.length()-1).replace('/', '.');
	    Main.getClassBundle().reachableIdentifier
		(clName+"."+ref.getName()+"."+ref.getType(), isVirtual);
	}
    }

    public void handleClass(String clName) {
	int i = 0;
	while (i < clName.length() && clName.charAt(i) == '[')
	    i++;
	if (i < clName.length() && clName.charAt(i) == 'L') {
	    clName = clName.substring(i+1, clName.length()-1);
	    Main.getClassBundle().reachableIdentifier(clName, false);
	}
    }

    public void handleOpcode(StackLocalInfo info) {
	Instruction instr = info.instr;
	constInfos.put(instr, unknownConstInfo);
	
	int opcode = instr.opcode;
	Handler[] handlers = bytecode.getExceptionHandlers();
	for (int i=0; i< handlers.length; i++) {
	    if (handlers[i].start.addr <= instr.addr
		&& handlers[i].end.addr >= instr.addr)
		mergeInfo(handlers[i].catcher, 
			  info.poppush(info.stack.length, unknownValue[0]));
	}
	ConstValue result;
	switch (opcode) {
        case opc_nop:
	    mergeInfo(instr.nextByAddr, info.pop(0));
	    break;

        case opc_ldc:
        case opc_ldc2_w:
	    result = new ConstValue(instr.objData);
	    mergeInfo(instr.nextByAddr, info.poppush(0, result));
	    break;

        case opc_iload: case opc_lload: 
        case opc_fload: case opc_dload: case opc_aload:
	    result = info.getLocal(instr.localSlot);
	    if (result == null) {
		dumpStackLocalInfo();
		System.err.println(info);
		System.err.println(instr);
	    }
	    if (result.value != ConstValue.VOLATILE) {
		ConstantInfo shortInfo = new ConstantInfo();
		constInfos.put(instr, shortInfo);
		shortInfo.flags |= CONSTANT;
		shortInfo.constant = result.value;
		result.addConstantListener(shortInfo);
	    }
	    mergeInfo(instr.nextByAddr, 
		      info.poppush(0, result)
		      .setLocal(instr.localSlot, result.copy()));
	    break;
        case opc_iaload: case opc_laload: 
        case opc_faload: case opc_daload: case opc_aaload:
        case opc_baload: case opc_caload: case opc_saload: {
//  	    ConstValue array = info.getStack(2);
//  	    ConstValue index = info.getStack(1);
//  	    ConstValue newValue = null;
//  	    if (index.value != index.ConstValue.VOLATILE
//  		&& array.value != array.ConstValue.VOLATILE
//  		&& array.value != null) {
//  		int indexVal = ((Integer) index.value).intValue();
//  		Object content;
//  		switch(opcode) {
//  		case opc_baload: 
//  		    content = new Integer
//  			(array.value instanceof byte[]
//  			 ? ((byte[])array.value)[indexVal]
//  			 : ((boolean[])array.value)[indexVal] ? 1 : 0);
//  		case opc_caload: 
//  		    content = new Integer(((char[])array.value)[indexVal]);
//  		    break;
//  		case opc_saload:
//  		    content = new Integer(((short[])array.value)[indexVal]);
//  		    break;
//  		case opc_iaload: 
//  		case opc_laload: 
//  		case opc_faload: 
//  		case opc_daload: 
//  		case opc_aaload:
//  		    content = Array.get(array.value, indexVal);
//  		    break;
//  		default:
//  		    throw new jode.AssertError("Can't happen.");
//  		}
//  		result = new ConstValue(content);
//  		array.addConstantListener(result);
//  		index.addConstantListener(result);
//  	    } else {
	    result = unknownValue[(opcode == opc_laload
				   || opcode == opc_daload) ? 1 : 0];
//  	    }
	    mergeInfo(instr.nextByAddr, info.poppush(2, result));
	    break;
	}
        case opc_istore: case opc_fstore: case opc_astore: {
	    mergeInfo(instr.nextByAddr, 
		      info.pop(1).setLocal(instr.localSlot, info.getStack(1)));
	    break;
	}
	case opc_lstore: case opc_dstore: {
	    mergeInfo(instr.nextByAddr, 
		      info.pop(2).setLocal(instr.localSlot, info.getStack(2)));
	    break;
	}
        case opc_iastore: case opc_lastore:
        case opc_fastore: case opc_dastore: case opc_aastore:
        case opc_bastore: case opc_castore: case opc_sastore: {
	    int size = (opcode == opc_lastore
			|| opcode == opc_dastore) ? 2 : 1;
	    mergeInfo(instr.nextByAddr, info.pop(2+size));
	    break;
	}
        case opc_pop: case opc_pop2:
	    mergeInfo(instr.nextByAddr, info.pop(opcode - (opc_pop - 1)));
	    break;

	case opc_dup: case opc_dup_x1: case opc_dup_x2:
        case opc_dup2: case opc_dup2_x1: case opc_dup2_x2:
	    mergeInfo(instr.nextByAddr,
		      info.dup((opcode - (opc_dup - 3)) / 3,
			       (opcode - (opc_dup - 3)) % 3));
	    break;
        case opc_swap:
	    mergeInfo(instr.nextByAddr, info.swap());
	    break;

        case opc_iadd: case opc_ladd: case opc_fadd: case opc_dadd:
        case opc_isub: case opc_lsub: case opc_fsub: case opc_dsub:
        case opc_imul: case opc_lmul: case opc_fmul: case opc_dmul:
        case opc_idiv: case opc_ldiv: case opc_fdiv: case opc_ddiv:
        case opc_irem: case opc_lrem: case opc_frem: case opc_drem:
        case opc_iand: case opc_land:
        case opc_ior : case opc_lor :
        case opc_ixor: case opc_lxor: {
	    int size = 1 + (opcode - opc_iadd & 1);
	    ConstValue value1 = info.getStack(2*size);
	    ConstValue value2 = info.getStack(1*size);
	    boolean known = value1.value != ConstValue.VOLATILE
		&& value2.value != ConstValue.VOLATILE;
	    if (known) {
		if ((opcode == opc_idiv 
		     && ((Integer)value2.value).intValue() == 0)
		    || (opcode == opc_ldiv 
			&& ((Long)value2.value).longValue() == 0))
		    known = false;
	    }
	    if (known) {
		Object newValue;
		switch (opcode) {
		case opc_iadd: 
		    newValue = new Integer
			(((Integer)value1.value).intValue()
			 + ((Integer)value2.value).intValue());
		    break;
		case opc_isub: 
		    newValue = new Integer
			(((Integer)value1.value).intValue()
			 - ((Integer)value2.value).intValue());
		    break;
		case opc_imul: 
		    newValue = new Integer
			(((Integer)value1.value).intValue()
			 * ((Integer)value2.value).intValue());
		    break;
		case opc_idiv: 
		    newValue = new Integer
			(((Integer)value1.value).intValue()
			 / ((Integer)value2.value).intValue());
		    break;
		case opc_irem: 
		    newValue = new Integer
			(((Integer)value1.value).intValue()
			 % ((Integer)value2.value).intValue());
		    break;
		case opc_iand: 
		    newValue = new Integer
			(((Integer)value1.value).intValue()
			 & ((Integer)value2.value).intValue());
		    break;
		case opc_ior: 
		    newValue = new Integer
			(((Integer)value1.value).intValue()
			 | ((Integer)value2.value).intValue());
		    break;
		case opc_ixor: 
		    newValue = new Integer
			(((Integer)value1.value).intValue()
			 ^ ((Integer)value2.value).intValue());
		    break;
		    
		case opc_ladd: 
		    newValue = new Long
			(((Long)value1.value).longValue()
			 + ((Long)value2.value).longValue());
		    break;
		case opc_lsub: 
		    newValue = new Long
			(((Long)value1.value).longValue()
			 - ((Long)value2.value).longValue());
		    break;
		case opc_lmul: 
		    newValue = new Long
			(((Long)value1.value).longValue()
			 * ((Long)value2.value).longValue());
		    break;
		case opc_ldiv: 
		    newValue = new Long
			(((Long)value1.value).longValue()
			 / ((Long)value2.value).longValue());
		    break;
		case opc_lrem: 
		    newValue = new Long
			(((Long)value1.value).longValue()
			 % ((Long)value2.value).longValue());
		    break;
		case opc_land: 
		    newValue = new Long
			(((Long)value1.value).longValue()
			 & ((Long)value2.value).longValue());
		    break;
		case opc_lor: 
		    newValue = new Long
			(((Long)value1.value).longValue()
			 | ((Long)value2.value).longValue());
		    break;
		case opc_lxor: 
		    newValue = new Long
			(((Long)value1.value).longValue()
			 ^ ((Long)value2.value).longValue());
		    break;
		    
		case opc_fadd: 
		    newValue = new Float
			(((Float)value1.value).floatValue()
			 + ((Float)value2.value).floatValue());
		    break;
		case opc_fsub: 
		    newValue = new Float
			(((Float)value1.value).floatValue()
			 - ((Float)value2.value).floatValue());
		    break;
		case opc_fmul: 
		    newValue = new Float
			(((Float)value1.value).floatValue()
			 * ((Float)value2.value).floatValue());
		    break;
		case opc_fdiv: 
		    newValue = new Float
			(((Float)value1.value).floatValue()
			 / ((Float)value2.value).floatValue());
		    break;
		case opc_frem: 
		    newValue = new Float
			(((Float)value1.value).floatValue()
			 % ((Float)value2.value).floatValue());
		    break;
		    
		case opc_dadd: 
		    newValue = new Double
			(((Double)value1.value).doubleValue()
			 + ((Double)value2.value).doubleValue());
		    break;
		case opc_dsub: 
		    newValue = new Double
			(((Double)value1.value).doubleValue()
			 - ((Double)value2.value).doubleValue());
		    break;
		case opc_dmul: 
		    newValue = new Double
			(((Double)value1.value).doubleValue()
			 * ((Double)value2.value).doubleValue());
		    break;
		case opc_ddiv: 
		    newValue = new Double
			(((Double)value1.value).doubleValue()
			 / ((Double)value2.value).doubleValue());
		    break;
		case opc_drem: 
		    newValue = new Double
			(((Double)value1.value).doubleValue()
			 % ((Double)value2.value).doubleValue());
		    break;
		default:
		    throw new jode.AssertError("Can't happen.");
		}
		ConstantInfo shortInfo = new ConstantInfo();
		constInfos.put(instr, shortInfo);
		shortInfo.flags |= CONSTANT;
		shortInfo.constant = newValue;
		result = new ConstValue(newValue);
		result.addConstantListener(shortInfo);
		value1.addConstantListener(result);
		value2.addConstantListener(result);
	    } else 
		result = unknownValue[size-1];
	    mergeInfo(instr.nextByAddr, info.poppush(2*size, result));
	    break;
	}
        case opc_ineg: case opc_lneg: case opc_fneg: case opc_dneg: {
	    int size = 1 + (opcode - opc_ineg & 1);
	    ConstValue value = info.getStack(size);
	    if (value.value != ConstValue.VOLATILE) {
		Object newValue;
		switch (opcode) {
		case opc_ineg: 
		    newValue = new Integer
			(-((Integer)value.value).intValue());
		    break;
		case opc_lneg: 
		    newValue = new Long
			(- ((Long)value.value).longValue());
		    break;
		case opc_fneg: 
		    newValue = new Float
			(- ((Float)value.value).floatValue());
		    break;
		case opc_dneg: 
		    newValue = new Double
			(- ((Double)value.value).doubleValue());
		    break;
		default:
		    throw new jode.AssertError("Can't happen.");
		}
		ConstantInfo shortInfo = new ConstantInfo();
		constInfos.put(instr, shortInfo);
		shortInfo.flags |= CONSTANT;
		shortInfo.constant = newValue;
		result = new ConstValue(newValue);
		result.addConstantListener(shortInfo);
		value.addConstantListener(result);
	    } else 
		result = unknownValue[size-1];
	    mergeInfo(instr.nextByAddr, info.poppush(size, result));
	    break;
	}
        case opc_ishl: case opc_lshl:
        case opc_ishr: case opc_lshr:
        case opc_iushr: case opc_lushr: {
	    int size = 1 + (opcode - opc_iadd & 1);
	    ConstValue value1 = info.getStack(size+1);
	    ConstValue value2 = info.getStack(1);
	    if (value1.value != ConstValue.VOLATILE
		&& value2.value != ConstValue.VOLATILE) {
		Object newValue;
		switch (opcode) {
		case opc_ishl: 
		    newValue = new Integer
			(((Integer)value1.value).intValue()
			 << ((Integer)value2.value).intValue());
		    break;
		case opc_ishr: 
		    newValue = new Integer
			(((Integer)value1.value).intValue()
			 >> ((Integer)value2.value).intValue());
		    break;
		case opc_iushr: 
		    newValue = new Integer
			(((Integer)value1.value).intValue()
			 >>> ((Integer)value2.value).intValue());
		    break;

		case opc_lshl: 
		    newValue = new Long
			(((Long)value1.value).longValue()
			 << ((Integer)value2.value).intValue());
		    break;
		case opc_lshr: 
		    newValue = new Long
			(((Long)value1.value).longValue()
			 >> ((Integer)value2.value).intValue());
		    break;
		case opc_lushr: 
		    newValue = new Long
			(((Long)value1.value).longValue()
			 >>> ((Integer)value2.value).intValue());
		    break;
		default:
		    throw new jode.AssertError("Can't happen.");
		}
		ConstantInfo shortInfo = new ConstantInfo();
		constInfos.put(instr, shortInfo);
		shortInfo.flags |= CONSTANT;
		shortInfo.constant = newValue;
		result = new ConstValue(newValue);
		result.addConstantListener(shortInfo);
		value1.addConstantListener(result);
		value2.addConstantListener(result);
	    } else 
		result = unknownValue[size-1];
	    mergeInfo(instr.nextByAddr, info.poppush(size+1, result));
	    break;
	}
        case opc_iinc: {
	    ConstValue local = info.getLocal(instr.localSlot);
	    if (local.value != ConstValue.VOLATILE) {
		result = new ConstValue
		    (new Integer(((Integer)local.value).intValue()
				 + instr.intData));
		local.addConstantListener(result);
	    } else 
		result = unknownValue[0];
	    mergeInfo(instr.nextByAddr, 
		      info.copy().setLocal(instr.localSlot, result));
	    break;
	}
        case opc_i2l: case opc_i2f: case opc_i2d:
        case opc_l2i: case opc_l2f: case opc_l2d:
        case opc_f2i: case opc_f2l: case opc_f2d:
        case opc_d2i: case opc_d2l: case opc_d2f: {
	    int insize = 1 + ((opcode - opc_i2l) / 3 & 1);
	    ConstValue stack = info.getStack(insize);
	    if (stack.value != ConstValue.VOLATILE) {
		Object newVal;
		switch(opcode) {
		case opc_l2i: case opc_f2i: case opc_d2i: 
		    newVal = new Integer(((Number)stack.value).intValue());
		    break;
		case opc_i2l: case opc_f2l: case opc_d2l: 
		    newVal = new Long(((Number)stack.value).longValue());
		    break;
		case opc_i2f: case opc_l2f: case opc_d2f:
		    newVal = new Float(((Number)stack.value).floatValue());
		    break;
		case opc_i2d: case opc_l2d: case opc_f2d:
		    newVal = new Double(((Number)stack.value).doubleValue());
		    break;
		default:
		    throw new jode.AssertError("Can't happen.");
		}
		ConstantInfo shortInfo = new ConstantInfo();
		constInfos.put(instr, shortInfo);
		shortInfo.flags |= CONSTANT;
		shortInfo.constant = newVal;
		result = new ConstValue(newVal);
		result.addConstantListener(shortInfo);
		stack.addConstantListener(result);
	    } else {
		switch (opcode) {
		case opc_i2l: case opc_f2l: case opc_d2l:
		case opc_i2d: case opc_l2d: case opc_f2d:
		    result = unknownValue[1];
		    break;
		default:
		    result = unknownValue[0];
		}
	    }
	    mergeInfo(instr.nextByAddr, info.poppush(insize, result));
	    break;
        }
        case opc_i2b: case opc_i2c: case opc_i2s: {
	    ConstValue stack = info.getStack(1);
	    if (stack.value != ConstValue.VOLATILE) {
		int val = ((Integer)stack.value).intValue();
		switch(opcode) {
		case opc_i2b:
		    val = (byte) val;
		    break;
		case opc_i2c: 
		    val = (char) val;
		    break;
		case opc_i2s:
		    val = (short) val;
		    break;
		}
		ConstantInfo shortInfo = new ConstantInfo();
		constInfos.put(instr, shortInfo);
		shortInfo.flags |= CONSTANT;
		shortInfo.constant = new Integer(val);
		result = new ConstValue(shortInfo.constant);
		stack.addConstantListener(shortInfo);
		stack.addConstantListener(result);
	    } else
		result = unknownValue[0];
	    mergeInfo(instr.nextByAddr, 
		      info.poppush(1, result));
	    break;
	}
        case opc_lcmp: {
	    ConstValue val1 = info.getStack(4);
	    ConstValue val2 = info.getStack(2);
	    if (val1.value != ConstValue.VOLATILE
		&& val2.value != ConstValue.VOLATILE) {
		long value1 = ((Long) val1.value).longValue();
		long value2 = ((Long) val1.value).longValue();
		ConstantInfo shortInfo = new ConstantInfo();
		constInfos.put(instr, shortInfo);
		shortInfo.flags |= CONSTANT;
		shortInfo.constant = new Integer(value1 == value2 ? 0
						 : value1 < value2 ? -1 : 1);
		result = new ConstValue(shortInfo.constant);
		result.addConstantListener(shortInfo);
		val1.addConstantListener(result);
		val2.addConstantListener(result);
	    } else
		result = unknownValue[0];
	    mergeInfo(instr.nextByAddr, info.poppush(4, result));
	    break;
	}
        case opc_fcmpl: case opc_fcmpg: {
	    ConstValue val1 = info.getStack(2);
	    ConstValue val2 = info.getStack(1);
	    if (val1.value != ConstValue.VOLATILE
		&& val2.value != ConstValue.VOLATILE) {
		float value1 = ((Float) val1.value).floatValue();
		float value2 = ((Float) val1.value).floatValue();
		ConstantInfo shortInfo = new ConstantInfo();
		constInfos.put(instr, shortInfo);
		shortInfo.flags |= CONSTANT;
		shortInfo.constant = new Integer
		    (value1 == value2 ? 0
		     : ( opcode == opc_fcmpg
			 ? (value1 < value2 ? -1 :  1)
			 : (value1 > value2 ?  1 : -1)));
		result = new ConstValue(shortInfo.constant);
		result.addConstantListener(shortInfo);
		val1.addConstantListener(result);
		val2.addConstantListener(result);
	    } else
		result = unknownValue[0];
	    mergeInfo(instr.nextByAddr, info.poppush(2, result));
	    break;
	}
        case opc_dcmpl: case opc_dcmpg: {
	    ConstValue val1 = info.getStack(4);
	    ConstValue val2 = info.getStack(2);
	    if (val1.value != ConstValue.VOLATILE
		&& val2.value != ConstValue.VOLATILE) {
		double value1 = ((Double) val1.value).doubleValue();
		double value2 = ((Double) val1.value).doubleValue();
		ConstantInfo shortInfo = new ConstantInfo();
		constInfos.put(instr, shortInfo);
		shortInfo.flags |= CONSTANT;
		shortInfo.constant = new Integer
		    (value1 == value2 ? 0
		     : ( opcode == opc_dcmpg
			 ? (value1 < value2 ? -1 :  1)
			 : (value1 > value2 ?  1 : -1)));
		result = new ConstValue(shortInfo.constant);
		result.addConstantListener(shortInfo);
		val1.addConstantListener(result);
		val2.addConstantListener(result);
	    } else
		result = unknownValue[0];
	    mergeInfo(instr.nextByAddr, info.poppush(4, result));
	    break;
	}
	case opc_ifeq: case opc_ifne: 
	case opc_iflt: case opc_ifge: 
	case opc_ifgt: case opc_ifle:
	case opc_if_icmpeq: case opc_if_icmpne:
	case opc_if_icmplt: case opc_if_icmpge: 
	case opc_if_icmpgt: case opc_if_icmple: 
	case opc_if_acmpeq: case opc_if_acmpne:
	case opc_ifnull: case opc_ifnonnull: {
	    int size = 1;
	    ConstValue stacktop = info.getStack(1);
	    ConstValue other = null;
	    boolean known = stacktop.value != ConstValue.VOLATILE;
	    if (opcode >= opc_if_icmpeq && opcode <= opc_if_acmpne) {
		other = info.getStack(2);
		size = 2;
		known &= other.value != ConstValue.VOLATILE;
	    }
	    if (known) {
		stacktop.addConstantListener(info);
		if (other != null)
		    other.addConstantListener(info);

		Instruction pc = instr.nextByAddr;
		int opc_mask;
		if (opcode >= opc_if_acmpeq) {
		    if (opcode >= opc_ifnull) {
			opc_mask = stacktop.value == null
			    ? CMP_EQUAL_MASK : CMP_GREATER_MASK;
			opcode -= opc_ifnull;
		    } else {
			opc_mask = stacktop.value == other.value
			    ? CMP_EQUAL_MASK : CMP_GREATER_MASK;
			opcode -= opc_if_acmpeq;
		    }
		} else {
		    int value = ((Integer) stacktop.value).intValue();
		    if (opcode >= opc_if_icmpeq) {
			int val1 = ((Integer) other.value).intValue();
			opc_mask = (val1 == value ? CMP_EQUAL_MASK
				    : val1 < value ? CMP_LESS_MASK
				    : CMP_GREATER_MASK);
			opcode -= opc_if_icmpeq;
		    } else {
			opc_mask = (value == 0 ? CMP_EQUAL_MASK
				    : value < 0 ? CMP_LESS_MASK
				    : CMP_GREATER_MASK);
			opcode -= opc_ifeq;
		    }
		}

		if ((opc_mask & (1<<opcode)) != 0)
		    pc = instr.succs[0];

		ConstantInfo shortInfo = new ConstantInfo();
		constInfos.put(instr, shortInfo);
		shortInfo.flags |= CONSTANTFLOW;
		shortInfo.constant = pc;
		mergeInfo(pc, info.pop(size));
	    } else {
		mergeInfo(instr.nextByAddr, info.pop(size));
		mergeInfo(instr.succs[0], info.pop(size));
	    }
	    break;
	}
        case opc_goto:
	    mergeInfo(instr.succs[0], info.copy());
	    break;
        case opc_jsr:
	    mergeInfo(instr.succs[0], 
		      info.poppush(0, new ConstValue
				   (new JSRTargetInfo(instr.succs[0]))));
	    break;
        case opc_tableswitch: {
	    ConstValue stacktop = info.getStack(1);
	    if (stacktop.value != ConstValue.VOLATILE) {
		stacktop.addConstantListener(info);
		Instruction pc;
		int value = ((Integer) stacktop.value).intValue();
		int low  = instr.intData;
		if (value >= low && value <= low + instr.succs.length - 2)
		    pc = instr.succs[value - low];
		else
		    pc = instr.succs[instr.succs.length-1];
		ConstantInfo shortInfo = new ConstantInfo();
		constInfos.put(instr, shortInfo);
		shortInfo.flags |= CONSTANTFLOW;
		shortInfo.constant = pc;
		mergeInfo(pc, info.pop(1));
	    } else {
		for (int i=0; i < instr.succs.length; i++)
		    mergeInfo(instr.succs[i], info.pop(1));
	    }
	    break;
	}
        case opc_lookupswitch: {
	    ConstValue stacktop = info.getStack(1);
	    if (stacktop.value != ConstValue.VOLATILE) {
		stacktop.addConstantListener(info);
		Instruction pc;
		int value = ((Integer) stacktop.value).intValue();
		int[] values = (int[]) instr.objData;
		pc = instr.succs[values.length];
		for (int i=0; i< values.length; i++) {
		    if (values[i] == value) {
			pc = instr.succs[i];
			break;
		    }
		}
		ConstantInfo shortInfo = new ConstantInfo();
		constInfos.put(instr, shortInfo);
		shortInfo.flags |= CONSTANTFLOW;
		shortInfo.constant = pc;
		mergeInfo(pc, info.pop(1));
	    } else {
		for (int i=0; i < instr.succs.length; i++)
		    mergeInfo(instr.succs[i], info.pop(1));
	    }
	    break;
        }
	case opc_ret: {
//  	    dumpStackLocalInfo();
//  	    System.err.println(instr);
	    result = info.getLocal(instr.localSlot);
	    JSRTargetInfo jsrInfo = (JSRTargetInfo) result.value;
	    jsrInfo.retInfo = info;
	    result.addConstantListener(info);
	    Instruction jsrTarget = jsrInfo.jsrTarget;
	    for (int i=0; i < jsrTarget.preds.length; i++) {
		Instruction jsr = jsrTarget.preds[i];
		if (jsr.tmpInfo == null)
		    continue;
		StackLocalInfo nextInfo
		    = ((StackLocalInfo) jsr.tmpInfo).copy();
		int maxLocals = bytecode.getMaxLocals();
		for (int slot = 0; slot < maxLocals; slot++) {
		    if (slot == instr.localSlot)
			nextInfo.setLocal(slot, null);
		    else if (jsrInfo.usedLocals.get(slot))
			nextInfo.setLocal(slot, info.getLocal(slot));
		}
		mergeInfo(jsr.nextByAddr, nextInfo);
	    }
	    break;
	}
        case opc_ireturn: case opc_lreturn: 
        case opc_freturn: case opc_dreturn: case opc_areturn:
        case opc_return:
        case opc_athrow:
	    break;

	case opc_putstatic:
	case opc_putfield: {
	    FieldIdentifier fi = (FieldIdentifier) canonizeReference(instr);
	    int size = (opcode == opc_putstatic) ? 0 : 1;
	    Reference ref = (Reference) instr.objData;
	    if (fi != null && !fi.isNotConstant()) {
		fi.setNotConstant();
		fieldNotConstant(fi);
	    }
	    Type type = Type.tType(ref.getType());
	    mergeInfo(instr.nextByAddr, info.pop(size + type.stackSize()));
	    break;
	}
	case opc_getstatic:
	case opc_getfield: {
	    int size = (opcode == opc_getstatic) ? 0 : 1;
	    FieldIdentifier fi = (FieldIdentifier) canonizeReference(instr);
	    Reference ref = (Reference) instr.objData;
	    Type type = Type.tType(ref.getType());
	    if (fi != null) {
		if (fi.isNotConstant()) {
		    fi.setReachable();
		    result = unknownValue[type.stackSize()-1];
		} else {
		    Object obj = fi.getConstant();
		    if (obj == null)
			obj = type.getDefaultValue();
		    ConstantInfo shortInfo = new ConstantInfo();
		    constInfos.put(instr, shortInfo);
		    shortInfo.flags |= CONSTANT;
		    shortInfo.constant = obj;
		    result = new ConstValue(obj);
		    result.addConstantListener(shortInfo);
		    fi.addFieldListener(listener);
		}
	    } else
		result = unknownValue[type.stackSize()-1];
	    mergeInfo(instr.nextByAddr, info.poppush(size, result));
	    break;
	}
	case opc_invokespecial:
	case opc_invokestatic:
	case opc_invokeinterface:
	case opc_invokevirtual: {
	    canonizeReference(instr);
	    Reference ref = (Reference) instr.objData;
	    MethodType mt = (MethodType) Type.tType(ref.getType());
	    boolean constant = true;
	    int size = 0;
	    Object   cls = null;
	    Object[] args = new Object[mt.getParameterTypes().length];
	    ConstValue clsValue = null;
	    ConstValue[] argValues = 
		new ConstValue[mt.getParameterTypes().length];
	    for (int i=mt.getParameterTypes().length-1; i >=0; i--) {
		size += mt.getParameterTypes()[i].stackSize();
		argValues[i] = info.getStack(size);
		if (argValues[i].value != ConstValue.VOLATILE)
		    args[i] = argValues[i].value;
		else
		    constant = false;
	    }
	    if (opcode != opc_invokestatic) {
		size++;
		clsValue = info.getStack(size);
		cls = clsValue.value;
		if (cls == ConstValue.VOLATILE
		    || cls == null
		    || !cls.getClass().getName().equals(ref.getClazz()))
		    constant = false;
	    }
	    Type retType = mt.getReturnType();
	    if (retType == Type.tVoid) {
		handleReference(ref, opcode == opc_invokevirtual 
				|| opcode == opc_invokeinterface);
		mergeInfo(instr.nextByAddr, info.pop(size));
		break;
	    }
	    if (constant
		&& retType != Type.tString
		&& retType.getTypeSignature().length() != 1) {
		/* This is not a valid constant type */
		constant = false;
	    }
	    Object methodResult = null;
	    if (constant) {
		try {
		    methodResult = runtime.invokeMethod
			(ref, opcode != opc_invokespecial, cls, args);
		} catch (InterpreterException ex) {
		    constant = false;
		    if (jode.GlobalOptions.verboseLevel > 3)
			GlobalOptions.err.println("Can't interpret "+ref+": "
					       + ex.getMessage());
		    /* result is not constant */
		} catch (InvocationTargetException ex) {
		    constant = false;
		    if (jode.GlobalOptions.verboseLevel > 3)
			GlobalOptions.err.println("Method "+ref
					       +" throwed exception: "
					       + ex.getTargetException()
					       .getMessage());
		    /* method always throws exception ? */
		}
	    }
	    ConstValue returnVal;
	    if (!constant) {
		handleReference(ref, opcode == opc_invokevirtual 
				|| opcode == opc_invokeinterface);
		returnVal = unknownValue[mt.getReturnType().stackSize()-1];
	    } else {
		ConstantInfo shortInfo = new ConstantInfo();
		constInfos.put(instr, shortInfo);
		shortInfo.flags |= CONSTANT;
		shortInfo.constant = methodResult;
		returnVal = new ConstValue(methodResult);
		returnVal.addConstantListener(shortInfo);
		if (clsValue != null)
		    clsValue.addConstantListener(returnVal);
		for (int i=0; i< argValues.length; i++)
		    argValues[i].addConstantListener(returnVal);
	    }
	    mergeInfo(instr.nextByAddr, info.poppush(size, returnVal));
	    break;
	}

        case opc_new: {
	    handleClass((String) instr.objData);
	    mergeInfo(instr.nextByAddr, info.poppush(0, unknownValue[0]));
	    break;
        }
        case opc_arraylength: {
//  	    ConstValue array = info.getStack(1);
//  	    if (array.value != ConstValue.VOLATILE
//  		&& array.value != null) {
//  		shortInfo.flags |= CONSTANT;
//  		shortInfo.constant = new Integer(Array.getLength(array.value));
//  		result = new ConstValue(shortInfo.constant);
//  		result.addConstantListener(shortInfo);
//  		array.addConstantListener(result);
//  	    } else
	    result = unknownValue[0];
	    mergeInfo(instr.nextByAddr, info.poppush(1, result));
	    break;
	}
        case opc_checkcast: {
	    handleClass((String) instr.objData);
	    mergeInfo(instr.nextByAddr, info.pop(0));
	    break;
        }
        case opc_instanceof: {
	    handleClass((String) instr.objData);
	    mergeInfo(instr.nextByAddr, info.poppush(1, unknownValue[0]));
	    break;
        }
        case opc_monitorenter:
        case opc_monitorexit:
	    mergeInfo(instr.nextByAddr, info.pop(1));
	    break;
        case opc_multianewarray:
	    handleClass((String) instr.objData);
	    mergeInfo(instr.nextByAddr, 
		      info.poppush(instr.intData, unknownValue[0]));
	    break;
        default:
            throw new jode.AssertError("Invalid opcode "+opcode);
        }
			
    }

    public void fieldNotConstant(FieldIdentifier fi) {
	for (Instruction instr = bytecode.getFirstInstr();
	     instr != null; instr = instr.nextByAddr) {
	    if (instr.opcode == opc_getfield
		|| instr.opcode == opc_getstatic) {
		Reference ref = (Reference) instr.objData;
		if (ref.getName().equals(fi.getName())
		    && ref.getType().equals(fi.getType())
		    && instr.tmpInfo != null) {
		    ((StackLocalInfo) instr.tmpInfo).enqueue();
		}
	    }
	}
    }

    public void dumpStackLocalInfo() {
	for (Instruction instr = bytecode.getFirstInstr(); 
	     instr != null; instr = instr.nextByAddr) {
	    StackLocalInfo info = (StackLocalInfo) instr.tmpInfo;
	    System.err.println(""+info);
	    System.err.println(instr.getDescription());
	}
    }

    public void analyzeCode(MethodIdentifier listener, BytecodeInfo bytecode) {
	this.bytecode = bytecode;
	working = true;	
	if (constInfos == null)
	    constInfos = new HashMap();
	Set modifiedQueue = new HashSet();
	MethodInfo minfo = bytecode.getMethodInfo();
	StackLocalInfo firstInfo = new StackLocalInfo 
	    (bytecode.getMaxLocals(), minfo.isStatic(), minfo.getType(), 
	     modifiedQueue);
	firstInfo.instr = bytecode.getFirstInstr();
	firstInfo.instr.tmpInfo = firstInfo;
	modifiedQueue.add(firstInfo);
	while (!modifiedQueue.isEmpty()) {
	    Iterator iter = modifiedQueue.iterator();
	    StackLocalInfo info = (StackLocalInfo) iter.next();
	    iter.remove();
	    handleOpcode(info);
	}

	Handler[] handlers = bytecode.getExceptionHandlers();
	for (int i=0; i< handlers.length; i++) {
	    if (handlers[i].catcher.tmpInfo != null 
		&& handlers[i].type != null)
		Main.getClassBundle().reachableIdentifier(handlers[i].type, false);
	}
	working = false;
	for (Instruction instr = bytecode.getFirstInstr();
	     instr != null; instr = instr.nextByAddr)
	    instr.tmpInfo = null;
    }

    public void insertOnePop(Instruction instr, int count) {
	/* Add a goto instruction after this opcode. */
	Instruction pop = instr.insertInstruction();
	pop.length = 1;
	pop.opcode = Instruction.opc_pop - 1 + count;
    }

    public void insertPop(Instruction instr) {
	switch(instr.opcode) {
        case opc_ldc:
        case opc_ldc2_w:
        case opc_iload: case opc_lload: 
        case opc_fload: case opc_dload: case opc_aload:
	case opc_getstatic:
	    break;
	case opc_arraylength:
	case opc_getfield:
        case opc_i2l: case opc_i2f: case opc_i2d:
        case opc_f2i: case opc_f2l: case opc_f2d:
        case opc_i2b: case opc_i2c: case opc_i2s:
        case opc_ineg: case opc_fneg: 
	    insertOnePop(instr, 1);
	    break;
	case opc_lcmp: 
	case opc_dcmpg: case opc_dcmpl: 
        case opc_ladd: case opc_dadd:
        case opc_lsub: case opc_dsub:
        case opc_lmul: case opc_dmul:
        case opc_ldiv: case opc_ddiv:
        case opc_lrem: case opc_drem:
	case opc_land: case opc_lor : case opc_lxor:
	    insertOnePop(instr, 2);
	    /* fall through */
	case opc_fcmpg: case opc_fcmpl:
        case opc_l2i: case opc_l2f: case opc_l2d:
        case opc_d2i: case opc_d2l: case opc_d2f:
	case opc_lneg: case opc_dneg:
        case opc_iadd: case opc_fadd:
        case opc_isub: case opc_fsub:
        case opc_imul: case opc_fmul:
        case opc_idiv: case opc_fdiv:
        case opc_irem: case opc_frem:
        case opc_iand: case opc_ior : case opc_ixor: 
        case opc_ishl: case opc_ishr: case opc_iushr:
        case opc_iaload: case opc_laload: 
        case opc_faload: case opc_daload: case opc_aaload:
        case opc_baload: case opc_caload: case opc_saload:
	    insertOnePop(instr, 2);
	    break;

	case opc_lshl: case opc_lshr: case opc_lushr:
	    insertOnePop(instr, 1);
	    insertOnePop(instr, 2);
	    break;
	case opc_putstatic:
	case opc_putfield:
	    if (Type.tType(((Reference)instr.objData).getType())
		.stackSize() == 2) {
		insertOnePop(instr, 2);
		if (instr.opcode == opc_putfield)
		    insertOnePop(instr, 1);
	    } else
		insertOnePop(instr, (instr.opcode == opc_putfield) ? 2 : 1);
	    break;
	case opc_invokespecial:
	case opc_invokestatic:
	case opc_invokeinterface:
	case opc_invokevirtual: {
	    Reference ref = (Reference) instr.objData;
	    MethodType mt = (MethodType) Type.tType(ref.getType());
	    for (int i=mt.getParameterTypes().length-1; i >=0; i--)
		insertOnePop(instr, mt.getParameterTypes()[i].stackSize());
	    if (instr.opcode != opc_invokestatic)
		insertOnePop(instr, 1);
	}
	}
    }
    
    public void appendJump(Instruction instr, Instruction dest) {
	/* Add a goto instruction after this opcode. */
	Instruction second = instr.appendInstruction();
	second.alwaysJumps = true;
	second.opcode = Instruction.opc_goto;
	second.length = 3;
	second.succs = new Instruction[] { dest };
	dest.addPredecessor(second);
    }
    
    public void transformCode(BytecodeInfo bytecode) {
	for (Instruction instr = bytecode.getFirstInstr();
	     instr != null; instr = instr.nextByAddr) {
	    ConstantInfo info = (ConstantInfo) constInfos.get(instr);
	    if (info == null || (info.flags & REACHABLE) == 0) {
		/* This instruction can't be reached logically */
		instr.removeInstruction();
	    } else if ((info.flags & CONSTANT) != 0) {
		insertPop(instr);
		if (instr.opcode > opc_ldc2_w) {
		    instr.opcode = (info.constant instanceof Long
				    || info.constant instanceof Double
				    ? opc_ldc2_w : opc_ldc);
		    instr.localSlot = -1;
		    instr.length = 2;
		    instr.objData = info.constant;
		    if (GlobalOptions.verboseLevel > 2)
			GlobalOptions.err.println
			    (bytecode + ": Replacing " + instr
			     + " with constant " + info.constant);
		}
	    } else if ((info.flags & CONSTANTFLOW) != 0) {
		Instruction pc = (Instruction) info.constant;
		if (instr.opcode >= opc_if_icmpeq
		    && instr.opcode <= opc_if_acmpne)
		    instr.opcode = opc_pop2;
		else
		    instr.opcode = opc_pop;
		if (GlobalOptions.verboseLevel > 2)
		    GlobalOptions.err.println
			(bytecode + ": Replacing " + instr
			 + " with goto " + pc.addr);
		instr.alwaysJumps = false;
		for (int i = 0; i< instr.succs.length; i++)
		    instr.succs[i].removePredecessor(instr);
		instr.succs = null;
		instr.length = 1;
		while (instr.nextByAddr != null) {
		    ConstantInfo nextinfo 
			= (ConstantInfo) constInfos.get(instr.nextByAddr);
		    if (nextinfo != null && (nextinfo.flags & REACHABLE) != 0)
			break;
		    /* Next instruction can't be reached logically */
		    instr.nextByAddr.removeInstruction();
		}
		
		if (pc != instr.nextByAddr) {
		    appendJump(instr, pc);
		    instr = instr.nextByAddr;
		}

	    } else {
		int opcode = instr.opcode;
		switch (opcode) {
		case opc_nop:
		    instr.removeInstruction();
		    break;

		case opc_goto:
		    while (instr.nextByAddr != null) {
			ConstantInfo nextinfo
			    = (ConstantInfo) constInfos.get(instr.nextByAddr);
			if (nextinfo != null
			    && (nextinfo.flags & REACHABLE) != 0)
			    break;
			/* Next instruction can't be reached logically */
			instr.nextByAddr.removeInstruction();
		    }
		    if (instr.succs[0] == instr.nextByAddr)
			instr.removeInstruction();
		    break;
		case opc_ifeq: case opc_ifne: 
		case opc_iflt: case opc_ifge: 
		case opc_ifgt: case opc_ifle:
		case opc_ifnull: case opc_ifnonnull:
		    if (instr.succs[0] == instr.nextByAddr) {
			instr.opcode = opc_pop;
			instr.succs[0].removePredecessor(instr);
			instr.succs = null;
		    }
		    break;

		case opc_if_icmpeq: case opc_if_icmpne:
		case opc_if_icmplt: case opc_if_icmpge: 
		case opc_if_icmpgt: case opc_if_icmple: 
		case opc_if_acmpeq: case opc_if_acmpne:
		    if (instr.succs[0] == instr.nextByAddr) {
			instr.opcode = opc_pop2;
			instr.succs[0].removePredecessor(instr);
			instr.succs = null;
		    }
		    break;

		case opc_putstatic:
		case opc_putfield: {
		    Reference ref = (Reference) instr.objData;
		    FieldIdentifier fi = (FieldIdentifier) 
			Main.getClassBundle().getIdentifier(ref);
		    if (fi != null
			&& (Main.stripping & Main.STRIP_UNREACH) != 0
			&& !fi.isReachable()) {
			insertPop(instr);
			instr.removeInstruction();
		    }
		    break;
		}
		}
	    }
	}
    }
}
