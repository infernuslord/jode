/* ConstantAnalyzer Copyright (C) 1999, 2000 Jochen Hoenicke.
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

package jode.obfuscator.modules;

import jode.GlobalOptions;
import jode.bytecode.*;
import jode.jvm.InterpreterException;
import jode.obfuscator.*;
import jode.util.StringQuoter;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.lang.reflect.InvocationTargetException;
import java.io.PrintWriter;
import java.util.BitSet;

///#def COLLECTIONS java.util
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.ListIterator;
///#enddef

/**
 * Analyze the code, assuming every field that is not yet written to
 * is constant.  This may imply that some code is dead code.
 *
 * While we analyze the Code we remember, which local variable and
 * which stack slot is constant for each instruction and if the
 * instruction is dead.  First we assume that every local and every
 * slot is constant for each instruction, and that all instructions
 * are dead code.  
 *
 * Now we mark all local variables of the first instruction as not
 * constant and the first instruction as modified.
 *
 * While there is a modified instruction, we pick one and repeat the
 * following algorithm:
 *
 * If the instruction produces a constant result (because it is a ldc
 * instruction, or it combines constant values), we put that instruction
 * on the ConstantListener-Queue of all constant inputs and put the 
 * constant result on the ConstantListener-Queue of that instruction.
 *       
 *
 * @author Jochen Hoenicke */
public class ConstantAnalyzer implements Opcodes, CodeAnalyzer {

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

    final static int CONSTANT        = 0x02;
    final static int CONSTANTFLOW    = 0x04;

    /**
     * The blocks, that are not analyzed yet, but whose before is
     * already set.
     */
    TodoQueue modifiedQueue = new TodoQueue();

    /**
     * The basic blocks for the current method.
     */
    BasicBlocks bb;
    /**
     * All block infos of all blocks in the current method.
     */
    BlockInfo[] infos;
    /**
     * The currently analyzed method, only valid while analyzeCode is running.
     */
    MethodIdentifier methodIdent;
    Map fieldDependencies;

    Map bbInfos = new HashMap();
    Map constantInfos = new HashMap();

    private interface ConstantListener {
	public void constantChanged();
    }

    private static class ConstValue implements ConstantListener {
	public final static Object VOLATILE = new Object();
	/**
	 * The constant value, VOLATILE if value is not constant.
	 *
	 * This may also be an instance of BlockInfo and point
	 * to the target block of the jsr, for which this variable
	 * contains the return value.
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
	
	public ConstValue(int stackSize) {
	    this.value = VOLATILE;
	    this.stackSize = stackSize;
	}
	
	public ConstValue(ConstValue constant) {
	    value = constant.value;
	    stackSize = constant.stackSize;
	    listeners = new HashSet();
	    constant.addConstantListener(this);
	}

	public ConstValue copy() {
	    return value == VOLATILE ? this
		: new ConstValue(this);
	}
	
	/**
	 * Merge the other value into this value.
	 */
	public void merge(ConstValue other) {
	    if (this == other)
		return;

	    if (value == null ? other.value == null
		: value.equals(other.value)) {
		if (other.value != VOLATILE)
		    other.addConstantListener(this);
		return;
	    }

	    if (value != VOLATILE)
		fireChanged();
	}

	public void addConstantListener(ConstantListener l) {
	    listeners.add(l);
	}
	
	public void removeConstantListener(ConstantListener l) {
	    listeners.remove(l);
	}
	
	public void fireChanged() {
	    value = VOLATILE;
	    for (Iterator i = listeners.iterator(); i.hasNext(); ) {
		ConstantListener l = (ConstantListener) i.next();
		System.err.println("  notifying: "+ l);
		l.constantChanged();
	    }
	    listeners = null;
	}
	
	public void constantChanged() {
	    if (value != VOLATILE)
		fireChanged();
	}
	
	public String toString() {
	    String result;
	    if (value == VOLATILE) 
		result = "vol("+stackSize+")";
	    else if (value instanceof String)
		result = StringQuoter.quote((String) value);
	    else
		result = String.valueOf(value);

//  	    StringBuffer sb = new StringBuffer(result).append('{');
//  	    Iterator i = listeners.iterator();
//  	    while (i.hasNext())
//  		sb.append(i.next()).append(',');
//  	    result = sb.append('}').toString();
	    return result+"@"+hashCode();
	}
    }

    private final static class JsrInfo implements Cloneable {
	BitSet usedLocals;
	BlockInfo jsrTarget;
	Collection callers;

	/**
	 * The dependent entries, that want to know if the bit set changed.
	 * This is either a StackLocalInfo (the ret info) or a single
	 * JsrInfo or a Collection of JsrInfos.
	 */
	Object dependent;

	public JsrInfo(BlockInfo target) {
	    jsrTarget = target;
	    callers = new ArrayList();
	    usedLocals = new BitSet();
	}

	public JsrInfo copy() {
	    try {
		JsrInfo result = (JsrInfo) clone();
		result.usedLocals = (BitSet) usedLocals.clone();
		addDependent(result);
		return result;
	    } catch (CloneNotSupportedException ex) {
		throw new IncompatibleClassChangeError(ex.getMessage());
	    }
	}

	private void addDependent(JsrInfo result) {
	    if (dependent == null || dependent == result)
		dependent = result;
	    else if (dependent instanceof JsrInfo) {
		Set newDeps = new HashSet();
		newDeps.add(dependent);
		newDeps.add(result);
	    } else if (dependent instanceof Collection) {
		((Collection) dependent).add(result);
	    }
	}

	public boolean uses(int localSlot) {
	    return usedLocals.get(localSlot);
	}

	public void setRetInfo(BlockInfo retInfo) {
	    dependent = retInfo;
	    for (Iterator i = callers.iterator(); i.hasNext(); )
		retInfo.mergeRetLocals(this, (BlockInfo)i.next());
	}

