/* LocalOptimizer Copyright (C) 1999 Jochen Hoenicke.
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
import java.util.*;
import jode.bytecode.*;
import jode.AssertError;
import jode.Obfuscator;

/**
 * This class takes some bytecode and tries to minimize the number
 * of locals used.  It will also remove unnecessary stores.  
 *
 * This class can only work on verified code.  There should also be
 * no deadcode, since we can't be sure that deadcode behaves okay.
 */
public class LocalOptimizer implements Opcodes {

    /**
     * This class keeps track of which locals must be the same, which
     * name and type each local (if there is a local variable table) and
     * which other locals have an intersecting life time.
     */
    class LocalInfo {
	LocalInfo shadow = null;

	public LocalInfo getReal() {
	    LocalInfo real = this;
	    while (real.shadow != null)
		real = real.shadow;
	    return real;
	}

	String name;
	String type;
	Vector usingInstrs = new Vector();
	Vector conflictingLocals = new Vector();
	int size;
	int newSlot = -1;
	/**
	 * If this local is used as returnAddress, this gives the
	 * ret that uses it.
	 */
	InstrInfo retInfo;

	LocalInfo() {
	}
	
	LocalInfo(InstrInfo instr) {
	    usingInstrs.addElement(instr);
	}

	void conflictsWith(LocalInfo l) {
	    if (shadow != null) {
		getReal().conflictsWith(l);
	    } else {
		l = l.getReal();
		if (!conflictingLocals.contains(l)) {
		    conflictingLocals.addElement(l);
		    l.conflictingLocals.addElement(this);
		}
	    }
	}
	
	void combineInto(LocalInfo l) {
	    if (shadow != null) {
		getReal().combineInto(l);
		return;
	    }
	    l = l.getReal();
	    if (this == l)
		return;
	    shadow = l;
	    if (shadow.name == null) {
		shadow.name = name;
		shadow.type = type;
	    }
	    if (retInfo != null) {
		if (retInfo != shadow.retInfo)
		    Obfuscator.err.println
			("Warning: Multiple rets on one jsr?");
		shadow.retInfo = retInfo;
	    }
	    Enumeration enum = usingInstrs.elements();
	    while (enum.hasMoreElements()) {
		InstrInfo instr = (InstrInfo) enum.nextElement();
		instr.local = l;
		l.usingInstrs.addElement(instr);
	    }
	}
    }


    /**
     * This class contains information for each instruction.
     */
    class InstrInfo {
	/**
	 * The LocalInfo that this instruction manipulates, or null
	 * if this is not an ret, iinc, load or store instruction.
	 */
	LocalInfo local;
	/**
	 * For each slot, this contains the LocalInfo of the next
	 * Instruction, that may read from that slot, without prior
	 * writing.  
	 */
	LocalInfo[] nextReads;
	/**
	 * For each slot if get() is true, no instruction may read
	 * this slot, since it may contain different locals, depending
	 * on flow.  
	 */
	BitSet conflictingLocals;
	/**
	 * If instruction is the destination of a jsr, this contains
	 * the single allowed ret instructions, or null if there is
	 * no ret at all.  
	 */
	Instruction retInstr;
	/**
	 * If instruction is a jsr, this contains the slots
	 * used by the sub routine
	 */
	BitSet usedBySub;
	/**
	 * The jsr to which the nextReads at this slot belongs to.
	 * I think I don't like jsrs any more.
	 */
	Vector[] belongsToJsrs;
	/**
	 * The Instruction of this info
	 */
	Instruction instr;
	/**
	 * The next info in the chain.
	 */
	InstrInfo nextInfo;
    }

    InstrInfo firstInfo;
    Stack changedInfos;
    Hashtable instrInfos;
    BytecodeInfo bc;
    boolean produceLVT;
    int maxlocals;
    int paramCount;

    public LocalOptimizer(BytecodeInfo bc, int paramCount) {
	this.bc = bc;
	this.paramCount = paramCount;
    }

