package jode.obfuscator;
import java.util.*;
import jode.bytecode.*;

public class LocalOptimizer {

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
	 * Tell if the localsToRead variable has changed.
	 */
	boolean changed;
	/**
	 * The LocalInfo of the next Instruction, that may read a local, 
	 * without prior writing.
	 */
	LocalInfo[] nextReads;
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
    }

    InstrInfo firstInfo;
    Hashtable instrInfos;
    BytecodeInfo bc;

    public LocalOptimizer(BytecodeInfo bc) {
	this.bc = bc;
    }

    public void promoteReads(InstrInfo info, Instruction preInstr) {
	InstrInfo preInfo = (InstrInfo) instrInfos.get(preInstr);
	int omitLocal = -1;
	if (preInstr.localSlot != -1
	    && preInstr.opcode >= BytecodeInfo.opc_istore
	    && preInstr.opcode < BytecodeInfo.opc_iastore) {
	    /* This is a store */
	    omitLocal = preInstr.localSlot;
	    if (info.nextReads[preInstr.localSlot] != null)
		preInfo.local.combineInto
		    (info.nextReads[preInstr.localSlot]);
	}
	int maxlocals = bc.getMaxLocals();
	for (int i=0; i< maxlocals; i++) {
	    if (info.nextReads[i] != null && i != omitLocal) {
		if (preInfo.nextReads[i] == null) {
		    preInfo.nextReads[i] = info.nextReads[i];
		    preInfo.changed = true;
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
	instrInfos = new Hashtable();
	{
	    InstrInfo info = firstInfo = new InstrInfo();
	    Instruction instr = bc.getFirstInstr();
	    while (true) {
		instrInfos.put(instr, info);
		info.instr = instr;
		info.nextReads = new LocalInfo[maxlocals];
		if (instr.localSlot != -1) {
		    info.local = new LocalInfo(info);
		    if (instr.opcode < BytecodeInfo.opc_istore
			|| instr.opcode > BytecodeInfo.opc_iastore) {
			/* this is a load instruction */
			info.nextReads[instr.localSlot] = info.local;
			info.changed = true;
		    }
		}
		if ((instr = instr.nextByAddr) == null)
		    break;
		info = info.nextInfo = new InstrInfo();
	    }
	}

	/* find out which locals are the same.
	 */
	boolean changed = true;
	while (changed) {
	    changed = false;
	    for (InstrInfo info = firstInfo;
		 info != null; info = info.nextInfo) {
		if (info.changed) {
		    info.changed = false;
		    Enumeration enum = info.instr.preds.elements();
		    while (enum.hasMoreElements()) {
			changed = true;
			Instruction preInstr
			    = (Instruction) enum.nextElement();
			promoteReads(info, preInstr);
		    }
		    for (int i=0; i<handlers.length; i++) {
			if (handlers[i].catcher == info.instr) {
			    for (Instruction preInstr = handlers[i].start;
				 preInstr != handlers[i].end; 
				 preInstr = preInstr.nextByAddr) {
				promoteReads(info, preInstr);
			    }
			}
		    }
		}
	    }
	}

	/* Now calculate the conflict settings.
	 */
	for (InstrInfo info = firstInfo; info != null; info = info.nextInfo) {
	    if (info.instr.localSlot != -1
		&& info.instr.opcode >= BytecodeInfo.opc_istore
		&& info.instr.opcode < BytecodeInfo.opc_iastore) {
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
    }

    public void distributeLocals(Vector locals) {
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
		if (((LocalInfo)conflenum.nextElement()).newSlot == slot)
		    continue next_slot;
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