	public void addCaller(BlockInfo caller) {
	    if (callers.contains(caller))
		return;
	    callers.add(caller);

	    if (dependent instanceof BlockInfo) {
		BlockInfo retInfo = ((BlockInfo) dependent);
		for (Iterator i = callers.iterator(); i.hasNext(); )
		    retInfo.mergeRetLocals(this, caller);
	    } else if (dependent instanceof JsrInfo) {
		((JsrInfo) dependent).addCaller(caller);
	    } else if (dependent instanceof Collection) {
		Iterator iter = ((Collection) dependent).iterator();
		while (iter.hasNext()) {
		    JsrInfo dep = (JsrInfo) iter.next();
		    dep.addCaller(caller);
		}		
	    }
	}

	public boolean hasRetInfo() {
	    if (dependent instanceof BlockInfo)
		return true;
	    else if (dependent instanceof JsrInfo)
		return ((JsrInfo) dependent).hasRetInfo();
	    else if (dependent instanceof Collection) {
		Iterator iter = ((Collection) dependent).iterator();
		while (iter.hasNext()) {
		    JsrInfo dep = (JsrInfo) iter.next();
		    if (dep.hasRetInfo())
			return true;
		}		
	    }
	    return false;
	}

	public void addUsed(int localSlot) {
	    if (usedLocals.get(localSlot))
		return;
	    usedLocals.set(localSlot);

	    if (dependent instanceof BlockInfo) {
		BlockInfo retInfo = ((BlockInfo) dependent);
		for (Iterator i = callers.iterator(); i.hasNext(); )
		    retInfo.mergeRetLocals(this, (BlockInfo)i.next());
	    } else if (dependent instanceof JsrInfo) {
		((JsrInfo) dependent).addUsed(localSlot);
	    } else if (dependent instanceof Collection) {
		Iterator iter = ((Collection) dependent).iterator();
		while (iter.hasNext()) {
		    JsrInfo dep = (JsrInfo) iter.next();
		    dep.addUsed(localSlot);
		}
	    }
	}

	public void merge(JsrInfo o) {
	    o.addDependent(this);
	    for (int slot = 0; slot < o.usedLocals.size(); slot++) {
		if (o.usedLocals.get(slot))
		    addUsed(slot);
	    }
	    for (Iterator i = o.callers.iterator(); i.hasNext();) {
		addCaller((BlockInfo) i.next());
	    }
	}

	public String toString() {
	    StringBuffer sb = new StringBuffer(String.valueOf(jsrTarget));
	    if (dependent instanceof BlockInfo)
		sb.append("->").append(((BlockInfo) dependent).nr);
	    return sb.append(usedLocals)
		.append('_').append(hashCode()).toString();
	}
    }

    private static class StackLocalInfo {
	ConstValue[] stack;
	ConstValue[] locals;
	int stackDepth;

	private StackLocalInfo(ConstValue[] stack, 
			       ConstValue[] locals, int stackDepth) {
	    this.stack = stack;
	    this.locals = locals;
	    this.stackDepth = stackDepth;
	}
    
	public StackLocalInfo(int maxStack, int numLocals,
			      boolean isStatic, String methodTypeSig) {
	    
	    String[] paramTypes 
		= TypeSignature.getParameterTypes(methodTypeSig);
	    locals = new ConstValue[numLocals];
	    stack = new ConstValue[maxStack];
	    stackDepth = 0;
	    int slot = 0;
	    if (!isStatic)
		locals[slot++] = unknownValue[0];
	    for (int i=0; i< paramTypes.length; i++) {
		int stackSize = TypeSignature.getTypeSize(paramTypes[i]);
		locals[slot] = unknownValue[stackSize-1];
		slot += stackSize;
	    }
	}

	public StackLocalInfo(StackLocalInfo original) {
	    locals = new ConstValue[original.locals.length];
	    for (int i=0; i< locals.length; i++) {
		if (original.locals[i] != null)
		    locals[i] = original.locals[i].copy();
	    }
	    stack = new ConstValue[original.stack.length];
	    for (int i=0; i< stackDepth; i++) {
		if (original.stack[i] != null)
		    stack[i] = original.stack[i].copy();
	    }
	}

	public StackLocalInfo poppush(int pops, ConstValue push) {
	    for (int i = pops; i>= 0; i--)
		stack[--stackDepth] = null;
	    stack[stackDepth] = push;
	    stackDepth += push.stackSize;
	    return this;
	}

	public StackLocalInfo pop(int pops) {
	    for (int i = pops; i>= 0; i--)
		stack[--stackDepth] = null;
	    return this;
	}

	public StackLocalInfo dup(int count, int depth) {
	    int bottom = stackDepth - count - depth;
	    System.arraycopy(stack, stackDepth - count,
			     stack, stackDepth, count);
	    if (depth > 0) {
		System.arraycopy(stack, bottom,
				 stack, bottom + count, depth);
		System.arraycopy(stack, stackDepth,
				 stack, bottom, count);
	    }
	    stackDepth += count;
	    return this;
	}

	public StackLocalInfo swap() {
	    ConstValue tmp = stack[stackDepth - 1];
	    stack[stackDepth-1] = stack[stackDepth-2];
	    stack[stackDepth-2] = tmp;
	    return this;
	}

	public StackLocalInfo copy() {
	    ConstValue[] newStack = (ConstValue[]) stack.clone();
	    ConstValue[] newLocals = (ConstValue[]) locals.clone();
	    return new StackLocalInfo(newStack, newLocals, stackDepth);
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
	    return this;
	}

	public void mergeOneLocal(int slot, ConstValue cv) {
	    if (locals[slot] != null) {
		if (cv == null)
		    // Other can be not initialized
		    // implies local can be not initialized
		    locals[slot] = null;
		else
		    locals[slot].merge(cv);
	    }
	}

	public void merge(StackLocalInfo other) {
	    for (int i=0; i < locals.length; i++)
		mergeOneLocal(i, other.locals[i]);
	    if (stack.length != other.stack.length)
		throw new InternalError("stack length differs");
	    for (int i=0; i < stack.length; i++) {
		if ((other.stack[i] == null) != (stack[i] == null))
		    throw new InternalError("stack types differ");
		else if (stack[i] != null)
		    stack[i].merge(other.stack[i]);
	    }
	}