    /**
     * This method determines which rets belong to a given jsr.  This
     * is needed, since the predecessors must be exact.  
     */
    void analyzeSubRoutine(InstrInfo subInfo) {
	Stack instrStack = new Stack();
	Instruction subInstr = subInfo.instr;
	if (subInfo.usedBySub != null)
	    return;

	subInfo.usedBySub = new BitSet(maxlocals);
	if (subInstr.opcode != opc_astore) {
	    /* Grrr, the bytecode verifier doesn't test if a
	     * jsr starts with astore.  So it is possible to
	     * do something else before putting the ret address
	     * into a local.  It would be even possible to pop
	     * the address, and never return.
	     */
	    throw new AssertError("Non standard jsr");
	}
	int slot = subInstr.localSlot;
	subInfo.usedBySub.set(slot);
	instrStack.push(subInstr.nextByAddr);
	while (!instrStack.isEmpty()) {
	    Instruction instr = (Instruction) instrStack.pop();
	    if (instr.localSlot == slot) {
		if (instr.opcode >= opc_istore
		    && instr.opcode <= opc_astore) 
		    /* Return address is overwritten, we will never
		     * return.
		     */
		    continue;
		
		if (instr.opcode != opc_ret
		    || (subInfo.retInstr != null
			&& subInfo.retInstr != instr))
		    /* This can't happen in legal bytecode. */
		    throw new AssertError("Illegal bytecode");
		
		subInfo.retInstr = instr;
	    } else if (instr.localSlot != -1) {
		subInfo.usedBySub.set(instr.localSlot);
	    }
	    if (!instr.alwaysJumps)
		instrStack.push(instr.nextByAddr);
	    if (instr.succs != null)
		for (int i=0; i< instr.succs.length; i++)
		    instrStack.push(instr.succs[i]);
	}
    }

    /**
     * Merges the given vector to a new vector.  Both vectors may
     * be null in which case they are interpreted as empty vectors.
     * The vectors will never changed, but the result may be one
     * of the given vectors.
     */
    Vector merge(Vector v1, Vector v2) {
	if (v1 == null || v1.isEmpty())
	    return v2;
	if (v2 == null || v2.isEmpty())
	    return v1;
	Vector result = (Vector) v1.clone();
	Enumeration enum = v2.elements();
	while (enum.hasMoreElements()) {
	    Object elem = enum.nextElement();
	    if (!result.contains(elem))
		result.addElement(elem);
	}
	return result;
    }

    void promoteSubRoutineReads(InstrInfo info, Instruction preInstr,
				InstrInfo jsrInfo) {
	InstrInfo preInfo = (InstrInfo) instrInfos.get(preInstr);
	int omitLocal = -1;
	if (preInstr.localSlot != -1
	    && preInstr.opcode >= opc_istore
	    && preInstr.opcode <= opc_astore) {
	    /* This is a store */
	    omitLocal = preInstr.localSlot;
	    if (info.nextReads[preInstr.localSlot] != null)
		preInfo.local.combineInto
		    (info.nextReads[preInstr.localSlot]);
	}
	for (int i=0; i < maxlocals; i++) {
	    if (info.nextReads[i] != null && i != omitLocal
		&& (jsrInfo == null || !jsrInfo.usedBySub.get(i))) {

		if (preInfo.nextReads[i] == null) {
		    preInfo.nextReads[i] = info.nextReads[i];
		    changedInfos.push(preInfo);
		} else {
		    preInfo.nextReads[i]
			.combineInto(info.nextReads[i]);
		}
	    }
	}
    }

