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
import jode.*;
import jode.bytecode.*;
import jode.decompiler.ClassAnalyzer;
import java.lang.reflect.*;

/**
 * This class is a java virtual machine written in java :-).  Well not
 * exactly.  It does only handle a subset of the opcodes and is mainly
 * written do deobfuscate Strings.
 *
 * @author Jochen Hoenicke
 */
public class Interpreter implements Opcodes {

    static boolean checkType(Type type1, Class type2) {
	if (type1 == Type.tBoolean) {
	    return type2 == Boolean.TYPE;
	} else if (type1 == Type.tByte) {
	    return type2 == Byte.TYPE;
	} else if (type1 == Type.tChar) {
	    return type2 == Character.TYPE;
	} else if (type1 == Type.tShort) {
	    return type2 == Short.TYPE;
	} else if (type1 == Type.tInt) {
	    return type2 == Integer.TYPE;
	} else if (type1 == Type.tLong) {
	    return type2 == Long.TYPE;
	} else if (type1 == Type.tFloat) {
	    return type2 == Float.TYPE;
	} else if (type1 == Type.tDouble) {
	    return type2 == Double.TYPE;
	} else if (type1 == Type.tVoid) {
	    return type2 == Void.TYPE;
	} else if (type1 instanceof ArrayType) {
	    if (!type2.isArray())
		return false;
	    return checkType(((ArrayType)type1).getElementType(), 
			     type2.getComponentType()); 
	} else if (type1 instanceof ClassInterfacesType) {
	    return type1.equals(Type.tClass(type2.getName()));
	} else
	    return false;
    }

    static boolean checkMethod(MethodType methodType,
			       Class[] paramTypes, Class retType) {
	Type[] params = methodType.getParameterTypes();
	if (params.length != paramTypes.length)
	    return false;
	for (int i=0; i < params.length; i++) {
	    if (!checkType(params[i], paramTypes[i]))
		return false;
	}
	return checkType(methodType.getReturnType(), retType);
    }

