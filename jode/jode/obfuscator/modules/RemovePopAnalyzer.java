/* RemovePopAnalyzer Copyright (C) 1999 Jochen Hoenicke.
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

package jode.obfuscator.modules;
import jode.bytecode.*;
import jode.obfuscator.*;
import jode.AssertError;
import jode.GlobalOptions;

import java.util.BitSet;
///#def COLLECTIONS java.util
import java.util.ListIterator;
import java.util.LinkedList;
import java.util.Stack;
///#enddef

public class RemovePopAnalyzer implements CodeTransformer, Opcodes {
    public RemovePopAnalyzer() {
    }

    static class BlockInfo {
	/* A bitset of stack entries at the beginning of the block,
	 * whose values should be never put put onto the stack.  
	 * This array is shared with all other blocks that have
	 * a common predecessor.
	 */
	int[] poppedBefore;

	/* A bitset of instructions, that should be removed, i.e. their
	 * parameters should just get popped.
	 */
	int[] removedInstrs;

	ArrayList predecessors;
	
	BlockInfo(int[] popped, int[] removed) {
	    this.poppedEntries = popped;
	    this.removedInstrs = removed;
	}

	boolean isPopped(int pos) {
	    return (poppedEntries[pos >> 5] & (1 << (pos & 31))) != 0;
	}
	
	boolean isRemoved(int pos) {
	    return (removedInstrs[pos >> 5] & (1 << (pos & 31))) != 0;
	}
    }

    public BlockInfo analyzeBlock(Block block, BlockInfo oldInfo) {
    }

    public BlockInfo[] calcBlockInfos(BasicBlocks bb) {
	Block[] blocks = bb.getBlocks();
	BlockInfo[] infos = new BlockInfo[blocks.length];
	int poppush[] = new int[2];
	int maxStack = bb.getMaxStack();
	int poppedLen = maxStack >> 5;

	for (int i = 0; i < blocks.length; i++) {
	    BitSet popped = new BitSet();
	    Instruction[] instrs = blocks[i].getInstructions();
	    int[] removed = instrs.length >> 5;

	    // Be conservative with stack depth at end of block
	    int depth = maxStack;

	    for (int j = instrs.length; j-- > 0; ) {
		Instruction instr = instrs[j];

		instr.getStackPopPush(poppush);

		// Now check if the parameters of this instr are needed.
		boolean params_needed = false;
		switch (instr.getOpcode()) {
		case opc_invokevirtual:
		case opc_invokespecial:
		case opc_invokestatic:
		case opc_invokeinterface:
		case opc_checkcast:
		    /* These instructions have side effects, parameters
		     * are always needed.  Adjust depth.
		     */
		    params_needed = true;
		    depth += poppush[1];
		    break;

		default:
		    /* Check if all results are needed, adjust depth */
		    for (int k = 0; k < poppush[1]; k++) {
			if (!popped.get(depth++))
			    params_needed = true;
		    }
		}

		if (params_needed) {
		    /* mark params as needed */
		    for (int k = 0; k < poppush[0]; k++)
			popped.clear(--depth);
		} else {
		    removed[j >> 5] |= 1 << (j & 31);
		    /* mark params as popped */
		    for (int k = 0; k < poppush[0]; k++)
			popped.set(--depth);
		}
	    }
		
	    int[] poppedArr = new int[poppedLen];
	    if (blocks[i] != bb.getStartBlock()) {
		/* Only can pop entries before this block if 
		 * this isn't the start block.
		 */
		for (int k = 0; k < block_pop; k++) {
		    if (popped.get(depth+k))
			poppedArr[k >> 5] |= 1 << (k & 31);
		}
	    }
	    infos[i] = new BlockInfo(poppedArr, removed);
	}
	
	/* Now start sharing poppedEntries as necessary. */
	int[] empty = new int[poppedLen];
    next_block:
	for (int i = 1; i < blocks.length; i++) {
	    /* Try to find an earlier block with a same predecessor. */
	    for (int j = 0; j < blocks.length; j++) {
		Block[] succs = blocks[j].getSuccs();
		if (succs.length < 2)
		    continue;

		int blockNr = -1;
		boolean isAPred = false;
		for (int k = 0; k < succs.length; k++) {
		    if (succs[k] == blocks[i])
			isAPred = true;
		    if (succs[k] != null && succs[k].getBlockNr() < i)
			blockNr = succs[k].getBlockNr();
		    if (isAPred && blockNr >= 0) {
			int[] common = infos[blockNr].poppedEntries;
			int[] my = infos[i].poppedEntries;
			for (int k = 0; k < poppedLen; k++)
			    common[k] &= my[k];
			infos[i].poppedEntries = common;
			continue next_block;
		    }		    
		}
	    }
	}

	/* Now propagate poppedEntries through blocks */
	boolean changed = true;
	while (changed) {
	    changed = false;
	next_block:
	    for (int i = 0; i < blocks.length; i++) {
		Block[] succs = blocks[i].getSuccs();
		int[] succPops = null;
		for (int j = 0; j < succs.length; j++) {
		    if (succs[j] != null) {
			succPops = infos[succs[j].getBlockNr()].poppedEntries;
		    }
		}
		if (succPops == null)
		    continue;
		blocks[i].getStackPopPush(poppush);
		int[] myPops = infos[i].poppedPush;
		for (int k = poppush[1]; k < maxStack; k++) {
		    
		if (succs.length == 0 || succs[0] == null)
		    continue;
		int[] succsPoppedEntries
		    = infos[succs[0].getBlockNr()].poppedEntries;
		for (int j = 0; j < succs.length; j++) {
		    if (succs[j] == null)
			continue next_block;
		    int[] thisPopped
			= infos[succs[j].getBlockNr()].poppedEntries;

		    for (int k = 0; k < poppedLen; k++) {
			succsPoppedEntries &= 


	    for (int j = instrs.length; j-- > 0; ) {
		Instruction instr = instrs[j];

		instr.getStackPopPush(poppush);

		// Now check if the parameters of this instr are needed.
		boolean params_needed = false;
		switch (instr.getOpcode()) {
		case opc_invokevirtual:
		case opc_invokespecial:
		case opc_invokestatic:
		case opc_invokeinterface:
		case opc_checkcast:
		    /* These instructions have side effects, parameters
		     * are always needed.  Adjust depth.
		     */
		    params_needed = true;
		    depth += poppush[1];
		    break;

		case opc_pop:
		case opc_pop2:
		    break;

		default:
		    /* Check if all results are needed, adjust depth */
		    for (int k = 0; k < poppush[1]; k++) {
			if (!popped.get(depth++))
			    params_needed = true;
		    }
		    /* If opcode has no result it has side effects. */
		    if (poppush[1] == 0)
			params_needed = true;
		}
		if (block_pop < maxStack - depth) 
		    block_pop = maxStack - depth;

		if (params_needed) {
		    /* mark params as needed */
		    for (int k = 0; k < poppush[0]; k++)
			popped.clear(--depth);
		} else {
		    removed[j >> 5] |= 1 << (j & 31);
		    /* mark params as popped */
		    for (int k = 0; k < poppush[0]; k++)
			popped.set(--depth);
		}
	    }

		    if (blocks[j] == null)
			;
		}
	    }
	}
	return infos;
    }

    public void transformCode(BasicBlocks bb) {
	if (bb.getStartBlock() == null)
	    return;

	BlockInfo[] infos = calcBlockInfos(bb);

	int poppush[] = new int[2];
	boolean poppedEntries[] = new boolean[bb.getMaxStack()];
	Block[] blocks = bb.getBlocks();
	for (int i = 0; i < blocks.length; i++) {
	    LinkedList newInstructions = new LinkedList();
	    Instruction[] oldInstrs = blocks[i].getInstructions();
	    int instrNr = oldInstrs.length;
	    int stackDepth = 0;
	    while (instrNr > 0) {
		Instruction instr = oldInstrs[instrNr];
		if (instr.getOpcode() == opc_nop)
		    continue;
		if (instr.getOpcode() == opc_pop) {
		    poppedEntries[stackDepth++] = true;
		    continue;
		}
		if (instr.getOpcode() == opc_pop2) {
		    poppedEntries[stackDepth++] = true;
		    poppedEntries[stackDepth++] = true;
		    continue;
		}

		instr.getStackPopPush(poppush);
		// First look if stackDepth was right
		if (stackDepth < poppush[1]) {
		    int diff = poppush[1] - stackDepth;
		    System.arraycopy(poppedEntries, 0,
				     poppedEntries, diff, stackDepth);
		    for (int j=0; j< diff; i++)
			poppedEntries[j] = false;
		}
		// Now check if this instr pushes a popped Entry.
		boolean push_a_popped = false;
		boolean push_all_popped = true;
		for (int j=0; j < poppush[1]; j++) {
		    if (poppedEntries[j])
			push_a_popped = true;
		    else
			push_all_popped = false;
		}
		if (push_a_popped) {
		    /* We push an entry, that gets popped later */
		    int opcode = instr.getOpcode();
		    switch (opcode) {
		    case opc_dup:
		    case opc_dup_x1:
		    case opc_dup_x2:
		    case opc_dup2: 
		    case opc_dup2_x1:
		    case opc_dup2_x2: {

			int count = (opcode - opc_dup)/3+1;
			int depth = (opcode - opc_dup)%3;
			stackDepth -= count;
			int bottom = stackDepth - count - depth;

			int popped1 = 0;
			int popped3 = 0;
			int newDepth = 0;

			// get the popped mask and adjust poppedEntries.
			for (int j=0; j< count; j++) {
			    if (poppedEntries[bottom + j])
				popped1 |= 1<<j;
			    if (poppedEntries[bottom + count + depth + j])
				popped3 |= 1<<j;
			}
			for (int j=0; j< depth; j++) {
			    if (poppedEntries[bottom + count + j])
				newDepth++;
			    poppedEntries[bottom + j]
				= poppedEntries[bottom + count + j];
			}
			for (int j=0; j< count; j++) {
			    poppedEntries[bottom + depth + j]
				= (popped1 & popped3 & (1 << j)) != 0;
			}
			// adjust the depth
			depth = newDepth;

			int all = (count == 2) ? 3 : 1;
			if (popped1 == all)
			    // dup was not necessary
			    break;
			if (depth == 0 && popped3 == all)
			    // dup was not necessary
			    break;

			// adjust the count
			if ((popped1 & popped3 & 1) != 0) {
			    count--;
			    popped1 >>= 1;
			    popped3 >>= 2;
			} else if ((popped1 & popped3 & 2) != 0) {
			    count--;
			    popped1 &= 1;
			    popped3 &= 1;
			}

			if (popped1 == 1) {
			    // count == 2, popped1 = 1, 
			    depth++;
			    count--;
			    popped1 = 0;
			    popped3 >>= 1;
			} 
			if (count == 2 && popped1 == 0 && popped3 > 0) {
			    // Do the normal dup2 and pop the right
			    // element afterwards.
			    if (popped3 == 3) {
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_pop2));
			    } else {
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_pop));
				if (popped3 == 1)
				    newInstructions.addFirst
					(Instruction.forOpcode(opc_swap));
			    }
			    popped3 = 0;
			}
			if (popped1 == popped3) {
			    // popped1 == popped3 == 0
			    // Do a dupcount_xdepth now.
			    if (depth < 3) {
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_pop - 3 + 
						     depth + 3 * count));
				break;
			    } else {
				// I hope that this will almost never happen.
				// depth = 3, count = 1;
				// Note that instructions are backwards.
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_pop2));    //DABCD
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_dup2_x2)); //DABCDAB
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_pop));     //DCDAB
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_dup_x2));  //DCDABD
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_pop));     //DCABD
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_dup2_x2)); //DCABDC
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_swap));    //ABDC
				break;
			    }
			}
			
			if (count == 1) {
			    // Possible states:
			    // popped1 = 0; popped3 = 1;
			    // depth = 1  or depth = 2
			    if (depth == 1) {
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_swap));
			    } else {
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_pop));
				newInstructions.addFirst(instr);
			    }
			    break;
			}

			// count = 2; popped1 = 2
			// Possible states:
			// dpth/pop3         0       1
			//   0   AB         AAB     AB
			//   1  ABC        BABC    BAC
			//   2 ABCD       CABCD   CABD
			if (popped3 == 0) {
			    if (depth == 0) {
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_swap));
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_dup_x1));
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_swap));
			    } else if (depth == 1) {
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_swap));    //BABC
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_dup_x2));  //BACB
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_swap));    //ACB
			    } else {
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_swap));    //CABCD
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_pop2));    //CABDC
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_dup2_x2)); //CABDCAB
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_pop));     //CDCAB
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_dup_x2));  //CDCABC
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_pop));     //CDABC
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_dup2_x2)); //CDABCD
			    }
			} else {
			    if (depth == 0) {
			    } else if (depth == 1) {
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_pop));     //BAC
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_dup_x2));  //BACB
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_swap));    //ACB
			    } else {
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_pop2));    //CABD
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_dup2_x1)); //CABDAB
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_pop2));    //CDAB
				newInstructions.addFirst
				    (Instruction.forOpcode(opc_dup2_x2)); //CDABCD
			    }
			}
			break;
		    }
		    case opc_swap: {
			// swap the popped status
			boolean tmp = poppedEntries[stackDepth - 1];
			poppedEntries[stackDepth - 1]
			    = poppedEntries[stackDepth - 2];
			poppedEntries[stackDepth - 2] = tmp;
		    }


		    // Now the simple instructions, that can be removed.
		    // delta = -2;
		    case opc_ldc2_w:
		    case opc_lload: case opc_dload:
			if (!push_all_popped)
			    throw new AssertError("pop half of a long");
			poppedEntries[--stackDepth] = false;
			poppedEntries[--stackDepth] = false;
			continue;

		    case opc_i2l: case opc_i2d:
		    case opc_f2l: case opc_f2d:
		    case opc_ldc:
		    case opc_iload: case opc_fload: case opc_aload:
		    case opc_new:
		    case opc_lneg: case opc_dneg:
		    case opc_l2d: case opc_d2l:
		    case opc_laload: case opc_daload:
		    case opc_ineg: case opc_fneg: 
		    case opc_i2f:  case opc_f2i:
		    case opc_i2b: case opc_i2c: case opc_i2s:
		    case opc_newarray: case opc_anewarray:
		    case opc_arraylength:
		    case opc_instanceof:
		    case opc_lshl: case opc_lshr: case opc_lushr:
		    case opc_iaload: case opc_faload: case opc_aaload:
		    case opc_baload: case opc_caload: case opc_saload:
		    case opc_iadd: case opc_fadd:
		    case opc_isub: case opc_fsub:
		    case opc_imul: case opc_fmul:
		    case opc_idiv: case opc_fdiv:
		    case opc_irem: case opc_frem:
		    case opc_iand: case opc_ior : case opc_ixor: 
		    case opc_ishl: case opc_ishr: case opc_iushr:
		    case opc_fcmpl: case opc_fcmpg:
		    case opc_l2i: case opc_l2f:
		    case opc_d2i: case opc_d2f:
		    case opc_ladd: case opc_dadd:
		    case opc_lsub: case opc_dsub:
		    case opc_lmul: case opc_dmul:
		    case opc_ldiv: case opc_ddiv:
		    case opc_lrem: case opc_drem:
		    case opc_land: case opc_lor : case opc_lxor:
		    case opc_lcmp:
		    case opc_dcmpl: case opc_dcmpg:
		    case opc_getstatic:
		    case opc_getfield:
		    case opc_multianewarray:

			if (!push_all_popped)
			    throw new AssertError("pop half of a long");
			if (poppush[0] < poppush[1]) {
			    for (int j=0; j < poppush[0] - poppush[1]; j++)
				poppedEntries[stackDepth++] = true;
			} else if (poppush[0] < poppush[1]) {
			    for (int j=0; j < poppush[0] - poppush[1]; j++)
				poppedEntries[--stackDepth] = false;
			}

		    case opc_invokevirtual:
		    case opc_invokespecial:
		    case opc_invokestatic:
		    case opc_invokeinterface:
		    case opc_checkcast:
			if (!push_all_popped)
			    throw new AssertError("pop half of a long");
			if (poppush[1] == 1) {
			    poppedEntries[--stackDepth] = false;
			    newInstructions
				.addFirst(Instruction.forOpcode(opc_pop));
			} else {
			    poppedEntries[--stackDepth] = false;
			    poppedEntries[--stackDepth] = false;
			    newInstructions
				.addFirst(Instruction.forOpcode(opc_pop2));
			}
			newInstructions.addFirst(instr);
		    default:
			throw new AssertError("Unexpected opcode!");
		    }
		} else {
		    // Add the instruction ..
		    newInstructions.addFirst(instr);
		    // .. and adjust stackDepth.
		    stackDepth += poppush[0] - poppush[1];
		}
	    }
	    for (int j=0; j < stackDepth; j++) {
		// XXXX
	    }
	    blocks[i].setCode((Instruction[]) newInstructions
			      .toArray(oldInstrs), blocks[i].getSuccs());
	}
    }
}