    void promoteNotModifiedReads(InstrInfo info, Instruction preInstr,
				 InstrInfo jsrInfo) {
	InstrInfo preInfo = (InstrInfo) instrInfos.get(preInstr);
	int omitLocal = -1;
	if (preInstr.localSlot != -1
	    && preInstr.opcode >= opc_istore
	    && preInstr.opcode <= opc_astore) {
	    /* This is a store */
	    omitLocal = preInstr.localSlot;
	    if (info.nextReads[preInstr.localSlot] != null)
		preInfo.local.combineInto
		    (info.nextReads[preInstr.localSlot]);
	}
	for (int i=0; i < maxlocals; i++) {
	    if (info.nextReads[i] != null && i != omitLocal
		&& (jsrInfo == null || !jsrInfo.usedBySub.get(i))) {

		if (preInfo.nextReads[i] == null) {
		    preInfo.nextReads[i] = info.nextReads[i];
		    changedInfos.push(preInfo);
		} else {
		    preInfo.nextReads[i]
			.combineInto(info.nextReads[i]);
		}
	    }
	}
    }

    void promoteReads(InstrInfo info, Instruction preInstr) {
	promoteNotModifiedReads(info, preInstr, null);
    }

    public LocalVariableInfo findLVTEntry(LocalVariableInfo[] lvt, 
					  Instruction instr) {
	int addr = instr.addr;
	if (instr.opcode >= opc_istore
	    && instr.opcode <= opc_astore)
	    addr += instr.length;
	LocalVariableInfo match = null;
	for (int i=0; i < lvt.length; i++) {
	    if (lvt[i].slot == instr.localSlot
		&& lvt[i].start.addr <= addr
		&& lvt[i].end.addr > addr) {
		if (match != null)
		    /* Multiple matches..., give no info */
		    return null;
		match = lvt[i];
	    }
	}
	return match;
    }

    public void calcLocalInfo() {
	maxlocals = bc.getMaxLocals();
	Handler[] handlers = bc.getExceptionHandlers();
	LocalVariableInfo[] lvt = bc.getLocalVariableTable();
	if (lvt != null)
	    produceLVT = true;
	/* Initialize the InstrInfos and LocalInfos
	 */
	changedInfos = new Stack();
	instrInfos = new Hashtable();
	{
	    InstrInfo info = firstInfo = new InstrInfo();
	    Instruction instr = bc.getFirstInstr();
	    while (true) {
		instrInfos.put(instr, info);
		info.instr = instr;
		info.nextReads = new LocalInfo[maxlocals];
		info.belongsToJsrs = new Vector[maxlocals];
		if (instr.localSlot != -1) {
		    info.local = new LocalInfo(info);
		    if (lvt != null) {
			LocalVariableInfo lvi = findLVTEntry(lvt, instr);
			if (lvi != null) {
			    info.local.name = lvi.name;
			    info.local.type = lvi.type;
			}
		    }
		    info.local.size = 1;
		    switch (instr.opcode) {
		    case opc_lload: case opc_dload:
			info.local.size = 2;
			/* fall through */
		    case opc_iload: case opc_fload: case opc_aload:
		    case opc_iinc:
			/* this is a load instruction */
			info.nextReads[instr.localSlot] = info.local;
			changedInfos.push(info);
			break;

		    case opc_ret:
			/* this is a ret instruction */
			info.local.retInfo = info;
			info.nextReads[instr.localSlot] = info.local;
			changedInfos.push(info);
			break;

		    case opc_lstore: case opc_dstore:
			info.local.size = 2;
		    //case opc_istore: case opc_fstore: case opc_astore:
		    }
		}
		if ((instr = instr.nextByAddr) == null)
		    break;
		info = info.nextInfo = new InstrInfo();
	    }
	}

//  	for (InstrInfo info = firstInfo; info != null; info = info.nextInfo)
//  	    if (info.instr.opcode == opc_jsr)
//  		analyzeSubRoutine(info.succs[0]);

	/* find out which locals are the same.
	 */
	while (!changedInfos.isEmpty()) {
	    InstrInfo info = (InstrInfo) changedInfos.pop();
	    Instruction instr = info.instr;

	    Instruction prevInstr = instr.prevByAddr;
	    if (prevInstr != null) {
		if (prevInstr.opcode == opc_jsr) {
		    /* Prev instr is a jsr, promote reads to the
		     * corresponding ret.
		     */
		    InstrInfo jsrInfo = 
			(InstrInfo) instrInfos.get(prevInstr.succs[0]);
		    if (jsrInfo.retInstr != null)
			promoteReads(info, jsrInfo.retInstr);

		    /* Now promote reads that aren't modified by the
		     * subroutine to prevInstr
		     */
		    promoteSubRoutineReads(info, prevInstr, jsrInfo);

		} else if (!prevInstr.alwaysJumps)
		    promoteReads(info, prevInstr);
	    }

	    if (instr.preds != null) {
		for (int i = 0; i < instr.preds.length; i++) {
		    if (instr.preds[i].opcode == opc_jsr) {
			if (info.instr.opcode != opc_astore) {
			    /* XXX Grrr, the bytecode verifier doesn't
			     * test if a jsr starts with astore.  So
			     * it is possible to do something else
			     * before putting the ret address into a
			     * local.  */
			    throw new AssertError("Non standard jsr");
			}
			LocalInfo local = info.nextReads[info.instr.localSlot];
			if (local != null && local.retInfo != null) {
			    info.retInstr = local.retInfo.instr;
			    /*XXX*/
			}
		    }
		    promoteReads(info, instr.preds[i]);
		}
	    }

	    for (int i=0; i < handlers.length; i++) {
		if (handlers[i].catcher == instr) {
		    for (Instruction preInstr = handlers[i].start;
			 preInstr != handlers[i].end.nextByAddr; 
			 preInstr = preInstr.nextByAddr) {
			promoteReads(info, preInstr);
		    }
		}
	    }
	}
	changedInfos = null;
    }

