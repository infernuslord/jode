/* BytecodeInfo Copyright (C) 1999 Jochen Hoenicke.
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
import jode.GlobalOptions;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Vector;
import java.util.Enumeration;

/**
 * This class represents the byte code of a method.  Each instruction is
 * stored in an Instruction instance.
 *
 * We canocalize some opcodes:  wide opcodes are mapped to short ones,
 * opcodes that load a constant are mapped to opc_ldc or opc_ldc2_w, and
 * opc_xload_x / opc_xstore_x opcodes are mapped to opc_xload / opc_xstore.
 */
public class BytecodeInfo extends BinaryInfo implements Opcodes {

    MethodInfo methodInfo;

    ConstantPool cp;
    int maxStack, maxLocals;
    int codeLength;
    Instruction firstInstr = null;
    Handler[] exceptionHandlers;
    LocalVariableInfo[] lvt;
    LineNumber[] lnt;

    public BytecodeInfo(MethodInfo mi) {
	methodInfo = mi;
    }

    private final static Object[] constants = {
	null, 
	new Integer(-1), new Integer(0), new Integer(1), 
	new Integer(2), new Integer(3), new Integer(4), new Integer(5),
	new Long(0), new Long(1), 
	new Float(0), new Float(1), new Float(2),
	new Double(0), new Double(1)
    };

