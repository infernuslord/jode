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

package jode;
import jode.flow.*;
import java.io.*;
import sun.tools.java.*;

/**
 * This is an abstract class which creates flow blocks for the
 * opcodes in a byte stream.
 */
public abstract class Opcodes implements RuntimeConstants {

    public final static Type ALL_INT_TYPE = MyType.tUInt;
    public final static Type     INT_TYPE = Type.tInt;
    public final static Type    LONG_TYPE = Type.tLong;
    public final static Type   FLOAT_TYPE = Type.tFloat;
    public final static Type  DOUBLE_TYPE = Type.tDouble;
    public final static Type  OBJECT_TYPE = MyType.tUObject;
    public final static Type BOOLEAN_TYPE = Type.tBoolean;
    public final static Type    BYTE_TYPE = Type.tByte;
    public final static Type    CHAR_TYPE = Type.tChar;
    public final static Type   SHORT_TYPE = Type.tShort;
    public final static Type    VOID_TYPE = Type.tVoid;

    
    public final static Type types[][] = {
        { ALL_INT_TYPE, LONG_TYPE, FLOAT_TYPE, DOUBLE_TYPE, OBJECT_TYPE },
        {     INT_TYPE, LONG_TYPE, FLOAT_TYPE, DOUBLE_TYPE, OBJECT_TYPE, 
             BYTE_TYPE, CHAR_TYPE, SHORT_TYPE }
    };


    public static FlowBlock createNormal(CodeAnalyzer ca, 
					 int addr, int length, 
                                         Instruction instr)
    {
        return new FlowBlock(ca, addr, length,
                             new InstructionBlock(instr, 
                                                  new Jump(addr+length)));
    }

    public static FlowBlock createGoto(CodeAnalyzer ca,
				       int addr, int length, 
                                       int destAddr)
    {
        return new FlowBlock(ca, addr, length, 
			     new EmptyBlock(new Jump(destAddr)));
    }

    public static FlowBlock createJsr(CodeAnalyzer ca,
				      int addr, int length, 
				      int destAddr)
    {
        return new FlowBlock(ca, addr, length, 
			     new JsrBlock(new Jump(addr+length),
					  new Jump(destAddr)));
    }

    public static FlowBlock createIfGoto(CodeAnalyzer ca, 
					 int addr, int length, 
                                         int destAddr, Instruction instr)
    {
        ConditionalBlock ifBlock = 
            new ConditionalBlock(instr, 
                                 new Jump(destAddr),
                                 new Jump(addr+length));
        return new FlowBlock(ca, addr, length, ifBlock);
    }

    public static FlowBlock createSwitch(CodeAnalyzer ca,
					 int addr, int length, 
                                         int[] cases, int[] dests)
    {
        return new FlowBlock(ca, addr, length, 
			     new SwitchBlock(new NopOperator(MyType.tInt), 
					     cases, dests));
    }

    public static FlowBlock createBlock(CodeAnalyzer ca,
					int addr, int length, 
                                        StructuredBlock block)
    {
        return new FlowBlock(ca, addr, length, block);
    }

    public static FlowBlock createRet(CodeAnalyzer ca,
				      int addr, int length, 
				      LocalInfo local)
    {
        return new FlowBlock(ca, addr, length, 
			     new RetBlock(local));
    }