    public void stripLocals() {
	for (InstrInfo info = firstInfo; info != null; info = info.nextInfo) {
	    if (info.local != null && info.local.usingInstrs.size() == 1) {
		/* If this is a store, whose value is never read; it can
                 * be removed, i.e replaced by a pop. */
		switch (info.instr.opcode) {
		case opc_istore:
		case opc_fstore:
		case opc_astore:
		    info.local = null;
		    info.instr.opcode = opc_pop;
		    info.instr.length = 1;
		    info.instr.localSlot = -1;
		    break;
		case opc_lstore:
		case opc_dstore:
		    info.local = null;
		    info.instr.opcode = opc_pop2;
		    info.instr.length = 1;
		    info.instr.localSlot = -1;
		    break;
		default:
		}
	    }
	}
    }

    void distributeLocals(Vector locals) {
	if (locals.size() == 0)
	    return;

	/* Find the local with the least conflicts. */
	int min = Integer.MAX_VALUE;
	LocalInfo bestLocal = null;
	Enumeration enum = locals.elements();
	while (enum.hasMoreElements()) {
	    LocalInfo li = (LocalInfo) enum.nextElement();
	    int conflicts = 0;
	    Enumeration conflenum = li.conflictingLocals.elements();
	    while (conflenum.hasMoreElements()) {
		if (((LocalInfo)conflenum.nextElement()).newSlot != -2)
		    conflicts++;
	    }
	    if (conflicts < min) {
		min = conflicts;
		bestLocal = li;
	    }
	}
	/* Mark the local as taken */
	locals.removeElement(bestLocal);
	bestLocal.newSlot = -2;
	/* Now distribute the remaining locals recursively. */
	distributeLocals(locals);

	/* Finally find a new slot */
    next_slot:
	for (int slot = 0; ; slot++) {
	    Enumeration conflenum = bestLocal.conflictingLocals.elements();
	    while (conflenum.hasMoreElements()) {
		LocalInfo conflLocal = (LocalInfo)conflenum.nextElement();
		if (bestLocal.size == 2 && conflLocal.newSlot == slot+1) {
		    slot++;
		    continue next_slot;
		}
		if (conflLocal.size == 2 && conflLocal.newSlot+1 == slot)
		    continue next_slot;
		if (conflLocal.newSlot == slot) {
		    if (conflLocal.size == 2)
			slot++;
		    continue next_slot;
		}
	    }
	    bestLocal.newSlot = slot;
	    break;
	}
    }
    
