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
import jode.MethodType;
import jode.Obfuscator;
import jode.Type;
import jode.bytecode.*;
import jode.jvm.InterpreterException;
import java.util.*;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;

interface ConstantListener {
    public void constantChanged();
}

class ConstantAnalyzerValue implements ConstantListener {
    public final static Object VOLATILE = new Object();
    /**
     * The constant value, VOLATILE if value is not constant.
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
    Vector listeners;

    public ConstantAnalyzerValue(Object constant) {
	value = constant;
	stackSize = (constant instanceof Double
		     || constant instanceof Long) ? 2 : 1;
	listeners = new Vector();
    }

    public ConstantAnalyzerValue(ConstantAnalyzerValue constant) {
	value = constant.value;
	stackSize = constant.stackSize;
	listeners = new Vector();
	constant.addConstantListener(this);
    }

    public ConstantAnalyzerValue(int stackSize) {
	this.value = VOLATILE;
	this.stackSize = stackSize;
    }

    public ConstantAnalyzerValue copy() {
	return (value == VOLATILE) ? this : new ConstantAnalyzerValue(this);
    }

    public void addConstantListener(ConstantListener l) {
	if (!listeners.contains(l))
	    listeners.addElement(l);
    }

    public void removeConstantListener(ConstantListener l) {
	listeners.removeElement(l);
    }

    public void fireChanged() {
	value = VOLATILE;
	Enumeration enum = listeners.elements();
	while (enum.hasMoreElements())
	    ((ConstantListener) enum.nextElement()).constantChanged();
	listeners = null;
    }

    public void constantChanged() {
	if (value != VOLATILE)
	    fireChanged();
    }

    /**
     * Merge this value with other.
     * @return true, if the value changed.
     */
    public void merge(ConstantAnalyzerValue other) {
	if (this == other
	    || (value == VOLATILE && other.value == VOLATILE))
	    return;

	if (value == other.value
	    || (value != null && value.equals(other.value))) {
	    other.addConstantListener(this);
	    return;
	}

	if (value != VOLATILE)
	    fireChanged();
	if (other.value != VOLATILE)
	    other.fireChanged();
    }
}

/**
 * Analyze the code, assuming every field that is not yet written to
 * is constant.  This may imply that some code is dead code.
 *
 * @author Jochen Hoenicke
 */
public class ConstantAnalyzer implements Opcodes, CodeAnalyzer {
    boolean working;
    MethodIdentifier m;
    BytecodeInfo bytecode;
    Hashtable constInfos = null;
    Stack instrStack;
    ConstantRuntimeEnvironment runtime;

    final static int REACHABLE     = 0x1;
    final static int CONSTANT      = 0x2;
    final static int CONSTANTFLOW  = 0x4;

    class ConstantAnalyzerInfo implements ConstantListener {
	ConstantAnalyzerValue[] stack;
	ConstantAnalyzerValue[] locals;
	Instruction instr;

	public ConstantAnalyzerValue copy(ConstantAnalyzerValue value) {
	    return (value == null) ? null : value.copy();
	}

	private ConstantAnalyzerInfo(ConstantAnalyzerValue[] stack, 
				     ConstantAnalyzerValue[] locals) {
	    this.stack = stack;
	    this.locals = locals;
	}
    
	public ConstantAnalyzerInfo(int numLocals, 
				    boolean isStatic, MethodType mt) {
	    locals = new ConstantAnalyzerValue[numLocals];
	    stack = new ConstantAnalyzerValue[0];
	    int slot = 0;
	    if (!isStatic)
		locals[slot++] = new ConstantAnalyzerValue(1);
	    for (int i=0; i< mt.getParameterTypes().length; i++) {
		locals[slot] = new ConstantAnalyzerValue
		    (mt.getParameterTypes()[i].stackSize());
		slot += locals[slot].stackSize;
	    }
	}
    
	public void constantChanged() {
	    if (!instrStack.contains(instr))
		instrStack.push(instr);
	}
    
	public ConstantAnalyzerInfo poppush(int pops, ConstantAnalyzerValue push) {
	    ConstantAnalyzerValue[] newStack 
		= new ConstantAnalyzerValue[stack.length - pops + push.stackSize];
	    System.arraycopy(stack, 0, newStack, 0, stack.length-pops);
	    newStack[stack.length-pops] = push.copy();
	    return new ConstantAnalyzerInfo(newStack, locals);
	}

