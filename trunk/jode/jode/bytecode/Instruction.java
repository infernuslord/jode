/* Instruction Copyright (C) 1999 Jochen Hoenicke.
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

package jode.bytecode;
import java.util.Vector;
import java.util.Enumeration;

/**
 * This class represents an instruction in the byte code.
 *
 * For simplicity currently most fields are public.  You shouldn't change
 * many of them, though.
 */
public class Instruction implements Opcodes{
    public BytecodeInfo codeinfo;
    /**
     * The opcode of the instruction.  We map some opcodes, e.g.
     * <pre>
     * iload_[0-3] -> iload, ldc_w -> ldc, wide iinc -> iinc.
     * </pre>
     */
    public int opcode;
    /**
     * If this opcode uses a local this gives the slot.  This info is
     * used when swapping locals.  
     */
    public int localSlot = -1;
    /**
     * Optional object data for this opcode.  This is mostly used for
     * method/field/class references,  but also for a value array
     * in a lookupswitch.
     */
    public Object objData;
    /**
     * Optional integer data for this opcode.  There are various uses
     * for this.
     */
    public int intData;
    /**
     * The address of this opcode.
     */
    public int addr;
    /**
     * The length of this opcode.  You shouldn't touch it, nor rely on
     * it, since the length of some opcodes may change automagically
     * (e.g. when changing localSlot  iload_0 <-> iload 5)
     */
    public int length;
    /**
     * If this is true, the instruction will never flow into the nextByAddr.
     */
    public boolean alwaysJumps = false;
    /**
     * The successors of this opcodes, where flow may lead to
     * (except that nextByAddr is implicit if !alwaysJump).  The
     * value null is equivalent to an empty array.
     */
    public Instruction[] succs;
    /**
     * The predecessors of this opcode, orthogonal to the succs array.
     * This must be null or a non empty array.
     */
    public Instruction[] preds;
    /**
     * The next instruction in code order.
     */
    public Instruction nextByAddr;
    /**
     * The previous instruction in code order, useful when changing
     * the order.
     */
    public Instruction prevByAddr;

    /**
     * You can use this field to add some info to each instruction.
     * After using, you must set it to null again.
     */
    public Object tmpInfo;

    public Instruction(BytecodeInfo ci) {
	this.codeinfo = ci;
    }

    public void addPredecessor(Instruction pred) {
	if (preds == null) {
	    preds = new Instruction[] { pred };
	    return;
	}
	int predsLength = preds.length;
	Instruction[] newPreds = new Instruction[predsLength+1];
	System.arraycopy(preds, 0, newPreds, 0, predsLength);
	newPreds[predsLength] = pred;
	preds = newPreds;
    }

    public void removePredecessor(Instruction pred) {
	/* Hopefully it doesn't matter if this is slow */
	int predLength = preds.length;
	if (predLength == 1) {
	    if (preds[0] != pred)
		throw new jode.AssertError
		    ("removing not existing predecessor");
	    preds = null;
	} else {
	    Instruction[] newPreds = new Instruction[predLength-1];
	    int j;
	    for (j = 0; preds[j] != pred; j++)
		newPreds[j] = preds[j];
	    System.arraycopy(preds, j+1, newPreds, j, predLength - j - 1);
	    preds = newPreds;
	}
    }

    public Instruction insertInstruction() {
	Instruction newInstr = new Instruction(codeinfo);
	newInstr.addr = addr;

	newInstr.prevByAddr = prevByAddr;
	if (prevByAddr != null)
	    prevByAddr.nextByAddr = newInstr;
	else
	    codeinfo.firstInstr = newInstr;
	newInstr.nextByAddr = this;
	prevByAddr = newInstr;

	/* promote the predecessors to newInstr */
	if (preds != null) {
	    for (int j=0; j < preds.length; j++)
		for (int i=0; i < preds[j].succs.length; i++)
		    if (preds[j].succs[i] == this)
			preds[j].succs[i] = newInstr;
	    newInstr.preds = preds;
	    preds = null;
	}
	return newInstr;
    }

    public Instruction appendInstruction() {
	Instruction newInstr = new Instruction(codeinfo);
	newInstr.addr = addr;
	newInstr.nextByAddr = nextByAddr;
	if (nextByAddr != null)
	    nextByAddr.prevByAddr = newInstr;
	newInstr.prevByAddr = this;

	nextByAddr = newInstr;
	return newInstr;
    }