    public void distributeLocals() {
	/* give locals new slots.  This is a graph coloring 
	 * algorithm (the optimal solution is NP complete, but this
	 * should be a good approximation).
	 */

	/* first give the params the same slot as they had before.
	 * The params should be the locals in firstInfo.nextReads
	 */
	for (int i=0; i< maxlocals; i++) {
	    if (firstInfo.nextReads[i] != null)
		firstInfo.nextReads[i].getReal().newSlot = i;
	}

	/* Now calculate the conflict settings.
	 */
	for (InstrInfo info = firstInfo; info != null; info = info.nextInfo) {
	    if (info.instr.localSlot != -1
		&& info.instr.opcode >= BytecodeInfo.opc_istore
		&& info.instr.opcode <= BytecodeInfo.opc_astore) {
		/* This is a store.  It conflicts with every local, whose
		 * value will be read without write.
		 */
		for (int i=0; i < maxlocals; i++) {
		    if (i != info.instr.localSlot
			&& info.nextReads[i] != null)
			info.local.conflictsWith(info.nextReads[i]);
		}
	    }
	}

	/* Now put the locals that need a color into a vector.
	 */
	Vector locals = new Vector();
	for (InstrInfo info = firstInfo; info != null; info = info.nextInfo) {
	    if (info.local != null 
		&& info.local.newSlot == -1
		&& !locals.contains(info.local))
		locals.addElement(info.local);
	}

	/* Now distribute slots recursive.
	 */
	distributeLocals(locals);

	/* Update the instructions and calculate new maxlocals.
	 */
	maxlocals = paramCount;
	for (InstrInfo info = firstInfo; info != null; info = info.nextInfo) {
	    if (info.local != null) {
		if (info.local.newSlot+info.local.size > maxlocals)
		    maxlocals = info.local.newSlot + info.local.size;
		info.instr.localSlot = info.local.newSlot;
	    }
	}
	bc.setMaxLocals(maxlocals);

	/* Update LocalVariableTable
	 */
	if (produceLVT)
	    buildNewLVT();
    }

    private LocalInfo CONFLICT = new LocalInfo();

    void promoteValues(InstrInfo info, Instruction nextInstr) {
	Instruction instr = info.instr;
	InstrInfo nextInfo = (InstrInfo) instrInfos.get(nextInstr);
	for (int i=0; i< maxlocals; i++) {
	    LocalInfo local = info.nextReads[i];
	    if (local != null)
		local = local.getReal();
	    if (instr.localSlot == i
		&& instr.opcode >= opc_istore
		&& instr.opcode <= opc_astore)
		local = info.local;

	    if (nextInfo.nextReads[i] == null)
		nextInfo.nextReads[i] = local;
	    else if (local != null
		     && local != nextInfo.nextReads[i].getReal())
		nextInfo.nextReads[i] = CONFLICT;
	}
    }

