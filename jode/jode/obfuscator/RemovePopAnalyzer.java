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
import jode.MethodType;
import jode.Type;

public class RemovePopAnalyzer implements CodeAnalyzer, Opcodes {
    MethodIdentifier m;
    BytecodeInfo bytecode;

    public RemovePopAnalyzer(BytecodeInfo bytecode, MethodIdentifier m) {
	this.m = m;
	this.bytecode = bytecode;
    }

    /**
     * Reads the opcodes out of the code info and determine its 
     * references
     * @return an enumeration of the references.
     */
    public void analyzeCode() {
    }

    class PopInfo {
	int firstPop = 0;
	int[] pops;
	Instruction nextInstr;
    }

    Instruction findMatchingPush(Instruction instr) {
	int count = 0;
	while (true) {
	    if (instr.preds != null)
		return null;
	    instr = instr.prevByAddr;
	    switch (instr.opcode) {
	    case opc_ldc2_w:
	    case opc_lload: case opc_dload:
		if (count < 2)
		    return count == 0 ? instr : null;
		count -= 2;
		break;
	    case opc_ldc: 
	    case opc_iload: case opc_fload: case opc_aload:
	    case opc_new:
		if (count == 0)
		    return instr;
		count --;
		break;

	    case opc_iaload: case opc_faload: case opc_aaload:
	    case opc_baload: case opc_caload: case opc_saload:
		if (count == 0)
		    return instr;
		count++;
		break;

	    case opc_dup: case opc_dup_x1: case opc_dup_x2: {
		/* XXX This is a very special case, if we pop a value
		 * that is dupped we can omit the dup;  it doesn't matter
		 * if we pop the dupped value or the original value. 
		 */
		int depth = (instr.opcode - opc_dup);
		if (count < 2 + depth)
		    return (count == 0 || count == depth+1) ? instr : null;
		count --;
		break;
	    }
	    case opc_dup2: case opc_dup2_x1: case opc_dup2_x2: {
		int depth = (instr.opcode - opc_dup2);
		if (count < 4 + depth)
		    return count == 0 ? instr : null;
		count -= 2;
		break;
	    }
	    case opc_swap:
	    case opc_lneg: case opc_dneg:
	    case opc_l2d: case opc_d2l:
	    case opc_laload: case opc_daload:
		if (count < 2)
		    return count == 0 ? instr : null;
		break;
	    case opc_ineg: case opc_fneg: 
	    case opc_i2f:  case opc_f2i:
	    case opc_i2b: case opc_i2c: case opc_i2s:
	    case opc_newarray: case opc_anewarray:
	    case opc_arraylength:
	    case opc_checkcast:
	    case opc_instanceof:
		if (count == 0)
		    return instr;
		break;
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
		if (count == 0)
		    return instr;
		count++;
		break;
	    case opc_ladd: case opc_dadd:
	    case opc_lsub: case opc_dsub:
	    case opc_lmul: case opc_dmul:
	    case opc_ldiv: case opc_ddiv:
	    case opc_lrem: case opc_drem:
	    case opc_land: case opc_lor : case opc_lxor:
		if (count < 2)
		    return count == 0 ? instr : null;
		count += 2;
		break;
	    case opc_lshl: case opc_lshr: case opc_lushr:
		if (count < 2)
		    return count == 0 ? instr : null;
		count++;
		break;
	    case opc_i2l: case opc_i2d:
	    case opc_f2l: case opc_f2d:
		if (count < 2)
		    return count == 0 ? instr : null;
		count--;
		break;
		    
	    case opc_lcmp:
	    case opc_dcmpl: case opc_dcmpg:
		if (count == 0)
		    return instr;
		count += 3;
		break;

	    case opc_invokevirtual:
	    case opc_invokespecial:
	    case opc_invokestatic:
	    case opc_invokeinterface: {
		Reference ref = (Reference) instr.objData;
		MethodType mt = (MethodType) Type.tType(ref.getType());
		if (count < mt.getReturnType().stackSize())
		    return (count == 0) ? instr : null;
		count -= mt.getReturnType().stackSize();
		if (instr.opcode != opc_invokestatic) 
		    count++;
		for (int i = mt.getParameterTypes().length-1; i >= 0; i--)
		    count += mt.getParameterTypes()[i].stackSize();
		break;
	    }

	    case opc_getstatic:
	    case opc_getfield: {
		Reference ref = (Reference) instr.objData;
		int size = Type.tType(ref.getType()).stackSize();
		if (count < size)
		    return count == 0 ? instr : null;
		count -= size;
		if (instr.opcode == opc_getfield)
		    count++;
		break;
	    }

	    case opc_multianewarray: {
		if (count == 0)
		    return instr;
		int dims = instr.prevByAddr.intData;
		count += dims - 1;
		break;
	    }

	    case opc_nop:
	    case opc_iinc:
		break;
	    case opc_putfield:
		count++;
		/* fall through */
	    case opc_putstatic:
		count += instr.objData instanceof Long 
		    || instr.objData instanceof Double ? 2 : 1;
		break;
	    case opc_monitorenter:
	    case opc_monitorexit:
	    case opc_istore:
	    case opc_fstore: case opc_astore:
	    case opc_pop:
		count++;
		break;

	    case opc_lstore: case opc_dstore:
	    case opc_pop2:
		count += 2;
		break;
		
	    case opc_iastore:
	    case opc_fastore: case opc_aastore:
	    case opc_bastore: case opc_castore: case opc_sastore:
		count += 3;
		break;
	    case opc_lastore: case opc_dastore:
		count += 4;
		break;
	    default:
		return null;
	    }
	}
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

    public BytecodeInfo stripCode() {
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
		Instruction prevInstr = findMatchingPush(instr);
		int opcode = prevInstr == null ? -1 : prevInstr.opcode;
		switch (opcode) {
		case opc_ldc2_w:
		case opc_lload: case opc_dload:
		    prevInstr.removeInstruction();
		    instr = shrinkPop(instr, 2);
		    continue;
		case opc_ldc:
		case opc_iload: case opc_fload: case opc_aload:
		case opc_dup: 
		case opc_new:
		    prevInstr.removeInstruction();
		    instr = shrinkPop(instr, 1);
		    continue;
		case opc_iaload: case opc_faload: case opc_aaload:
		case opc_baload: case opc_caload: case opc_saload:
		    /* We have to pop one entry more. */
		    prevInstr.opcode = opc_pop;
		    instr = prevInstr;
		    continue;

		case opc_dup_x1:
		    prevInstr.opcode = opc_swap;
		    instr = shrinkPop(instr, 1);
		    continue;
		case opc_dup2: 
		    if (instr.opcode == opc_pop2) {
			prevInstr.removeInstruction();
		    } else
			prevInstr.opcode = opc_dup;
		    instr = instr.nextByAddr;
		    instr.prevByAddr.removeInstruction();
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
		    prevInstr.removeInstruction();
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
		    prevInstr.opcode = opc_pop2;
		    shrinkPop(instr, 1);
		    instr = prevInstr;
		    continue;
		case opc_ladd: case opc_dadd:
		case opc_lsub: case opc_dsub:
		case opc_lmul: case opc_dmul:
		case opc_ldiv: case opc_ddiv:
		case opc_lrem: case opc_drem:
		case opc_land: case opc_lor : case opc_lxor:
		    if (instr.opcode != opc_pop2)
			break;
		    prevInstr.opcode = opc_pop2;
		    instr = prevInstr;
		    continue;
		case opc_lshl: case opc_lshr: case opc_lushr:
		    if (instr.opcode != opc_pop2)
			break;
		    prevInstr.opcode = opc_pop;
		    instr = prevInstr;
		    continue;

		case opc_i2l: case opc_i2d:
		case opc_f2l: case opc_f2d:
		    if (instr.opcode != opc_pop2)
			break;
		    prevInstr.removeInstruction();
		    instr.opcode = opc_pop;
		    continue;
		    
		case opc_lcmp:
		case opc_dcmpl: case opc_dcmpg:
		    prevInstr.opcode = opc_pop2;
		    if (instr.opcode == opc_pop)
			instr.opcode = opc_pop2;
		    else {
			Instruction thirdPop = instr.appendInstruction();
			thirdPop.length = 1;
			thirdPop.opcode = opc_pop;
		    }
		    instr = prevInstr;
		    continue;
				    
		case opc_getstatic:
		case opc_getfield: {
		    Reference ref = (Reference) prevInstr.objData;
		    int count = Type.tType(ref.getType()).stackSize();
		    if (prevInstr.opcode == opc_getfield)
			count--;
		    prevInstr.removeInstruction();
		    if (count > 0)
			instr = shrinkPop(instr, count);
		    continue;
		}

		case opc_multianewarray: {
		    int dims = prevInstr.intData;
		    prevInstr.removeInstruction();
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
			 Type.tType(((Reference) prevInstr.objData).getType()))
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
	return bytecode;
    }
}