	public ConstantAnalyzerInfo pop(int pops) {
	    ConstantAnalyzerValue[] newStack 
		= new ConstantAnalyzerValue[stack.length - pops];
	    System.arraycopy(stack, 0, newStack, 0, stack.length-pops);
	    return new ConstantAnalyzerInfo(newStack, locals);
	}

	public ConstantAnalyzerInfo dup(int count, int depth) {
	    ConstantAnalyzerValue[] newStack 
		= new ConstantAnalyzerValue[stack.length + count];
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
	    return new ConstantAnalyzerInfo(newStack, locals);
	}

	public ConstantAnalyzerInfo swap() {
	    ConstantAnalyzerValue[] newStack 
		= new ConstantAnalyzerValue[stack.length];
	    System.arraycopy(stack, 0, newStack, 0, stack.length - 2);
	    newStack[stack.length-2] = stack[stack.length-1].copy();
	    newStack[stack.length-1] = stack[stack.length-2].copy();
	    return new ConstantAnalyzerInfo(newStack, locals);
	}

	public ConstantAnalyzerValue getLocal(int slot) {
	    return locals[slot];
	}

	public ConstantAnalyzerValue getStack(int depth) {
	    return stack[stack.length - depth];
	}

	public ConstantAnalyzerInfo setLocal(int slot, 
					     ConstantAnalyzerValue value) {
	    ConstantAnalyzerValue[] newLocals
		= (ConstantAnalyzerValue[]) locals.clone();
	    newLocals[slot] = value;
	    if (value.stackSize == 2)
		newLocals[slot+1] = null;
	    return new ConstantAnalyzerInfo(stack, newLocals);
	}
	
