/* 
 * Opcodes (c) 1998 Jochen Hoenicke
 *
 * You may distribute under the terms of the GNU General Public License.
 *
 * IN NO EVENT SHALL JOCHEN HOENICKE BE LIABLE TO ANY PARTY FOR DIRECT,
 * INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT OF
 * THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF JOCHEN HOENICKE 
 * HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * JOCHEN HOENICKE SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS"
 * BASIS, AND JOCHEN HOENICKE HAS NO OBLIGATION TO PROVIDE MAINTENANCE,
 * SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * $Id$
 */
package jode.jvm;
import jode.bytecode.*;
import java.lang.reflect.*;

/**
 * This class is a java virtual machine written in java :-).  Well not
 * exactly.  It does only handle a subset of the opcodes and is mainly
 * written do deobfuscate Strings.
 *
 * @author Jochen Hoenicke
 */
public class Interpreter implements Opcodes {
    public static Object interpretMethod
	(ClassInfo currentClass, byte[] code, Value[] locals, Value[] stack)
	throws InterpreterException, ClassFormatException {
	try {
	ConstantPool cpool = currentClass.getConstantPool();
	int pc = 0;
	int stacktop = 0;
	for(;;) {
	    int opcode = code[pc++] & 0xff;
	    switch (opcode) {
	    case opc_nop:
		break;
	    case opc_aconst_null:
		stack[stacktop++].setObject(null);
		break;
	    case opc_iconst_m1: 
	    case opc_iconst_0: case opc_iconst_1: case opc_iconst_2:
	    case opc_iconst_3: case opc_iconst_4: case opc_iconst_5:
		stack[stacktop++].setInt(opcode - opc_iconst_0);
		break;
	    case opc_lconst_0: case opc_lconst_1:
		stack[stacktop++].setLong(opcode - opc_lconst_0);
		break;
	    case opc_fconst_0: case opc_fconst_1: case opc_fconst_2:
		stack[stacktop++].setFloat(opcode - opc_fconst_0);
		break;
	    case opc_dconst_0: case opc_dconst_1:
		stack[stacktop++].setDouble(opcode - opc_dconst_0);
		break;
	    case opc_bipush:
		stack[stacktop++].setInt(code[pc++]);
		break;
	    case opc_sipush:
		stack[stacktop++].setInt((code[pc++] << 8) 
					 | (code[pc++] & 0xff));
		break;
	    case opc_ldc:
		stack[stacktop++].setObject
		    (cpool.getConstant(code[pc++] & 0xff));
		break;
	    case opc_ldc_w:
	    case opc_ldc2_w: {
		int index = (code[pc++] << 8) & 0xff00 | code[pc++] & 0xff;
		stack[stacktop++].setObject(cpool.getConstant(index));
		break;
	    }
	    case opc_iload: case opc_lload: 
	    case opc_fload: case opc_dload: case opc_aload:
		stack[stacktop++].setValue(locals[code[pc++]]);
		break;
	    case opc_iload_0: case opc_iload_1: 
	    case opc_iload_2: case opc_iload_3:
	    case opc_lload_0: case opc_lload_1: 
	    case opc_lload_2: case opc_lload_3:
	    case opc_fload_0: case opc_fload_1: 
	    case opc_fload_2: case opc_fload_3:
	    case opc_dload_0: case opc_dload_1: 
	    case opc_dload_2: case opc_dload_3:
	    case opc_aload_0: case opc_aload_1: 
	    case opc_aload_2: case opc_aload_3:
		stack[stacktop++].setValue(locals[(opcode - opc_iload_0) & 3]);
		break;
	    case opc_iaload: case opc_laload: 
	    case opc_faload: case opc_daload: case opc_aaload:
	    case opc_baload: case opc_caload: case opc_saload: {
		int index = stack[--stacktop].intValue();
		Object array = stack[--stacktop].objectValue();
		switch(opcode) {
		case opc_baload: 
		    stack[stacktop++].setInt(((byte[])array)[index]);
		    break;
		case opc_caload: 
		    stack[stacktop++].setInt(((char[])array)[index]);
		    break;
		case opc_saload:
		    stack[stacktop++].setInt(((short[])array)[index]);
		    break;
		case opc_iaload: 
		    stack[stacktop++].setInt(((int[])array)[index]);
		    break;
		case opc_laload: 
		    stack[stacktop++].setLong(((long[])array)[index]);
		    break;
		case opc_faload: 
		    stack[stacktop++].setFloat(((float[])array)[index]);
		    break;
		case opc_daload: 
		    stack[stacktop++].setDouble(((double[])array)[index]);
		    break;
		case opc_aaload:
		    stack[stacktop++].setObject(((Object[])array)[index]);
		    break;
		}
		break;
	    }
	    case opc_istore: case opc_lstore: 
	    case opc_fstore: case opc_dstore: case opc_astore:
		locals[code[pc++]].setValue(stack[--stacktop]);
		break;
	    case opc_istore_0: case opc_istore_1: 
	    case opc_istore_2: case opc_istore_3:
	    case opc_lstore_0: case opc_lstore_1: 
	    case opc_lstore_2: case opc_lstore_3:
	    case opc_fstore_0: case opc_fstore_1:
	    case opc_fstore_2: case opc_fstore_3:
	    case opc_dstore_0: case opc_dstore_1:
	    case opc_dstore_2: case opc_dstore_3:
	    case opc_astore_0: case opc_astore_1:
	    case opc_astore_2: case opc_astore_3:
		locals[(opcode-opc_istore_0) & 3].setValue(stack[--stacktop]);
		break;
	    case opc_iastore: case opc_lastore:
	    case opc_fastore: case opc_dastore: case opc_aastore:
	    case opc_bastore: case opc_castore: case opc_sastore: {
		Value value = stack[--stacktop];
		int index = stack[--stacktop].intValue();
		Object array = stack[--stacktop].objectValue();
		switch(opcode) {
		case opc_baload: 
		    ((byte[])array)[index] = (byte) value.intValue();
		    break;
		case opc_caload: 
		    ((char[])array)[index] = (char) value.intValue();
		    break;
		case opc_saload:
		    ((short[])array)[index] = (short) value.intValue();
		    break;
		case opc_iaload: 
		    ((int[])array)[index] = value.intValue();
		    break;
		case opc_laload: 
		    ((long[])array)[index] = value.longValue();
		    break;
		case opc_faload: 
		    ((float[])array)[index] = value.floatValue();
		    break;
		case opc_daload: 
		    ((double[])array)[index] = value.doubleValue();
		    break;
		case opc_aaload:
		    ((Object[])array)[index] = value.objectValue();
		    break;
		}
		break;
	    }
	    case opc_pop: case opc_pop2:
		stacktop -= opcode - (opc_pop-1);
		break;
	    case opc_dup: case opc_dup_x1: case opc_dup_x2: {
		int depth = (opcode - opc_dup)%3;
		for (int i=0; i < depth+1; i++)
		    stack[stacktop+1-i].setValue(stack[stacktop-i]);
		stack[stacktop-depth].setValue(stack[stacktop]);
		stacktop++;
		break;
	    }
	    case opc_dup2: case opc_dup2_x1: case opc_dup2_x2: {
		int depth = (opcode - opc_dup)%3;
		for (int i=0; i < depth+2; i++)
		    stack[stacktop+2-i] = stack[stacktop-i];
		stack[stacktop-depth].setValue(stack[stacktop+1]);
		stack[stacktop-depth-1].setValue(stack[stacktop]);
		stacktop += 2;
	    }
	    case opc_swap: {
		Value tmp = stack[stacktop-1];
		stack[stacktop-1] = stack[stacktop-2];
		stack[stacktop-2] = tmp;
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

	    case opc_iinc: {
		int slot = code[pc++] & 0xff;
		locals[slot].setInt(locals[slot].intValue() + code[pc++]);
		break;
	    }
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
		int offset = ((code[pc++] << 8) | (code[pc++] & 0xff)) - 3;
		if (value > 0 && (opcode == opc_ifgt || opcode == opc_ifge)
		    || value < 0 && (opcode == opc_iflt || opcode == opc_ifle)
		    || value == 0 && (opcode == opc_ifge || opcode == opc_ifle
				      || opcode == opc_ifeq))
		    pc += opcode;
		break;
	    }
	    case opc_jsr:
		stack[stacktop++].setObject(new ReturnAddress(pc));
		/* fall through */
	    case opc_goto:
		pc = (pc-1) + ((code[pc] << 8) | (code[pc+1] & 0xff));
		break;
	    case opc_ret:
		pc = ((ReturnAddress) locals[code[pc] & 0xff].objectValue())
		    .getPC();
		break;
	    case opc_tableswitch: {
		int value = stack[--stacktop].intValue();
		int start = pc - 1;
		pc += 3-(start % 4);
		int dest = ((code[pc++] << 8) | (code[pc++] & 0xff));
		int low  = ((code[pc++] << 8) | (code[pc++] & 0xff));
		int high = ((code[pc++] << 8) | (code[pc++] & 0xff));
		if (value >= low && value <= high) {
		    pc += (value - low) << 1;
		    dest = ((code[pc] << 8) | (code[pc+1] & 0xff));
		}
		pc = start + dest;
		break;
            }
	    case opc_lookupswitch: {
		int value = stack[--stacktop].intValue();
		int start = pc - 1;
		pc += 3-(start % 4);
		int dest = ((code[pc++] << 8) | (code[pc++] & 0xff));
		int npairs = ((code[pc++] << 8) | (code[pc++] & 0xff));
		for (int i=0; i < npairs; i++) {
		    if (value == ((code[pc++] << 8) | (code[pc++] & 0xff))) {
			dest = ((code[pc++] << 8) | (code[pc++] & 0xff));
			break;
		    }
		    pc+=2;
		}
		pc = start + dest;
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
		String[] ref = cpool.getRef((code[pc++] << 8) & 0xff00
					    | code[pc++] & 0xff);
		int argcount= (opcode == opc_invokeinterface) 
		    ? (code[pc++] << 8) | (code[pc++] & 0xff) : -1;
		
//  		if (ref[0] == currentClass.getName()) {
//  		    /* invoke interpreter again */
//  		} else {
		Class clazz;
		try {
		    clazz = Class.forName
			(cpool.getClassName((code[pc++] << 8) & 0xff00
					    | code[pc++] & 0xff)
			 .replace('/','.'));
		} catch (ClassNotFoundException ex) {
		    throw new InterpreterException
			("Class "+ref[0]+" not found");
		}
		try {
		    if (ref[1].equals("<init>")) {
			Constructor[] cs = clazz.getConstructors();
			Constructor c = null;
			for (int i=0; i< cs.length; i++) {
			    /* check types XXX */
			    c = cs[i];
			    break;
			}
			if (c == null)
			    throw new InterpreterException("Constructor "
						       +ref[0]+"."
						       +ref[1]+" not found.");
			Object[] args
			    = new Object[c.getParameterTypes().length];
			for (int i=args.length - 1; i >= 0; i--)
			    args[i] = stack[--stacktop].objectValue();
			NewObject newObj = 
			    (NewObject) stack[--stacktop].objectValue();
			if (!newObj.getClass().equals(ref[0]))
			    throw new InterpreterException("constructor not called"
							   +" on new instance");
			newObj.setObject(c.newInstance(args));
		    } else {
			Method[] ms = clazz.getMethods();
			Method m = null;
			for (int i=0; i< ms.length; i++) {
			    if (ms[i].getName().equals(ref[1])) {
				/* check types XXX */
				m = ms[i];
				break;
			    }
			}
			if (m == null)
			    throw new InterpreterException("Method "+ref[0]+"."
						       +ref[1]+" not found.");
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
			("Method "+ref[0]+"."+ref[1]+" not accessible");
		} catch (InstantiationException ex) {
		    throw new InterpreterException
			("InstantiationException in "+ref[0]+"."+ref[1]+".");
		} catch (InvocationTargetException ex) {
		    throw new InterpreterException
			("Method "+ref[0]+"."+ref[1]+" throwed an exception");
		    /*XXX exception handler?*/
		}
		break;
	    }
	    case opc_new: {
		String clazz = cpool.getClassName((code[pc++] << 8) & 0xff00
						  | code[pc++] & 0xff);
		stack[stacktop++].setObject(new NewObject(clazz));
		break;
	    }
	    case opc_newarray: {
		int length = stack[--stacktop].intValue();
		switch (code[pc++]) {
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
		    clazz = Class.forName
			(cpool.getClassName((code[pc++] << 8) & 0xff00
					    | code[pc++] & 0xff)
			 .replace('/','.'));
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
		    clazz = Class.forName
			(cpool.getClassName((code[pc++] << 8) & 0xff00
					    | code[pc++] & 0xff)
			 .replace('/','.'));
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
	    case opc_instanceof: {
		Class clazz;
		try {
		    clazz = Class.forName
			(cpool.getClassName((code[pc++] << 8) & 0xff00
					    | code[pc++] & 0xff)
			 .replace('/','.'));
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
	    case opc_wide: {
		opcode = code[pc++] & 0xff;
		switch (opcode) {
		case opc_iload: case opc_lload: 
		case opc_fload: case opc_dload: case opc_aload: {
		    int slot = (code[pc++] << 8) | (code[pc++] & 0xff);
		    stack[stacktop++].setValue(locals[slot & 0xffff]);
		    break;
		}
		case opc_istore: case opc_lstore: 
		case opc_fstore: case opc_dstore: case opc_astore: {
		    int slot = (code[pc++] << 8) & 0xff00 | code[pc++] & 0xff;
		    locals[slot & 0xffff].setValue(stack[--stacktop]);
		    break;
		}
		case opc_iinc: {
		    int slot = (code[pc++] << 8) & 0xff00 | code[pc++] & 0xff;
		    int value = (code[pc++] << 8) | code[pc++] & 0xff;
		    locals[slot].setInt(locals[slot].intValue()
						 + value);
                }
		case opc_ret: {
		    int slot = (code[pc++] << 8) & 0xff00 | code[pc++] & 0xff;
		    pc = ((ReturnAddress)locals[slot].objectValue()).getPC();
		}
		default:
		    throw new 
			ClassFormatException("Invalid wide opcode "+opcode);
		}
	    }
	    case opc_multianewarray: {
		Class clazz;
		try {
		    clazz = Class.forName
			(cpool.getClassName((code[pc++] << 8) & 0xff00
					    | code[pc++] & 0xff)
			 .replace('/','.'));
		} catch (ClassNotFoundException ex) {
		    throw new InterpreterException
			("Class "+ex.getMessage()+" not found");
		}
		int dimension = code[pc++] & 0xff;
		int[] dims = new int[dimension];
		for (int i=dimension-1; i >= 0; i--)
		    dims[i-1] = stack[--stacktop].intValue();
		stack[stacktop++].setObject(Array.newInstance(clazz, dims));
		break;
	    }
	    case opc_jsr_w:
		stack[stacktop++].setObject(new ReturnAddress(pc));
		/* fall through */
	    case opc_goto_w:
		pc = pc-1 + (code[pc] << 24 | ((code[pc+1]&0xff) << 16)
			     | ((code[pc+2]&0xff) << 8) | code[pc+3]&0xff);
	    default:
		throw new ClassFormatException("Invalid opcode "+opcode);
	    }
	}
	} catch(RuntimeException ex) {
	    throw new InterpreterException("Caught RuntimeException: "
					   + ex.toString());
	}
    }
}
