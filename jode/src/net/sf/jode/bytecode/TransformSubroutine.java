/* TransformSubroutine Copyright (C) 1999-2000 Jochen Hoenicke.
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

package net.sf.jode.bytecode;
import java.util.BitSet;

/**
 * <p>This class contains some code to transform the subroutines of
 * a method into a normal form.  The normal form is as following.</p>
 *
 * <p>Each subroutine block, meaning a block where some jsr
 * instructions may jump to, must store the return address in a local
 * variable immediately.  There must be exactly one block with the
 * corresponding <code>opc_ret</code> instruction and this block must
 * be reachable from all blocks that belong to this subroutine. </b>
 *
 * <p>The JVM spec allows a subroutine, to leave the return address on
 * stack for a while, even longer than the subroutine actually exists.
 * One can also pop that value instead of storing it in a local
 * variable.  And finally its possible to store it in a variable, but
 * there exists no reachable <code>opc_ret</code> instruction that returns
 * to that address.</p>
 *
 * <p>If the return address is not used by the subroutine, we simply 
 * replace the jsr by a jump and remove the pop/store instruction that
 * popped the return address.</p>
 *
 * <p>If the return address is used, but not immediately stored, we simply
 * move the corresponding astore to the start of the subroutine.</p>
 *
 * @see net.sf.jode.bytecode.Block
 * @see net.sf.jode.bytecode.Instruction 
 */
public class TransformSubroutine implements Opcodes {
    private final static int SUBSTATUS_SUBROUTINE = 1;
    private final static int SUBSTATUS_REMOVEDSUB = 2;
    private final static int SUBSTATUS_NOTSUB     = 3;
    private final static int SUBSTATUS_INPROGRESS = 4;

    BasicBlocks bb;
    Blocks[] blocks;
    byte[] substatus;
    Subroutine[] subInfo;

//      {
//  	for (int i=0; i < blocks.length; i++) {
//  	    Instructions[] instrs = blocks[i].getInstructions();
//  	    if (instrs[instrs.length-1].getOpcode() == opc_jsr) {
//  		int srBlock = instrs.getSuccs()[0].getBlockNr();
//  		if (substatus[srBlock] == 0)
//  		    analyzeSubroutine(srBlock);
//  		if (substatus[srBlock] == SUBSTATUS_REMOVED) {
//  		    Instructions[] newInstrs
//  			= new Instruction[instrs.length-1];
//  		    System.arraycopy(instrs, 0, newInstrs, 0, 
//  				     newInstrs.length);
//  		    Block[] newSuccs = new Block[1] { instrs.getSuccs()[1]; };
//  		    blocks[i].setCode(newInstrs, newSuccs);
//  		}
//  	    }
//  	}
//      }

    class SubroutineInfo {
	int retPosition;
	BitSet accessedLocals;
	SubroutineInfo outer;

	SubroutineInfo(int retPos, Bitset accLocals, SubroutineInfo outer) {
	    this.outer = outer;
	    this.retPosition = retPos;
	    this.accessedLocals = accLocals;
	}

	boolean merge(int retPos, BitSet accLocals, SubroutineInfo outer) {
	    if ((retPos < 0 || this.retPosition < 0)
		&& retPos != this.retPosition)
		throw new
	}
    }

    public TransformSubroutine(BasicBlocks bb)
	throws ClassFormatException 
    {
	if (bb.getStartBlock() == null)
	    return;

	blocks = bb.getBlocks();
	substatus = new byte[blocks.length];
	analyzeBlock(blockNr, SUBSTATUS_NOTSUB, null);
    }

    public void analyzeBlock(int blockNr, int status, SubroutineInfo outer,
			     BitSet retsOnStack) {
	Block block = blocks[blockNr];
	if (status == SUBSTATUS_INPROGRESS) {
	    
	}
	
    }