    protected void readAttribute(String name, int length, ConstantPool cp,
				 DataInputStream input, 
				 int howMuch) throws IOException {
	if ((howMuch & ALL_ATTRIBUTES) != 0 
	    && name.equals("LocalVariableTable")) {
	    if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_LVT) != 0) 
		GlobalOptions.err.println("LocalVariableTable of "+methodInfo.clazzInfo.getName() + "." + methodInfo.getName());
            int count = input.readUnsignedShort();
	    if (length != 2 + count * 10) {
		if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_LVT) != 0) 
		    GlobalOptions.err.println("Illegal LVT length, ignoring it");
		return;
	    }
	    lvt = new LocalVariableInfo[count];
            for (int i=0; i < count; i++) {
		lvt[i] = new LocalVariableInfo();
                int start  = input.readUnsignedShort();
                int end    = start + input.readUnsignedShort();
		int nameIndex = input.readUnsignedShort();
		int typeIndex = input.readUnsignedShort();
		int slot = input.readUnsignedShort();
		Instruction startInstr, endInstr;
		for (startInstr = firstInstr; 
		     startInstr.addr < start && startInstr != null;
		     startInstr = startInstr.nextByAddr) {
		    /* empty */
		}
		endInstr = startInstr;
		if (startInstr != null) {
		    while (endInstr.nextByAddr != null 
			   && endInstr.nextByAddr.addr < end)
			endInstr = endInstr.nextByAddr;
		}
		if (startInstr == null
		    || startInstr.addr != start
		    || endInstr == null
		    || endInstr.addr + endInstr.length != end
		    || nameIndex == 0 || typeIndex == 0
		    || slot >= maxLocals
		    || cp.getTag(nameIndex) != cp.UTF8
		    || cp.getTag(typeIndex) != cp.UTF8) {

		    // This is probably an evil lvt as created by HashJava
		    // simply ignore it.
		    if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_LVT) != 0) 
			GlobalOptions.err.println("Illegal entry, ignoring LVT");
		    lvt = null;
		    return;
		}
		lvt[i].start = startInstr;
		lvt[i].end = endInstr;
		lvt[i].name = cp.getUTF8(nameIndex);
                lvt[i].type = cp.getUTF8(typeIndex);
                lvt[i].slot = slot;
                if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_LVT) != 0)
                    GlobalOptions.err.println("\t" + lvt[i].name + ": "
					   + lvt[i].type
					   +" range "+start+" - "+end
					   +" slot "+slot);
            }
	} else if ((howMuch & ALL_ATTRIBUTES) != 0 
		   && name.equals("LineNumberTable")) {
	    int count = input.readUnsignedShort();
	    if (length != 2 + count * 4) {
		GlobalOptions.err.println
		    ("Illegal LineNumberTable, ignoring it");
		return;
	    }
	    lnt = new LineNumber[count];
	    for (int i = 0; i < count; i++) {
		lnt[i] = new LineNumber();
		int start = input.readUnsignedShort();
		Instruction startInstr;
		for (startInstr = firstInstr; 
		     startInstr.addr < start && startInstr != null;
		     startInstr = startInstr.nextByAddr) {
		    /* empty */
		}
		if (startInstr == null
		    || startInstr.addr != start) {
		    GlobalOptions.err.println
			("Illegal entry, ignoring LineNumberTable table");
		    lnt = null;
		    return;
		}
		lnt[i].start = startInstr;
		lnt[i].linenr = input.readUnsignedShort();
	    }
	} else
	    super.readAttribute(name, length, cp, input, howMuch);
    }

    public void read(ConstantPool cp, 
		     DataInputStream input) throws IOException {
	this.cp = cp;
        maxStack = input.readUnsignedShort();
        maxLocals = input.readUnsignedShort();
        codeLength = input.readInt();
	Instruction[] instrs = new Instruction[codeLength];
	int[][] succAddrs = new int[codeLength][];
	int[] predcounts = new int[codeLength];
	{
	    int addr = 0;
	    Instruction lastInstr = null;
	    while (addr < codeLength) {
		Instruction instr = new Instruction(this);
		if (lastInstr != null) {
		    lastInstr.nextByAddr = instr;
		    instr.prevByAddr = lastInstr;
		}
		instrs[addr] = instr;
		instr.addr = addr;
		lastInstr = instr;

		int opcode = input.readUnsignedByte();
		if ((GlobalOptions.debuggingFlags
		     & GlobalOptions.DEBUG_BYTECODE) != 0) 
		if ((GlobalOptions.debuggingFlags
		     & GlobalOptions.DEBUG_BYTECODE) != 0) 
		    GlobalOptions.err.print(addr+": "+opcodeString[opcode]);

		instr.opcode = opcode;
		switch (opcode) {
		case opc_wide: {
		    int wideopcode = input.readUnsignedByte();
		    instr.opcode = wideopcode;
		    switch (wideopcode) {
		    case opc_iload: case opc_fload: case opc_aload:
		    case opc_istore: case opc_fstore: case opc_astore:
			instr.localSlot = input.readUnsignedShort();
			if (instr.localSlot >= maxLocals)
			    throw new ClassFormatError
				("Invalid local slot "+instr.localSlot);
			instr.length = 4;
			if ((GlobalOptions.debuggingFlags
			     & GlobalOptions.DEBUG_BYTECODE) != 0) 
			    GlobalOptions.err.print(" "+opcodeString[wideopcode]
						 +" "+instr.localSlot);
			break;
		    case opc_lload: case opc_dload:
		    case opc_lstore: case opc_dstore:
			instr.localSlot = input.readUnsignedShort();
			if (instr.localSlot >= maxLocals-1)
			    throw new ClassFormatError
				("Invalid local slot "+instr.localSlot);
			instr.length = 4;
			if ((GlobalOptions.debuggingFlags
			     & GlobalOptions.DEBUG_BYTECODE) != 0) 
			    GlobalOptions.err.print(" "+opcodeString[wideopcode]
						 +" "+instr.localSlot);
			break;
		    case opc_ret:
			instr.localSlot = input.readUnsignedShort();
			if (instr.localSlot >= maxLocals)
			    throw new ClassFormatError
				("Invalid local slot "+instr.localSlot);
			instr.length = 4;
			instr.alwaysJumps = true;
			if ((GlobalOptions.debuggingFlags
			     & GlobalOptions.DEBUG_BYTECODE) != 0) 
			    GlobalOptions.err.print(" ret "+instr.localSlot);
			break;
			
		    case opc_iinc:
			instr.localSlot = input.readUnsignedShort();
			if (instr.localSlot >= maxLocals)
			    throw new ClassFormatError
				("Invalid local slot "+instr.localSlot);
			instr.intData = input.readShort();
			instr.length = 6;
			if ((GlobalOptions.debuggingFlags
			     & GlobalOptions.DEBUG_BYTECODE) != 0) 
			    GlobalOptions.err.print(" iinc "+instr.localSlot
						 +" "+instr.intData);
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
		    if (instr.localSlot >= maxLocals)
			throw new ClassFormatError
			    ("Invalid local slot "+instr.localSlot);
		    instr.length = 1;
		    break;
		case opc_istore_0: case opc_istore_1: 
		case opc_istore_2: case opc_istore_3:
		case opc_fstore_0: case opc_fstore_1:
		case opc_fstore_2: case opc_fstore_3:
		case opc_astore_0: case opc_astore_1:
		case opc_astore_2: case opc_astore_3:
		    instr.opcode = opc_istore + (opcode-opc_istore_0)/4;
		    instr.localSlot = (opcode-opc_istore_0) & 3;
		    if (instr.localSlot >= maxLocals)
			throw new ClassFormatError
			    ("Invalid local slot "+instr.localSlot);
		    instr.length = 1;
		    break;
		case opc_lstore_0: case opc_lstore_1: 
		case opc_lstore_2: case opc_lstore_3:
		case opc_dstore_0: case opc_dstore_1:
		case opc_dstore_2: case opc_dstore_3:
		    instr.opcode = opc_istore + (opcode-opc_istore_0)/4;
		    instr.localSlot = (opcode-opc_istore_0) & 3;
		    if (instr.localSlot >= maxLocals-1)
			throw new ClassFormatError
			    ("Invalid local slot "+instr.localSlot);
		    instr.length = 1;
		    break;
		case opc_iload: case opc_fload: case opc_aload:
		case opc_istore: case opc_fstore: case opc_astore:
		    instr.localSlot = input.readUnsignedByte();
		    if (instr.localSlot >= maxLocals)
			throw new ClassFormatError
			    ("Invalid local slot "+instr.localSlot);
		    instr.length = 2;
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+instr.localSlot);
		    break;
		case opc_lstore: case opc_dstore:
		case opc_lload: case opc_dload:
		    instr.localSlot = input.readUnsignedByte();
		    if (instr.localSlot >= maxLocals - 1)
			throw new ClassFormatError
			    ("Invalid local slot "+instr.localSlot);
		    instr.length = 2;
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+instr.localSlot);
		    break;

		case opc_ret:
		    instr.localSlot = input.readUnsignedByte();
		    if (instr.localSlot >= maxLocals)
			throw new ClassFormatError
			    ("Invalid local slot "+instr.localSlot);
		    instr.alwaysJumps = true;
		    instr.length = 2;
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+instr.localSlot);
		    break;

		case opc_aconst_null:
		case opc_iconst_m1: 
		case opc_iconst_0: case opc_iconst_1: case opc_iconst_2:
		case opc_iconst_3: case opc_iconst_4: case opc_iconst_5:
		case opc_fconst_0: case opc_fconst_1: case opc_fconst_2:
		    instr.objData = constants[instr.opcode - opc_aconst_null];
		    instr.opcode = opc_ldc;
		    instr.length = 1;
		    break;
		case opc_lconst_0: case opc_lconst_1:
		case opc_dconst_0: case opc_dconst_1:
		    instr.objData = constants[instr.opcode - opc_aconst_null];
		    instr.opcode = opc_ldc2_w;
		    instr.length = 1;
		    break;
		case opc_bipush:
		    instr.opcode = opc_ldc;
		    instr.objData = new Integer(input.readByte());
		    instr.length = 2;
		    break;
		case opc_sipush:
		    instr.opcode = opc_ldc;
		    instr.objData = new Integer(input.readShort());
		    instr.length = 3;
		    break;
		case opc_ldc: {
		    int index = input.readUnsignedByte();
		    int tag = cp.getTag(index);
		    if (tag != cp.STRING
			 && tag != cp.INTEGER && tag != cp.FLOAT)
			throw new ClassFormatException
			    ("wrong constant tag: "+tag);
		    instr.objData = cp.getConstant(index);
		    instr.length = 2;
		    break;
		}
		case opc_ldc_w: {
		    int index = input.readUnsignedShort();
		    int tag = cp.getTag(index);
		    if (tag != cp.STRING
			 && tag != cp.INTEGER && tag != cp.FLOAT)
			throw new ClassFormatException
			    ("wrong constant tag: "+tag);
		    instr.opcode = opc_ldc;
		    instr.objData = cp.getConstant(index);
		    instr.length = 3;
		    break;
		}
		case opc_ldc2_w: {
		    int index = input.readUnsignedShort();
		    int tag = cp.getTag(index);
		    if (tag != cp.LONG && tag != cp.DOUBLE)
			throw new ClassFormatException
			    ("wrong constant tag: "+tag);
		    instr.objData = cp.getConstant(index);
		    instr.length = 3;
		    break;
		}
		case opc_iinc:
		    instr.localSlot = input.readUnsignedByte();
		    if (instr.localSlot >= maxLocals)
			throw new ClassFormatError
			    ("Invalid local slot "+instr.localSlot);
		    instr.intData = input.readByte();
		    instr.length = 3;
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+instr.localSlot
					     +" "+instr.intData);
		    break;

		case opc_goto:
		case opc_jsr:
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
		    succAddrs[addr] = new int[1];
		    succAddrs[addr][0] = addr+input.readShort();
		    predcounts[succAddrs[addr][0]]++;
		    instr.length = 3;
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+succAddrs[addr][0]);
		    break;

		case opc_goto_w:
		case opc_jsr_w:
		    instr.alwaysJumps = true;
		    instr.opcode -= opc_goto_w - opc_goto;
		    succAddrs[addr] = new int[1];
		    succAddrs[addr][0] = addr+input.readInt();
		    predcounts[succAddrs[addr][0]]++;
		    instr.length = 5;
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+succAddrs[addr][0]);
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
			predcounts[succAddrs[addr][i]]++;
		    }
		    succAddrs[addr][high-low+1] = addr + def;
		    predcounts[addr + def]++;
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
			predcounts[succAddrs[addr][i]]++;
		    }
		    succAddrs[addr][npairs] = addr + def;
		    predcounts[addr + def]++;
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
		case opc_invokevirtual: {
		    int index = input.readUnsignedShort();
		    int tag = cp.getTag(index);
		    if (instr.opcode < opc_invokevirtual) {
			if (tag != cp.FIELDREF)
			    throw new ClassFormatException
				("field tag mismatch: "+tag);
		    } else {
			if (tag != cp.METHODREF)
			    throw new ClassFormatException
				("method tag mismatch: "+tag);
		    }
		    Reference ref = cp.getRef(index);
		    if (ref.getName().charAt(0) == '<'
			&& (!ref.getName().equals("<init>")
			    || opcode != opc_invokespecial))
			throw new ClassFormatException
			    ("Illegal call of special method/field "+ref);
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+ref);
		    instr.objData = ref;
		    instr.length = 3;
		    break;
		}
		case opc_invokeinterface: {
		    int index = input.readUnsignedShort();
		    int tag = cp.getTag(index);
		    if (tag != cp.INTERFACEMETHODREF)
			throw new ClassFormatException
			    ("interface tag mismatch: "+tag);
		    Reference ref = cp.getRef(index);
		    if (ref.getName().charAt(0) == '<')
			throw new ClassFormatException
			    ("Illegal call of special method "+ref);
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+ref);
		    instr.objData = ref;
		    instr.intData = input.readUnsignedByte();
		    if (input.readUnsignedByte() != 0)
			throw new ClassFormatException
			    ("Illegal call of special method "+ref);
			
		    instr.length = 5;
		    break;
		}

		case opc_new:
		case opc_checkcast:
		case opc_instanceof: {
		    String type = cp.getClassType(input.readUnsignedShort());
		    if (opcode == opc_new && type.charAt(0) == '[')
			throw new ClassFormatException
			    ("Can't create array with opc_new");
		    instr.objData = type;
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+instr.objData);
		    instr.length = 3;
		    break;
		}
		case opc_multianewarray:
		    instr.objData = cp.getClassType(input.readUnsignedShort());
		    instr.intData = input.readUnsignedByte();
		    for (int i=0; i<instr.intData; i++)
			if (((String)instr.objData).charAt(i) != '[')
			    throw new ClassFormatException
				("multianewarray called for non array:"
				 + instr.getDescription());
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+instr.objData
					     +" "+instr.intData);
		    instr.length = 4;
		    break;
		case opc_anewarray: {
		    String type = cp.getClassType(input.readUnsignedShort());
		    instr.opcode = opc_multianewarray;
		    instr.objData = ("["+type).intern();
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+instr.objData);
		    instr.intData = 1;
		    instr.length = 3;
		    break;
		}
		case opc_newarray: {
		    char sig = newArrayTypes.charAt
			(input.readUnsignedByte()-4);
		    instr.opcode = opc_multianewarray;
		    instr.objData = new String (new char[] { '[', sig });
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+instr.objData);
		    instr.intData = 1;
		    instr.length = 2;
		    break;
		}
		
		default:
		    if (opcode == opc_xxxunusedxxx
			|| opcode >= opc_breakpoint)
			throw new ClassFormatError("Invalid opcode "+opcode);
		    else
			instr.length = 1;
		}
		addr += lastInstr.length;
		if ((GlobalOptions.debuggingFlags
		     & GlobalOptions.DEBUG_BYTECODE) != 0) 
		    GlobalOptions.err.println();
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
		    int succAddr = succAddrs[addr][i];
		    instr.succs[i] = instrs[succAddr];
		    if (instr.succs[i].preds == null) 
			instr.succs[i].preds
			    = new Instruction[predcounts[succAddr]];
		    instr.succs[i].preds[--predcounts[succAddr]] = instr;
		}
	    }
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

    public void dumpCode(java.io.PrintWriter output) {
	for (Instruction instr = firstInstr; 
	     instr != null; instr = instr.nextByAddr) {
	    output.println(instr.getDescription() + " "
			   + Integer.toHexString(hashCode()));
	    if (instr.succs != null) {
		output.print("\tsuccs: "+instr.succs[0]);
		for (int i = 1; i < instr.succs.length; i++)
		    output.print(", "+instr.succs[i]);
		output.println();
	    }
	    if (instr.preds != null) {
		output.print("\tpreds: " + instr.preds[0]);
		for (int i=1; i < instr.preds.length; i++)
		    output.print(", " + instr.preds[i]);
		output.println();
	    }
	}
	for (int i=0; i< exceptionHandlers.length; i++) {
	    output.println("catch " + exceptionHandlers[i].type 
			   + " from " + exceptionHandlers[i].start 
			   + " to " + exceptionHandlers[i].end
			   + " catcher " + exceptionHandlers[i].catcher);
	}
    }

    public void reserveSmallConstants(GrowableConstantPool gcp) {
    next_instr:
	for (Instruction instr = firstInstr; 
	     instr != null; instr = instr.nextByAddr) {
	    if (instr.opcode == opc_ldc) {
		if (instr.objData == null)
		    continue next_instr;
		for (int i=1; i < constants.length; i++) {
		    if (instr.objData.equals(constants[i]))
			continue next_instr;
		}
		if (instr.objData instanceof Integer) {
		    int value = ((Integer) instr.objData).intValue();
		    if (value >= Short.MIN_VALUE
			&& value <= Short.MAX_VALUE)
			continue next_instr;
		}
		gcp.reserveConstant(instr.objData);
	    }
	}
    }

    public void prepareWriting(GrowableConstantPool gcp) {
	/* Recalculate addr and length */
	int addr = 0;
    next_instr:
	for (Instruction instr = firstInstr; 
	     instr != null; addr += instr.length, instr = instr.nextByAddr) {
	    instr.addr = addr;
	    if (instr.opcode == opc_ldc
		|| instr.opcode == opc_ldc2_w) {
		if (instr.objData == null) {
		    instr.length = 1;
		    continue next_instr;
		}
		for (int i=1; i < constants.length; i++) {
		    if (instr.objData.equals(constants[i])) {
			instr.length = 1;
			continue next_instr;
		    }
		}
		if (instr.opcode == opc_ldc2_w) {
		    gcp.putLongConstant(instr.objData);
		    instr.length = 3;
		    continue;
		}
		if (instr.objData instanceof Integer) {
		    int value = ((Integer) instr.objData).intValue();
		    if (value >= Byte.MIN_VALUE
			&& value <= Byte.MAX_VALUE) {
			instr.length = 2;
			continue;
		    } else if (value >= Short.MIN_VALUE
			       && value <= Short.MAX_VALUE) {
			instr.length = 3;
			continue;
		    }
		}
		if (gcp.putConstant(instr.objData) < 256) {
		    instr.length = 2;
		} else {
		    instr.length = 3;
		}
	    } else if (instr.localSlot != -1) {
		if (instr.opcode == opc_iinc) {
		    if (instr.localSlot < 256 
			&& instr.intData >= Byte.MIN_VALUE 
			&& instr.intData <= Byte.MAX_VALUE)
			instr.length = 3;
		    else
			instr.length = 6;
		} else {
		    if (instr.opcode != opc_ret && instr.localSlot < 4)
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
	    } else if (instr.opcode == opc_multianewarray) {
		if (instr.intData == 1) {
		    String clazz = ((String) instr.objData).substring(1);
		    if (newArrayTypes.indexOf(clazz.charAt(0))
			!= -1) {
			instr.length = 2;
		    } else {
			gcp.putClassType(clazz);
			instr.length = 3;
		    }
		} else {
		    gcp.putClassType((String)instr.objData);
		    instr.length = 4;
		}
	    } else if (instr.opcode >= opc_getstatic
		       && instr.opcode <= opc_invokeinterface) {
		int tag = (instr.opcode <= opc_putfield ? gcp.FIELDREF
			   : instr.opcode <= opc_invokestatic ? gcp.METHODREF
			   : gcp.INTERFACEMETHODREF);
		gcp.putRef(tag, (Reference) instr.objData);
	    } else if (instr.opcode == opc_new
		       || instr.opcode == opc_checkcast
		       || instr.opcode == opc_instanceof) {
		gcp.putClassType((String) instr.objData);
	    }
	}
	codeLength = addr;
	for (int i=0; i< exceptionHandlers.length; i++)
	    if (exceptionHandlers[i].type != null)
		gcp.putClassName(exceptionHandlers[i].type);
	if (lvt != null) {
	    gcp.putUTF8("LocalVariableTable");
            for (int i=0; i < lvt.length; i++) {
		gcp.putUTF8(lvt[i].name);
		gcp.putUTF8(lvt[i].type);
            }
	}
	if (lnt != null)
	    gcp.putUTF8("LineNumberTable");
	prepareAttributes(gcp);
    }

    protected int getKnownAttributeCount() {
	int count = 0;
	if (lvt != null)
	    count++;
	if (lnt != null)
	    count++;
	return count;
    }

    public void writeKnownAttributes(GrowableConstantPool gcp,
				     DataOutputStream output) 
	throws IOException {
	if (lvt != null) {
	    output.writeShort(gcp.putUTF8("LocalVariableTable"));
            int count = lvt.length;
	    int length = 2 + 10 * count;
	    output.writeInt(length);
	    output.writeShort(count);
            for (int i=0; i < count; i++) {
		output.writeShort(lvt[i].start.addr);
		output.writeShort(lvt[i].end.addr + lvt[i].end.length
				  - lvt[i].start.addr);
		output.writeShort(gcp.putUTF8(lvt[i].name));
		output.writeShort(gcp.putUTF8(lvt[i].type));
		output.writeShort(lvt[i].slot);
            }
	}
	if (lnt != null) {
	    output.writeShort(gcp.putUTF8("LineNumberTable"));
            int count = lnt.length;
	    int length = 2 + 4 * count;
	    output.writeInt(length);
	    output.writeShort(count);
            for (int i=0; i < count; i++) {
		output.writeShort(lnt[i].start.addr);
		output.writeShort(lnt[i].linenr);
            }
	}
    }

    public void write(GrowableConstantPool gcp, 
		      DataOutputStream output) throws IOException {
	output.writeShort(maxStack);
	output.writeShort(maxLocals);
	output.writeInt(codeLength);
	for (Instruction instr = firstInstr; 
	     instr != null; instr = instr.nextByAddr) {
        switch_opc:
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
	    case opc_ldc2_w:
		if (instr.objData == null) {
		    output.writeByte(opc_aconst_null);
		    instr.length = 1;
		    break switch_opc;
		}
		for (int i=1; i < constants.length; i++) {
		    if (instr.objData.equals(constants[i])) {
			output.writeByte(opc_aconst_null + i);
			break switch_opc;
		    }
		}
		if (instr.opcode == opc_ldc2_w) {
		    output.writeByte(instr.opcode);
		    output.writeShort(gcp.putLongConstant(instr.objData));
		} else {
		    if (instr.objData instanceof Integer) {
			int value = ((Integer) instr.objData).intValue();
			if (value >= Byte.MIN_VALUE
			    && value <= Byte.MAX_VALUE) {
			    
			    output.writeByte(opc_bipush);
			    output.writeByte(((Integer)instr.objData)
					     .intValue());
			    break switch_opc;
			} else if (value >= Short.MIN_VALUE
				   && value <= Short.MAX_VALUE) {
			    output.writeByte(opc_sipush);
			    output.writeShort(((Integer)instr.objData)
					      .intValue());
			    break switch_opc;
			}
		    }
		    if (instr.length == 2) {
			output.writeByte(opc_ldc);
			output.writeByte(gcp.putConstant(instr.objData));
		    } else {
			output.writeByte(opc_ldc_w);
			output.writeShort(gcp.putConstant(instr.objData));
		    }
		}
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
					     (Reference) instr.objData));
		break;

	    case opc_invokespecial:
	    case opc_invokestatic:
	    case opc_invokeinterface:
	    case opc_invokevirtual:
		output.writeByte(instr.opcode);
		if (instr.opcode == opc_invokeinterface) {
		    output.writeShort(gcp.putRef(gcp.INTERFACEMETHODREF, 
						 (Reference) instr.objData));
		    output.writeByte(instr.intData);
		    output.writeByte(0);
		} else 
		    output.writeShort(gcp.putRef(gcp.METHODREF, 
						 (Reference) instr.objData));
		break;
	    case opc_new:
	    case opc_checkcast:
	    case opc_instanceof:
		output.writeByte(instr.opcode);
		output.writeShort(gcp.putClassType((String) instr.objData));
		break;
	    case opc_multianewarray:
		if (instr.intData == 1) {
		    String clazz = ((String) instr.objData).substring(1);
		    int index = newArrayTypes.indexOf(clazz.charAt(0));
		    if (index != -1) {
			output.writeByte(opc_newarray);
			output.writeByte(index + 4);
		    } else {
			output.writeByte(opc_anewarray);
			output.writeShort(gcp.putClassType(clazz));
		    }
		} else {
		    output.writeByte(instr.opcode);
		    output.writeShort(gcp.putClassType((String)instr.objData));
		    output.writeByte(instr.intData);
		}
		break;

	    default:
		if (instr.opcode == opc_xxxunusedxxx
		    || instr.opcode >= opc_breakpoint)
		    throw new ClassFormatError("Invalid opcode "+instr.opcode);
		else if (instr.length != 1)
		    throw new ClassFormatError("Length differs at "
					       + instr.addr + " opcode "
					       + opcodeString[instr.opcode]);
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
			      : gcp.putClassName(exceptionHandlers[i].type));
	}
	writeAttributes(gcp, output);
    }

    public int getSize() {
	/* maxStack:    2
	 * maxLocals:   2
	 * code:        4 + codeLength
	 * exc count:   2
	 * exceptions:  n * 8
	 * attributes:
	 *  lvt_name:    2
	 *  lvt_length:  4
	 *  lvt_count:   2
	 *  lvt_entries: n * 10
	 * attributes:
	 *  lnt_name:    2
	 *  lnt_length:  4
	 *  lnt_count:   2
	 *  lnt_entries: n * 4
	 */
	int size = 0;
	if (lvt != null)
	    size += 8 + lvt.length * 10;
	if (lnt != null)
	    size += 8 + lnt.length * 4;
	return 10 + codeLength + exceptionHandlers.length * 8
	    + getAttributeSize() + size;
    }

    public int getMaxStack() {
        return maxStack;
    }

    public int getMaxLocals() {
        return maxLocals;
    }

    public MethodInfo getMethodInfo() {
	return methodInfo;
    }

    public Instruction getFirstInstr() {
	return firstInstr;
    }
    
    public Handler[] getExceptionHandlers() {
	return exceptionHandlers;
    }

    public LocalVariableInfo[] getLocalVariableTable() {
	return lvt;
    }

    public LineNumber[] getLineNumberTable() {
	return lnt;
    }

    public void setMaxStack(int ms) {
        maxStack = ms;
    }

    public void setMaxLocals(int ml) {
        maxLocals = ml;
    }

    public void setExceptionHandlers(Handler[] handlers) {
        exceptionHandlers = handlers;
    }

    public void setLocalVariableTable(LocalVariableInfo[] newLvt) {
	lvt = newLvt;
    }

    public void setLineNumberTable(LineNumber[] newLnt) {
	lnt = newLnt;
    }
}
