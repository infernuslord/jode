/* SlotInstruction Copyright (C) 1999 Jochen Hoenicke.
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

/**
 * This class represents an instruction in the byte code.
 *
 */
public class SlotInstruction extends Instruction {
    private LocalVariableInfo lvi;

    /**
     */
    public SlotInstruction(int opcode, LocalVariableInfo lvi, int lineNr) {
	super(opcode, lineNr);
	if (opcode != opc_iinc && opcode != opc_ret
	    && (opcode < opc_iload || opcode > opc_aload)
	    && (opcode < opc_istore || opcode > opc_astore))
	    throw new IllegalArgumentException("Instruction has no slot");
	this.lvi = lvi;
    }

    /**
     */
    public SlotInstruction(int opcode, int slot, int lineNr) {
	this(opcode, LocalVariableInfo.getInfo(slot), lineNr);
    }

    /**
     */
    public SlotInstruction(int opcode, LocalVariableInfo lvi) {
	this(opcode, lvi, -1);
    }

    /**
     */
    public SlotInstruction(int opcode, int slot) {
	this(opcode, LocalVariableInfo.getInfo(slot), -1);
    }

    public boolean isStore() {
	int opcode = getOpcode();
	return opcode >= opc_istore && opcode <= opc_astore;
    }

    public boolean hasLocal() {
	return true;
    }
	    
    public final int getLocalSlot()
    {
	return lvi.getSlot();
    }

    public final LocalVariableInfo getLocalInfo()
    {
	return lvi;
    }

    public final void setLocalInfo(LocalVariableInfo info) 
    {
	this.lvi = info;
    }

    public final void setLocalSlot(int slot) 
    {
	if (lvi.getName() == null)
	    this.lvi = LocalVariableInfo.getInfo(slot);
	else
	    this.lvi = LocalVariableInfo.getInfo(slot, 
						 lvi.getName(), lvi.getType());
    }

    public String toString() {
	return super.toString()+' '+lvi;
    }
}