    public void buildNewLVT() {
	/* We reuse the nextReads array for a differen purpose.  
	 * For every slot we remember in this array, which local is
	 * there 
	 * find out how long they remain there value.  
	 */
	Stack changedInfo = new Stack();
	Handler[] handlers = bc.getExceptionHandlers();
	for (InstrInfo info = firstInfo; 
	     info != null; info = info.nextInfo)
		changedInfo.push(info);
	while (!changedInfo.isEmpty()) {
	    InstrInfo info = (InstrInfo) changedInfo.pop();
	    Instruction instr = info.instr;
	    if (!instr.alwaysJumps) {
		Instruction nextInstr = instr.nextByAddr;
		    promoteValues(info, instr.nextByAddr);
	    }
	    
	    if (instr.succs != null) {
		for (int i = 0; i < instr.succs.length; i++)
		    promoteValues(info, instr.succs[i]);
	    }
	    for (int i=0; i < handlers.length; i++) {
		if (handlers[i].start.addr >= instr.addr
		    && handlers[i].end.addr <= instr.addr)
		    promoteValues(info, handlers[i].catcher);
	    }
	}

	Vector lvtEntries = new Vector();
	LocalVariableInfo[] lvi = new LocalVariableInfo[maxlocals];
	LocalInfo[] currentLocal = new LocalInfo[maxlocals];
	Instruction lastInstr = null;
	for (InstrInfo info = firstInfo; info != null; info = info.nextInfo) {
	    for (int i=0; i< maxlocals; i++) {
		LocalInfo lcl = info.nextReads[i];
		if (lcl == CONFLICT)
		    lcl = null;
		else if (lcl != null)
		    lcl = lcl.getReal();
		if (lcl != currentLocal[i]) {
		    if (lvi[i] != null) {
			lvi[i].end = info.instr.prevByAddr;
			lvtEntries.addElement(lvi[i]);
		    }
		    lvi[i] = null;
		    currentLocal[i] = lcl;
		    if (currentLocal[i] != null
			&& currentLocal[i].name != null) {
			lvi[i] = new LocalVariableInfo();
			lvi[i].name = currentLocal[i].name;
			lvi[i].type = currentLocal[i].type;
			lvi[i].start = info.instr;
			lvi[i].slot = i;
		    }
		    changedInfo.push(info);
		}
	    }
	    lastInstr = info.instr;
	}
	for (int i=0; i< maxlocals; i++) {
	    if (lvi[i] != null) {
		lvi[i].end = lastInstr;
		lvtEntries.addElement(lvi[i]);
	    }
	}
	LocalVariableInfo[] lvt = new LocalVariableInfo[lvtEntries.size()];
	lvtEntries.copyInto(lvt);
	bc.setLocalVariableTable(lvt);
    }

    public void dumpLocals() {
	Vector locals = new Vector();
	for (InstrInfo info = firstInfo; info != null; info = info.nextInfo) {
//  	    System.err.print("addr: "+info.instr.addr+ "  locals: ");
//  	    for (int i=0; i<maxlocals; i++)
//  		if (info.nextReads[i] == null)
//  		    System.err.print("-,");
//  		else
//  		    System.err.print(((InstrInfo)info.nextReads[i]
//  				      .usingInstrs.elementAt(0)).instr.addr
//  				     +",");
//  	    System.err.println();
	    if (info.local != null && !locals.contains(info.local))
		locals.addElement(info.local);
	}
	Enumeration enum = locals.elements();
	while (enum.hasMoreElements()) {
	    LocalInfo li = (LocalInfo) enum.nextElement();
	    int slot = ((InstrInfo)li.usingInstrs.elementAt(0))
		.instr.localSlot;
	    System.err.print("Slot: "+slot+" conflicts:");
	    Enumeration enum1 = li.conflictingLocals.elements();
	    while (enum1.hasMoreElements()) {
		LocalInfo cfl = (LocalInfo)enum1.nextElement();
		System.err.print(((InstrInfo)cfl.usingInstrs.elementAt(0))
				 .instr.addr+", ");
	    }
	    System.err.println();
	    System.err.print(((InstrInfo)li.usingInstrs.elementAt(0))
			     .instr.addr);
	    System.err.print("     instrs: ");
	    Enumeration enum2 = li.usingInstrs.elements();
	    while (enum2.hasMoreElements())
		System.err.print(((InstrInfo)enum2.nextElement()).instr.addr+", ");
	    System.err.println();
	}
	System.err.println("-----------");
    }
}
