package jode.bytecode;
import jode.Decompiler/*XXX*/;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Vector;

public class BytecodeInfo extends BinaryInfo implements Opcodes {

    ConstantPool cp;
    int maxStack, maxLocals;
    Instruction firstInstr = null;
    Handler[] exceptionHandlers;

    private final static Object[] constants = {
	null, 
	new Integer(-1), new Integer(0), new Integer(1), 
	new Integer(2), new Integer(3), new Integer(4), new Integer(5),
	new Long(0), new Long(1), 
	new Float(0), new Float(1), new Float(2),
	new Double(0), new Double(1)
    };

    public void read(ConstantPool cp, 
		     DataInputStream input) throws IOException {
	this.cp = cp;
        maxStack = input.readUnsignedShort();
        maxLocals = input.readUnsignedShort();
        int codeLength = input.readInt();
	Instruction[] instrs = new Instruction[codeLength];
	int[][] succAddrs = new int[codeLength][];
	{
	    int addr = 0;
	    Instruction lastInstr = null;
	    while (addr < codeLength) {
		Instruction instr = new Instruction();
		if (lastInstr != null) {
		    lastInstr.nextByAddr = instr;
		    instr.prevByAddr = lastInstr;
		}
		instrs[addr] = instr;
		instr.addr = addr;
		lastInstr = instr;

		int opcode = input.readUnsignedByte();
		if (Decompiler.isDebugging)/*XXX*/
		    Decompiler.err.println(addr+": "+opcodeString[opcode]);

		instr.opcode = opcode;
		switch (opcode) {
		case opc_wide: {
		    int wideopcode = input.readUnsignedByte();
		    instr.opcode = wideopcode;
		    switch (wideopcode) {
		    case opc_iload: case opc_lload: 
		    case opc_fload: case opc_dload: case opc_aload:
		    case opc_istore: case opc_lstore: 
		    case opc_fstore: case opc_dstore: case opc_astore:
			instr.localSlot = input.readUnsignedShort();
			instr.length = 4;
			break;
		    case opc_ret:
			instr.localSlot = input.readUnsignedShort();
			instr.length = 4;
			instr.alwaysJumps = true;
			break;
			
		    case opc_iinc:
			instr.localSlot = input.readUnsignedShort();
			instr.intData = input.readShort();
			instr.length = 6;
			break;
		    default:
			throw new ClassFormatError("Invalid wide opcode "
						   +wideopcode);
		    }
		    break;
		}
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
		    instr.opcode = opc_iload + (opcode-opc_iload_0)/4;
		    instr.localSlot = (opcode-opc_iload_0) & 3;
		    instr.length = 1;
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
		    instr.opcode = opc_istore + (opcode-opc_istore_0)/4;
		    instr.localSlot = (opcode-opc_istore_0) & 3;
		    instr.length = 1;
		    break;
		case opc_iload: case opc_lload: 
		case opc_fload: case opc_dload: case opc_aload:
		case opc_istore: case opc_lstore: 
		case opc_fstore: case opc_dstore: case opc_astore:
		    instr.localSlot = input.readUnsignedByte();
		    instr.length = 2;
		    break;

		case opc_ret:
		    instr.localSlot = input.readUnsignedByte();
		    instr.alwaysJumps = true;
		    instr.length = 2;
		    break;

		case opc_aconst_null:
		case opc_iconst_m1: 
		case opc_iconst_0: case opc_iconst_1: case opc_iconst_2:
		case opc_iconst_3: case opc_iconst_4: case opc_iconst_5:
		case opc_lconst_0: case opc_lconst_1:
		case opc_fconst_0: case opc_fconst_1: case opc_fconst_2:
		case opc_dconst_0: case opc_dconst_1:
		    instr.objData = constants[instr.opcode - opc_aconst_null];
		    instr.length = 1;
		    break;
		case opc_bipush:
		    instr.objData = new Integer(input.readByte());
		    instr.length = 2;
		    break;
		case opc_sipush:
		    instr.objData = new Integer(input.readShort());
		    instr.length = 3;
		    break;
		case opc_ldc:
		    instr.objData = cp.getConstant(input.readUnsignedByte());
		    instr.length = 2;
		    break;
		case opc_ldc_w:
		    instr.opcode = opc_ldc;
		    /* fall through */
		case opc_ldc2_w:
		    instr.objData = cp.getConstant(input.readUnsignedShort());
		    instr.length = 3;
		    break;
		
		case opc_iinc:
		    instr.localSlot = input.readUnsignedByte();
		    instr.intData = input.readByte();
		    instr.length = 3;
		    break;

		case opc_goto:
		    instr.alwaysJumps = true;
		    /* fall through */
		case opc_ifeq: case opc_ifne: 
		case opc_iflt: case opc_ifge: 
		case opc_ifgt: case opc_ifle:
		case opc_if_icmpeq: case opc_if_icmpne:
		case opc_if_icmplt: case opc_if_icmpge: 
		case opc_if_icmpgt: case opc_if_icmple:
		case opc_if_acmpeq: case opc_if_acmpne:
		case opc_ifnull: case opc_ifnonnull:
		case opc_jsr:
		    succAddrs[addr] = new int[1];
		    succAddrs[addr][0] = addr+input.readShort();
		    instr.length = 3;
		    break;

		case opc_goto_w:
		    instr.alwaysJumps = true;
		    /* fall through */
		case opc_jsr_w:
		    instr.opcode -= opc_goto_w - opc_goto;
		    succAddrs[addr] = new int[1];
		    succAddrs[addr][0] = addr+input.readInt();
		    instr.length = 5;
		    break;

		case opc_tableswitch: {
		    int length = 3-(addr % 4);
		    input.readFully(new byte[length]);
		    int def  = input.readInt();
		    int low  = input.readInt();
		    int high = input.readInt();
		    instr.intData = low;
		    succAddrs[addr] = new int[high-low+2];
		    for (int i=0; i+low <= high; i++) {
			succAddrs[addr][i] = addr + input.readInt();
		    }
		    succAddrs[addr][high-low+1] = addr + def;
		    instr.alwaysJumps = true;
		    instr.length = length + 13 + 4 * (high-low+1);
		    break;
		}
		case opc_lookupswitch: {
		    int length = 3-(addr % 4);
		    input.readFully(new byte[length]);
		    int def = input.readInt();
		    int npairs = input.readInt();
		    succAddrs[addr] = new int[npairs + 1];
		    int[] values = new int[npairs];
		    for (int i=0; i < npairs; i++) {
			values[i] = input.readInt();
			succAddrs[addr][i] = addr + input.readInt();
		    }
		    succAddrs[addr][npairs] = addr + def;
		    instr.objData = values;
		    instr.alwaysJumps = true;
		    instr.length = length + 9 + 8 * npairs;
		    break;
		}
		
		case opc_ireturn: case opc_lreturn: 
		case opc_freturn: case opc_dreturn: case opc_areturn:
		case opc_return: 
		case opc_athrow:
		    instr.alwaysJumps = true;
		    instr.length = 1;
		    break;
	    
		case opc_getstatic:
		case opc_getfield:
		case opc_putstatic:
		case opc_putfield:
		case opc_invokespecial:
		case opc_invokestatic:
		case opc_invokevirtual:
		    instr.objData = cp.getRef(input.readUnsignedShort());
		    instr.length = 3;
		    break;
		case opc_invokeinterface:
		    instr.objData = cp.getRef(input.readUnsignedShort());
		    instr.intData = input.readUnsignedShort();
		    instr.length = 5;
		    break;

		case opc_new:
		case opc_anewarray:
		case opc_checkcast:
		case opc_instanceof:
		    instr.objData = cp.getClassName(input.readUnsignedShort())
			.replace('/','.');
		    instr.length = 3;
		    break;
		case opc_multianewarray:
		    instr.objData = cp.getClassName(input.readUnsignedShort())
			.replace('/','.');
		    instr.intData = input.readUnsignedByte();
		    instr.length = 4;
		    break;
		case opc_newarray:
		    instr.intData = input.readUnsignedByte();
		    instr.length = 2;
		    break;
		
		default:
		    if (opcode == opc_xxxunusedxxx
			|| opcode >= opc_breakpoint)
			throw new ClassFormatError("Invalid opcode "+opcode);
		    else
			instr.length = 1;
		}
		addr += lastInstr.length;
	    }
	}
	firstInstr = instrs[0];
	for (Instruction instr = firstInstr; 
	     instr != null; instr = instr.nextByAddr) {
	    int addr = instr.addr;
	    if (succAddrs[addr] != null) {
		int length = succAddrs[addr].length;
		instr.succs = new Instruction[length];
		for (int i=0; i < length; i++) {
		    instr.succs[i] = instrs[succAddrs[addr][i]];
		    instr.succs[i].preds.addElement(instr);
		}
	    }
	    /* YES, if the last instruction is not reachable it may
	     * not alwaysJump.  This happens under jikes
	     */
	    if (!instr.alwaysJumps && instr.nextByAddr != null)
		instr.nextByAddr.preds.addElement(instr);
	}
	succAddrs = null;

	{
	    int handlersLength = input.readUnsignedShort();
	    exceptionHandlers = new Handler[handlersLength];
	    for (int i=0; i< handlersLength; i ++) {
		exceptionHandlers[i] = new Handler();
		exceptionHandlers[i].start
		    = instrs[input.readUnsignedShort()];
		exceptionHandlers[i].end
		    = instrs[input.readUnsignedShort()].prevByAddr;
		exceptionHandlers[i].catcher
		    = instrs[input.readUnsignedShort()];
		int index = input.readUnsignedShort();
		exceptionHandlers[i].type = (index == 0) ? null
		    : cp.getClassName(index).replace('/','.');
	    }
	}
	readAttributes(cp, input, FULLINFO);
    }