    /**
     * Read an opcode out of a data input stream containing the bytecode.
     * @param addr    The current address.
     * @param stream  The stream containing the java byte code.
     * @param ca      The Code Analyzer 
     *                (where further information can be get from).
     * @return The FlowBlock representing this opcode
     *         or null if the stream is empty.
     * @exception IOException  if an read error occured.
     * @exception ClassFormatError  if an invalid opcode is detected.
     */
    public static FlowBlock readOpcode(int addr, DataInputStream stream,
                                       CodeAnalyzer ca) 
        throws IOException, ClassFormatError
    {
        try {
            int opcode = stream.readUnsignedByte();
            switch (opcode) {
            case opc_nop:
                return createNormal(ca, addr, 1, new NopOperator());
            case opc_aconst_null:
                return createNormal
		    (ca, addr, 1, new ConstOperator(OBJECT_TYPE, "null"));
	    case opc_iconst_m1: 
            case opc_iconst_0: case opc_iconst_1: case opc_iconst_2:
            case opc_iconst_3: case opc_iconst_4: case opc_iconst_5:
                return createNormal
                    (ca, addr, 1, new ConstOperator
		     (ALL_INT_TYPE, Integer.toString(opcode - opc_iconst_0)));
            case opc_lconst_0: case opc_lconst_1:
		return createNormal
		    (ca, addr, 1, new ConstOperator
		     (LONG_TYPE, 
		      Integer.toString(opcode - opc_lconst_0) + "L"));
	    case opc_fconst_0: case opc_fconst_1: case opc_fconst_2:
		return createNormal
		    (ca, addr, 1, new ConstOperator
		     (FLOAT_TYPE, 
		      Integer.toString(opcode - opc_fconst_0) + ".0F"));
            case opc_dconst_0: case opc_dconst_1:
		return createNormal
		    (ca, addr, 1, new ConstOperator
		     (DOUBLE_TYPE, 
		      Integer.toString(opcode - opc_dconst_0) + ".0"));
            case opc_bipush:
		return createNormal
		    (ca, addr, 2, new ConstOperator
		     (ALL_INT_TYPE, Integer.toString(stream.readByte())));
            case opc_sipush:
		return createNormal
                    (ca, addr, 3, new ConstOperator
		     (ALL_INT_TYPE, Integer.toString(stream.readShort())));
            case opc_ldc: {
                int index = stream.readUnsignedByte();
		return createNormal
                    (ca, addr, 2, new ConstOperator
		     (ca.env.getConstantType(index),
		      ca.env.getConstant(index).toString()));
            }
            case opc_ldc_w:
            case opc_ldc2_w: {
                int index = stream.readUnsignedShort();
		return createNormal
                    (ca, addr, 3, new ConstOperator
		     (ca.env.getConstantType(index),
		      ca.env.getConstant(index).toString()));
	    }
            case opc_iload: case opc_lload: 
            case opc_fload: case opc_dload: case opc_aload:
		return createNormal
                    (ca, addr, 2, new LocalLoadOperator
		     (types[0][opcode-opc_iload],
		      ca.getLocalInfo(addr, stream.readUnsignedByte())));
            case opc_iload_0: case opc_iload_1: case opc_iload_2: case opc_iload_3:
            case opc_lload_0: case opc_lload_1: case opc_lload_2: case opc_lload_3:
            case opc_fload_0: case opc_fload_1: case opc_fload_2: case opc_fload_3:
            case opc_dload_0: case opc_dload_1: case opc_dload_2: case opc_dload_3:
            case opc_aload_0: case opc_aload_1: case opc_aload_2: case opc_aload_3:
		return createNormal
                    (ca, addr, 1, new LocalLoadOperator
		     (types[0][(opcode-opc_iload_0)/4],
		      ca.getLocalInfo(addr, (opcode-opc_iload_0) & 3)));
            case opc_iaload: case opc_laload: 
            case opc_faload: case opc_daload: case opc_aaload:
            case opc_baload: case opc_caload: case opc_saload:
		return createNormal
		    (ca, addr, 1, new ArrayLoadOperator
		     (types[1][opcode - opc_iaload]));
            case opc_istore: case opc_lstore: 
            case opc_fstore: case opc_dstore: case opc_astore:
		return createNormal
                    (ca, addr, 2, new LocalStoreOperator
		     (types[0][opcode-opc_istore],
		      ca.getLocalInfo(addr, stream.readUnsignedByte()),
		      Operator.ASSIGN_OP));
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
		return createNormal
                    (ca, addr, 1, new LocalStoreOperator
		     (types[0][(opcode-opc_istore_0)/4],
		      ca.getLocalInfo(addr, (opcode-opc_istore_0) & 3),
		      Operator.ASSIGN_OP));
            case opc_iastore: case opc_lastore:
            case opc_fastore: case opc_dastore: case opc_aastore:
            case opc_bastore: case opc_castore: case opc_sastore:
		return createNormal
                    (ca, addr, 1, new ArrayStoreOperator
		     (types[1][opcode - opc_iastore]));
            case opc_pop: case opc_pop2:
		return createNormal
		    (ca, addr, 1, new PopOperator(opcode - opc_pop + 1));
            case opc_dup: case opc_dup_x1: case opc_dup_x2:
            case opc_dup2: case opc_dup2_x1: case opc_dup2_x2:
		return createNormal
                    (ca, addr, 1, new DupOperator
		     ((opcode - opc_dup)%3, (opcode - opc_dup)/3+1));
            case opc_swap:
                return createNormal(ca, addr, 1, new SwapOperator());
            case opc_iadd: case opc_ladd: case opc_fadd: case opc_dadd:
            case opc_isub: case opc_lsub: case opc_fsub: case opc_dsub:
            case opc_imul: case opc_lmul: case opc_fmul: case opc_dmul:
            case opc_idiv: case opc_ldiv: case opc_fdiv: case opc_ddiv:
            case opc_irem: case opc_lrem: case opc_frem: case opc_drem:
		return createNormal
                    (ca, addr, 1, new BinaryOperator
		     (types[0][(opcode - opc_iadd)%4],
		      (opcode - opc_iadd)/4+Operator.ADD_OP));
            case opc_ineg: case opc_lneg: case opc_fneg: case opc_dneg:
		return createNormal
                    (ca, addr, 1, new UnaryOperator
		     (types[0][opcode - opc_ineg], Operator.NEG_OP));
            case opc_ishl: case opc_lshl:
            case opc_ishr: case opc_lshr:
            case opc_iushr: case opc_lushr:
                return createNormal
		    (ca, addr, 1, new ShiftOperator
		     (types[0][(opcode - opc_ishl)%2],
		      (opcode - opc_ishl)/2 + Operator.SHIFT_OP));
            case opc_iand: case opc_land:
            case opc_ior : case opc_lor :
            case opc_ixor: case opc_lxor:
                return createNormal
		    (ca, addr, 1, new BinaryOperator
		     (types[0][(opcode - opc_iand)%2],
		      (opcode - opc_iand)/2 + Operator.AND_OP));
            case opc_iinc: {
                int local = stream.readUnsignedByte();
                int value = stream.readByte();
                int operation = Operator.ADD_OP;
                if (value < 0) {
                    value = -value;
                    operation = Operator.NEG_OP;
                }
                LocalInfo li = ca.getLocalInfo(addr, local);
                return createNormal
		    (ca, addr, 3, new IIncOperator
		     (li, Integer.toString(value),
		      operation + Operator.OPASSIGN_OP));
            }
            case opc_i2l: case opc_i2f: case opc_i2d:
            case opc_l2i: case opc_l2f: case opc_l2d:
            case opc_f2i: case opc_f2l: case opc_f2d:
            case opc_d2i: case opc_d2l: case opc_d2f: {
                int from = (opcode-opc_i2l)/3;
                int to   = (opcode-opc_i2l)%3;
                if (to >= from)
                    to++;
                return createNormal
		    (ca, addr, 1, new ConvertOperator(types[0][from], 
						  types[0][to]));
            }
            case opc_i2b: case opc_i2c: case opc_i2s:
                return createNormal
		    (ca, addr, 1, new ConvertOperator
		     (ALL_INT_TYPE, types[1][(opcode-opc_i2b)+5]));
	    case opc_lcmp:
            case opc_fcmpl: case opc_fcmpg:
            case opc_dcmpl: case opc_dcmpg:
                return createNormal
		    (ca, addr, 1, new CompareToIntOperator
		     (types[0][(opcode-opc_lcmp+3)/2], (opcode-opc_lcmp+3)%2));
            case opc_ifeq: case opc_ifne: 
            case opc_iflt: case opc_ifge: case opc_ifgt: case opc_ifle:
                return createIfGoto
		    (ca, addr, 3, addr+stream.readShort(),
		     new CompareUnaryOperator
		     (ALL_INT_TYPE, opcode - opc_ifeq+Operator.COMPARE_OP));
            case opc_if_icmpeq: case opc_if_icmpne: case opc_if_icmplt: 
            case opc_if_icmpge: case opc_if_icmpgt: case opc_if_icmple:
                return createIfGoto
		    (ca, addr, 3, addr+stream.readShort(),
		     new CompareBinaryOperator
		     (ALL_INT_TYPE, opcode - opc_if_icmpeq+Operator.COMPARE_OP));
            case opc_if_acmpeq: case opc_if_acmpne:
                return createIfGoto
		    (ca, addr, 3, addr+stream.readShort(),
		     new CompareBinaryOperator
		     (OBJECT_TYPE, opcode - opc_if_acmpeq+Operator.COMPARE_OP));
            case opc_goto:
                return createGoto
		    (ca, addr, 3, addr+stream.readShort());
            case opc_jsr:
                return createJsr
		    (ca, addr, 3, addr+stream.readShort());
            case opc_ret:
                return createRet
		    (ca, addr, 2, 
		     ca.getLocalInfo(addr, stream.readUnsignedByte()));
            case opc_tableswitch: {
                int length = 3-(addr % 4);
                stream.skip(length);
                int def  = addr + stream.readInt();
                int low  = stream.readInt();
                int high = stream.readInt();
                int[] cases = new int[high-low+1];
                int[] dests = new int[high-low+2];
                for (int i=0; i+low <= high; i++) {
                    cases[i] = i+low;
                    dests[i] = addr + stream.readInt();
                }
                dests[cases.length] = def;
                length += 13 + 4 * cases.length;
                return createSwitch
		    (ca, addr, length, cases, dests);
            }
            case opc_lookupswitch: {
                int length = 3-(addr % 4);
                stream.skip(length);
                int def    = addr + stream.readInt();
                int npairs = stream.readInt();
                int[] cases = new int[npairs];
                int[] dests = new int[npairs+1];
                for (int i=0; i < npairs; i++) {
                    cases[i] = stream.readInt();
                    dests[i] = addr + stream.readInt();
                }
                dests[npairs] = def;
                length += 9 + 8 * npairs;
                return createSwitch
		    (ca, addr, length, cases, dests);
            }
            case opc_ireturn: case opc_lreturn: 
            case opc_freturn: case opc_dreturn: case opc_areturn: {
                Type retType = MyType.intersection
                    (ca.getMethod().mdef.getType().getReturnType(),
                     types[0][opcode-opc_ireturn]);
		return createBlock
		    (ca, addr, 1, new ReturnBlock(new NopOperator(retType)));
            }
	    case opc_return:
                /* Address -1 is interpreted as end of method */
		return createBlock
		    (ca, addr, 1, new EmptyBlock(new Jump(-1)));
            case opc_getstatic:
            case opc_getfield:
                return createNormal
		    (ca, addr, 3, new GetFieldOperator
		     (ca, opcode == opc_getstatic,
		      (FieldDefinition)ca.env.getConstant
		      (stream.readUnsignedShort())));
            case opc_putstatic:
            case opc_putfield:
		return createNormal
                    (ca, addr, 3, new PutFieldOperator
		     (ca, opcode == opc_putstatic,
		      (FieldDefinition)ca.env.getConstant
		      (stream.readUnsignedShort())));
            case opc_invokevirtual:
            case opc_invokespecial:
            case opc_invokestatic :
                return createNormal
		    (ca, addr, 3, new InvokeOperator
		     (ca, 
                      opcode == opc_invokestatic, opcode == opc_invokespecial, 
		      (FieldDefinition)ca.env.getConstant
		      (stream.readUnsignedShort())));
            case opc_invokeinterface: {
                FlowBlock fb = createNormal
		    (ca, addr, 5, new InvokeOperator
		     (ca, false, false,
		      (FieldDefinition)ca.env.getConstant
		      (stream.readUnsignedShort())));
                int reserved = stream.readUnsignedShort();
		return fb;
            }
            case opc_new: {
                ClassDeclaration cldec = (ClassDeclaration) 
                    ca.env.getConstant(stream.readUnsignedShort());
                Type type = MyType.tClassOrArray(cldec.getName());
                return createNormal
		    (ca, addr, 3, new NewOperator(type, ca.env.getTypeString(type)));
            }
            case opc_newarray: {
                Type type;
                switch (stream.readUnsignedByte()) {
                case  4: type = Type.tBoolean; break;
                case  5: type = Type.tChar   ; break;
                case  6: type = Type.tFloat  ; break;
                case  7: type = Type.tDouble ; break;
                case  8: type = Type.tByte   ; break;
                case  9: type = Type.tShort  ; break;
                case 10: type = Type.tInt    ; break;
                case 11: type = Type.tLong   ; break;
                default:
                    throw new ClassFormatError("Invalid newarray operand");
                }
                return createNormal
                    (ca, addr, 2,
                     new NewArrayOperator(MyType.tArray(type),
                                          type.toString(), 1));
            }
            case opc_anewarray: {
                ClassDeclaration cldec = (ClassDeclaration) ca.env.getConstant
                    (stream.readUnsignedShort());
                Identifier ident = cldec.getName();
                Type type = MyType.tClassOrArray(cldec.getName());
                return createNormal
		    (ca, addr, 3, new NewArrayOperator
                     (MyType.tArray(type), ca.env.getTypeString(type),1));
            }
            case opc_arraylength:
                return createNormal
		    (ca, addr, 1, new ArrayLengthOperator());
            case opc_athrow:
                return createBlock
		    (ca, addr, 1, 
                     new ThrowBlock(new NopOperator(MyType.tUObject)));
            case opc_checkcast: {
                ClassDeclaration cldec = (ClassDeclaration) ca.env.getConstant
                    (stream.readUnsignedShort());
                Type type = MyType.tClassOrArray(cldec.getName());
                return createNormal
		    (ca, addr, 3, new CheckCastOperator
		     (type, ca.env.getTypeString(type)));
            }
            case opc_instanceof: {
                ClassDeclaration cldec = (ClassDeclaration) ca.env.getConstant
                    (stream.readUnsignedShort());
                Type type = MyType.tClassOrArray(cldec.getName());
                return createNormal
		    (ca, addr, 3,
		     new InstanceOfOperator(type, ca.env.getTypeString(type)));
            }
            case opc_monitorenter:
                return createNormal(ca, addr, 1,
					     new MonitorEnterOperator());
            case opc_monitorexit:
                return createNormal(ca, addr, 1,
					     new MonitorExitOperator());
            case opc_wide: {
                switch (opcode=stream.readUnsignedByte()) {
                case opc_iload: case opc_lload: 
                case opc_fload: case opc_dload: case opc_aload:
                    return createNormal
			(ca, addr, 4,
			 new LocalLoadOperator
			 (types[0][opcode-opc_iload],
			  ca.getLocalInfo(addr, stream.readUnsignedShort())));
                case opc_istore: case opc_lstore: 
                case opc_fstore: case opc_dstore: case opc_astore:
                    return createNormal
		    (ca, addr, 4,
		     new LocalStoreOperator
                     (types[0][opcode-opc_istore],
                      ca.getLocalInfo(addr, stream.readUnsignedShort()),
                      Operator.ASSIGN_OP));
                case opc_iinc: {
		    int local = stream.readUnsignedShort();
		    int value = stream.readShort();
		    int operation = Operator.ADD_OP;
		    if (value < 0) {
			value = -value;
			operation = Operator.NEG_OP;
		    }
                    LocalInfo li = ca.getLocalInfo(addr, local);
		    return createNormal
			(ca, addr, 6, new IIncOperator
			  (li, Integer.toString(value),
			   operation + Operator.OPASSIGN_OP));
		}
                case opc_ret:
		  return createRet
		    (ca, addr, 4, 
		     ca.getLocalInfo(addr, stream.readUnsignedShort()));
                default:
                    throw new ClassFormatError("Invalid wide opcode "+opcode);
                }
            }
            case opc_multianewarray: {
                ClassDeclaration cldec = (ClassDeclaration) ca.env.getConstant
                    (stream.readUnsignedShort());
                Type type = MyType.tClassOrArray(cldec.getName());
                int dimension = stream.readUnsignedByte();
                Type baseType = type;
                for (int i=0; i<dimension; i++)
                    baseType = baseType.getElementType();
                return createNormal
		    (ca, addr, 4,
		     new NewArrayOperator
                     (type, ca.env.getTypeString(baseType), dimension));
            }
            case opc_ifnull: case opc_ifnonnull:
                return createIfGoto
		    (ca, addr, 3, addr+stream.readShort(),
		     new CompareUnaryOperator
		     (OBJECT_TYPE, opcode - opc_ifnull+Operator.COMPARE_OP));
            case opc_goto_w:
                return createGoto
		    (ca, addr, 5, addr + stream.readInt());
            case opc_jsr_w:
		return createJsr
		    (ca, addr, 5, addr+stream.readInt());
            default:
		throw new ClassFormatError("Invalid opcode "+opcode);
            }
	} catch (ClassCastException ex) {
            ex.printStackTrace();
            throw new ClassFormatError("Constant has wrong type");
        }
    }
}
