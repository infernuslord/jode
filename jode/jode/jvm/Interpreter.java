/* Interpreter Copyright (C) 1999 Jochen Hoenicke.
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

package jode.jvm;
import jode.AssertError;
import jode.GlobalOptions;
import jode.type.*;
import jode.bytecode.*;
import jode.decompiler.ClassAnalyzer;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;

/**
 * This class is a java virtual machine written in java :-).  Well not
 * exactly.  It does only handle a subset of the opcodes and is mainly
 * written do deobfuscate Strings.
 *
 * @author Jochen Hoenicke
 */
public class Interpreter implements Opcodes {

    private final static int CMP_EQ = 0;
    private final static int CMP_NE = 1;
    private final static int CMP_LT = 2;
    private final static int CMP_GE = 3;
    private final static int CMP_GT = 4;
    private final static int CMP_LE = 5;
    private final static int CMP_GREATER_MASK
	= (1 << CMP_GT)|(1 << CMP_GE)|(1 << CMP_NE);
    private final static int CMP_LESS_MASK
	= (1 << CMP_LT)|(1 << CMP_LE)|(1 << CMP_NE);
    private final static int CMP_EQUAL_MASK
	= (1 << CMP_GE)|(1 << CMP_LE)|(1 << CMP_EQ);

    public static Object interpretMethod
	(RuntimeEnvironment env, BytecodeInfo code, Value[] locals)
	throws InterpreterException, InvocationTargetException {
	if ((GlobalOptions.debuggingFlags 
	     & GlobalOptions.DEBUG_INTERPRT) != 0)
	    GlobalOptions.err.println("Interpreting "+code.getMethodInfo());
	Value[] stack = new Value[code.getMaxStack()];
	for (int i=0; i< stack.length; i++)
	    stack[i] = new Value();

	Instruction pc = code.getFirstInstr();
	int stacktop = 0;
	big_loop:
	for(;;) {
	    try {
		Instruction instr = pc;
		if ((GlobalOptions.debuggingFlags 
		     & GlobalOptions.DEBUG_INTERPRT) != 0) {
		    GlobalOptions.err.println(instr.getDescription());
		    GlobalOptions.err.print("stack: [");
		    for (int i=0; i<stacktop; i++) {
			if (i>0)
			    GlobalOptions.err.print(",");
			GlobalOptions.err.print(stack[i]);
			if (stack[i].objectValue() instanceof char[]) {
			    GlobalOptions.err.print(new String((char[])stack[i].objectValue()));
			}
		    }
		    GlobalOptions.err.println("]");
		    GlobalOptions.err.print("local: [");
		    for (int i=0; i<locals.length; i++)
			GlobalOptions.err.print(locals[i]+",");
		    GlobalOptions.err.println("]");
		}
		pc = instr.nextByAddr;
		int opcode = instr.opcode;
		switch (opcode) {
		case opc_nop:
		    break;
		case opc_ldc:
		    stack[stacktop++].setObject(instr.objData);
		    break;
		case opc_ldc2_w:
		    stack[stacktop].setObject(instr.objData);
		    stacktop += 2;
		    break;
		case opc_iload:	case opc_fload: case opc_aload:
		    stack[stacktop++].setValue(locals[instr.localSlot]);
		    break;
		case opc_lload: case opc_dload: 
		    stack[stacktop].setValue(locals[instr.localSlot]);
		    stacktop += 2;
		    break;
		case opc_iaload: case opc_laload: 
		case opc_faload: case opc_daload: case opc_aaload:
		case opc_baload: case opc_caload: case opc_saload: {
		    int index = stack[--stacktop].intValue();
		    Object array = stack[--stacktop].objectValue();
		    Object result;
		    try {
			switch(opcode) {
			case opc_baload:
			    result = new Integer
				(array instanceof byte[]
				 ? ((byte[])array)[index]
				 : ((boolean[])array)[index] ? 1 : 0);
			    break;
			case opc_caload: 
			    result = new Integer(((char[])array)[index]);
			    break;
			case opc_saload:
			    result = new Integer(((short[])array)[index]);
			    break;
			default:
			    result = Array.get(array, index);
			    break;
			}
		    } catch (NullPointerException ex) {
			throw new InvocationTargetException(ex);
		    } catch (ArrayIndexOutOfBoundsException ex) {
			throw new InvocationTargetException(ex);
		    }
		    stack[stacktop++].setObject(result);
		    if (opcode == opc_laload || opcode == opc_daload)
			stacktop++;
		    break;
		}
		case opc_istore: case opc_fstore: case opc_astore:
		    locals[instr.localSlot].setValue(stack[--stacktop]);
		    break;
		case opc_lstore: case opc_dstore: 
		    locals[instr.localSlot].setValue(stack[stacktop -= 2]);
		    break;

		case opc_lastore: case opc_dastore: 
		    stacktop--;
		    /* fall through */
		case opc_iastore: case opc_fastore: case opc_aastore:
		case opc_bastore: case opc_castore: case opc_sastore: {
		    Value value = stack[--stacktop];
		    int index = stack[--stacktop].intValue();
		    Object array = stack[--stacktop].objectValue();
		    try {
			switch(opcode) {
			case opc_bastore: 
			    if (array instanceof byte[]) 
				((byte[])array)[index] 
				    = (byte) value.intValue();
			    else
				((boolean[])array)[index]
				    = value.intValue() != 0;
			    break;
			case opc_castore: 
			    ((char[])array)[index] = (char) value.intValue();
			    break;
			case opc_sastore:
			    ((short[])array)[index] = (short) value.intValue();
			    break;
			default:
			    Array.set(array, index, value.objectValue());
			}
		    } catch (NullPointerException ex) {
			throw new InvocationTargetException(ex);
		    } catch (ArrayIndexOutOfBoundsException ex) {
			throw new InvocationTargetException(ex);
		    } catch (ArrayStoreException ex) {
			throw new InvocationTargetException(ex);
		    }
		    break;
		}
		case opc_pop: case opc_pop2:
		    stacktop -= opcode - (opc_pop-1);
		    break;
		case opc_dup: case opc_dup_x1: case opc_dup_x2: {
		    int depth = opcode - opc_dup;
		    for (int i=0; i < depth+1; i++)
			stack[stacktop-i].setValue(stack[stacktop-i-1]);
		    stack[stacktop-depth-1].setValue(stack[stacktop]);
		    stacktop++;
		    break;
		}
		case opc_dup2: case opc_dup2_x1: case opc_dup2_x2: {
		    int depth = opcode - opc_dup2;
		    for (int i=0; i < depth+2; i++)
			stack[stacktop+1-i].setValue(stack[stacktop-1-i]);
		    stack[stacktop-depth-1].setValue(stack[stacktop+1]);
		    stack[stacktop-depth-2].setValue(stack[stacktop]);
		    stacktop += 2;
		    break;
		}
		case opc_swap: {
		    Value tmp = stack[stacktop-1];
		    stack[stacktop-1] = stack[stacktop-2];
		    stack[stacktop-2] = tmp;
		    break;
		}
		case opc_iadd: 
		    stack[stacktop-2].setInt(stack[stacktop-2].intValue()
					     + stack[stacktop-1].intValue());
		    stacktop--;
		    break;
		case opc_isub: 
		    stack[stacktop-2].setInt(stack[stacktop-2].intValue()
					     - stack[stacktop-1].intValue());
		    stacktop--;
		    break;
		case opc_imul: 
		    stack[stacktop-2].setInt(stack[stacktop-2].intValue()
					     * stack[stacktop-1].intValue());
		    stacktop--;
		    break;
		case opc_idiv: 
		    try {
			stack[stacktop-2].setInt
			    (stack[stacktop-2].intValue()
			     / stack[stacktop-1].intValue());
		    } catch (ArithmeticException ex) {
			throw new InvocationTargetException(ex);
		    }
		    stacktop--;
		    break;
		case opc_irem: 
		    try {
			stack[stacktop-2].setInt
			    (stack[stacktop-2].intValue()
			     % stack[stacktop-1].intValue());
		    } catch (ArithmeticException ex) {
			throw new InvocationTargetException(ex);
		    }
		    stacktop--;
		    break;

		case opc_ladd: 
		    stacktop-=2;
		    stack[stacktop-2].setLong(stack[stacktop-2].longValue()
					      + stack[stacktop].longValue());
		    break;
		case opc_lsub: 
		    stacktop-=2;
		    stack[stacktop-2].setLong(stack[stacktop-2].longValue()
					      - stack[stacktop].longValue());
		    break;
		case opc_lmul: 
		    stacktop-=2;
		    stack[stacktop-2].setLong(stack[stacktop-2].longValue()
					      * stack[stacktop].longValue());
		    break;
		case opc_ldiv: 
		    stacktop-=2;
		    try {
			stack[stacktop-2].setLong
			    (stack[stacktop-2].longValue()
			     / stack[stacktop].longValue());
		    } catch (ArithmeticException ex) {
			throw new InvocationTargetException(ex);
		    }
		    break;
		case opc_lrem: 
		    stacktop-=2;
		    try {
			stack[stacktop-2].setLong
			    (stack[stacktop-2].longValue()
			     % stack[stacktop].longValue());
		    } catch (ArithmeticException ex) {
			throw new InvocationTargetException(ex);
		    }
		    break;

		case opc_fadd: 
		    stack[stacktop-2].setFloat(stack[stacktop-2].floatValue()
					       + stack[stacktop-1].floatValue());
		    stacktop--;
		    break;
		case opc_fsub: 
		    stack[stacktop-2].setFloat(stack[stacktop-2].floatValue()
					       - stack[stacktop-1].floatValue());
		    stacktop--;
		    break;
		case opc_fmul: 
		    stack[stacktop-2].setFloat(stack[stacktop-2].floatValue()
					       * stack[stacktop-1].floatValue());
		    stacktop--;
		    break;
		case opc_fdiv: 
		    stack[stacktop-2].setFloat(stack[stacktop-2].floatValue()
					       / stack[stacktop-1].floatValue());
		    stacktop--;
		    break;
		case opc_frem: 
		    stack[stacktop-2].setFloat(stack[stacktop-2].floatValue()
					       % stack[stacktop-1].floatValue());
		    stacktop--;
		    break;

		case opc_dadd: 
		    stacktop-=2;
		    stack[stacktop-2].setDouble(stack[stacktop-2].doubleValue()
						+ stack[stacktop].doubleValue());
		    break;
		case opc_dsub: 
		    stacktop-=2;
		    stack[stacktop-2].setDouble(stack[stacktop-2].doubleValue()
						- stack[stacktop].doubleValue());
		    break;
		case opc_dmul: 
		    stacktop-=2;
		    stack[stacktop-2].setDouble(stack[stacktop-2].doubleValue()
						* stack[stacktop].doubleValue());
		    break;
		case opc_ddiv: 
		    stacktop-=2;
		    stack[stacktop-2].setDouble(stack[stacktop-2].doubleValue()
						/ stack[stacktop].doubleValue());
		    break;
		case opc_drem: 
		    stacktop-=2;
		    stack[stacktop-2].setDouble(stack[stacktop-2].doubleValue()
						% stack[stacktop].doubleValue());
		    break;

		case opc_ineg: 
		    stack[stacktop-1].setInt(-stack[stacktop-1].intValue());
		    break;
		case opc_lneg: 
		    stack[stacktop-2].setLong(-stack[stacktop-2].longValue());
		    break;
		case opc_fneg: 
		    stack[stacktop-1].setFloat(-stack[stacktop-1].floatValue());
		    break;
		case opc_dneg: 
		    stack[stacktop-2].setDouble(-stack[stacktop-2].doubleValue());
		    break;

		case opc_ishl:
		    stack[stacktop-2].setInt(stack[stacktop-2].intValue()
					     << stack[stacktop-1].intValue());
		    stacktop--;
		    break;
		case opc_ishr:
		    stack[stacktop-2].setInt(stack[stacktop-2].intValue()
					     >> stack[stacktop-1].intValue());
		    stacktop--;
		    break;
		case opc_iushr:
		    stack[stacktop-2].setInt(stack[stacktop-2].intValue()
					     >>> stack[stacktop-1].intValue());
		    stacktop--;
		    break;
		case opc_iand:
		    stack[stacktop-2].setInt(stack[stacktop-2].intValue()
					     & stack[stacktop-1].intValue());
		    stacktop--;
		    break;
		case opc_ior :
		    stack[stacktop-2].setInt(stack[stacktop-2].intValue()
					     | stack[stacktop-1].intValue());
		    stacktop--;
		    break;
		case opc_ixor:
		    stack[stacktop-2].setInt(stack[stacktop-2].intValue()
					     ^ stack[stacktop-1].intValue());
		    stacktop--;
		    break;

		case opc_lshl:
		    stack[stacktop-3].setLong(stack[stacktop-3].longValue()
					      << stack[stacktop-1].intValue());
		    stacktop--;
		    break;
		case opc_lshr:
		    stack[stacktop-3].setLong(stack[stacktop-3].longValue()
					      >> stack[stacktop-1].intValue());
		    stacktop--;
		    break;
		case opc_lushr:
		    stack[stacktop-3].setLong(stack[stacktop-3].longValue()
					      >>> stack[stacktop-1].intValue());
		    stacktop--;
		    break;
		case opc_land:
		    stacktop-=2;
		    stack[stacktop-2].setLong(stack[stacktop-2].longValue()
					      & stack[stacktop].longValue());
		    break;
		case opc_lor :
		    stacktop-=2;
		    stack[stacktop-2].setLong(stack[stacktop-2].longValue()
					      | stack[stacktop].longValue());
		    break;
		case opc_lxor:
		    stacktop-=2;
		    stack[stacktop-2].setLong(stack[stacktop-2].longValue()
					      ^ stack[stacktop].longValue());
		    break;

		case opc_iinc:
		    locals[instr.localSlot].setInt
			(locals[instr.localSlot].intValue() + instr.intData);
		    break;
		case opc_i2l:
		    stack[stacktop-1]
			.setLong((long)stack[stacktop-1].intValue());
		    stacktop++;
		    break;
		case opc_i2f: 
		    stack[stacktop-1]
			.setFloat((float)stack[stacktop-1].intValue());
		    break;
		case opc_i2d:
		    stack[stacktop-1]
			.setDouble((double)stack[stacktop-1].intValue());
		    stacktop++;
		    break;

		case opc_l2i:
		    stacktop--;
		    stack[stacktop-1]
			.setInt((int)stack[stacktop-1].longValue());
		    break;
		case opc_l2f: 
		    stacktop--;
		    stack[stacktop-1]
			.setFloat((float)stack[stacktop-1].longValue());
		    break;
		case opc_l2d:
		    stack[stacktop-2]
			.setDouble((double)stack[stacktop-2].longValue());
		    break;

		case opc_f2i: 
		    stack[stacktop-1]
			.setInt((int)stack[stacktop-1].floatValue());
		    break;
		case opc_f2l:
		    stack[stacktop-1]
			.setLong((long)stack[stacktop-1].floatValue());
		    stacktop++;
		    break;
		case opc_f2d:
		    stack[stacktop-1]
			.setDouble((double)stack[stacktop-1].floatValue());
		    stacktop++;
		    break;

		case opc_d2i:
		    stacktop--;
		    stack[stacktop-1]
			.setInt((int)stack[stacktop-1].doubleValue());
		    break;
		case opc_d2l:
		    stack[stacktop-2]
			.setLong((long)stack[stacktop-2].doubleValue());
		    break;
		case opc_d2f: 
		    stacktop--;
		    stack[stacktop-1]
			.setFloat((float)stack[stacktop-1].doubleValue());
		    break;

		case opc_i2b: 
		    stack[stacktop-1]
			.setInt((byte)stack[stacktop-1].intValue());
		    break;		
		case opc_i2c: 
		    stack[stacktop-1]
			.setInt((char)stack[stacktop-1].intValue());
		    break;
		case opc_i2s:
		    stack[stacktop-1]
			.setInt((short)stack[stacktop-1].intValue());
		    break;
		case opc_lcmp: {
		    stacktop -= 3;
		    long val1 = stack[stacktop-1].longValue();
		    long val2 = stack[stacktop+1].longValue();
		    stack[stacktop-1].setInt
			(val1 == val2 ? 0 : val1 < val2 ? -1 : 1);
		    break;
		}
		case opc_fcmpl: case opc_fcmpg: {
		    float val1 = stack[stacktop-2].floatValue();
		    float val2 = stack[--stacktop].floatValue();
		    stack[stacktop-1].setInt
			(val1 == val2 ? 0
			 : ( opcode == opc_fcmpg
			     ? (val1 < val2 ? -1 :  1)
			     : (val1 > val2 ?  1 : -1)));
		    break;
		}
		case opc_dcmpl: case opc_dcmpg: {
		    stacktop -= 3;
		    double val1 = stack[stacktop-1].doubleValue();
		    double val2 = stack[stacktop+1].doubleValue();
		    stack[stacktop-1].setInt
			(val1 == val2 ? 0
			 : ( opcode == opc_dcmpg
			     ? (val1 < val2 ? -1 :  1)
			     : (val1 > val2 ?  1 : -1)));
		    break;
		}
		case opc_ifeq: case opc_ifne: 
		case opc_iflt: case opc_ifge: 
		case opc_ifgt: case opc_ifle:
		case opc_if_icmpeq: case opc_if_icmpne:
		case opc_if_icmplt: case opc_if_icmpge: 
		case opc_if_icmpgt: case opc_if_icmple: 
		case opc_if_acmpeq: case opc_if_acmpne:
		case opc_ifnull: case opc_ifnonnull: {
		    int value;
		    if (opcode >= opc_if_acmpeq) {
			Object objValue = stack[--stacktop].objectValue();
			if (opcode >= opc_ifnull) {
			    value = objValue == null ? 0 : 1;
			    opcode -= opc_ifnull;
			} else {
			    value = objValue
				== stack[--stacktop].objectValue() ? 0 : 1;
			    opcode -= opc_if_acmpeq;
			}
		    } else {
			value = stack[--stacktop].intValue();
			if (opcode >= opc_if_icmpeq) {
			    int val1 = stack[--stacktop].intValue();
			    value = (val1 == value ? 0
				     : val1 < value ? -1 : 1);
			    opcode -= opc_if_icmpeq;
			} else
			    opcode -= opc_ifeq;
		    }
		    int opc_mask = 1 << opcode;
		    if (value > 0 && (opc_mask & CMP_GREATER_MASK) != 0
			|| value < 0 && (opc_mask & CMP_LESS_MASK) != 0
			|| value == 0 && (opc_mask & CMP_EQUAL_MASK) != 0)
			pc = instr.succs[0];
		    break;
		}
		case opc_jsr:
		case opc_jsr_w:
		    stack[stacktop++].setObject(instr);
		    /* fall through */
		case opc_goto:
		case opc_goto_w:
		    pc = instr.succs[0];
		    break;
		case opc_ret:
		    pc = (Instruction)locals[instr.localSlot].objectValue();
		    break;
		case opc_tableswitch: {
		    int value = stack[--stacktop].intValue();
		    int low  = instr.intData;
		    if (value >= low && value <= low + instr.succs.length - 2)
			pc = instr.succs[value - low];
		    else
			pc = instr.succs[instr.succs.length-1];
		    break;
		}
		case opc_lookupswitch: {
		    int value = stack[--stacktop].intValue();
		    int[] values = (int[]) instr.objData;
		    pc = instr.succs[values.length];
		    for (int i=0; i< values.length; i++) {
			if (values[i] == value) {
			    pc = instr.succs[i];
			    break;
			}
		    }
		    break;
		}
		case opc_ireturn: case opc_freturn: case opc_areturn:
		    return stack[--stacktop].objectValue();
		case opc_lreturn: case opc_dreturn:
		    return stack[stacktop -= 2].objectValue();
		case opc_return:
		    return Void.TYPE;
		case opc_getstatic: {
		    Reference ref = (Reference) instr.objData;
		    stack[stacktop].setObject
			(env.getField((Reference) instr.objData, null));
		    stacktop += Type.tType(ref.getType()).stackSize();
		    break;
		}
		case opc_getfield: {
		    Reference ref = (Reference) instr.objData;
		    Object cls = stack[--stacktop];
		    if (cls == null)
			throw new InvocationTargetException
			    (new NullPointerException());
		    stack[stacktop].setObject
			(env.getField((Reference) instr.objData, cls));
		    stacktop += Type.tType(ref.getType()).stackSize();
		    break;
		}
		case opc_putstatic: {
		    Reference ref = (Reference) instr.objData;
		    stacktop -= Type.tType(ref.getType()).stackSize();
		    Object value = stack[stacktop];
		    env.putField((Reference) instr.objData, null, value);
		    break;
		}
		case opc_putfield: {
		    Reference ref = (Reference) instr.objData;
		    stacktop -= Type.tType(ref.getType()).stackSize();
		    Object value = stack[stacktop];
		    Object cls = stack[--stacktop];
		    if (cls == null)
			throw new InvocationTargetException
			    (new NullPointerException());
		    env.putField((Reference) instr.objData, cls, value);
		    break;
		}
		case opc_invokevirtual:
		case opc_invokespecial:
		case opc_invokestatic :
		case opc_invokeinterface: {
		    Reference ref = (Reference) instr.objData;
		    MethodType mt = (MethodType) Type.tType(ref.getType());
		    Object[] args = new Object[mt.getParameterTypes().length];
		    for (int i=args.length - 1; i >= 0; i--) {
			stacktop -= mt.getParameterTypes()[i].stackSize();
			args[i] = stack[stacktop].objectValue();
		    }
		
		    Object result = null;
		    if (ref.getName().equals("<init>")) {
			NewObject newObj = stack[--stacktop].getNewObject();
//  			if (!newObj.getType().equals(ref.getClazz()))
//  			    throw new InterpreterException
//  				("constructor called on wrong type");
			newObj.setObject(env.invokeConstructor(ref, args));
		    } else if (opcode == opc_invokestatic) {
			result = env.invokeMethod(ref, false, null, args);
		    } else {
			Object cls = stack[--stacktop].objectValue();
			if (cls == null)
			    throw new InvocationTargetException
				(new NullPointerException());
			result = env.invokeMethod
			    (ref, opcode != opc_invokespecial, cls, args);
		    }
		    if (mt.getReturnType() != Type.tVoid) {
			stack[stacktop].setObject(result);
			stacktop += mt.getReturnType().stackSize();
		    }
		    break;
		}
		case opc_new: {
		    String clazz = (String) instr.objData;
		    stack[stacktop++].setNewObject(new NewObject(clazz));
		    break;
		}
		case opc_newarray: {
		    int length = stack[--stacktop].intValue();
		    try {
			switch (instr.intData) {
			case  4: 
			    stack[stacktop++].setObject(new boolean[length]); 
			    break;
			case  5:
			    stack[stacktop++].setObject(new char[length]); 
			    break;
			case  6:
			    stack[stacktop++].setObject(new float[length]); 
			    break;
			case  7:
			    stack[stacktop++].setObject(new double[length]); 
			    break;
			case  8:
			    stack[stacktop++].setObject(new byte[length]); 
			    break;
			case  9:
			    stack[stacktop++].setObject(new short[length]); 
			    break;
			case 10:
			    stack[stacktop++].setObject(new int[length]); 
			    break;
			case 11:
			    stack[stacktop++].setObject(new long[length]); 
			    break;
			default:
			    throw new AssertError("Invalid newarray operand");
			}
		    } catch (NegativeArraySizeException ex) {
			throw new InvocationTargetException(ex);
		    }
		    break;
		}
		case opc_anewarray: {
		    int length = stack[--stacktop].intValue();
		    try {
			stack[stacktop++].setObject
			    (env.newArray((String) instr.objData, 
					  new int[] { length }));
		    } catch (NegativeArraySizeException ex) {
			throw new InvocationTargetException(ex);
		    }
		    break;
		}
		case opc_arraylength: {
		    Object array = stack[--stacktop].objectValue();
		    stack[stacktop++].setInt(Array.getLength(array));
		    break;
		}
		case opc_athrow: {
		    Throwable exc = 
			(Throwable) stack[--stacktop].objectValue();
		    throw new InvocationTargetException
			(exc == null ? new NullPointerException() : exc);
		}
		case opc_checkcast: {
		    Object obj = stack[stacktop-1].objectValue();
		    if (obj != null
			&& !env.instanceOf(obj, (String) instr.objData))
			throw new InvocationTargetException
			    (new ClassCastException(obj.getClass().getName()));
		    break;
		}
		case opc_instanceof: {
		    Object obj = stack[--stacktop].objectValue();
		    stack[stacktop++].setInt
			(env.instanceOf(obj, (String) instr.objData) ? 1 : 0);
		    break;
		}
		case opc_monitorenter:
		    env.enterMonitor(stack[--stacktop].objectValue());
		    break;
		case opc_monitorexit:
		    env.exitMonitor(stack[--stacktop].objectValue());
		    break;
		case opc_multianewarray: {
		    int dimension = instr.intData;
		    int[] dims = new int[dimension];
		    for (int i=dimension - 1; i >= 0; i--)
			dims[i] = stack[--stacktop].intValue();
		    try {
			stack[stacktop++].setObject
			    (env.newArray((String) instr.objData, dims));
		    } catch (NegativeArraySizeException ex) {
			throw new InvocationTargetException(ex);
		    }
		    break;
		}
		default:
		    throw new AssertError("Invalid opcode "+opcode);
		}
	    } catch (InvocationTargetException ex) {
		Handler[] handlers = code.getExceptionHandlers();
		Throwable obj = ex.getTargetException();
		for (int i=0; i< handlers.length; i++) {
		    if (handlers[i].start.addr <= pc.addr
			&& handlers[i].end.addr >= pc.addr
			&& (handlers[i].type == null
			    || env.instanceOf(obj, handlers[i].type))) {
			stacktop = 0;
			stack[stacktop++].setObject(obj);
			pc = handlers[i].catcher;
			continue big_loop;
		    }
		}
		throw ex;
	    }
	}
    }
}