    public void writeCode(GrowableConstantPool gcp, 
			  jode.obfuscator.ClassBundle bundle,
			  DataOutputStream output) throws IOException {
	/* Recalculate addr and length */
	int addr = 0;
	for (Instruction instr = firstInstr; 
	     instr != null; instr = instr.nextByAddr) {
	    instr.addr = addr;
	    if (instr.opcode == opc_ldc) {
		if (gcp.reserveConstant(instr.objData) < 256)
		    instr.length = 2;
		else
		    instr.length = 3;
//  		if (instr.objData == null) {
//  		    instr.length = 1;

//  		} else if (instr.objData instanceof Integer) {
//  		    int value = ((Integer) instr.objData).intValue();
//  		    if (value >= -1 && value <= 5)
//  			instr.length = 1;
//  		    else if (value >= -Byte.MIN_VALUE
//  			     && value <= Byte.MAX_VALUE)
//  			instr.length = 2;
//  		    else if (value >= -Short.MIN_VALUE
//  			&& value <= Short.MAX_VALUE)
//  			instr.length = 3;
//  		    else if (gcp.reserveConstant(instr.objData) < 256)
//  			instr.length = 2;
//  		    else
//  			instr.length = 3;
		   
//  		} else if (instr.objData instanceof Long) {
//  		    long value = ((Long) instr.objData).longValue();
//  		    if (value == 0L || value == 1L)
//  			instr.length = 1;
//  		    else
//  			instr.length = 3;

//  		} else if (instr.objData instanceof Float) {
//  		    float value = ((Float) instr.objData).floatValue();
//  		    if (value == 0.0F || value == 1.0F)
//  			instr.length = 1;
//  		    else if (gcp.reserveConstant(instr.objData) < 256)
//  			instr.length = 2;
//  		    else
//  			instr.length = 3;

//  		} else if (instr.objData instanceof Double) {
//  		    double value = ((Double) instr.objData).doubleValue();
//  		    if (value == 0.0 || value == 1.0)
//  			instr.length = 1;
//  		    else
//  			instr.length = 3;

//  		} else {
//  		    if (gcp.reserveConstant(instr.objData) < 256)
//  			instr.length = 2;
//  		    else
//  			instr.length = 3;
//  		}
	    } else if (instr.localSlot != -1) {
		if (instr.opcode == opc_iinc) {
		    if (instr.localSlot < 256 
			&& instr.intData >= Byte.MIN_VALUE 
			&& instr.intData <= Byte.MAX_VALUE)
			instr.length = 3;
		    else
			instr.length = 6;
		} else {
		    if (instr.localSlot < 4)
			instr.length = 1;
		    else if (instr.localSlot < 256)
			instr.length = 2;
		    else 
			instr.length = 4;
		}
	    } else if (instr.opcode == opc_tableswitch) {
		int length = 3-(addr % 4);
		instr.length = length + 9 + 4 * instr.succs.length;
	    } else if (instr.opcode == opc_lookupswitch) {
		int length = 3-(addr % 4);
		instr.length = length + 1 + 8 * instr.succs.length;
	    } else if (instr.opcode == opc_goto
		       || instr.opcode == opc_jsr) {
		int dist = instr.succs[0].addr - instr.addr;
		if (dist < Short.MIN_VALUE || dist > Short.MAX_VALUE) {
		    instr.length = 5;
		} else
		    instr.length = 3;
	    }
	    addr += instr.length;
	}

	/* Now output the code */
	output.writeShort(maxStack);
	output.writeShort(maxLocals);
	output.writeInt(addr);
	for (Instruction instr = firstInstr; 
	     instr != null; instr = instr.nextByAddr) {
	    switch (instr.opcode) {
	    case opc_iload: case opc_lload: 
	    case opc_fload: case opc_dload: case opc_aload:
	    case opc_istore: case opc_lstore: 
	    case opc_fstore: case opc_dstore: case opc_astore:
		if (instr.length == 1) {
		    if (instr.opcode < opc_istore)
			output.writeByte(opc_iload_0
					 + 4*(instr.opcode-opc_iload)
					 + instr.localSlot);
		    else
			output.writeByte(opc_istore_0
					 + 4*(instr.opcode-opc_istore)
					 + instr.localSlot);
		} else if (instr.length == 2) {
		    output.writeByte(instr.opcode);
		    output.writeByte(instr.localSlot);
		} else {
		    output.writeByte(opc_wide);
		    output.writeByte(instr.opcode);
		    output.writeShort(instr.localSlot);
		}
		break;
		
	    case opc_ret:
		if (instr.length == 2) {
		    output.writeByte(instr.opcode);
		    output.writeByte(instr.localSlot);
		} else {
		    output.writeByte(opc_wide);
		    output.writeByte(instr.opcode);
		    output.writeShort(instr.localSlot);
		}
		break;

	    case opc_ldc:
//  		if (instr.objData == null)
//  		    output.writeByte(opc_aconst_null);

//  		else if (instr.length == 1) {
//  		    int value = ((Number) instr.objData).intValue();
//  		    if (instr.objData instanceof Integer)
//  			output.writeByte(opc_iconst_0+value);
//  		    else if (instr.objData instanceof Long)
//  			output.writeByte(opc_lconst_0+value);
//  		    else if (instr.objData instanceof Float)
//  			output.writeByte(opc_fconst_0+value);
//  		    else if (instr.objData instanceof Double)
//  			output.writeByte(opc_dconst_0+value);

//  		} else if (instr.objData instanceof Long
//  			   || instr.objData instanceof Double) {
//  		    output.writeByte(opc_ldc2_w);
//  		    output.writeShort(gcp.putConstant(instr.objData));

//  		} else {
//  		    if (instr.objData instanceof Integer) {
//  			int value = ((Integer) instr.objData).intValue();
//  			if (value >= -Byte.MIN_VALUE
//  			    && value <= Byte.MAX_VALUE) {
//  			    output.writeByte(opc_bipush);
//  			    output.writeByte(value);
//  			    break;
//  			} else if (value >= -Short.MIN_VALUE
//  				   && value <= Short.MAX_VALUE) {
//  			    output.writeByte(opc_sipush);
//  			    output.writeShort(value);
//  			    break;
//  			}
//  		    }
		if (instr.length == 2) {
		    output.writeByte(opc_ldc);
		    output.writeByte(gcp.putConstant(instr.objData));
		} else {
		    output.writeByte(opc_ldc_w);
		    output.writeShort(gcp.putConstant(instr.objData));
		}
		break;
	    case opc_ldc2_w:
		output.writeByte(instr.opcode);
		output.writeShort(gcp.putConstant(instr.objData));
		break;
	    case opc_bipush:
		output.writeByte(instr.opcode);
		output.writeByte(((Integer)instr.objData).intValue());
		break;
	    case opc_sipush:
		output.writeByte(instr.opcode);
		output.writeShort(((Integer)instr.objData).intValue());
		break;

	    case opc_iinc:
		if (instr.length == 3) {
		    output.writeByte(instr.opcode);
		    output.writeByte(instr.localSlot);
		    output.writeByte(instr.intData);
		} else {
		    output.writeByte(opc_wide);
		    output.writeByte(instr.opcode);
		    output.writeShort(instr.localSlot);
		    output.writeShort(instr.intData);
		}
		break;

	    case opc_goto:
	    case opc_ifeq: case opc_ifne: 
	    case opc_iflt: case opc_ifge: 
	    case opc_ifgt: case opc_ifle:
	    case opc_if_icmpeq: case opc_if_icmpne:
	    case opc_if_icmplt: case opc_if_icmpge: 
	    case opc_if_icmpgt: case opc_if_icmple:
	    case opc_if_acmpeq: case opc_if_acmpne:
	    case opc_ifnull: case opc_ifnonnull:
	    case opc_jsr:
		if (instr.length == 3) {
		    output.writeByte(instr.opcode);
		    output.writeShort(instr.succs[0].addr - instr.addr);
		} else {
		    /* wide goto or jsr */
		    output.writeByte(instr.opcode + (opc_goto_w - opc_goto));
		    output.writeInt(instr.succs[0].addr - instr.addr);
		}
		break;

	    case opc_tableswitch: {
		output.writeByte(instr.opcode);
		int align = 3-(instr.addr % 4);
		int numcases = instr.succs.length - 1;
		output.write(new byte[align]);
		/* def */
		output.writeInt(instr.succs[numcases].addr - instr.addr);
		/* low */
		output.writeInt(instr.intData); 
		/* high */
		output.writeInt(instr.intData + numcases - 1);
		for (int i=0; i < numcases; i++)
		    output.writeInt(instr.succs[i].addr - instr.addr);
		break;
	    }
	    case opc_lookupswitch: {
		output.writeByte(instr.opcode);
		int[] values = (int[]) instr.objData;
		int align = 3-(instr.addr % 4);
		int numcases = values.length;
		output.write(new byte[align]);
		/* def */
		output.writeInt(instr.succs[numcases].addr - instr.addr);
		output.writeInt(numcases);
		for (int i=0; i < numcases; i++) {
		    output.writeInt(values[i]);
		    output.writeInt(instr.succs[i].addr - instr.addr);
		}
		break;
	    }

	    case opc_getstatic:
	    case opc_getfield:
	    case opc_putstatic:
	    case opc_putfield:
		output.writeByte(instr.opcode);
		output.writeShort(gcp.putRef(gcp.FIELDREF, 
					     (String[]) instr.objData));
		break;

	    case opc_invokespecial:
	    case opc_invokestatic:
	    case opc_invokeinterface:
	    case opc_invokevirtual:
		output.writeByte(instr.opcode);
		if (instr.opcode == opc_invokeinterface) {
		    output.writeShort(gcp.putRef(gcp.INTERFACEMETHODREF, 
						 (String[]) instr.objData));
		    output.writeShort(instr.intData);
		} else 
		    output.writeShort(gcp.putRef(gcp.METHODREF, 
						 (String[]) instr.objData));
		break;
	    case opc_new:
	    case opc_anewarray:
	    case opc_checkcast:
	    case opc_instanceof:
	    case opc_multianewarray:
		output.writeByte(instr.opcode);
		output.writeShort(gcp.putClassRef((String) instr.objData));
		if (instr.opcode == opc_multianewarray)
		    output.writeByte(instr.intData);
		break;

	    case opc_newarray:
		output.writeByte(instr.opcode);
		output.writeByte(instr.intData);
		break;

	    default:
		if (instr.opcode == opc_xxxunusedxxx
		    || instr.opcode >= opc_breakpoint)
		    throw new ClassFormatError("Invalid opcode "+instr.opcode);
		else
		    output.writeByte(instr.opcode);
	    }
	}

	output.writeShort(exceptionHandlers.length);
	for (int i=0; i< exceptionHandlers.length; i++) {
	    output.writeShort(exceptionHandlers[i].start.addr);
	    output.writeShort(exceptionHandlers[i].end.nextByAddr.addr);
	    output.writeShort(exceptionHandlers[i].catcher.addr);
	    output.writeShort((exceptionHandlers[i].type == null) ? 0
			      : gcp.putClassRef(exceptionHandlers[i].type));
	}
	output.writeShort(0); // No Attributes;
    }

    public int getMaxStack() {
        return maxStack;
    }

    public int getMaxLocals() {
        return maxLocals;
    }

    public Instruction getFirstInstr() {
	return firstInstr;
    }
    
    public Handler[] getExceptionHandlers() {
	return exceptionHandlers;
    }

    public void setExceptionHandlers(Handler[] handlers) {
        exceptionHandlers = handlers;
    }
}
