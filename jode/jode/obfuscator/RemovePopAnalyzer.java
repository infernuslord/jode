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

package jode.obfuscator;
import jode.bytecode.*;
import jode.AssertError;
import jode.GlobalOptions;
import jode.type.MethodType;
import jode.type.Type;

public class RemovePopAnalyzer implements CodeTransformer, Opcodes {
    public RemovePopAnalyzer() {
    }

    class PopInfo {
	int firstPop = 0;
	int[] pops;
	Instruction nextInstr;
    }

    static Instruction shrinkPop(Instruction popInstr, int amount) {
	int newPop = popInstr.opcode - (opc_pop-1) - amount;
	if (newPop < 0)
	    throw new jode.AssertError("pop1 on long or double");
	if (newPop == 0) {
	    Instruction nextInstr = popInstr.nextByAddr;
	    popInstr.removeInstruction();
	    return nextInstr;
	}
	popInstr.opcode = opc_pop - 1 + newPop;
	return popInstr;
    }

    public void transformCode(BytecodeInfo bytecode) {
	int poppush[] = new int[2];
	Instruction instr = bytecode.getFirstInstr(); 
	while (instr != null) {
	    switch (instr.opcode) {
	    case opc_nop: {
		Instruction nextInstr = instr.nextByAddr;
		instr.removeInstruction();
		instr = nextInstr;
		continue;
	    }
	    case opc_pop:
	    case opc_pop2: {
		/* find push instruction */
		int count = 0;
		Instruction pushInstr = instr;
		while (true) {
		    if (pushInstr.preds != null) {
			pushInstr = null;
			break;
		    }
		    pushInstr = pushInstr.prevByAddr;
		    if (pushInstr == null
			|| pushInstr.succs != null 
			|| pushInstr.alwaysJumps) {
			pushInstr = null;
			break;
		    }
		    pushInstr.getStackPopPush(poppush);
		    if (count < poppush[1])
			break;
		    count += poppush[0] - poppush[1];
		}
		int opcode = pushInstr == null ? -1 : pushInstr.opcode;

		if (count > 0) {
		    /* If this is a dup and the instruction popped is the 
		     * duplicated element, remove the dup
		     */
		    if (count <= 2 && opcode == (opc_dup + count - 1)) {
			pushInstr.removeInstruction();
			instr = shrinkPop(instr, 1);
			continue;
		    }
		    
		    if (instr.opcode == opc_pop2
			&& count > 1 && count <= 3 
			&& opcode == (opc_dup2 + count-2)) {
			pushInstr.removeInstruction();
			instr = shrinkPop(instr, 2);
			continue;
		    }
		    /* Otherwise popping is not possible */
		    opcode = -1;
		}
		switch (opcode) {
		case opc_ldc2_w:
		case opc_lload: case opc_dload:
		    pushInstr.removeInstruction();
		    instr = shrinkPop(instr, 2);
		    continue;
		case opc_ldc:
		case opc_iload: case opc_fload: case opc_aload:
		case opc_dup: 
		case opc_new:
		    pushInstr.removeInstruction();
		    instr = shrinkPop(instr, 1);
		    continue;
		case opc_iaload: case opc_faload: case opc_aaload:
		case opc_baload: case opc_caload: case opc_saload:
		    /* We have to pop one entry more. */
		    pushInstr.opcode = opc_pop;
		    instr = pushInstr;
		    continue;

		case opc_dup_x1:
		    pushInstr.opcode = opc_swap;
		    instr = shrinkPop(instr, 1);
		    continue;
		case opc_dup2: 
		    if (instr.opcode == opc_pop2) {
			pushInstr.removeInstruction();
			instr = shrinkPop(instr, 2);
		    }
		    continue;

		case opc_lneg: case opc_dneg:
		case opc_l2d: case opc_d2l:
		case opc_laload: case opc_daload:
		    if (instr.opcode != opc_pop2)
			break;
		    /* fall through */
		case opc_ineg: case opc_fneg: 
		case opc_i2f:  case opc_f2i:
		case opc_i2b: case opc_i2c: case opc_i2s:
		case opc_newarray: case opc_anewarray:
		case opc_arraylength:
		case opc_instanceof:
		    pushInstr.removeInstruction();
		    continue;

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
		    pushInstr.opcode = opc_pop2;
		    shrinkPop(instr, 1);
		    instr = pushInstr;
		    continue;
		case opc_ladd: case opc_dadd:
		case opc_lsub: case opc_dsub:
		case opc_lmul: case opc_dmul:
		case opc_ldiv: case opc_ddiv:
		case opc_lrem: case opc_drem:
		case opc_land: case opc_lor : case opc_lxor:
		    if (instr.opcode != opc_pop2)
			break;
		    pushInstr.opcode = opc_pop2;
		    instr = pushInstr;
		    continue;
		case opc_lshl: case opc_lshr: case opc_lushr:
		    if (instr.opcode != opc_pop2)
			break;
		    pushInstr.opcode = opc_pop;
		    instr = pushInstr;
		    continue;

		case opc_i2l: case opc_i2d:
		case opc_f2l: case opc_f2d:
		    if (instr.opcode != opc_pop2)
			break;
		    pushInstr.removeInstruction();
		    instr.opcode = opc_pop;
		    continue;
		    
		case opc_lcmp:
		case opc_dcmpl: case opc_dcmpg:
		    pushInstr.opcode = opc_pop2;
		    if (instr.opcode == opc_pop)
			instr.opcode = opc_pop2;
		    else {
			Instruction thirdPop = instr.appendInstruction();
			thirdPop.length = 1;
			thirdPop.opcode = opc_pop;
		    }
		    instr = pushInstr;
		    continue;
				    
		case opc_getstatic:
		case opc_getfield: {
		    Reference ref = (Reference) pushInstr.objData;
		    int size = Type.tType(ref.getType()).stackSize();
		    if (pushInstr.opcode == opc_getfield)
			size--;
		    pushInstr.removeInstruction();
		    if (size > 0)
			instr = shrinkPop(instr, size);
		    continue;
		}

		case opc_multianewarray: {
		    int dims = pushInstr.intData;
		    pushInstr.removeInstruction();
		    if (dims == 0)
			instr = shrinkPop(instr, 1);
		    else {
			dims--;
			while (dims > 0) {
			    Instruction aPop = instr.insertInstruction();
			    aPop.length = 1;
			    aPop.opcode = opc_pop;
			    dims--;
			    instr = aPop;
			}
		    }
		    continue;
		}

		case opc_invokevirtual:
		case opc_invokespecial:
		case opc_invokestatic:
		case opc_invokeinterface:
		    if (((MethodType) 
			 Type.tType(((Reference) pushInstr.objData).getType()))
			.getReturnType().stackSize() != 1)
			break;
		    /* fall through */
		case opc_checkcast:
		case -1:
		    if (instr.opcode == opc_pop2) {
			/* This is/may be a double pop on a single value
			 * split it and continue with second half
			 */
			instr.opcode = opc_pop;
			instr = instr.appendInstruction();
			instr.opcode = opc_pop;
			instr.length = 1;
			continue;
		    }
		}
		if (instr.opcode == opc_pop  && instr.preds == null
		    && instr.prevByAddr.opcode == opc_pop) {
		    /* merge two single pops together. */
		    instr.prevByAddr.removeInstruction();
		    instr.opcode = opc_pop2;
		}
		/* Cant do anything with this pop */
	    }
	    /* fall through */
	    default:
		instr = instr.nextByAddr;
		continue;
	    }
	}
    }
}
