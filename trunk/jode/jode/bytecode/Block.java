/* Block Copyright (C) 2000 Jochen Hoenicke.
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
 * <p>Represents a single basic block.  It contains a list of
 * instructions and the successor blocks.</p>
 *
 * <p>All jump instructions must be at the end of the block.  These
 * jump instructions are <code>opc_lookupswitch</code>,
 * <code>opc_if</code>xxx, <code>opc_jsr</code>, <code>opc_ret</code>,
 * <code>opc_</code>x<code>return</code> and <code>opc_return</code>.
 * An <code>opc_goto</code> is implicit if the basic block doesn't end
 * with a jump instructions, or if it ends with an conditional jump or
 * jsr.</p>
 *
 * <p>The jump instructions don't remember their destinations, instead
 * the Block does it.  This are the successor block.  There are
 * several cases:</p>
 *
 * <ul> 
 * <li>Block ends with <code>opc_lookupswitch</code> with
 * <code>n</code> values.  Then there must be <code>n+1</code>
 * successors where the first <code>n</code> successors correspond to
 * the values and the last successor is the default successor.</li>
 * <li>Block ends with <code>opc_if</code>xxx, then there must be two
 * successors: The first one is the successor if the condition evaluates
 * to true, the second one is for the false branch. </li>
 * <li>Block ends with <code>opc_jsr</code>, then there must be two
 * successors: The first one is the subroutine, the second is the next
 * block after the subroutine. </li>
 * <li>Block ends with <code>opc_</code>x</code>return</code> or
 * <code>opc_ret</code>, then there must no successor at all. </li>
 * <li>In any other case there must be exactly one successor.</li>
 * </ul>
 *
 * <p>If any successor is <code>null</code> it represents end of
 * method, i.e. a return instruction.  You can also use
 * <code>null</code> successors for conditional jumps and switch
 * instruction. You normally shouldn't use <code>opc_return</code>
 * instructions.  They are only necessary, if you want to return with
 * a non-empty stack. </p>
 * 
 * @author Jochen Hoenicke
 * @see jode.bytecode.BasicBlocks
 * @see jode.bytecode.Instruction
 */
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
     * The blockNr of this block.  Set by BasicBlocks.
     */
    int lineNr;

    /**
     * The number of items this block takes from the stack.
     */
    int pop;
    /**
     * The number of items this block puts on the stack.
     */
    int push;


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
    public Instruction[] getInstructions() {
	return instrs;
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

    private void initCode() {
	int size = instrs.length;
	int depth = 0;
	int poppush[] = new int[2];
	boolean needGoto = true;
	for (int i = 0; i < size; i++) {
	    instrs[i].getStackPopPush(poppush);
	    depth += poppush[0];
	    if (pop < depth)
		pop = depth;
	    depth -= poppush[1];

	    int opcode = instrs[i].getOpcode();
	    switch (opcode) {
	    case Opcodes.opc_goto:
		throw new IllegalArgumentException("goto in block");
		
	    case Opcodes.opc_lookupswitch:
		if (succs.length != instrs[i].getValues().length + 1)
		    throw new IllegalArgumentException
			("no successors for switch");
		if (i != size - 1)
		    throw new IllegalArgumentException
			("switch in the middle!");
		needGoto = false;
		break;

	    case Opcodes.opc_ret: case Opcodes.opc_athrow:
	    case Opcodes.opc_ireturn: case Opcodes.opc_lreturn: 
	    case Opcodes.opc_freturn: case Opcodes.opc_dreturn: 
	    case Opcodes.opc_areturn: case Opcodes.opc_return:
		if (succs.length != 0)
		    throw new IllegalArgumentException
			("throw or return with successor.");
		if (i != size - 1)
		    throw new IllegalArgumentException
			("return in the middle!");
		needGoto = false;
		break;

	    case Opcodes.opc_ifeq: case Opcodes.opc_ifne: 
	    case Opcodes.opc_iflt: case Opcodes.opc_ifge: 
	    case Opcodes.opc_ifgt: case Opcodes.opc_ifle:
	    case Opcodes.opc_if_icmpeq: case Opcodes.opc_if_icmpne:
	    case Opcodes.opc_if_icmplt: case Opcodes.opc_if_icmpge: 
	    case Opcodes.opc_if_icmpgt: case Opcodes.opc_if_icmple:
	    case Opcodes.opc_if_acmpeq: case Opcodes.opc_if_acmpne:
	    case Opcodes.opc_ifnull: case Opcodes.opc_ifnonnull:
	    case Opcodes.opc_jsr:
		if (succs.length != 2)
		    throw new IllegalArgumentException
			("successors inappropriate for if/jsr");
		if (succs[0] == null && opcode == Opcodes.opc_jsr)
		    throw new IllegalArgumentException
			("null successors inappropriate for jsr");
		if (i != size - 1)
		    throw new IllegalArgumentException
			("if/jsr in the middle!");
		needGoto = false;
	    }
	}
	push = pop - depth;
	if (needGoto && succs.length != 1)
	    throw new IllegalArgumentException("no single successor block");
    }

    public void getStackPopPush (int[] poppush) {
	poppush[0] = pop;
	poppush[1] = push;
	return;
    }
    
    /**
     * Set the code, i.e. instructions and successor blocks.
     * The instructions must be valid and match the successors.
     */
    public void setCode(Instruction[] instrs, Block[] succs) {
	this.instrs = instrs;
	this.succs = succs;
	try {
	    initCode();
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
