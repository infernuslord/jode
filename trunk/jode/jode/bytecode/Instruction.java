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
     * The predecessors of this opcode, including the prevByAddr, if
     * that does not alwaysJump.
     */
    public Vector preds = new Vector();
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
	Enumeration enum = preds.elements();
	while (enum.hasMoreElements()) {
	    Instruction pred = (Instruction) enum.nextElement();
	    if (pred.succs != null)
		for (int i=0; i < pred.succs.length; i++)
		    if (pred.succs[i] == this)
			pred.succs[i] = newInstr;
	}
	newInstr.preds = preds;
	preds = new Vector();
	preds.addElement(newInstr);

	return newInstr;
    }

    public Instruction appendInstruction(boolean nextAlwaysJumps) {
	Instruction newInstr = new Instruction(codeinfo);
	newInstr.addr = addr;
	newInstr.nextByAddr = nextByAddr;
	if (nextByAddr != null)
	    nextByAddr.prevByAddr = newInstr;
	newInstr.prevByAddr = this;
	newInstr.alwaysJumps = nextAlwaysJumps;

	if (nextByAddr != null) {
	    if (!this.alwaysJumps)
		nextByAddr.preds.removeElement(this);
	    if (!nextAlwaysJumps)
		nextByAddr.preds.addElement(newInstr);
	}

	nextByAddr = newInstr;
	if (!this.alwaysJumps)
	    newInstr.preds.addElement(this);
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
	if (!alwaysJumps && nextByAddr != null)
	    nextByAddr.preds.removeElement(this);
	if (succs != null) {
	    for (int i=0; i < succs.length; i++)
		succs[i].preds.removeElement(this);
	}

	/* promote the predecessors to nextByAddr */
	Enumeration enum = preds.elements();
	while (enum.hasMoreElements()) {
	    Instruction pred = (Instruction) enum.nextElement();
	    if (pred.succs != null)
		for (int i=0; i < pred.succs.length; i++)
		    if (pred.succs[i] == this)
			pred.succs[i] = nextByAddr;
	    if (nextByAddr != null)
		nextByAddr.preds.addElement(pred);
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
    }

    public String toString() {
	String result =  ""+addr+"_"+Integer.toHexString(hashCode());
	for (Instruction instr = codeinfo.firstInstr;
	     instr != null; instr = instr.nextByAddr) {
	    if (instr == this)
		return result;
	}
	return result+"*";
    }
}