    public void analyzeBlock(int blockNr, int status, SubroutineInfo outer) {
	substatus[blockNr] = status;
	accessedLocals[blockNr] = accessed;
	Stack todo = new Stack();

	todo.add(new BlockInfo(startBlockNr, 0, null));
	while (!todo.isEmpty()) {
	    BlockInfo info = todo.pop();
	    Block block = blocks[info.blockNr];
	    Instruction[] instrs = block.getInstructions();
	    Instruction[] newInstrs = null;
	    Block[] succs = block.getSuccessors();


	    if (substatus[info.blockNr]
		== SUBSTATUS_INPROGRESS) {
		int retPosition = info.retPosition;
		BitSet  = 
		    retPosition < 0 ? info.accessedLocals.clone() : null;
		
		for (int i = 0; i < instrs.length; i++) {
		    Instruction instr = instrs[i];
		    if (instr instanceof SlotInstruction) {
			if (instr.getOpcode() == opc_astore
			    && retPosition == -1) {
			    
			    /* We found the store operation.  At least
			     * a candidate, since there may not be a
			     * corresponding ret.  
			     */
			    
			    retPosition = instr.getLocalSlot();
			    accessedLocals = null;
			    /* remove store Instruction.
			     */
			    newInstrs = new Instruction[instrs.length - 1];
			    System.arraycopy(instrs, 0, newInstrs, 0, i);
			    System.arraycopy(instrs, i+1, newInstrs, i, 
					     newInstrs.length - i);
			    
			} else {
			    if (retPosition < 0) {
				accessedLocals.add(instr.getLocalSlot());
				switch (instr.getOpcode()) {
				case opc_lload:
				case opc_dload:
				case opc_lstore:
				case opc_dstore:
				    accessedLocals.add(instr.getLocalSlot()+1);
				}
			    }
			}
		    } else if (instr.getOpcode() == opc_pop
			       && retPosition == -1) {
			/* We spontanously left the subroutine by popping.
			 * Remove the pop Instruction.
			 */
			newInstrs = new Instruction[instrs.length - 1];
			System.arraycopy(instrs, 0, newInstrs, 0, i);
			System.arraycopy(instrs, i+1, newInstrs, i, 
					 newInstrs.length - i);
			substatus[info.blockNr] = SUBSTATUS_NOTSUB;
			break;
		    } else if ((instr.getOpcode() == opc_pop2 &&
				(retPosition == -1 || retPosition == -2))) {
			/* We spontanously left the subroutine by popping.
			 * Replace the pop2 with a pop.
			 */
			newInstrs = new Instruction[instrs.length];
			System.arraycopy(instrs, 0, newInstrs, 0, 
					 instrs.length);
			newInstrs[i] = Instruction.forOpcode(opc_pop);
			substatus[info.blockNr] = SUBSTATUS_NOTSUB;
			break;
		    } else if (instr.getOpcode() == opc_jsr) {
			/* A recursive subroutine (or have we already
			 * spontanously left this subroutine?)
			 */
			int srBlock = instrs.getSuccs()[0].getBlockNr();
			if (substatus[srBlock] == 0)
			analyzeSubroutine(srBlock);
			if (substatus[srBlock] == SUBSTATUS_INPROGRESS) {
			    /* We spontanously left this subroutine! */
			    if (retPosition < 0)
				/* This can't happen in valid code.
				 */
				throw new CodeFormatException
				    ("Can't merge return instr on Stack.");
			    substatus[info.blockNr] = SUBSTATUS_NOTSUB;
			}
			leftSub = true;
			break;
		    } else if (substatus[srBlock] == SUBSTATUS_REMOVED) {
			Instructions[] newInstrs
			    = new Instruction[instrs.length-1];
			System.arraycopy(instrs, 0, newInstrs, 0, 
					 newInstrs.length);
			Block[] newSuccs = new Block[1] { instrs.getSuccs()[1]; };
			blocks[i].setCode(newInstrs, newSuccs);
		    }
		}
	    }
	    if (!leftSub) {
		

	    }
	}
    }
}