	public String toString() {
	    return "Locals: "+Arrays.asList(locals)
		+"Stack: "+Arrays.asList(stack);
	}
    }

    private static class ConstantInfo implements ConstantListener {
	ConstantInfo() {
	    this(0, null);
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

    /**
     * The block info contains the info needed for a single block.
     */
    private class BlockInfo implements ConstantListener {

	int nr;
	Block block;
	BlockInfo nextTodo;
	
	int constantFlow = -1;
	/**
	 * The state of the locals and stack before this block is
	 * executed.
	 */
	StackLocalInfo before;

	/**
	 * The state of the locals and stack after this block is
	 * executed, but before the last jump instruction is done.
	 * So for conditional jumps the stack still contains the
	 * operands.
	 */
	StackLocalInfo after;

	/**
	 * The JsrInfo of jsrs that must be visited to reach
	 * this block. 
	 */
	JsrInfo[] jsrInfos;

	public BlockInfo(int nr, Block block) {
	    this.nr = nr;
	    this.block = block;
	}

	public boolean isReachable() {
	    return before != null;
	}

	public void mergeOneLocal(int slot, ConstValue cv) {
	    before.mergeOneLocal(slot, cv);
	}

	public void mergeBefore(StackLocalInfo info, 
				JsrInfo[] newJsrInfos) {
	    if (before == null) {
		System.err.println("mergeBefore:::"+info);
		before = new StackLocalInfo(info);
		this.jsrInfos = new JsrInfo[newJsrInfos.length];
		for (int i = 0; i < newJsrInfos.length; i++)
		    jsrInfos[i] = newJsrInfos[i].copy();
		modifiedQueue.enqueue(this);
	    } else {
		System.err.println("merging:::: "+before+":::AND:::"+info);
		before.merge(info);
		int newJsrCount = 0;
		int ptr = 0;
		for (int i = 0; i < jsrInfos.length; i++) {
		    for (int j = ptr; j < newJsrInfos.length; j++) {
			if (newJsrInfos[j].jsrTarget
			    == jsrInfos[i].jsrTarget) {
			    jsrInfos[newJsrCount] = jsrInfos[i];
			    jsrInfos[newJsrCount].merge(newJsrInfos[j]);
			    newJsrCount++;
			    ptr = j+1;
			    break;
			}
		    }
		}
		if (newJsrCount < jsrInfos.length) {
		    JsrInfo[] newJsr = new JsrInfo[newJsrCount];
		    System.arraycopy(jsrInfos, 0, newJsr, 0, newJsrCount);
		    jsrInfos = newJsr;
		}
		if (constantFlow >= 0)
		    propagateAfter();
	    }
	}

	public void constantChanged() {
	    if (constantFlow >= 0)
		propagateAfter();
	}

	public void useLocal(int slot) {
	    for (int i=0; i< jsrInfos.length; i++)
		jsrInfos[i].addUsed(slot);
	}

	public void mergeRetLocals(JsrInfo myJsrInfo, BlockInfo caller) {
	    if (constantFlow == 0) {
		Instruction[] instrs = block.getInstructions();
		// remove the constantFlow info
		constantInfos.remove(instrs[instrs.length-1]);
	    }
	    Block nextBlock = caller.block.getSuccs()[1];
	    if (nextBlock == null) {
		/* The calling jsr is just before a return.  We don't
		 * have to fuzz around, since nobody is interested in
		 * constant values.
		 */
		return;
	    }

	    ConstValue[] newLocals = (ConstValue[]) after.locals.clone();
	    for (int slot = 0; slot < newLocals.length; slot++) {
		if (!myJsrInfo.uses(slot))
		    newLocals[slot] = caller.after.locals[slot];
		else
		    caller.useLocal(slot);
	    }
	    StackLocalInfo nextInfo
		= new StackLocalInfo(after.stack, newLocals, after.stackDepth);
	    int newCount = jsrInfos.length - 1;
	    while (jsrInfos[newCount].jsrTarget != myJsrInfo.jsrTarget)
		newCount--;
	    JsrInfo[] retJsrInfos = new JsrInfo[newCount];
	    System.arraycopy(jsrInfos, 0, 
			     retJsrInfos, 0, newCount);
	    infos[nextBlock.getBlockNr()].mergeBefore(nextInfo, retJsrInfos);
	}
	
	public void propagateAfter() {
	    Instruction[] instrs = block.getInstructions();
	    Instruction instr = instrs[instrs.length-1];
	    int opcode = instr.getOpcode();
	    switch (opcode) {
	    case opc_ifeq: case opc_ifne: 
	    case opc_iflt: case opc_ifge: 
	    case opc_ifgt: case opc_ifle:
	    case opc_if_icmpeq: case opc_if_icmpne:
	    case opc_if_icmplt: case opc_if_icmpge: 
	    case opc_if_icmpgt: case opc_if_icmple: 
	    case opc_if_acmpeq: case opc_if_acmpne:
	    case opc_ifnull: case opc_ifnonnull: {
		int size = 1;
		ConstValue stacktop = after.getStack(1);
		ConstValue other = null;
		boolean known = stacktop.value != ConstValue.VOLATILE;
		if (opcode >= opc_if_icmpeq && opcode <= opc_if_acmpne) {
		    other = after.getStack(2);
		    size = 2;
		    known &= other.value != ConstValue.VOLATILE;
		}
		StackLocalInfo nextInfo = after.copy().pop(size);
		if (known) {
		    if (constantFlow >= 0)
			/* Nothing changed... */
			return;
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
		    
		    constantFlow = ((opc_mask & (1<<opcode)) != 0) ? 0 : 1;
		    ConstantInfo constInfo = new ConstantInfo
			(CONSTANTFLOW, new Integer(constantFlow));
		    constantInfos.put(instr, constInfo);
		    
		    stacktop.addConstantListener(this);
		    if (other != null)
			other.addConstantListener(this);
		    Block constantSucc = block.getSuccs()[constantFlow];
		    if (constantSucc != null)
			infos[constantSucc.getBlockNr()]
			    .mergeBefore(nextInfo, jsrInfos);
		} else {
		    constantInfos.remove(instr);
		    for (int i=0; i < 2; i++) {
			if (i != constantFlow) {
			    Block succ = block.getSuccs()[i];
			    if (succ != null)
				infos[succ.getBlockNr()]
				    .mergeBefore(nextInfo, jsrInfos);
			}
		    }
		    constantFlow = -1;
		}
		break;
	    }
	    case opc_lookupswitch: {
		ConstValue stacktop = after.getStack(1);
		StackLocalInfo nextInfo = after.copy().pop(1);
		if (stacktop.value != ConstValue.VOLATILE) {
		    if (constantFlow >= 0)
			/* Nothing changed... */
			return;
		    Instruction pc;
		    int value = ((Integer) stacktop.value).intValue();
		    int[] values = instr.getValues();
		    constantFlow = Arrays.binarySearch(values, value);
		    if (constantFlow < 0)
			constantFlow = values.length;
		    ConstantInfo constInfo = new ConstantInfo
			(CONSTANTFLOW, new Integer(constantFlow));
		    constantInfos.put(instr, constInfo);
		    stacktop.addConstantListener(this);
		    Block constantSucc = block.getSuccs()[constantFlow];
		    if (constantSucc != null)
			infos[constantSucc.getBlockNr()]
			    .mergeBefore(nextInfo, jsrInfos);
		} else {
		    constantInfos.remove(instr);
		    Block[] succs = block.getSuccs();
		    for (int i=0; i < succs.length; i++) {
			if (i != constantFlow) {
			    if (succs[i] != null) 
				infos[succs[i].getBlockNr()]
				    .mergeBefore(nextInfo, jsrInfos);
			}
		    }
		    constantFlow = -1;
		}
		break;
	    }
	    case opc_jsr: {
		// XXXXXXXXXXXX System.err
		dumpBlockInfo(GlobalOptions.err);
		GlobalOptions.err.println(instr);

		BlockInfo target = infos[block.getSuccs()[0].getBlockNr()];
		ConstValue result = new ConstValue(target);
		int jsrInfosCount = jsrInfos.length;
		for (int i=0; i< jsrInfosCount; i++) {
		    if (jsrInfos[i].jsrTarget == target) {
			// recursive jsrs are forbidden. But maybe the jsr
			// just terminated without a ret.
			jsrInfosCount = i;
		    }
		}
		JsrInfo[] newJsrInfos = new JsrInfo[jsrInfosCount + 1];
		System.arraycopy(jsrInfos, 0, newJsrInfos, 0, jsrInfosCount);
		newJsrInfos[jsrInfosCount] = new JsrInfo(target);
		newJsrInfos[jsrInfosCount].addCaller(this);
		constantFlow = 0;
		ConstantInfo constInfo = new ConstantInfo
		    (CONSTANTFLOW, new Integer(0));
		constantInfos.put(instr, constInfo);
		target.mergeBefore(after.copy().poppush(0, result), 
				   newJsrInfos);
		break;
	    }
	    case opc_ret: {
		ConstValue result = after.getLocal(instr.getLocalSlot());
		BlockInfo jsrTarget = (BlockInfo) result.value;

		int i = jsrInfos.length-1; 
		while (jsrInfos[i].jsrTarget != jsrTarget)
		    i--;
		jsrInfos[i].setRetInfo(this);
		break;
	    }
	    case opc_ireturn: case opc_lreturn: 
	    case opc_freturn: case opc_dreturn:
	    case opc_areturn: case opc_return:
	    case opc_athrow:
		break;
	    default: {
		Block succ = block.getSuccs()[0];
		if (succ != null)
		    infos[succ.getBlockNr()].mergeBefore(after, jsrInfos);
	    }
	    }
	}
	    
	public void analyze() {
	    System.err.println("analyze:");
	    StackLocalInfo info = before.copy();
	    Handler[] handlers = block.getCatchers();

	    if (handlers.length > 0) {
		ConstValue[] newStack = new ConstValue[info.stack.length];
		newStack[0] = unknownValue[0];
		StackLocalInfo catchInfo = 
		    new StackLocalInfo(newStack, info.locals, 1);

		for (int i=0; i< handlers.length; i++) {
		    if (handlers[i].getType() != null)
			Main.getClassBundle().reachableClass
			    (handlers[i].getType());
		    
		    infos[handlers[i].getCatcher().getBlockNr()]
			.mergeBefore(catchInfo, jsrInfos);
		}
	    }
	    
	    Instruction[] instrs = block.getInstructions();
	    for (int idx = 0 ; idx < instrs.length; idx++) {
		Instruction instr = instrs[idx];
		info = handleOpcode(instr, info, this);
		if (instr.isStore() && handlers.length > 0) {
		    int slot = instr.getLocalSlot();
		    ConstValue newValue = info.locals[slot];
		    for (int i=0; i< handlers.length; i++) {
			infos[handlers[i].getCatcher().getBlockNr()]
			    .mergeOneLocal(slot, info.locals[slot]);
			if (newValue.stackSize > 1)
			    infos[handlers[i].getCatcher().getBlockNr()]
				.mergeOneLocal(slot+1, info.locals[slot+1]);
		    }
		}
	    }
	    after = info;
	    dumpInfo(GlobalOptions.err);
	    propagateAfter();
	}

	public void dumpInfo(PrintWriter output) {
	    output.println("/-["+nr+"]-"+before);
	    block.dumpCode(output);
	    output.println("\\-["+nr+"]-"+after);
	}

	public String toString() {
	    return "BlockAnalyzer["+nr+"]";
	}
    }


    /**
     * The TodoQueue is a linked list of BlockInfo
     *
     * There is only one TodoQueue, the modifiedQueue in analyzeCode
     *
     * The queue operations are in StackLocalInfo.
     */
    static class TodoQueue {
	BlockInfo first;

	public void enqueue(BlockInfo info) {
	    if (info.nextTodo == null) {
		info.nextTodo = first;
		first = info;
	    }
	}

	public BlockInfo dequeue() {
	    BlockInfo result = first;
	    if (result != null) {
		first = result.nextTodo;
		result.nextTodo = null;
	    }
	    return result;
	}
    }

    public void fieldNotConstant(FieldIdentifier fi) {
	ConstValue value = (ConstValue) fieldDependencies.remove(fi);
	if (value != null)
	    value.constantChanged();
	fi.removeFieldListener(methodIdent);
	fi.setNotConstant();
    }
    
    private ClassInfo canonizeIfaceRef(ClassInfo clazz, Reference ref) {
	while (clazz != null) {
	    if (clazz.findMethod(ref.getName(), ref.getType()) != null)
		return clazz;
	    ClassInfo[] ifaces = clazz.getInterfaces();
	    for (int i = 0; i < ifaces.length; i++) {
		ClassInfo realClass = canonizeIfaceRef(ifaces[i], ref);
		if (realClass != null)
		    return realClass;
	    }
	    clazz = clazz.getSuperclass();
	}
	return null;
    }

    private Identifier canonizeReference(Instruction instr) {
	Reference ref = instr.getReference();
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
		clazz = ClassInfo.forName("java.lang.Object");
	    } else {
		clazz = ClassInfo.forName
		    (clName.substring(1, clName.length()-1)
		     .replace('/','.'));
	    }
	    if (instr.getOpcode() == opc_invokeinterface) {
		clazz = canonizeIfaceRef(clazz, ref);
	    } else if (instr.getOpcode() >= opc_invokevirtual) {
		while (clazz != null
		       && clazz.findMethod(ref.getName(), 
					   ref.getType()) == null)
		    clazz = clazz.getSuperclass();
	    } else {
		while (clazz != null
		       && clazz.findField(ref.getName(), 
					  ref.getType()) == null)
		    clazz = clazz.getSuperclass();
	    }

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
	    instr.setReference(ref);
	}
	return ident;
    }

