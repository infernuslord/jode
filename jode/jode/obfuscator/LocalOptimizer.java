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

public class LocalOptimizer implements Opcodes {

    class LocalInfo {
	LocalInfo shadow = null;

	public LocalInfo getReal() {
	    LocalInfo real = this;
	    while (real.shadow != null)
		real = real.shadow;
	    return real;
	}

	Vector usingInstrs = new Vector();
	Vector conflictingLocals = new Vector();
	int size;
	int newSlot = -1;

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
	    Enumeration enum = usingInstrs.elements();
	    while (enum.hasMoreElements()) {
		InstrInfo instr = (InstrInfo) enum.nextElement();
		instr.local = l;
		l.usingInstrs.addElement(instr);
	    }
	}
    }

    class InstrInfo {
	/**
	 * The LocalInfo of the next Instruction, that may read a local, 
	 * without prior writing.
	 */
	LocalInfo[] nextReads;
	/**
	 * The jsr to which the nextReads at this slot belongs to.
	 * I think I don't like jsrs any more.
	 */
	Vector[] belongsToJsrs;
	/**
	 * The LocalInfo for this local
	 */
	LocalInfo local;
	/**
	 * The Instruction of this info
	 */
	Instruction instr;
	/**
	 * The next info in the chain.
	 */
	InstrInfo nextInfo;
	/**
	 * If instruction is a jsr, this array contains the ret instructions.
	 */
	Instruction[] retInstrs;
    }

    InstrInfo firstInfo;
    Stack changedInfos;
    Hashtable instrInfos;
    BytecodeInfo bc;

    public LocalOptimizer(BytecodeInfo bc) {
	this.bc = bc;
    }

    /**
     * This method determines which rets belong to a given jsr.  This
     * is needed, since the predecessors must be exact.  
     */
    void findRets(InstrInfo jsrInfo) {
	Vector rets = new Vector();
	Stack instrStack = new Stack();
	Instruction subInstr = jsrInfo.instr.succs[0];
	if (subInstr.opcode != opc_astore)
	    throw new AssertError("Non standard jsr");
	int slot = subInstr.localSlot;
	instrStack.push(subInstr.nextByAddr);
	while (!instrStack.isEmpty()) {
	    Instruction instr = (Instruction) instrStack.pop();
	    if (instr.localSlot == slot) {
		if (instr.opcode != opc_ret)
		    throw new AssertError("Non standard jsr");
		rets.addElement(instr);
	    }
	    if (!instr.alwaysJumps)
		instrStack.push(instr.nextByAddr);
	    if (instr.succs != null)
		for (int i=0; i< instr.succs.length; i++)
		    instrStack.push(instr.succs[i]);
	}
	jsrInfo.retInstrs = new Instruction[rets.size()];
	rets.copyInto(jsrInfo.retInstrs);
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

    void promoteReads(InstrInfo info, Instruction preInstr, 
		      Instruction belongingJsr) {
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
	int maxlocals = bc.getMaxLocals();
	for (int i=0; i< maxlocals; i++) {
	    Vector newBelongs = info.belongsToJsrs[i];
	    if (preInstr.opcode == opc_jsr
		&& newBelongs != null) {
		if (!info.belongsToJsrs[i].contains(preInstr))
		    /* This was the wrong jsr */
		    continue;
		if (info.belongsToJsrs[i].size() == 1)
		    newBelongs = null;
		else {
		    newBelongs = (Vector) info.belongsToJsrs[i].clone();
		    newBelongs.removeElement(preInstr);
		}
	    }
	    if (belongingJsr != null) {
		if (newBelongs == null)
		    newBelongs = new Vector();
		newBelongs.addElement(belongingJsr);
	    }
	    if (info.nextReads[i] != null && i != omitLocal) {
		preInfo.belongsToJsrs[i] 
		    = merge(preInfo.belongsToJsrs[i], newBelongs);
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

    public void calcLocalInfo() {
	int maxlocals = bc.getMaxLocals();
	Handler[] handlers = bc.getExceptionHandlers();
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
		    info.local.size = 1;
		    switch (instr.opcode) {
		    case opc_lload: case opc_dload:
			info.local.size = 2;
			/* fall through */
		    case opc_iload: case opc_fload: case opc_aload:
		    case opc_ret:
		    case opc_iinc:
			/* this is a load instruction */
			info.nextReads[instr.localSlot] = info.local;
			changedInfos.push(info);
			break;

		    case opc_lstore: case opc_dstore:
			info.local.size = 2;
		    case opc_istore: case opc_fstore: case opc_astore:
		    }
		}
		if ((instr = instr.nextByAddr) == null)
		    break;
		info = info.nextInfo = new InstrInfo();
	    }
	}

	for (InstrInfo info = firstInfo; info != null; info = info.nextInfo)
	    if (info.instr.opcode == opc_jsr)
		findRets(info);

	/* find out which locals are the same.
	 */
	while (!changedInfos.isEmpty()) {
	    InstrInfo info = (InstrInfo) changedInfos.pop();
	    Enumeration enum = info.instr.preds.elements();
	    while (enum.hasMoreElements()) {
		Instruction preInstr
		    = (Instruction) enum.nextElement();
		if (preInstr.opcode == opc_jsr
		    && info.instr != preInstr.succs[0]) {
		    /* Prev expr is a jsr, continue with the
		     * corresponding ret, instead with the jsr.
		     */
		    InstrInfo jsrInfo = 
			(InstrInfo) instrInfos.get(preInstr);
		    for (int i= jsrInfo.retInstrs.length; i-- > 0; )
			promoteReads(info, jsrInfo.retInstrs[i], preInstr);
		} else
		    promoteReads(info, preInstr, null);
	    }
	    for (int i=0; i<handlers.length; i++) {
		if (handlers[i].catcher == info.instr) {
		    for (Instruction preInstr = handlers[i].start;
			 preInstr != handlers[i].end.nextByAddr; 
			 preInstr = preInstr.nextByAddr) {
			promoteReads(info, preInstr, null);
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
	int maxlocals = bc.getMaxLocals(); 


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

	/* Update the instructions.
	 */
	for (InstrInfo info = firstInfo; info != null; info = info.nextInfo) {
	    if (info.local != null)
		info.instr.localSlot = info.local.newSlot;
	}
    }

    public void dumpLocals() {
	Vector locals = new Vector();
	for (InstrInfo info = firstInfo; info != null; info = info.nextInfo) {
//  	    System.err.print("addr: "+info.instr.addr+ "  locals: ");
//  	    for (int i=0; i<bc.info.getMaxLocals(); i++)
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
