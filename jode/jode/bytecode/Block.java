/* Block Copyright (C) 1999 Jochen Hoenicke.
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

import java.io.PrintWriter;
///#def COLLECTIONS java.util
import java.util.Collection;
import java.util.Arrays;
import java.util.List;
import java.util.Iterator;
///#enddef

/**
 * Represents a single basic block. It contains a list of
 * instructions, the successor blocks and the exception handlers for
 * this block.  The last Instruction, and only the last, may be a
 * conditional jump, a tableswitch or a jsr.
 *
 * @author Jochen Hoenicke */
public final class Block {
    /**
     * The opcodes of the instructions in this block.
     */
    private Instruction[] instrs;
    
    /**
     * The blockNr of successor blocks
     */
    private Block[] succs;
    
    /**
     * The catching blocks.  Set by BasicBlocks.
     */
    Handler[] catchers;

    /**
     * The blockNr of this block.  Set by BasicBlocks.
     */
    int blockNr;

    /**
     * Creates a new empty block, with a null successor array.
     */
    public Block() {
	instrs = new Instruction[0];
	succs  = null;
    }
    
    /**
     * Gets the list of instructions.  The returned list should not be
     * modified, except that the instructions (but not their opcodes)
     * may be modified.
     */
    public List getInstructions() {
	return Arrays.asList(instrs);
    }
    
    /**
     * Gets the successor array.  The last successor is the next basic
     * block that is jumped to via goto or the default part of a
     * switch.  For conditional jumps and jsrs the second successor gives
     * the destination.
     */
    public Block[] getSuccs() {
	return succs;
    }
    
    /**
     * Gets the exception handlers which try part contains this block.
     * You can't set them since they are calculated automatically. 
     * @return the exception handlers.
     * @see BasicBlocks#setExceptionHandlers
     */
    public Handler[] getCatchers() {
	return catchers;
    }
    
    /**
     * Gets the block number.  The block numbers are consecutive number
     * from 0 to the number of blocks in a method.  The equation
     * <pre> BasicBlocks.getBlock()[i].getBlockNr() == i </pre>
     * always holds (as long as you don't do something dirty, like adding
     * the same block to different BasicBlocks, or to the same but more
     * than once).
     * @return the block number.
     */
    public int getBlockNr() {
	return blockNr;
    }

    private void checkConsistent() {
	/* Check if all instructions are of correct type */
	int size = instrs.length;
	for (int i = 0; i < size; i++) {
	    int opcode = instrs[i].getOpcode();
	    switch (opcode) {
	    case Opcodes.opc_goto:
		throw new IllegalArgumentException("goto in block");
		
	    case Opcodes.opc_lookupswitch:
		if (succs == null || succs.length == 0)
		    throw new IllegalArgumentException
			("no successors for switch");
		if (i != size - 1)
		    throw new IllegalArgumentException
			("switch in the middle!");
		return;

	    case Opcodes.opc_ret: case Opcodes.opc_athrow:
	    case Opcodes.opc_ireturn: case Opcodes.opc_lreturn: 
	    case Opcodes.opc_freturn: case Opcodes.opc_dreturn: 
	    case Opcodes.opc_areturn: case Opcodes.opc_return:
		if (succs == null || succs.length > 0)
		    throw new IllegalArgumentException
			("throw or return with successor.");
		if (i != size - 1)
		    throw new IllegalArgumentException
			("return in the middle!");
		return;

	    case Opcodes.opc_ifeq: case Opcodes.opc_ifne: 
	    case Opcodes.opc_iflt: case Opcodes.opc_ifge: 
	    case Opcodes.opc_ifgt: case Opcodes.opc_ifle:
	    case Opcodes.opc_if_icmpeq: case Opcodes.opc_if_icmpne:
	    case Opcodes.opc_if_icmplt: case Opcodes.opc_if_icmpge: 
	    case Opcodes.opc_if_icmpgt: case Opcodes.opc_if_icmple:
	    case Opcodes.opc_if_acmpeq: case Opcodes.opc_if_acmpne:
	    case Opcodes.opc_ifnull: case Opcodes.opc_ifnonnull:
	    case Opcodes.opc_jsr:
		if (succs == null || succs.length != 2)
		    throw new IllegalArgumentException
			("successors inappropriate for if/jsr");
		if (i != size - 1)
		    throw new IllegalArgumentException
			("if/jsr in the middle!");
		return;
	    }
	}
	if (succs == null || succs.length != 1)
	    throw new IllegalArgumentException("no single successor block");
    }
    
    /**
     * Set the code, i.e. instructions and successor blocks.
     * The instructions must be valid and match the successors.
     */
    public void setCode(Collection instrs, Block[] succs) {
	this.instrs = (Instruction[]) 
	    instrs.toArray(new Instruction[instrs.size()]);
	this.succs = succs;
	try {
	    checkConsistent();
	} catch (IllegalArgumentException ex) {
	    dumpCode(jode.GlobalOptions.err);
	    throw ex;
	}
    }

    public void dumpCode(PrintWriter output) {
	output.println("    "+this+":");
	for (int i = 0; i < instrs.length; i++) {
	    Instruction instr = instrs[i];
	    if (i == instrs.length - 1 && succs != null) {
		int opcode = instr.getOpcode();
		if (opcode == Opcodes.opc_lookupswitch) {
		    // Special case for switch:
		    output.println("\tswitch");
		    int[] values = instr.getValues();
		    for (int j = 0; j < values.length; j++)
			output.println("\t  case"+values[j]
				       +": goto "+succs[j]);
		    output.println("\t  default: goto"+
				   succs[values.length]);
		    return;
		} else if (succs.length > 1) {
		    output.println("\t"+instr.getDescription()
				   +" "+succs[0]);
		    break;
		}
	    }
	    output.println("\t"+instr.getDescription());

	}
	if (succs != null && succs.length > 0) {
	    if (succs[succs.length-1] == null)
		output.println("\treturn");
	    else
		output.println("\tgoto "+succs[succs.length-1]);
	}
    }

    public String toString() {
	return "Block_"+blockNr;
    }
}