	public void merge(ConstantAnalyzerInfo other) {
	    for (int i=0; i < locals.length; i++) {
		if (locals[i] != null) {
		    if (other.locals[i] == null) {
			if (!instrStack.contains(instr))
			    instrStack.push(instr);
			locals[i] = null;
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
    }

    class ConstantInfo implements ConstantListener {
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

    static ConstantAnalyzerValue[] unknownValue = {
	new ConstantAnalyzerValue(1), new ConstantAnalyzerValue(2)
    };

    public ConstantAnalyzer(BytecodeInfo code, MethodIdentifier method) {
	this.bytecode = code;
	this.m = method;
	this.runtime = new ConstantRuntimeEnvironment(m);
    }

    public void mergeInfo(Instruction instr, 
			  ConstantAnalyzerInfo info) {
	if (instr.tmpInfo == null) {
	    if (!instrStack.contains(instr))
		instrStack.push(instr);
	    instr.tmpInfo = info;
	    info.instr = instr;
	} else
	    ((ConstantAnalyzerInfo)instr.tmpInfo).merge(info);
    }

    public void handleReference(Reference ref, boolean isVirtual) {
	String clName = ref.getClazz();
	/* Don't have to reach array methods */
	if (clName.charAt(0) != '[') {
	    clName = clName.substring(1, clName.length()-1).replace('/', '.');
	    m.clazz.bundle.reachableIdentifier
		(clName+"."+ref.getName()+"."+ref.getType(), isVirtual);
	}
    }

    public void handleClass(String clName) {
	int i = 0;
	while (i < clName.length() && clName.charAt(i) == '[')
	    i++;
	if (i < clName.length() && clName.charAt(i) == 'L') {
	    clName = clName.substring(i+1, clName.length()-1);
	    m.clazz.bundle.reachableIdentifier(clName, false);
	}
    }

    public void handleOpcode(Instruction instr) {
	ConstantAnalyzerInfo info = (ConstantAnalyzerInfo) instr.tmpInfo;
	ConstantInfo shortInfo = (ConstantInfo) constInfos.get(instr);
	if (shortInfo == null)
	    constInfos.put(instr, shortInfo = new ConstantInfo());
	shortInfo.flags = REACHABLE;
	
	int opcode = instr.opcode;
	Handler[] handlers = bytecode.getExceptionHandlers();
	for (int i=0; i< handlers.length; i++) {
	    if (handlers[i].start.addr <= instr.addr
		&& handlers[i].end.addr >= instr.addr)
		mergeInfo(handlers[i].catcher, 
			  info.poppush(info.stack.length, unknownValue[0]));
	}
	ConstantAnalyzerValue result;
	switch (opcode) {
        case opc_nop:
	    mergeInfo(instr.nextByAddr, info.pop(0));
	    break;

        case opc_ldc:
        case opc_ldc2_w:
	    result = new ConstantAnalyzerValue(instr.objData);
	    mergeInfo(instr.nextByAddr, info.poppush(0, result));
	    break;

        case opc_iload: case opc_lload: 
        case opc_fload: case opc_dload: case opc_aload:
	    result = info.getLocal(instr.localSlot);
	    if (result.value != ConstantAnalyzerValue.VOLATILE) {
		shortInfo.flags |= CONSTANT;
		shortInfo.constant = result.value;
		result.addConstantListener(shortInfo);
	    }
	    mergeInfo(instr.nextByAddr, 
		      info.setLocal(instr.localSlot, result.copy())
		      .poppush(0, result));
	    break;
        case opc_iaload: case opc_laload: 
        case opc_faload: case opc_daload: case opc_aaload:
        case opc_baload: case opc_caload: case opc_saload: {
//  	    ConstantAnalyzerValue array = info.getStack(2);
//  	    ConstantAnalyzerValue index = info.getStack(1);
//  	    ConstantAnalyzerValue newValue = null;
//  	    if (index.value != index.ConstantAnalyzerValue.VOLATILE
//  		&& array.value != array.ConstantAnalyzerValue.VOLATILE
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
//  		result = new ConstantAnalyzerValue(content);
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
	    mergeInfo(instr.nextByAddr, info.pop(3));
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
	    ConstantAnalyzerValue value1 = info.getStack(2*size);
	    ConstantAnalyzerValue value2 = info.getStack(1*size);
	    if (value1.value != ConstantAnalyzerValue.VOLATILE
		&& value2.value != ConstantAnalyzerValue.VOLATILE) {
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
		shortInfo.flags |= CONSTANT;
		shortInfo.constant = newValue;
		result = new ConstantAnalyzerValue(newValue);
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
	    ConstantAnalyzerValue value = info.getStack(size);
	    if (value.value != ConstantAnalyzerValue.VOLATILE) {
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
		shortInfo.flags |= CONSTANT;
		shortInfo.constant = newValue;
		result = new ConstantAnalyzerValue(newValue);
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
	    ConstantAnalyzerValue value1 = info.getStack(size+1);
	    ConstantAnalyzerValue value2 = info.getStack(1);
	    if (value1.value != ConstantAnalyzerValue.VOLATILE
		&& value2.value != ConstantAnalyzerValue.VOLATILE) {
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
		shortInfo.flags |= CONSTANT;
		shortInfo.constant = newValue;
		result = new ConstantAnalyzerValue(newValue);
		result.addConstantListener(shortInfo);
		value1.addConstantListener(result);
		value2.addConstantListener(result);
	    } else 
		result = unknownValue[size-1];
	    mergeInfo(instr.nextByAddr, info.poppush(size+1, result));
	    break;
	}
        case opc_iinc: {
	    ConstantAnalyzerValue local = info.getLocal(instr.localSlot);
	    if (local.value != ConstantAnalyzerValue.VOLATILE) {
		result = new ConstantAnalyzerValue
		    (new Integer(((Integer)local.value).intValue()
				 + instr.intData));
		local.addConstantListener(result);
	    } else 
		result = unknownValue[0];
	    mergeInfo(instr.nextByAddr, 
		      info.setLocal(instr.localSlot, result));
	    break;
	}
        case opc_i2l: case opc_i2f: case opc_i2d:
        case opc_l2i: case opc_l2f: case opc_l2d:
        case opc_f2i: case opc_f2l: case opc_f2d:
        case opc_d2i: case opc_d2l: case opc_d2f: {
	    int insize = 1 + ((opcode - opc_i2l) / 3 & 1);
	    ConstantAnalyzerValue stack = info.getStack(insize);
	    if (stack.value != ConstantAnalyzerValue.VOLATILE) {
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
		shortInfo.flags |= CONSTANT;
		shortInfo.constant = newVal;
		result = new ConstantAnalyzerValue(newVal);
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
	    ConstantAnalyzerValue stack = info.getStack(1);
	    if (stack.value != ConstantAnalyzerValue.VOLATILE) {
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
		shortInfo.flags |= CONSTANT;
		shortInfo.constant = new Integer(val);
		result = new ConstantAnalyzerValue(shortInfo.constant);
		stack.addConstantListener(shortInfo);
		stack.addConstantListener(result);
	    } else
		result = unknownValue[0];
	    mergeInfo(instr.nextByAddr, 
		      info.poppush(1, result));
	    break;
	}
        case opc_lcmp: {
	    ConstantAnalyzerValue val1 = info.getStack(4);
	    ConstantAnalyzerValue val2 = info.getStack(2);
	    if (val1.value != ConstantAnalyzerValue.VOLATILE
		&& val2.value != ConstantAnalyzerValue.VOLATILE) {
		long value1 = ((Long) val1.value).longValue();
		long value2 = ((Long) val1.value).longValue();
		shortInfo.flags |= CONSTANT;
		shortInfo.constant = new Integer(value1 == value2 ? 0
						 : value1 < value2 ? -1 : 1);
		result = new ConstantAnalyzerValue(shortInfo.constant);
		result.addConstantListener(shortInfo);
		val1.addConstantListener(result);
		val2.addConstantListener(result);
	    } else
		result = unknownValue[0];
	    mergeInfo(instr.nextByAddr, info.poppush(4, result));
	    break;
	}
        case opc_fcmpl: case opc_fcmpg: {
	    ConstantAnalyzerValue val1 = info.getStack(2);
	    ConstantAnalyzerValue val2 = info.getStack(1);
	    if (val1.value != ConstantAnalyzerValue.VOLATILE
		&& val2.value != ConstantAnalyzerValue.VOLATILE) {
		float value1 = ((Float) val1.value).floatValue();
		float value2 = ((Float) val1.value).floatValue();
		shortInfo.flags |= CONSTANT;
		shortInfo.constant = new Integer
		    (value1 == value2 ? 0
		     : ( opcode == opc_fcmpg
			 ? (value1 < value2 ? -1 :  1)
			 : (value1 > value2 ?  1 : -1)));
		result = new ConstantAnalyzerValue(shortInfo.constant);
		result.addConstantListener(shortInfo);
		val1.addConstantListener(result);
		val2.addConstantListener(result);
	    } else
		result = unknownValue[0];
	    mergeInfo(instr.nextByAddr, info.poppush(2, result));
	    break;
	}
        case opc_dcmpl: case opc_dcmpg: {
	    ConstantAnalyzerValue val1 = info.getStack(4);
	    ConstantAnalyzerValue val2 = info.getStack(2);
	    if (val1.value != ConstantAnalyzerValue.VOLATILE
		&& val2.value != ConstantAnalyzerValue.VOLATILE) {
		double value1 = ((Double) val1.value).doubleValue();
		double value2 = ((Double) val1.value).doubleValue();
		shortInfo.flags |= CONSTANT;
		shortInfo.constant = new Integer
		    (value1 == value2 ? 0
		     : ( opcode == opc_dcmpg
			 ? (value1 < value2 ? -1 :  1)
			 : (value1 > value2 ?  1 : -1)));
		result = new ConstantAnalyzerValue(shortInfo.constant);
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
	    ConstantAnalyzerValue stacktop = info.getStack(1);
	    ConstantAnalyzerValue other = null;
	    boolean known = stacktop.value != ConstantAnalyzerValue.VOLATILE;
	    if (opcode >= opc_if_icmpeq && opcode <= opc_if_acmpne) {
		other = info.getStack(2);
		size = 2;
		known &= other.value != ConstantAnalyzerValue.VOLATILE;
	    }
	    if (known) {
		stacktop.addConstantListener(info);
		if (other != null)
		    other.addConstantListener(info);

		Instruction pc = instr.nextByAddr;
		int value = -1;
		if (opcode >= opc_if_acmpeq) {
		    if (opcode >= opc_ifnull) {
			value = stacktop.value == null ? 0 : 1;
			opcode += opc_ifeq - opc_ifnull;
		    } else {
			value = stacktop.value == other.value ? 0 : 1;
		    }
		} else {
		    value = ((Integer) stacktop.value).intValue();
		    if (opcode >= opc_if_icmpeq) {
			int val1 = ((Integer) other.value).intValue();
			value = (val1 == value ? 0
				 : val1 < value ? -1 : 1);
			opcode += opc_ifeq - opc_if_icmpeq;
		    }
		}
		if (value > 0 && (opcode == opc_ifgt || opcode == opc_ifge)
		    || value < 0 && (opcode == opc_iflt || opcode == opc_ifle)
		    || value == 0 && (opcode == opc_ifge || opcode == opc_ifle
				      || opcode == opc_ifeq))
		    pc = instr.succs[0];

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
	    mergeInfo(instr.succs[0], info.pop(0));
	    break;
        case opc_jsr:
	    mergeInfo(instr.nextByAddr, info.pop(0));
	    mergeInfo(instr.succs[0], info.poppush(0, unknownValue[0]));
	    break;
        case opc_tableswitch: {
	    ConstantAnalyzerValue stacktop = info.getStack(1);
	    if (stacktop.value != ConstantAnalyzerValue.VOLATILE) {
		stacktop.addConstantListener(info);
		Instruction pc;
		int value = ((Integer) stacktop.value).intValue();
		int low  = instr.intData;
		if (value >= low && value <= low + instr.succs.length - 2)
		    pc = instr.succs[value - low];
		else
		    pc = instr.succs[instr.succs.length-1];
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
	    ConstantAnalyzerValue stacktop = info.getStack(1);
	    if (stacktop.value != ConstantAnalyzerValue.VOLATILE) {
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
		shortInfo.flags |= CONSTANTFLOW;
		shortInfo.constant = pc;
		mergeInfo(pc, info.pop(1));
	    } else {
		for (int i=0; i < instr.succs.length; i++)
		    mergeInfo(instr.succs[i], info.pop(1));
	    }
	    break;
        }
	case opc_ret:
        case opc_ireturn: case opc_lreturn: 
        case opc_freturn: case opc_dreturn: case opc_areturn:
        case opc_return:
        case opc_athrow:
	    break;

	case opc_putstatic:
	case opc_putfield: {
	    int size = (opcode == opc_putstatic) ? 0 : 1;
	    Reference ref = (Reference) instr.objData;
	    FieldIdentifier fi = (FieldIdentifier) 
		m.clazz.bundle.getIdentifier(ref);
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
	    Reference ref = (Reference) instr.objData;
	    Type type = Type.tType(ref.getType());
	    FieldIdentifier fi = (FieldIdentifier) 
		m.clazz.bundle.getIdentifier(ref);
	    if (fi != null) {
		if (fi.isNotConstant()) {
		    fi.setReachable();
		    result = unknownValue[type.stackSize()-1];
		} else {
		    Object obj = fi.getConstant();
		    if (obj == null)
			obj = type.getDefaultValue();
		    shortInfo.flags |= CONSTANT;
		    shortInfo.constant = obj;
		    result = new ConstantAnalyzerValue(obj);
		    result.addConstantListener(shortInfo);
		    fi.addFieldListener(m);
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
	    Reference ref = (Reference) instr.objData;
	    MethodType mt = (MethodType) Type.tType(ref.getType());
	    boolean constant = true;
	    int size = 0;
	    Object   cls = null;
	    Object[] args = new Object[mt.getParameterTypes().length];
	    ConstantAnalyzerValue clsValue = null;
	    ConstantAnalyzerValue[] argValues = 
		new ConstantAnalyzerValue[mt.getParameterTypes().length];
	    for (int i=mt.getParameterTypes().length-1; i >=0; i--) {
		size += mt.getParameterTypes()[i].stackSize();
		argValues[i] = info.getStack(size);
		if (argValues[i].value != ConstantAnalyzerValue.VOLATILE)
		    args[i] = argValues[i].value;
		else
		    constant = false;
	    }
	    if (opcode != opc_invokestatic) {
		size++;
		clsValue = info.getStack(size);
		cls = clsValue.value;
		if (cls == ConstantAnalyzerValue.VOLATILE
		    || cls == null)
		    constant = false;
	    }
	    if (mt.getReturnType() == Type.tVoid) {
		handleReference(ref, opcode == opc_invokevirtual 
				|| opcode == opc_invokeinterface);
		mergeInfo(instr.nextByAddr, info.pop(size));
		break;
	    }
	    Object methodResult = null;
	    if (constant) {
		try {
		    if (jode.Obfuscator.isDebugging)
			jode.Decompiler.isDebugging = true; /*XXX*/
		    methodResult = runtime.invokeMethod
			(ref, opcode != opc_invokespecial, cls, args);
		} catch (InterpreterException ex) {
		    constant = false;
		    if (jode.Obfuscator.verboseLevel > 3)
			Obfuscator.err.println("Can't interpret "+ref+": "
					       + ex.getMessage());
		    /* result is not constant */
		} catch (InvocationTargetException ex) {
		    constant = false;
		    if (jode.Obfuscator.verboseLevel > 3)
			Obfuscator.err.println("Method "+ref
					       +" throwed exception: "
					       + ex.getTargetException()
					       .getMessage());
		    /* method always throws exception ? */
		}
	    }
	    ConstantAnalyzerValue returnVal;
	    if (!constant) {
		handleReference(ref, opcode == opc_invokevirtual 
				|| opcode == opc_invokeinterface);
		returnVal = 
		    unknownValue[mt.getReturnType().stackSize()-1];
	    } else {
		shortInfo.flags |= CONSTANT;
		shortInfo.constant = methodResult;
		returnVal = new ConstantAnalyzerValue(methodResult);
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
	    ConstantAnalyzerValue array = info.getStack(1);
    	    if (array.value != ConstantAnalyzerValue.VOLATILE
		&& array.value != null) {
		shortInfo.flags |= CONSTANT;
		shortInfo.constant = new Integer(Array.getLength(array.value));
		result = new ConstantAnalyzerValue(shortInfo.constant);
		result.addConstantListener(shortInfo);
		array.addConstantListener(result);
	    } else
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

    public void analyzeCode() {
	working = true;	
	if (constInfos == null)
	    constInfos = new Hashtable();
	instrStack = new Stack();
	bytecode.getFirstInstr().tmpInfo = new ConstantAnalyzerInfo
	    (bytecode.getMaxLocals(), m.info.isStatic(), m.info.getType());
	instrStack.push(bytecode.getFirstInstr());
	while (!instrStack.isEmpty()) {
	    Instruction instr = (Instruction) instrStack.pop();
	    handleOpcode(instr);
	}

	Handler[] handlers = bytecode.getExceptionHandlers();
	for (int i=0; i< handlers.length; i++) {
	    if (handlers[i].catcher.tmpInfo != null 
		&& handlers[i].type != null)
		m.clazz.bundle.reachableIdentifier(handlers[i].type, false);
	}
	working = false;
	for (Instruction instr = bytecode.getFirstInstr();
	     instr != null; instr = instr.nextByAddr)
	    instr.tmpInfo = null;
	instrStack = null;
//  	System.gc();
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
		    if (!instrStack.contains(instr))
			instrStack.push(instr);
		}
	    }
	}
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
    
    public BytecodeInfo stripCode() {
	if (constInfos == null)
	    analyzeCode();
// 	bytecode.dumpCode(Obfuscator.err);
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
		    if (Obfuscator.verboseLevel > 2)
			Obfuscator.err.println
			    (m + ": Replacing " + instr
			     + " with constant " + info.constant);
		}
		instr.tmpInfo = null;
	    } else if ((info.flags & CONSTANTFLOW) != 0) {
		Instruction pc = (Instruction) info.constant;
		if (instr.opcode >= opc_if_icmpeq
		    && instr.opcode <= opc_if_acmpne)
		    instr.opcode = opc_pop2;
		else
		    instr.opcode = opc_pop;
		instr.alwaysJumps = false;
		instr.succs[0].removePredecessor(instr);
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
		    /* fall through */
		case opc_ifeq: case opc_ifne: 
		case opc_iflt: case opc_ifge: 
		case opc_ifgt: case opc_ifle:
		case opc_if_icmpeq: case opc_if_icmpne:
		case opc_if_icmplt: case opc_if_icmpge: 
		case opc_if_icmpgt: case opc_if_icmple: 
		case opc_if_acmpeq: case opc_if_acmpne:
		case opc_ifnull: case opc_ifnonnull:
		    if (instr.succs[0] == instr.nextByAddr)
			instr.removeInstruction();
		    break;

		case opc_putstatic:
		case opc_putfield: {
		    Reference ref = (Reference) instr.objData;
		    FieldIdentifier fi = (FieldIdentifier) 
			m.clazz.bundle.getIdentifier(ref);
		    if (fi != null
			&& jode.Obfuscator.shouldStrip && !fi.isReachable()) {
			insertPop(instr);
			instr.removeInstruction();
		    } 
		    break;
		}
		}
		instr.tmpInfo = null;
	    }
	}
	return bytecode;
    }
}