    void handleReference(Reference ref, boolean isVirtual) {
	Main.getClassBundle().reachableReference(ref, isVirtual);
    }

    void handleClass(String clName) {
	int i = 0;
	while (i < clName.length() && clName.charAt(i) == '[')
	    i++;
	if (i < clName.length() && clName.charAt(i) == 'L') {
	    clName = clName.substring(i+1, clName.length()-1);
	    Main.getClassBundle().reachableClass(clName);
	}
    }

    StackLocalInfo handleOpcode(Instruction instr, 
				StackLocalInfo info, BlockInfo block) {
	constantInfos.remove(instr);
	int opcode = instr.getOpcode();
	ConstValue result;
	switch (opcode) {
        case opc_nop:
	    return info.pop(0);

        case opc_ldc:
        case opc_ldc2_w:
	    result = new ConstValue(instr.getConstant());
	    return info.poppush(0, result);

        case opc_iload: case opc_lload: 
        case opc_fload: case opc_dload: case opc_aload:
	    result = info.getLocal(instr.getLocalSlot());
	    if (result.value != ConstValue.VOLATILE) {
		ConstantInfo constInfo
		    = new ConstantInfo(CONSTANT, result.value);
		result.addConstantListener(constInfo);
		constantInfos.put(instr, constInfo);
	    }
	    return info.poppush(0, result)
		.setLocal(instr.getLocalSlot(), result);
        case opc_iaload: case opc_laload: 
        case opc_faload: case opc_daload: case opc_aaload:
        case opc_baload: case opc_caload: case opc_saload: {
	    result = unknownValue[(opcode == opc_laload
				   || opcode == opc_daload) ? 1 : 0];
	    return info.poppush(2, result);
	}
        case opc_istore: case opc_fstore: case opc_astore: {
	    int slot = instr.getLocalSlot();
	    block.useLocal(slot);
	    return info.pop(1).setLocal(slot, info.getStack(1));
	}
	case opc_lstore: case opc_dstore: {
	    int slot = instr.getLocalSlot();
	    block.useLocal(slot);
	    block.useLocal(slot + 1);
	    return info.pop(2).setLocal(slot, info.getStack(2));
	}
        case opc_iastore: case opc_lastore:
        case opc_fastore: case opc_dastore: case opc_aastore:
        case opc_bastore: case opc_castore: case opc_sastore: {
	    int size = (opcode == opc_lastore
			|| opcode == opc_dastore) ? 2 : 1;
	    return info.pop(2+size);
	}
        case opc_pop: 
	    return info.pop(1);
	case opc_pop2:
	    return info.pop(2);

	case opc_dup: case opc_dup_x1: case opc_dup_x2:
        case opc_dup2: case opc_dup2_x1: case opc_dup2_x2:
	    return info.dup((opcode - (opc_dup - 3)) / 3,
			    (opcode - (opc_dup - 3)) % 3);
        case opc_swap:
	    return info.swap();

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
		if (((opcode == opc_idiv || opcode == opc_irem)
		     && ((Integer)value2.value).intValue() == 0)
		    || ((opcode == opc_ldiv || opcode == opc_lrem)
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
		    throw new InternalError("Can't happen.");
		}
		ConstantInfo constInfo = new ConstantInfo(CONSTANT, newValue);
		constantInfos.put(instr, constInfo);
		result = new ConstValue(newValue);
		result.addConstantListener(constInfo);
		value1.addConstantListener(result);
		value2.addConstantListener(result);
	    } else 
		result = unknownValue[size-1];
	    return info.poppush(2*size, result);
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
		    throw new InternalError("Can't happen.");
		}
		ConstantInfo constInfo = new ConstantInfo(CONSTANT, newValue);
		constantInfos.put(instr, constInfo);
		result = new ConstValue(newValue);
		result.addConstantListener(constInfo);
		value.addConstantListener(result);
	    } else 
		result = unknownValue[size-1];
	    return info.poppush(size, result);
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
		    throw new InternalError("Can't happen.");
		}
		ConstantInfo constInfo = new ConstantInfo(CONSTANT, newValue);
		constantInfos.put(instr, constInfo);
		result = new ConstValue(newValue);
		result.addConstantListener(constInfo);
		value1.addConstantListener(result);
		value2.addConstantListener(result);
	    } else 
		result = unknownValue[size-1];
	    return info.poppush(size+1, result);
	}
        case opc_iinc: {
	    ConstValue local = info.getLocal(instr.getLocalSlot());
	    if (local.value != ConstValue.VOLATILE) {
		result = new ConstValue
		    (new Integer(((Integer)local.value).intValue()
				 + instr.getIncrement()));
		local.addConstantListener(result);
	    } else 
		result = unknownValue[0];
	    return info.setLocal(instr.getLocalSlot(), result);
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
		    throw new InternalError("Can't happen.");
		}
		ConstantInfo constInfo = new ConstantInfo(CONSTANT, newVal);
		constantInfos.put(instr, constInfo);
		result = new ConstValue(newVal);
		result.addConstantListener(constInfo);
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
	    return info.poppush(insize, result);
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
		Integer newVal = new Integer(val);
		ConstantInfo constInfo = new ConstantInfo(CONSTANT, newVal);
		constantInfos.put(instr, constInfo);
		result = new ConstValue(newVal);
		stack.addConstantListener(constInfo);
		stack.addConstantListener(result);
	    } else
		result = unknownValue[0];
	    return info.poppush(1, result);
	}
        case opc_lcmp: {
	    ConstValue val1 = info.getStack(4);
	    ConstValue val2 = info.getStack(2);
	    if (val1.value != ConstValue.VOLATILE
		&& val2.value != ConstValue.VOLATILE) {
		long value1 = ((Long) val1.value).longValue();
		long value2 = ((Long) val1.value).longValue();
		Integer newVal = new Integer(value1 == value2 ? 0
					     : value1 < value2 ? -1 : 1);
		ConstantInfo constInfo = new ConstantInfo(CONSTANT, newVal);
		constantInfos.put(instr, constInfo);
		result = new ConstValue(newVal);
		result.addConstantListener(constInfo);
		val1.addConstantListener(result);
		val2.addConstantListener(result);
	    } else
		result = unknownValue[0];
	    return info.poppush(4, result);
	}
        case opc_fcmpl: case opc_fcmpg: {
	    ConstValue val1 = info.getStack(2);
	    ConstValue val2 = info.getStack(1);
	    if (val1.value != ConstValue.VOLATILE
		&& val2.value != ConstValue.VOLATILE) {
		float value1 = ((Float) val1.value).floatValue();
		float value2 = ((Float) val1.value).floatValue();
		Integer newVal = new Integer
		    (value1 == value2 ? 0
		     : ( opcode == opc_fcmpg
			 ? (value1 < value2 ? -1 :  1)
			 : (value1 > value2 ?  1 : -1)));
		ConstantInfo constInfo = new ConstantInfo(CONSTANT, newVal);
		constantInfos.put(instr, constInfo);
		result = new ConstValue(newVal);
		result.addConstantListener(constInfo);
		val1.addConstantListener(result);
		val2.addConstantListener(result);
	    } else
		result = unknownValue[0];
	    return info.poppush(2, result);
	}
        case opc_dcmpl: case opc_dcmpg: {
	    ConstValue val1 = info.getStack(4);
	    ConstValue val2 = info.getStack(2);
	    if (val1.value != ConstValue.VOLATILE
		&& val2.value != ConstValue.VOLATILE) {
		double value1 = ((Double) val1.value).doubleValue();
		double value2 = ((Double) val1.value).doubleValue();
		Integer newVal = new Integer
		    (value1 == value2 ? 0
		     : ( opcode == opc_dcmpg
			 ? (value1 < value2 ? -1 :  1)
			 : (value1 > value2 ?  1 : -1)));
		ConstantInfo constInfo = new ConstantInfo(CONSTANT, newVal);
		constantInfos.put(instr, constInfo);
		result = new ConstValue(newVal);
		result.addConstantListener(constInfo);
		val1.addConstantListener(result);
		val2.addConstantListener(result);
	    } else
		result = unknownValue[0];
	    return info.poppush(4, result);
	}
	case opc_ifeq: case opc_ifne: 
	case opc_iflt: case opc_ifge: 
	case opc_ifgt: case opc_ifle:
	case opc_if_icmpeq: case opc_if_icmpne:
	case opc_if_icmplt: case opc_if_icmpge: 
	case opc_if_icmpgt: case opc_if_icmple: 
	case opc_if_acmpeq: case opc_if_acmpne:
	case opc_ifnull: case opc_ifnonnull:
        case opc_lookupswitch:
        case opc_ireturn: case opc_lreturn: 
        case opc_freturn: case opc_dreturn: case opc_areturn:
        case opc_athrow:
        case opc_jsr:
	case opc_ret:
        case opc_goto:
        case opc_return:
	    return info;

	case opc_putstatic:
	case opc_putfield: {
	    final FieldIdentifier fi
		= (FieldIdentifier) canonizeReference(instr);
	    Reference ref = instr.getReference();
	    int size = TypeSignature.getTypeSize(ref.getType());
	    if (fi != null && !fi.isNotConstant()) {
		ConstValue stacktop = info.getStack(size);
		Object fieldVal = fi.getConstant();
		if (fieldVal == null)
		    fieldVal = TypeSignature.getDefaultValue(ref.getType());
		if (stacktop.value == null ? fieldVal == null
		    : stacktop.value.equals(fieldVal)) {
		    stacktop.addConstantListener(new ConstantListener() {
			public void constantChanged() {
			    fieldNotConstant(fi);
			}
		    });
		} else {
		    fieldNotConstant(fi);
		}
	    }
	    size += (opcode == opc_putstatic) ? 0 : 1;
	    return info.pop(size);
	}
	case opc_getstatic:
	case opc_getfield: {
	    int size = (opcode == opc_getstatic) ? 0 : 1;
	    FieldIdentifier fi = (FieldIdentifier) canonizeReference(instr);
	    Reference ref = instr.getReference();
	    int typesize = TypeSignature.getTypeSize(ref.getType());
	    if (fi != null) {
		if (fi.isNotConstant()) {
		    fi.setReachable();
		    result = unknownValue[typesize - 1];
		} else {
		    Object obj = fi.getConstant();
		    if (obj == null)
			obj = TypeSignature.getDefaultValue(ref.getType());
		    ConstantInfo constInfo = new ConstantInfo(CONSTANT, obj);
		    constantInfos.put(instr, constInfo);
		    result = new ConstValue(obj);
		    fi.addFieldListener(methodIdent);
		    ConstValue prev = (ConstValue) fieldDependencies.get(fi);
		    if (prev != null)
			prev.addConstantListener(result);
		    else
			fieldDependencies.put(fi, result);
		}
	    } else
		result = unknownValue[typesize - 1];
	    return info.poppush(size, result);
	}
	case opc_invokespecial:
	case opc_invokestatic:
	case opc_invokeinterface:
	case opc_invokevirtual: {
	    canonizeReference(instr);
	    Reference ref = instr.getReference();
	    boolean constant = true;
	    int size = 0;
	    Object   cls = null;
	    String[] paramTypes 
		= TypeSignature.getParameterTypes(ref.getType());
	    Object[] args = new Object[paramTypes.length];
	    ConstValue clsValue = null;
	    ConstValue[] argValues = new ConstValue[paramTypes.length];

	    for (int i = paramTypes.length - 1; i >= 0; i--) {
		size += TypeSignature.getTypeSize(paramTypes[i]);
		Object value = (argValues[i] = info.getStack(size)).value;
		if (value != ConstValue.VOLATILE)
		    args[i] = value;
		else
		    constant = false;
	    }
	    
	    if (opcode != opc_invokestatic) {
		size++;
		clsValue = info.getStack(size);
		cls = clsValue.value;
		if (cls == ConstValue.VOLATILE || cls == null)
		    constant = false;
	    }
	    String retType = TypeSignature.getReturnType(ref.getType());
	    if (retType.equals("V")) {
		handleReference(ref, opcode == opc_invokevirtual 
				|| opcode == opc_invokeinterface);
		return info.pop(size);
	    }
	    if (constant && !runtime.isWhite(retType)) {
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
						  + ex.getTargetException());
		    /* method always throws exception ? */
		}
	    }
	    ConstValue returnVal;
	    if (!constant) {
		handleReference(ref, opcode == opc_invokevirtual 
				|| opcode == opc_invokeinterface);
		int retsize = TypeSignature.getTypeSize(retType);
		returnVal = unknownValue[retsize - 1];
	    } else {
		ConstantInfo constInfo = 
		    new ConstantInfo(CONSTANT, methodResult);
		constantInfos.put(instr, constInfo);
		returnVal = new ConstValue(methodResult);
		returnVal.addConstantListener(constInfo);
		if (clsValue != null)
		    clsValue.addConstantListener(returnVal);
		for (int i=0; i< argValues.length; i++)
		    argValues[i].addConstantListener(returnVal);
	    }
	    return info.poppush(size, returnVal);
	}

        case opc_new: {
	    handleClass(instr.getClazzType());
	    return info.poppush(0, unknownValue[0]);
        }
        case opc_arraylength: {
	    return info.poppush(1, unknownValue[0]);
	}
        case opc_checkcast: {
	    handleClass(instr.getClazzType());
	    return info.pop(0);
        }
        case opc_instanceof: {
	    handleClass(instr.getClazzType());
	    return info.poppush(1, unknownValue[0]);
        }
        case opc_monitorenter:
        case opc_monitorexit:
	    return info.pop(1);
        case opc_multianewarray:
	    handleClass(instr.getClazzType());
	    return info.poppush(instr.getDimensions(), unknownValue[0]);
	default:
            throw new IllegalArgumentException("Invalid opcode "+opcode);
        }
    }


    public ConstantAnalyzer() {
    }


    public void dumpBlockInfo(PrintWriter output) {
	for (int i=0; i < infos.length; i++)
	    infos[i].dumpInfo(output);
    }

    public void analyzeCode(MethodIdentifier methodIdent, 
			    BasicBlocks bb) {
	Block[] blocks = bb.getBlocks();
	this.methodIdent = methodIdent;
	this.bb = bb;
	this.infos = new BlockInfo[blocks.length];
	this.fieldDependencies = new HashMap();

	for (int i=0; i< infos.length; i++)
	    infos[i] = new BlockInfo(i, blocks[i]);

	Block startBlock = bb.getStartBlock();
	if (startBlock != null) {
	    MethodInfo minfo = bb.getMethodInfo();
	    infos[startBlock.getBlockNr()].mergeBefore
		(new StackLocalInfo(bb.getMaxStack(), bb.getMaxLocals(), 
				    minfo.isStatic(), minfo.getType()),
		 new JsrInfo[0]);

	    BlockInfo info;
	    while ((info = modifiedQueue.dequeue()) != null) {
		System.err.println(info);
		info.analyze();
	    }
	}
	
	BitSet reachableBlocks = new BitSet();
	for (int i=0; i< infos.length; i++) {
	    if (infos[i].isReachable())
		reachableBlocks.set(i);
	}
	bbInfos.put(bb, reachableBlocks);
	
	this.methodIdent = null;
	this.bb = null;
	this.infos = null;
	this.fieldDependencies = null;
    }

    public static void replaceWith(ArrayList newCode, Instruction instr,
				   Instruction replacement) {
	switch(instr.getOpcode()) {
	case opc_jsr:
	    newCode.add(Instruction.forOpcode(opc_ldc, (Object) null));
	    break;
        case opc_ldc:
        case opc_ldc2_w:
        case opc_iload: case opc_lload: 
        case opc_fload: case opc_dload: case opc_aload:
	case opc_getstatic:
	    if (replacement != null)
		newCode.add(replacement);
	    return;
	case opc_ifeq: case opc_ifne: 
	case opc_iflt: case opc_ifge: 
	case opc_ifgt: case opc_ifle:
	case opc_ifnull: case opc_ifnonnull:
	case opc_arraylength:
	case opc_lookupswitch:
	case opc_getfield:
        case opc_i2l: case opc_i2f: case opc_i2d:
        case opc_f2i: case opc_f2l: case opc_f2d:
        case opc_i2b: case opc_i2c: case opc_i2s:
        case opc_ineg: case opc_fneg: 
	    newCode.add(Instruction.forOpcode(opc_pop));
	    break;
	case opc_if_icmpeq: case opc_if_icmpne:
	case opc_if_icmplt: case opc_if_icmpge: 
	case opc_if_icmpgt: case opc_if_icmple: 
	case opc_if_acmpeq: case opc_if_acmpne:
	case opc_lcmp: 
	case opc_dcmpg: case opc_dcmpl: 
        case opc_ladd: case opc_dadd:
        case opc_lsub: case opc_dsub:
        case opc_lmul: case opc_dmul:
        case opc_ldiv: case opc_ddiv:
        case opc_lrem: case opc_drem:
	case opc_land: case opc_lor : case opc_lxor:
	    newCode.add(Instruction.forOpcode(opc_pop2));
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
	    newCode.add(Instruction.forOpcode(opc_pop2));
	    break;

	case opc_lshl: case opc_lshr: case opc_lushr:
	    newCode.add(Instruction.forOpcode(opc_pop));
	    newCode.add(Instruction.forOpcode(opc_pop2));
	    break;
	case opc_putstatic:
	case opc_putfield:
	    if (TypeSignature
		.getTypeSize(instr.getReference().getType()) == 2) {
		newCode.add(Instruction.forOpcode(opc_pop2));
		if (instr.getOpcode() == opc_putfield)
		    newCode.add(Instruction.forOpcode(opc_pop));
	    } else
		newCode.add(Instruction.forOpcode(instr.getOpcode()
					       == opc_putfield
					       ? opc_pop2 : opc_pop));
	    break;
	case opc_invokespecial:
	case opc_invokestatic:
	case opc_invokeinterface:
	case opc_invokevirtual: {
	    Reference ref = instr.getReference();
	    String[] pt = TypeSignature.getParameterTypes(ref.getType());
	    int arg = 0;
	    if (instr.getOpcode() != opc_invokestatic)
		newCode.add(Instruction.forOpcode(opc_pop));
	    else if (pt.length > 0) {
		newCode.add(Instruction.forOpcode(TypeSignature.getTypeSize(pt[0])
					       + opc_pop - 1));
		arg++;
	    } else {
		if (replacement != null)
		    newCode.add(replacement);
		return;
	    }
	    
	    for (int i=arg; i < pt.length; i++)
		newCode.add(Instruction.forOpcode
			    (TypeSignature.getTypeSize(pt[i])
			     + opc_pop - 1));
	}
	default:
	    throw new InternalError("Unexpected opcode");
	}
	if (replacement != null)
	    newCode.add(replacement);
    }
    
    public void transformCode(BasicBlocks bb) {
	BitSet reachable = (BitSet) bbInfos.remove(bb);
	Block[] blocks = bb.getBlocks();
	Handler[] handlers = bb.getExceptionHandlers();

	Block newStartBlock = bb.getStartBlock();
	int newBlockCtr = 0;
	int newHandlerCtr = 0;
    next_handler:
	for (int i = 0; i < handlers.length; i++) {
	    int start = handlers[i].getStart().getBlockNr();
	    int end = handlers[i].getEnd().getBlockNr();
	    while (!reachable.get(end)) {
		if (start == end)
		    /* handler not reachable, check next one. */
		    continue next_handler;
		start++;
	    }
	    while (!reachable.get(end)) {
		end--;
	    }
	    handlers[i].setStart(blocks[start]);
	    handlers[i].setEnd(blocks[start]);
	    /* Catcher is always reachable */
	    handlers[newHandlerCtr++] = handlers[i];
	}
	for (int i=0; i < blocks.length; i++) {
	    if (!reachable.get(i))
		continue;
	    blocks[newBlockCtr] = blocks[i];
	    Instruction[] oldCode = blocks[i].getInstructions();
	    Block[] succs = blocks[i].getSuccs();
	    ArrayList newCode = new ArrayList(oldCode.length);
	    for (int idx = 0; idx < oldCode.length; idx++) {
		Instruction instr = oldCode[idx];
		ConstantInfo info = (ConstantInfo) constantInfos.remove(instr);
		if ((info.flags & CONSTANT) != 0) {
		    Instruction ldcInstr = Instruction.forOpcode
			(info.constant instanceof Long
			 || info.constant instanceof Double
			 ? opc_ldc2_w : opc_ldc, info.constant);
		    if (GlobalOptions.verboseLevel > 2)
			GlobalOptions.err.println
				(bb + ": Replacing " + instr
				 + " with constant " + info.constant);
		    replaceWith(newCode, instr, ldcInstr);
		} else if ((info.flags & CONSTANTFLOW) != 0) {
		    int succnr = ((Integer)info.constant).intValue();
		    replaceWith(newCode, instr, null);
		    if (GlobalOptions.verboseLevel > 2)
			GlobalOptions.err.println
			    (bb + ": Removing " + instr);
		    succs = new Block[] { succs[succnr] };
		} else {
		    int opcode = instr.getOpcode();
		    switch (opcode) {
		    case opc_nop:
			break;

		    case opc_putstatic:
		    case opc_putfield: {
			Reference ref = instr.getReference();
			FieldIdentifier fi = (FieldIdentifier) 
			    Main.getClassBundle().getIdentifier(ref);
			if (fi != null
			    && (Main.stripping & Main.STRIP_UNREACH) != 0
			    && !fi.isReachable()) {
			    replaceWith(newCode, instr, null);
			}
			break;
		    }
		    default:
			newCode.add(instr);
		    }
		}
	    }
	    blocks[i].setCode((Instruction[]) newCode.toArray(oldCode), succs);
	    newBlockCtr++;
	}
	if (newBlockCtr < blocks.length) {
	    Block[] newBlocks = new Block[newBlockCtr];
	    Handler[] newHandlers = new Handler[newHandlerCtr];
	    System.arraycopy(blocks, 0, newBlocks, 0, newBlockCtr);
	    System.arraycopy(handlers, 0, newHandlers, 0, newHandlerCtr);
	    bb.setBlocks(newBlocks, newStartBlock, newHandlers);
	}
    }
}