    /**
     * Removes this instruction (as if it would be replaced by a nop).
     */
    public void removeInstruction() {
	/* remove from chained list */
	if (prevByAddr != null)
	    prevByAddr.nextByAddr = nextByAddr;
	else
	    codeinfo.firstInstr = nextByAddr;

	if (nextByAddr != null)
	    nextByAddr.prevByAddr = prevByAddr;

	/* remove predecessors of successors */
	if (succs != null) {
	    for (int i=0; i < succs.length; i++)
		succs[i].removePredecessor(this);
	    succs = null;
	}

	Instruction alternative = nextByAddr != null ? nextByAddr : prevByAddr;
	/* remove the predecessors to alternative */
	if (preds != null) {
	    for (int j=0; j < preds.length; j++)
		for (int i=0; i < preds[j].succs.length; i++)
		    if (preds[j].succs[i] == this)
			preds[j].succs[i] = alternative;
	    if (alternative.preds == null)
		alternative.preds = preds;
	    else {
		Instruction[] newPreds
		    = new Instruction[alternative.preds.length + preds.length];
		System.arraycopy(preds, 0, newPreds, 0, preds.length);
		System.arraycopy(alternative.preds, 0, newPreds, preds.length, 
				 alternative.preds.length);
		alternative.preds = newPreds;
	    }
	    preds = null;
	}

	/* adjust exception handlers */
	Handler[] handlers = codeinfo.getExceptionHandlers();
	for (int i=0; i< handlers.length; i++) {
	    if (handlers[i].start == this)
		handlers[i].start = nextByAddr;
	    if (handlers[i].end == this)
		handlers[i].end = prevByAddr;
	    if (handlers[i].catcher == this)
		handlers[i].catcher = nextByAddr;

	    if (handlers[i].start == null
		|| handlers[i].end == null
		|| handlers[i].end.nextByAddr == handlers[i].start) {
		/* Remove the handler.
		 * This is very seldom, so we can make it slow */
		Handler[] newHandlers = new Handler[handlers.length - 1];
		System.arraycopy(handlers, 0, newHandlers, 0, i);
		System.arraycopy(handlers, i+1, newHandlers, i, 
				 handlers.length - (i+1));
		handlers = newHandlers;
		codeinfo.setExceptionHandlers(newHandlers);
		i--;
	    }
	}

	/* adjust local variable table and line number table */
	LocalVariableInfo[] lvt = codeinfo.getLocalVariableTable();
	if (lvt != null) {
	    for (int i=0; i< lvt.length; i++) {
		if (lvt[i].start == this)
		    lvt[i].start = nextByAddr;
		if (lvt[i].end == this)
		    lvt[i].end = prevByAddr;
		if (lvt[i].start == null
		    || lvt[i].end == null
		    || lvt[i].end.nextByAddr == lvt[i].start) {
		    /* Remove the local variable info.
		     * This is very seldom, so we can make it slow */
		    LocalVariableInfo[] newLVT = 
			new LocalVariableInfo[lvt.length - 1];
		    System.arraycopy(lvt, 0, newLVT, 0, i);
		    System.arraycopy(lvt, i+1, newLVT, i, 
				     newLVT.length - i);
		    lvt = newLVT;
		    codeinfo.setLocalVariableTable(newLVT);
		    i--;
		}
	    }
	}
	LineNumber[] lnt = codeinfo.getLineNumberTable();
	if (lnt != null) {
	    for (int i=0; i< lnt.length; i++) {
		if (lnt[i].start == this)
		    lnt[i].start = nextByAddr;
		if (lnt[i].start == null
		    || (i+1 < lnt.length && lnt[i].start == lnt[i+1].start)) {
		    /* Remove the line number.
		     * This is very seldom, so we can make it slow */
		    LineNumber[] newLNT = 
			new LineNumber[lnt.length - 1];
		    System.arraycopy(lnt, 0, newLNT, 0, i);
		    System.arraycopy(lnt, i+1, newLNT, i, 
				     newLNT.length - i);
		    lnt = newLNT;
		    codeinfo.setLineNumberTable(newLNT);
		    i--;
		}
	    }
	}
    }

    public String getDescription() {
	StringBuffer result = new StringBuffer(String.valueOf(addr))
	    .append('_').append(Integer.toHexString(hashCode()))
	    .append(": ").append(opcodeString[opcode]);
	switch (opcode) {
	case opc_iload: case opc_lload: 
	case opc_fload: case opc_dload: case opc_aload:
	case opc_istore: case opc_lstore: 
	case opc_fstore: case opc_dstore: case opc_astore:
	case opc_ret:
	    result.append(" ").append(localSlot);
	    break;
	case opc_iinc:
	    result.append(" ").append(localSlot).append(" ").append(intData);
	    break;
	case opc_ldc: case opc_ldc2_w:    
	case opc_getstatic: case opc_getfield:
	case opc_putstatic: case opc_putfield:
	case opc_invokespecial:	case opc_invokestatic: case opc_invokevirtual:
	case opc_new: 
	case opc_checkcast: 
	case opc_instanceof:
	    result.append(" ").append(objData);
	    break;
	case opc_anewarray: 
	case opc_newarray:
	    result.append(" ").append(((String)objData).substring(1));
	    break;
	case opc_multianewarray:
	case opc_invokeinterface:
	    result.append(" ").append(objData).append(" ").append(intData);
	    break;
	case opc_ifeq: case opc_ifne: 
	case opc_iflt: case opc_ifge: 
	case opc_ifgt: case opc_ifle:
	case opc_if_icmpeq: case opc_if_icmpne:
	case opc_if_icmplt: case opc_if_icmpge: 
	case opc_if_icmpgt: case opc_if_icmple: 
	case opc_if_acmpeq: case opc_if_acmpne:
	case opc_ifnull: case opc_ifnonnull:
	case opc_goto:
	case opc_jsr:
	    result.append(" ").append(succs[0].addr);
	    break;
	}
	return result.toString();
    }

    public String toString() {
	return ""+addr+"_"+Integer.toHexString(hashCode());
    }
}

