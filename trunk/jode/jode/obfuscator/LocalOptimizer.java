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
import jode.type.Type;
import jode.AssertError;
import jode.Obfuscator;

/**
 * This class takes some bytecode and tries to minimize the number
 * of locals used.  It will also remove unnecessary stores.  
 *
 * This class can only work on verified code.  There should also be no
 * deadcode, since the verifier doesn't check that deadcode behaves
 * okay.  
 *
 * This is done in two phases.  First we determine which locals are
 * the same, and which locals have a overlapping life time. In the
 * second phase we will then redistribute the locals with a coloring
 * graph algorithm.
 *
 * The idea for the first phase is: For each read we follow the
 * instruction flow backward to find the corresponding writes.  We can
 * also merge with another control flow that has a different read, in
 * this case we merge with that read, too.
 *
 * The tricky part is the subroutine handling.  We follow the local
 * that is used in a ret and find the corresponding jsr target (there
 * must be only one, if the verifier should accept this class).  While
 * we do this we remember in the info of the ret, which locals are
 * used in that subroutine.
 *
 * When we know the jsr target<->ret correlation, we promote from the
 * nextByAddr of every jsr the locals that are accessed by the
 * subroutine to the corresponding ret and the others to the jsr.  Also
 * we will promote all reads from the jsr targets to the jsr.
 *
 * If you think this might be to complicated, keep in mind that jsr's
 * are not only left by the ret instructions, but also "spontanously"
 * (by not reading the return address again).
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
	 * For each slot, this contains the InstrInfo of one of the
	 * next Instruction, that may read from that slot, without
	 * prior writing.  */
	InstrInfo[] nextReads;

	/**
	 * This only has a value for ret instructions.  In that case
	 * this bitset contains all locals, that may be used between
	 * jsr and ret.  
	 */
	BitSet usedBySub;
	/**
	 * For each slot if get() is true, no instruction may read
	 * this slot, since it may contain different locals, depending
	 * on flow.  
	 */
	LocalInfo[] lifeLocals;
	/**
	 * If instruction is the destination of a jsr, this contains
	 * the single allowed ret instruction info, or null if there
	 * is no ret at all (or not yet detected).  
	 */
	InstrInfo retInfo;
	/**
	 * If this instruction is a ret, this contains the single
	 * allowed jsr target to which this ret belongs.  
	 */
	InstrInfo jsrTargetInfo;
	/**
	 * The Instruction of this info
	 */
	Instruction instr;
	/**
	 * The next info in the chain.
	 */
	InstrInfo nextInfo;
    }

    BytecodeInfo bc;
    MethodInfo methodInfo;

    InstrInfo firstInfo;
    Stack changedInfos;
    Hashtable instrInfos;
    boolean produceLVT;
    int maxlocals;

    LocalInfo[] paramLocals;

    public LocalOptimizer(BytecodeInfo bc, MethodInfo methodInfo) {
	this.bc = bc;
	this.methodInfo = methodInfo;
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
		      BitSet mergeSet, boolean inverted) {
	InstrInfo preInfo = (InstrInfo) instrInfos.get(preInstr);
	int omitLocal = -1;
	if (preInstr.localSlot != -1
	    && preInstr.opcode >= opc_istore
	    && preInstr.opcode <= opc_astore) {
	    /* This is a store */
	    omitLocal = preInstr.localSlot;
	    if (info.nextReads[preInstr.localSlot] != null)
		preInfo.local.combineInto
		    (info.nextReads[preInstr.localSlot].local);
	}
	for (int i=0; i < maxlocals; i++) {
	    if (info.nextReads[i] != null && i != omitLocal
		&& (mergeSet == null || mergeSet.get(i) != inverted)) {

		if (preInfo.nextReads[i] == null) {
		    preInfo.nextReads[i] = info.nextReads[i];
		    changedInfos.push(preInfo);
		} else {
		    preInfo.nextReads[i].local
			.combineInto(info.nextReads[i].local);
		}
	    }
	}
    }

    void promoteReads(InstrInfo info, Instruction preInstr) {
	promoteReads(info, preInstr, null, false);
    }

    public LocalVariableInfo findLVTEntry(LocalVariableInfo[] lvt, 
					  int slot, int addr) {
	LocalVariableInfo match = null;
	for (int i=0; i < lvt.length; i++) {
	    if (lvt[i].slot == slot
		&& lvt[i].start.addr <= addr
		&& lvt[i].end.addr >= addr) {
		if (match != null
		    && (!match.name.equals(lvt[i].name)
			|| !match.type.equals(lvt[i].type))) {
		    /* Multiple matches..., give no info */
		    return null;
		}
		match = lvt[i];
	    }
	}
	return match;
    }

    public LocalVariableInfo findLVTEntry(LocalVariableInfo[] lvt, 
					  Instruction instr) {
	int addr = instr.addr;
	if (instr.opcode >= opc_istore
	    && instr.opcode <= opc_astore)
	    addr += instr.length;
	return findLVTEntry(lvt, instr.localSlot, addr);
    }

    public void calcLocalInfo() {
	maxlocals = bc.getMaxLocals();
	Handler[] handlers = bc.getExceptionHandlers();
	LocalVariableInfo[] lvt = bc.getLocalVariableTable();
	if (lvt != null)
	    produceLVT = true;

	/* Initialize paramLocals */
	{
	    int paramCount = methodInfo.isStatic() ? 0 : 1;
	    Type[] paramTypes = 
		Type.tMethod(methodInfo.getType()).getParameterTypes();
	    for (int i = paramTypes.length; i-- > 0;)
		paramCount += paramTypes[i].stackSize();
	    paramLocals = new LocalInfo[paramCount];
	    int slot = 0;
	    if (!methodInfo.isStatic()) {
		LocalInfo local = new LocalInfo();
		if (lvt != null) {
		    LocalVariableInfo lvi = findLVTEntry(lvt, 0, 0);
		    if (lvi != null) {
			local.name = lvi.name;
			local.type = lvi.type;
		    }
		}
		local.size = 1;
		paramLocals[slot++] = local;
	    }
	    for (int i = 0; i< paramTypes.length; i++) {
		LocalInfo local = new LocalInfo();
		if (lvt != null) {
		    LocalVariableInfo lvi = findLVTEntry(lvt, slot, 0);
		    if (lvi != null) {
			local.name = lvi.name;
		    }
		}
		local.type = paramTypes[i].getTypeSignature();
		local.size = paramTypes[i].stackSize();
		paramLocals[slot] = local;
		slot += local.size;
	    }
	}

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
		info.nextReads = new InstrInfo[maxlocals];
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
			info.nextReads[instr.localSlot] = info;
			changedInfos.push(info);
			break;

		    case opc_ret:
			/* this is a ret instruction */
			info.usedBySub = new BitSet();
			info.nextReads[instr.localSlot] = info;
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

	/* find out which locals are the same.
	 */
	while (!changedInfos.isEmpty()) {
	    InstrInfo info = (InstrInfo) changedInfos.pop();
	    Instruction instr = info.instr;

	    /* Mark the local as used in all ret instructions */
	    if (instr.localSlot != -1) {
		for (int i=0; i< maxlocals; i++) {
		    InstrInfo retInfo = info.nextReads[i];
		    if (retInfo != null && retInfo.instr.opcode == opc_ret
			&& !retInfo.usedBySub.get(instr.localSlot)) {
			retInfo.usedBySub.set(instr.localSlot);
			if (retInfo.jsrTargetInfo != null)
			    changedInfos.push(retInfo.jsrTargetInfo);
		    }
		    
		}
	    }

	    Instruction prevInstr = instr.prevByAddr;
	    if (prevInstr != null) {
		if (prevInstr.opcode == opc_jsr) {
		    /* Prev instr is a jsr, promote reads to the
		     * corresponding ret.
		     */
		    InstrInfo jsrInfo = 
			(InstrInfo) instrInfos.get(prevInstr.succs[0]);
		    if (jsrInfo.retInfo != null) {
			/* Now promote reads that are modified by the
			 * subroutine to the ret, and those that are not
			 * to the jsr instruction.
			 */
			promoteReads(info, jsrInfo.retInfo.instr,
				     jsrInfo.retInfo.usedBySub, false);
			promoteReads(info, prevInstr, 
				     jsrInfo.retInfo.usedBySub, true);
		    }
		} else if (!prevInstr.alwaysJumps)
		    promoteReads(info, prevInstr);
	    }

	    if (instr.preds != null) {
		for (int i = 0; i < instr.preds.length; i++) {
		    Instruction predInstr = instr.preds[i];
		    if (instr.preds[i].opcode == opc_jsr) {
			/* This is the target of a jsr instr.
			 */
			if (info.instr.opcode != opc_astore) {
			    /* XXX Grrr, the bytecode verifier doesn't
			     * test if a jsr starts with astore.  So
			     * it is possible to do something else
			     * before putting the ret address into a
			     * local.  */
			    throw new AssertError("Non standard jsr");
			}
			InstrInfo retInfo 
			    = info.nextReads[info.instr.localSlot];

			if (retInfo != null) {
			    if (retInfo.instr.opcode != opc_ret)
				throw new AssertError
				    ("reading return address");

			    info.retInfo = retInfo;
			    retInfo.jsrTargetInfo = info;

			    /* Now promote reads from the instruction
			     * after the jsr to the ret instruction if
			     * they are modified by the subroutine,
			     * and to the jsr instruction otherwise.  
			     */
			    Instruction nextInstr = predInstr.nextByAddr;
			    InstrInfo nextInfo 
				= (InstrInfo) instrInfos.get(nextInstr);

			    promoteReads(nextInfo, retInfo.instr,
					 retInfo.usedBySub, false);

			    promoteReads(nextInfo, predInstr, 
					 retInfo.usedBySub, true);
			}
		    } else
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

	/* Now merge with the parameters
	 * The params should be the locals in firstInfo.nextReads
	 */
	for (int i=0; i< paramLocals.length; i++) {
	    if (firstInfo.nextReads[i] != null) {
		firstInfo.nextReads[i].local.combineInto(paramLocals[i]);
		paramLocals[i] = paramLocals[i].getReal();
	    }
	}
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
	 */
	for (int i=0; i<paramLocals.length; i++)
	    if (paramLocals[i] != null)
		paramLocals[i].newSlot = i;

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
			info.local.conflictsWith(info.nextReads[i].local);
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
	maxlocals = paramLocals.length;
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

    private InstrInfo CONFLICT = new InstrInfo();

    boolean promoteLifeLocals(LocalInfo[] newLife, InstrInfo nextInfo) {
	if (nextInfo.lifeLocals == null) {
	    nextInfo.lifeLocals = (LocalInfo[]) newLife.clone();
	    return true;
	}
	boolean changed = false;
	for (int i=0; i< maxlocals; i++) {
	    LocalInfo local = nextInfo.lifeLocals[i];
	    if (local == null)
		/* A conflict has already happened, or this slot
		 * may not have been initialized. */
		continue;

	    local = local.getReal();
	    LocalInfo newLocal = newLife[i];
	    if (newLocal != null)
		newLocal = newLocal.getReal();
	    if (local != newLocal) {
		nextInfo.lifeLocals[i] = null;
		changed = true;
	    }
	}
	return changed;
    }

    public void buildNewLVT() {
	/* First we recalculate the usedBySub, to use the new local numbers.
	 */
	for (InstrInfo info = firstInfo; info != null; info = info.nextInfo)
	    if (info.usedBySub != null)
		info.usedBySub = new BitSet();
	for (InstrInfo info = firstInfo; info != null; info = info.nextInfo) {
	    if (info.local != null) {
		for (int i=0; i < info.nextReads.length; i++) {
		    if (info.nextReads[i] != null 
			&& info.nextReads[i].instr.opcode == opc_ret)
			info.nextReads[i].usedBySub.set(info.local.newSlot);
		}
	    }
	}

	/* Now we begin with the first Instruction and follow program flow.
	 * We remember which locals are life in lifeLocals.
	 */

	firstInfo.lifeLocals = new LocalInfo[maxlocals];
	for (int i=0; i < paramLocals.length; i++)
	    firstInfo.lifeLocals[i] = paramLocals[i];

	Stack changedInfo = new Stack();
	changedInfo.push(firstInfo);
	Handler[] handlers = bc.getExceptionHandlers();
	while (!changedInfo.isEmpty()) {
	    InstrInfo info = (InstrInfo) changedInfo.pop();
	    Instruction instr = info.instr;
	    LocalInfo[] newLife = info.lifeLocals;
	    if (instr.localSlot != -1) {
		LocalInfo instrLocal = info.local.getReal();
		newLife = (LocalInfo[]) newLife.clone();
		newLife[instr.localSlot] = instrLocal;
		if (instrLocal.name != null) {
		    for (int j=0; j< newLife.length; j++) {
			if (j != instr.localSlot
			    && newLife[j] != null
			    && instrLocal.name.equals(newLife[j].name)) {
			    /* This local changed the slot. */
		 	   newLife[j] = null;
			}
		    }
		}
	    }
	    
	    if (!instr.alwaysJumps) {
		InstrInfo nextInfo = info.nextInfo;
		if (promoteLifeLocals(newLife, nextInfo))
		    changedInfo.push(nextInfo);
	    } 
	    if (instr.succs != null) {
		for (int i = 0; i < instr.succs.length; i++) {
		    InstrInfo nextInfo
			= (InstrInfo) instrInfos.get(instr.succs[i]);
		    if (promoteLifeLocals(newLife, nextInfo))
			changedInfo.push(nextInfo);
		}
	    }
	    for (int i=0; i < handlers.length; i++) {
		if (handlers[i].start.addr <= instr.addr
		    && handlers[i].end.addr >= instr.addr) {
		    InstrInfo nextInfo
			= (InstrInfo) instrInfos.get(handlers[i].catcher);
		    if (promoteLifeLocals(newLife, nextInfo))
			changedInfo.push(nextInfo);
		}
	    }

	    if (instr.opcode == opc_ret) {
		/* On a ret we do a special merge */

		Instruction jsrTargetInstr = info.jsrTargetInfo.instr;
		for (int j=0; j< jsrTargetInstr.preds.length; j++) {
		    InstrInfo jsrInfo
			= (InstrInfo) instrInfos.get(jsrTargetInstr.preds[j]);

		    LocalInfo[] retLife = (LocalInfo[]) newLife.clone();
		    for (int i=0; i< maxlocals; i++) {
			if (!info.usedBySub.get(i))
			    retLife[i] = jsrInfo.lifeLocals[i];
		    }
		    if (promoteLifeLocals(retLife, jsrInfo.nextInfo))
			changedInfo.push(jsrInfo.nextInfo);
		}
	    }
	}

	Vector lvtEntries = new Vector();
	LocalVariableInfo[] lvi = new LocalVariableInfo[maxlocals];
	LocalInfo[] currentLocal = new LocalInfo[maxlocals];
	for (int i=0; i< paramLocals.length; i++) {
	    if (paramLocals[i] != null) {
		currentLocal[i] = paramLocals[i];
		if (currentLocal[i].name != null) {
		    lvi[i] = new LocalVariableInfo();
		    lvtEntries.addElement(lvi[i]);
		    lvi[i].name = currentLocal[i].name;
		    lvi[i].type = currentLocal[i].type;
		    lvi[i].start = bc.getFirstInstr();
		    lvi[i].slot = i;
		}
	    }
	}
	Instruction lastInstr = null;
	for (InstrInfo info = firstInfo; info != null; info = info.nextInfo) {
	    for (int i=0; i< maxlocals; i++) {
		LocalInfo lcl = info.lifeLocals != null ? info.lifeLocals[i]
		    : null;
		if (lcl != currentLocal[i]
		    && (lcl == null || currentLocal[i] == null
			|| !lcl.name.equals(currentLocal[i].name)
			|| !lcl.type.equals(currentLocal[i].type))) {
		    if (lvi[i] != null) {
			lvi[i].end = info.instr.prevByAddr;
		    }
		    lvi[i] = null;
		    currentLocal[i] = lcl;
		    if (currentLocal[i] != null
			&& currentLocal[i].name != null) {
			lvi[i] = new LocalVariableInfo();
			lvtEntries.addElement(lvi[i]);
			lvi[i].name = currentLocal[i].name;
			lvi[i].type = currentLocal[i].type;
			lvi[i].start = info.instr;
			lvi[i].slot = i;
		    }
		}
	    }
	    lastInstr = info.instr;
	}
	for (int i=0; i< maxlocals; i++) {
	    if (lvi[i] != null)
		lvi[i].end = lastInstr;
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