    public static Object interpretMethod
	(ClassAnalyzer ca, BytecodeInfo code, Value[] locals, Value[] stack)
	throws InterpreterException, ClassFormatException {
	try {
	Instruction pc = code.getFirstInstr();
	int stacktop = 0;
	for(;;) {
	    Instruction instr = pc;
//  	    System.err.print(instr.addr+": [");
//  	    for (int i=0; i<stacktop; i++) {
//  		if (i>0)
//  		    System.err.print(",");
//  		System.err.print(stack[i]);
//  		if (stack[i].objectValue() instanceof char[]) {
//  		    System.err.print(new String((char[])stack[i].objectValue()));
//  		}
//  	    }
//  	    System.err.println("]");
//  	    System.err.print("local: [");
//  	    for (int i=0; i<locals.length; i++)
//  		System.err.print(locals[i]+",");
//  	    System.err.println("]");
	    pc = instr.nextByAddr;
	    int opcode = instr.opcode;
	    switch (opcode) {
	    case opc_nop:
		break;
	    case opc_ldc:
	    case opc_ldc2_w: {
		stack[stacktop++].setObject(instr.objData);
		break;
	    }
	    case opc_iload: case opc_lload: 
	    case opc_fload: case opc_dload: case opc_aload:
		stack[stacktop++].setValue(locals[instr.localSlot]);
		break;
	    case opc_iaload: case opc_laload: 
	    case opc_faload: case opc_daload: case opc_aaload:
	    case opc_baload: case opc_caload: case opc_saload: {
		int index = stack[--stacktop].intValue();
		Object array = stack[--stacktop].objectValue();
		switch(opcode) {
		case opc_baload: 
		    stack[stacktop++].setInt
			(array instanceof byte[]
			 ? ((byte[])array)[index]
			 : ((boolean[])array)[index] ? 1 : 0);
		case opc_caload: 
		    stack[stacktop++].setInt(((char[])array)[index]);
		    break;
		case opc_saload:
		    stack[stacktop++].setInt(((short[])array)[index]);
		    break;
		case opc_iaload: 
		case opc_laload: 
		case opc_faload: 
		case opc_daload: 
		case opc_aaload:
		    stack[stacktop++].setObject(Array.get(array, index));
		    break;
		}
		break;
	    }
	    case opc_istore: case opc_lstore: 
	    case opc_fstore: case opc_dstore: case opc_astore:
		locals[instr.localSlot].setValue(stack[--stacktop]);
		break;
	    case opc_iastore: case opc_lastore:
	    case opc_fastore: case opc_dastore: case opc_aastore:
	    case opc_bastore: case opc_castore: case opc_sastore: {
		Value value = stack[--stacktop];
		int index = stack[--stacktop].intValue();
		Object array = stack[--stacktop].objectValue();
		switch(opcode) {
		case opc_bastore: 
		    if (array instanceof byte[]) 
			((byte[])array)[index] = (byte) value.intValue();
		    else
			((boolean[])array)[index] = value.intValue() != 0;
		    break;
		case opc_castore: 
		    ((char[])array)[index] = (char) value.intValue();
		    break;
		case opc_sastore:
		    ((short[])array)[index] = (short) value.intValue();
		    break;
		case opc_iastore: 
		case opc_lastore: 
		case opc_fastore: 
		case opc_dastore: 
		case opc_aastore:
		    Array.set(array, index, value.objectValue());
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
		stack[stacktop-2].setInt(stack[stacktop-2].intValue()
					 / stack[stacktop-1].intValue());
		stacktop--;
		break;
	    case opc_irem: 
		stack[stacktop-2].setInt(stack[stacktop-2].intValue()
					 % stack[stacktop-1].intValue());
		stacktop--;
		break;

	    case opc_ladd: 
		stack[stacktop-2].setLong(stack[stacktop-2].longValue()
					  + stack[stacktop-1].longValue());
		stacktop--;
		break;
	    case opc_lsub: 
		stack[stacktop-2].setLong(stack[stacktop-2].longValue()
					  - stack[stacktop-1].longValue());
		stacktop--;
		break;
	    case opc_lmul: 
		stack[stacktop-2].setLong(stack[stacktop-2].longValue()
					  * stack[stacktop-1].longValue());
		stacktop--;
		break;
	    case opc_ldiv: 
		stack[stacktop-2].setLong(stack[stacktop-2].longValue()
					  / stack[stacktop-1].longValue());
		stacktop--;
		break;
	    case opc_lrem: 
		stack[stacktop-2].setLong(stack[stacktop-2].longValue()
					  % stack[stacktop-1].longValue());
		stacktop--;
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
		stack[stacktop-2].setDouble(stack[stacktop-2].doubleValue()
					    + stack[stacktop-1].doubleValue());
		stacktop--;
		break;
	    case opc_dsub: 
		stack[stacktop-2].setDouble(stack[stacktop-2].doubleValue()
					    - stack[stacktop-1].doubleValue());
		stacktop--;
		break;
	    case opc_dmul: 
		stack[stacktop-2].setDouble(stack[stacktop-2].doubleValue()
					    * stack[stacktop-1].doubleValue());
		stacktop--;
		break;
	    case opc_ddiv: 
		stack[stacktop-2].setDouble(stack[stacktop-2].doubleValue()
					    / stack[stacktop-1].doubleValue());
		stacktop--;
		break;
	    case opc_drem: 
		stack[stacktop-2].setDouble(stack[stacktop-2].doubleValue()
					    % stack[stacktop-1].doubleValue());
		stacktop--;
		break;

	    case opc_ineg: 
		stack[stacktop-1].setInt(-stack[stacktop-1].intValue());
		break;
	    case opc_lneg: 
		stack[stacktop-1].setLong(-stack[stacktop-1].longValue());
		break;
	    case opc_fneg: 
		stack[stacktop-1].setFloat(-stack[stacktop-1].floatValue());
		break;
	    case opc_dneg: 
		stack[stacktop-1].setDouble(-stack[stacktop-1].doubleValue());
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
		stack[stacktop-2].setLong(stack[stacktop-2].longValue()
					  << stack[stacktop-1].longValue());
		stacktop--;
		break;
	    case opc_lshr:
		stack[stacktop-2].setLong(stack[stacktop-2].longValue()
					  >> stack[stacktop-1].longValue());
		stacktop--;
		break;
	    case opc_lushr:
		stack[stacktop-2].setLong(stack[stacktop-2].longValue()
					  >>> stack[stacktop-1].longValue());
		stacktop--;
		break;
	    case opc_land:
		stack[stacktop-2].setLong(stack[stacktop-2].longValue()
					  & stack[stacktop-1].longValue());
		stacktop--;
		break;
	    case opc_lor :
		stack[stacktop-2].setLong(stack[stacktop-2].longValue()
					  | stack[stacktop-1].longValue());
		stacktop--;
		break;
	    case opc_lxor:
		stack[stacktop-2].setLong(stack[stacktop-2].longValue()
					  ^ stack[stacktop-1].longValue());
		stacktop--;
		break;

	    case opc_iinc:
		locals[instr.localSlot].setInt
		    (locals[instr.localSlot].intValue() + instr.intData);
		break;
	    case opc_i2l:
		stack[stacktop-1]
		    .setLong((long)stack[stacktop-1].intValue());
		break;
	    case opc_i2f: 
		stack[stacktop-1]
		    .setFloat((float)stack[stacktop-1].intValue());
		break;
	    case opc_i2d:
		stack[stacktop-1]
		    .setDouble((double)stack[stacktop-1].intValue());
		break;

	    case opc_l2i:
		stack[stacktop-1]
		    .setInt((int)stack[stacktop-1].longValue());
		break;
	    case opc_l2f: 
		stack[stacktop-1]
		    .setFloat((float)stack[stacktop-1].longValue());
		break;
	    case opc_l2d:
		stack[stacktop-1]
		    .setDouble((double)stack[stacktop-1].longValue());
		break;

	    case opc_f2i: 
		stack[stacktop-1]
		    .setInt((int)stack[stacktop-1].floatValue());
		break;
	    case opc_f2l:
		stack[stacktop-1]
		    .setLong((long)stack[stacktop-1].floatValue());
		break;
	    case opc_f2d:
		stack[stacktop-1]
		    .setDouble((double)stack[stacktop-1].floatValue());
		break;

	    case opc_d2i:
		stack[stacktop-1]
		    .setInt((int)stack[stacktop-1].doubleValue());
		break;
	    case opc_d2l:
		stack[stacktop-1]
		    .setLong((long)stack[stacktop-1].doubleValue());
		break;
	    case opc_d2f: 
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
		long val1 = stack[stacktop-2].longValue();
		long val2 = stack[--stacktop].longValue();
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
		double val1 = stack[stacktop-2].doubleValue();
		double val2 = stack[--stacktop].doubleValue();
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
		    if (opcode >= opc_ifnull) {
			value = 
			    stack[--stacktop].objectValue() == null ? 0 : 1;
			opcode += opc_ifeq - opc_ifnull;
		    } else {
			value = 
			    stack[--stacktop].objectValue()
			    == stack[--stacktop].objectValue() ? 0 : 1;
			opcode += opc_ifeq - opc_if_acmpeq;
		    }
		} else {
		    value = stack[--stacktop].intValue();
		    if (opcode >= opc_if_icmpeq) {
			int val1 = stack[--stacktop].intValue();
			value = (val1 == value ? 0
				 : val1 < value ? -1 : 1);
			opcode += opc_ifeq - opc_if_icmpeq;
		    }
		}
		if (value > 0 && (opcode == opc_ifgt || opcode == opc_ifge)
		    || value < 0 && (opcode == opc_iflt || opcode == opc_ifle)
		    || value == 0 && (opcode == opc_ifge || opcode == opc_ifle
				      || opcode == opc_ifeq))
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
	    case opc_ireturn: case opc_lreturn: 
	    case opc_freturn: case opc_dreturn: case opc_areturn:
		return stack[--stacktop].objectValue();
	    case opc_return:
		return Void.TYPE;
	    case opc_getstatic:
	    case opc_getfield:
	    case opc_putstatic:
	    case opc_putfield:
		throw new InterpreterException
		    ("Implement get/put-static/field?");
	    case opc_invokevirtual:
	    case opc_invokespecial:
	    case opc_invokestatic :
	    case opc_invokeinterface: {
		Reference ref = (Reference) instr.objData;
		
		if (ref.getClazz().equals(ca.getClazz().getName())) {
		    MethodType mt = (MethodType) Type.tType(ref.getType());
		    BytecodeInfo info = ca.getMethod(ref.getName(), mt)
			.getCode().getBytecodeInfo();
		    Value[] newLocals = new Value[info.getMaxLocals()];
		    for (int i=0; i< newLocals.length; i++)
			newLocals[i] = new Value();
		    Value[] newStack = new Value[info.getMaxStack()];
		    for (int i=0; i< newStack.length; i++)
			newStack[i] = new Value();
		    int param = mt.getParameterTypes().length;
		    int slot = 0;
		    if (opcode != opc_invokestatic)
			newLocals[slot++].setValue(stack[stacktop-param-1]);
		    for (int i = 0; i < param; i++) {
			newLocals[slot].setValue(stack[stacktop-param+i]);
			slot += mt.getParameterTypes()[i].stackSize();
		    }
		    Object result = interpretMethod(ca, info, 
						    newLocals, newStack);
		    if (mt.getReturnType() != Type.tVoid)
			stack[stacktop++].setObject(result);
		} else {
		    Class clazz;
		    try {
			clazz = Class.forName(ref.getClazz());
		    } catch (ClassNotFoundException ex) {
			throw new InterpreterException
			    ("Class "+ref.getClazz()+" not found");
		    }
		    try {
			if (ref.getName().equals("<init>")) {
			    Constructor[] cs = clazz.getConstructors();
			    Constructor c = null;
			    for (int i=0; i< cs.length; i++) {
				if (checkMethod((MethodType) 
						Type.tType(ref.getType()), 
						cs[i].getParameterTypes(), 
						Void.TYPE)) {
				    c = cs[i];
				    break;
				}
			    }
			    if (c == null)
				throw new InterpreterException
				    ("Constructor " + ref + " not found.");
			    Object[] args
				= new Object[c.getParameterTypes().length];
			    for (int i=args.length - 1; i >= 0; i--)
				args[i] = stack[--stacktop].objectValue();
			    NewObject newObj = stack[--stacktop].getNewObject();
			    if (!newObj.getType().equals(ref.getClazz()))
				throw new InterpreterException
				    ("constructor called on wrong type");
			    newObj.setObject(c.newInstance(args));
			} else {
			    Method[] ms = clazz.getMethods();
			    Method m = null;
			    for (int i=0; i< ms.length; i++) {
				if (ms[i].getName().equals(ref.getName())) {
				    if (checkMethod((MethodType) 
						    Type.tType(ref.getType()),
						    ms[i].getParameterTypes(), 
						    ms[i].getReturnType())) {
					m = ms[i];
					break;
				    }
				}
			    }
			    if (m == null)
				throw new InterpreterException
				    ("Method " + ref + " not found.");
			    Object obj = null;
			    Object[] args 
				= new Object[m.getParameterTypes().length];
			    for (int i=args.length - 1; i >= 0; i--)
				args[i] = stack[--stacktop].objectValue();
			    if (opcode != opc_invokestatic)
				obj = stack[--stacktop].objectValue();
			    /* special and constructor? XXX*/
			Object result = m.invoke(obj, args);
			if (m.getReturnType() != Void.TYPE)
			    stack[stacktop++].setObject(result);
			}
		    } catch (IllegalAccessException ex) {
			throw new InterpreterException
			    ("Method " + ref + " not accessible");
		    } catch (InstantiationException ex) {
			throw new InterpreterException
			    ("InstantiationException in " + ref + ".");
		    } catch (InvocationTargetException ex) {
			throw new InterpreterException
			    ("Method " + ref + " throwed an exception");
			/*XXX exception handler?*/
		    }
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
		    throw new ClassFormatException("Invalid newarray operand");
		}
		break;
	    }
	    case opc_anewarray: {
		int length = stack[--stacktop].intValue();
		Class clazz;
		try {
		    clazz = Class.forName((String) instr.objData);
		} catch (ClassNotFoundException ex) {
		    throw new InterpreterException
			("Class "+ex.getMessage()+" not found");
		}
		stack[stacktop++].setObject(Array.newInstance(clazz, length));
		break;
	    }
	    case opc_arraylength: {
		Object array = stack[--stacktop].objectValue();
		stack[stacktop++].setInt(Array.getLength(array));
                break;
	    }
	    case opc_athrow:
		/*XXX Throw and catch ?? */
		throw new InterpreterException("Throw not implemented");
	    case opc_checkcast: {
		Class clazz;
		try {
		    clazz = Class.forName((String) instr.objData);
		} catch (ClassNotFoundException ex) {
		    throw new InterpreterException
			("Class "+ex.getMessage()+" not found");
		}
		Object obj = stack[stacktop-1].objectValue();
		if (obj != null && !clazz.isInstance(obj)) {
		    /*XXX*/
		    throw new InterpreterException
			("Throw ClassCastException not implemented");
		}
		break;
	    }
	    case opc_instanceof: {
		Class clazz;
		try {
		    clazz = Class.forName((String) instr.objData);
		} catch (ClassNotFoundException ex) {
		    throw new InterpreterException
			("Class "+ex.getMessage()+" not found");
		}
		Object obj = stack[--stacktop].objectValue();
		if (obj != null && !clazz.isInstance(obj)) {
		    /*XXX*/
		    throw new InterpreterException
			("Throw ClassCastException not implemented");
		}
		break;
	    }
	    case opc_monitorenter:
	    case opc_monitorexit:
		throw new InterpreterException
		    ("MonitorEnter/Exit not implemented");
	    case opc_multianewarray: {
		Class clazz;
		try {
		    clazz = Class.forName((String) instr.objData);
		} catch (ClassNotFoundException ex) {
		    throw new InterpreterException
			("Class "+ex.getMessage()+" not found");
		}
		int dimension = instr.intData;
		int[] dims = new int[dimension];
		for (int i=dimension - 1; i >= 0; i--)
		    dims[i-1] = stack[--stacktop].intValue();
		stack[stacktop++].setObject(Array.newInstance(clazz, dims));
		break;
	    }
	    default:
		throw new ClassFormatException("Invalid opcode "+opcode);
	    }
	}
	} catch(RuntimeException ex) {
	    ex.printStackTrace();
	    throw new InterpreterException("Caught RuntimeException: "
					   + ex.toString());
	}
    }
}
