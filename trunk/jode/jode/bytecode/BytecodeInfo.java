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
import java.util.NoSuchElementException;

///#ifdef JDK12
///import java.util.Collection;
///import java.util.AbstractCollectoin;
///import java.util.Iterator;
///#else
import jode.util.Collection;
import jode.util.AbstractCollection;
import jode.util.Iterator;
///#endif


/**
 * This class represents the byte code of a method.  Each instruction is
 * stored in an Instruction instance.
 *
 * We canonicalize some opcodes:  wide opcodes are mapped to short ones,
 * opcodes that load a constant are mapped to opc_ldc or opc_ldc2_w, and
 * opc_xload_x / opc_xstore_x opcodes are mapped to opc_xload / opc_xstore.
 */
public class BytecodeInfo extends BinaryInfo implements Opcodes {

    MethodInfo methodInfo;

    ConstantPool cp;
    int maxStack, maxLocals;
    int codeLength;
    Instruction firstInstr = null;
    int instructionCount = 0;
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
		     startInstr.getAddr() < start && startInstr != null;
		     startInstr = startInstr.getNextByAddr()) {
		    /* empty */
		}
		endInstr = startInstr;
		if (startInstr != null) {
		    while (endInstr.getNextByAddr() != null 
			   && endInstr.getNextByAddr().getAddr() < end)
			endInstr = endInstr.getNextByAddr();
		}
		if (startInstr == null
		    || startInstr.getAddr() != start
		    || endInstr == null
		    || endInstr.getAddr() + endInstr.getLength() != end
		    || nameIndex == 0 || typeIndex == 0
		    || slot >= maxLocals
		    || cp.getTag(nameIndex) != cp.UTF8
		    || cp.getTag(typeIndex) != cp.UTF8) {

		    // This is probably an evil lvt as created by HashJava
		    // simply ignore it.
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_LVT) != 0) 
			GlobalOptions.err.println
			    ("Illegal entry, ignoring LVT");
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
		     startInstr.getAddr() < start && startInstr != null;
		     startInstr = startInstr.getNextByAddr()) {
		    /* empty */
		}
		if (startInstr == null
		    || startInstr.getAddr() != start) {
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
	    firstInstr = new Instruction(this);
	    instructionCount++;
	    Instruction lastInstr = null;
	    while (addr < codeLength) {
		Instruction instr = lastInstr != null 
		    ? lastInstr.appendInstruction(opc_nop) : firstInstr;

		instrs[addr] = instr;
		lastInstr = instr;

		int opcode = input.readUnsignedByte();
		if ((GlobalOptions.debuggingFlags
		     & GlobalOptions.DEBUG_BYTECODE) != 0) 
		if ((GlobalOptions.debuggingFlags
		     & GlobalOptions.DEBUG_BYTECODE) != 0) 
		    GlobalOptions.err.print(addr+": "+opcodeString[opcode]);

		switch (opcode) {
		case opc_wide: {
		    int wideopcode = input.readUnsignedByte();
		    instr.replaceInstruction(wideopcode);
		    switch (wideopcode) {
		    case opc_iload: case opc_fload: case opc_aload:
		    case opc_istore: case opc_fstore: case opc_astore: {
			int slot = input.readUnsignedShort();
			if (slot >= maxLocals)
			    throw new ClassFormatError
				("Invalid local slot "+slot);
			instr.setLocalSlot(slot);
			instr.setLength(4);
			if ((GlobalOptions.debuggingFlags
			     & GlobalOptions.DEBUG_BYTECODE) != 0) 
			    GlobalOptions.err.print
				(" " + opcodeString[wideopcode] + " " + slot);
			break;
		    }
		    case opc_lload: case opc_dload:
		    case opc_lstore: case opc_dstore: {
			int slot = input.readUnsignedShort();
			if (slot >= maxLocals-1)
			    throw new ClassFormatError
				("Invalid local slot "+slot);
			instr.setLocalSlot(slot);
			instr.setLength(4);
			if ((GlobalOptions.debuggingFlags
			     & GlobalOptions.DEBUG_BYTECODE) != 0) 
			    GlobalOptions.err.print(" "+opcodeString[wideopcode]
						 +" "+slot);
			break;
		    }
		    case opc_ret: {
			int slot = input.readUnsignedShort();
			if (slot >= maxLocals)
			    throw new ClassFormatError
				("Invalid local slot "+slot);
			instr.setLocalSlot(slot);
			instr.setLength(4);
			if ((GlobalOptions.debuggingFlags
			     & GlobalOptions.DEBUG_BYTECODE) != 0) 
			    GlobalOptions.err.print(" ret "+slot);
			break;
		    }
		    case opc_iinc: {
			int slot = input.readUnsignedShort();
			if (slot >= maxLocals)
			    throw new ClassFormatError
				("Invalid local slot "+slot);
			instr.setLocalSlot(slot);
			instr.setIntData(input.readShort());
			instr.setLength(6);
			if ((GlobalOptions.debuggingFlags
			     & GlobalOptions.DEBUG_BYTECODE) != 0) 
			    GlobalOptions.err.print(" iinc "+slot
						    +" "+instr.getIntData());
			break;
		    }
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
		case opc_aload_2: case opc_aload_3: {
		    int slot = (opcode-opc_iload_0) & 3;
		    if (slot >= maxLocals)
			throw new ClassFormatError
			    ("Invalid local slot "+slot);
		    instr.replaceInstruction(opc_iload + 
					     (opcode-opc_iload_0)/4);
		    instr.setLocalSlot(slot);
		    instr.setLength(1);
		    break;
		}
		case opc_istore_0: case opc_istore_1: 
		case opc_istore_2: case opc_istore_3:
		case opc_fstore_0: case opc_fstore_1:
		case opc_fstore_2: case opc_fstore_3:
		case opc_astore_0: case opc_astore_1:
		case opc_astore_2: case opc_astore_3: {
		    int slot = (opcode-opc_istore_0) & 3;
		    if (slot >= maxLocals)
			throw new ClassFormatError
			    ("Invalid local slot "+slot);
		    instr.replaceInstruction(opc_istore + 
					     (opcode-opc_istore_0)/4);
		    instr.setLocalSlot(slot);
		    instr.setLength(1);
		    break;
		}
		case opc_lstore_0: case opc_lstore_1: 
		case opc_lstore_2: case opc_lstore_3:
		case opc_dstore_0: case opc_dstore_1:
		case opc_dstore_2: case opc_dstore_3: {
		    int slot = (opcode-opc_istore_0) & 3;
		    if (slot >= maxLocals-1)
			throw new ClassFormatError
			    ("Invalid local slot "+slot);
		    instr.replaceInstruction(opc_istore
					     + (opcode-opc_istore_0)/4);
		    instr.setLocalSlot(slot);
		    instr.setLength(1);
		    break;
		}
		case opc_iload: case opc_fload: case opc_aload:
		case opc_istore: case opc_fstore: case opc_astore: {
		    int slot = input.readUnsignedByte();
		    if (slot >= maxLocals)
			throw new ClassFormatError
			    ("Invalid local slot "+slot);
		    instr.replaceInstruction(opcode);
		    instr.setLocalSlot(slot);
		    instr.setLength(2);
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+slot);
		    break;
		}
		case opc_lstore: case opc_dstore:
		case opc_lload: case opc_dload: {
		    int slot = input.readUnsignedByte();
		    if (slot >= maxLocals - 1)
			throw new ClassFormatError
			    ("Invalid local slot "+slot);
		    instr.replaceInstruction(opcode);
		    instr.setLocalSlot(slot);
		    instr.setLength(2);
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+slot);
		    break;
		}
		case opc_ret: {
		    int slot = input.readUnsignedByte();
		    if (slot >= maxLocals)
			throw new ClassFormatError
			    ("Invalid local slot "+slot);
		    instr.replaceInstruction(opcode);
		    instr.setLocalSlot(slot);
		    instr.setLength(2);
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+slot);
		    break;
		}
		case opc_aconst_null:
		case opc_iconst_m1: 
		case opc_iconst_0: case opc_iconst_1: case opc_iconst_2:
		case opc_iconst_3: case opc_iconst_4: case opc_iconst_5:
		case opc_fconst_0: case opc_fconst_1: case opc_fconst_2:
		    instr.replaceInstruction(opc_ldc);
		    instr.setConstant
			(constants[opcode - opc_aconst_null]);
		    instr.setLength(1);
		    break;
		case opc_lconst_0: case opc_lconst_1:
		case opc_dconst_0: case opc_dconst_1:
		    instr.replaceInstruction(opc_ldc2_w);
		    instr.setConstant
			(constants[opcode - opc_aconst_null]);
		    instr.setLength(1);
		    break;
		case opc_bipush:
		    instr.replaceInstruction(opc_ldc);
		    instr.setConstant(new Integer(input.readByte()));
		    instr.setLength(2);
		    break;
		case opc_sipush:
		    instr.replaceInstruction(opc_ldc);
		    instr.setConstant(new Integer(input.readShort()));
		    instr.setLength(3);
		    break;
		case opc_ldc: {
		    int index = input.readUnsignedByte();
		    int tag = cp.getTag(index);
		    if (tag != cp.STRING
			 && tag != cp.INTEGER && tag != cp.FLOAT)
			throw new ClassFormatException
			    ("wrong constant tag: "+tag);
		    instr.replaceInstruction(opcode);
		    instr.setConstant(cp.getConstant(index));
		    instr.setLength(2);
		    break;
		}
		case opc_ldc_w: {
		    int index = input.readUnsignedShort();
		    int tag = cp.getTag(index);
		    if (tag != cp.STRING
			 && tag != cp.INTEGER && tag != cp.FLOAT)
			throw new ClassFormatException
			    ("wrong constant tag: "+tag);
		    instr.replaceInstruction(opc_ldc);
		    instr.setConstant(cp.getConstant(index));
		    instr.setLength(3);
		    break;
		}
		case opc_ldc2_w: {
		    int index = input.readUnsignedShort();
		    int tag = cp.getTag(index);
		    if (tag != cp.LONG && tag != cp.DOUBLE)
			throw new ClassFormatException
			    ("wrong constant tag: "+tag);
		    instr.replaceInstruction(opcode);
		    instr.setConstant(cp.getConstant(index));
		    instr.setLength(3);
		    break;
		}
		case opc_iinc: {
		    int slot = input.readUnsignedByte();
		    if (slot >= maxLocals)
			throw new ClassFormatError
			    ("Invalid local slot "+slot);
		    instr.replaceInstruction(opcode);
		    instr.setLocalSlot(slot);
		    instr.setIntData(input.readByte());
		    instr.setLength(3);
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+slot
						+" "+instr.getIntData());
		    break;
		}
		case opc_goto:
		case opc_jsr:
		case opc_ifeq: case opc_ifne: 
		case opc_iflt: case opc_ifge: 
		case opc_ifgt: case opc_ifle:
		case opc_if_icmpeq: case opc_if_icmpne:
		case opc_if_icmplt: case opc_if_icmpge: 
		case opc_if_icmpgt: case opc_if_icmple:
		case opc_if_acmpeq: case opc_if_acmpne:
		case opc_ifnull: case opc_ifnonnull:
		    instr.replaceInstruction(opcode);
		    instr.setLength(3);
		    succAddrs[addr] = new int[] { addr+input.readShort() };
		    predcounts[succAddrs[addr][0]]++;
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+succAddrs[addr][0]);
		    break;

		case opc_goto_w:
		case opc_jsr_w:
		    instr.replaceInstruction(opcode - (opc_goto_w - opc_goto));
		    instr.setLength(5);
		    succAddrs[addr] = new int[] { addr+input.readInt() };
		    predcounts[succAddrs[addr][0]]++;
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
		    instr.replaceInstruction(opcode);
		    instr.setIntData(low);
		    succAddrs[addr] = new int[high-low+2];
		    for (int i=0; i+low <= high; i++) {
			succAddrs[addr][i] = addr + input.readInt();
			predcounts[succAddrs[addr][i]]++;
		    }
		    succAddrs[addr][high-low+1] = addr + def;
		    predcounts[addr + def]++;
		    instr.setLength(length + 13 + 4 * (high-low+1));
		    break;
		}
		case opc_lookupswitch: {
		    int length = 3-(addr % 4);
		    input.readFully(new byte[length]);
		    int def = input.readInt();
		    int npairs = input.readInt();
		    instr.replaceInstruction(opcode);
		    succAddrs[addr] = new int[npairs + 1];
		    int[] values = new int[npairs];
		    for (int i=0; i < npairs; i++) {
			values[i] = input.readInt();
			succAddrs[addr][i] = addr + input.readInt();
			predcounts[succAddrs[addr][i]]++;
		    }
		    succAddrs[addr][npairs] = addr + def;
		    predcounts[addr + def]++;
		    instr.setValues(values);
		    instr.setLength(length + 9 + 8 * npairs);
		    break;
		}
			    
		case opc_getstatic:
		case opc_getfield:
		case opc_putstatic:
		case opc_putfield:
		case opc_invokespecial:
		case opc_invokestatic:
		case opc_invokevirtual: {
		    int index = input.readUnsignedShort();
		    int tag = cp.getTag(index);
		    if (opcode < opc_invokevirtual) {
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
		    instr.replaceInstruction(opcode);
		    instr.setReference(ref);
		    instr.setLength(3);
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+ref);
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
		    int nargs = input.readUnsignedByte();
		    if (TypeSignature.getArgumentSize(ref.getType())
			!= nargs - 1)
			throw new ClassFormatException
			    ("Interface nargs mismatch: "+ref+" vs. "+nargs);
		    if (input.readUnsignedByte() != 0)
			throw new ClassFormatException
			    ("Interface reserved param not zero");

		    instr.replaceInstruction(opcode);
		    instr.setReference(ref);
		    instr.setLength(5);
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+ref);
		    break;
		}

		case opc_new:
		case opc_checkcast:
		case opc_instanceof: {
		    String type = cp.getClassType(input.readUnsignedShort());
		    if (opcode == opc_new && type.charAt(0) == '[')
			throw new ClassFormatException
			    ("Can't create array with opc_new");
		    instr.replaceInstruction(opcode);
		    instr.setClazzType(type);
		    instr.setLength(3);
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+type);
		    break;
		}
		case opc_multianewarray: {
		    String type = cp.getClassType(input.readUnsignedShort());
		    int dims = input.readUnsignedByte();
		    for (int i=0; i < dims; i++)
			/* Note that since type is a valid type
			 * signature, there must be a non bracket
			 * character, before the string is over.  
			 * So there is no StringIndexOutOfBoundsException.
			 */
			if (type.charAt(i) != '[')
			    throw new ClassFormatException
				("multianewarray called for non array:"
				 + instr.getDescription());
		    instr.replaceInstruction(opcode);
		    instr.setClazzType(type);
		    instr.setIntData(dims);
		    instr.setLength(4);
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" " + type + " " + dims);
		    break;
		}
		case opc_anewarray: {
		    String type = cp.getClassType(input.readUnsignedShort());
		    instr.replaceInstruction(opc_multianewarray);
		    instr.setClazzType(("["+type).intern());
		    instr.setIntData(1);
		    instr.setLength(3);
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+type);
		    break;
		}
		case opc_newarray: {
		    char sig = newArrayTypes.charAt
			(input.readUnsignedByte()-4);
		    String type = new String (new char[] { '[', sig });
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_BYTECODE) != 0) 
			GlobalOptions.err.print(" "+type);
		    instr.replaceInstruction(opc_multianewarray);
		    instr.setClazzType(type);
		    instr.setIntData(1);
		    instr.setLength(2);
		    break;
		}
		
		case opc_nop:
		case opc_iaload: case opc_laload: case opc_faload:
		case opc_daload: case opc_aaload:
		case opc_baload: case opc_caload: case opc_saload:
		case opc_iastore: case opc_lastore: case opc_fastore:
		case opc_dastore: case opc_aastore:
		case opc_bastore: case opc_castore: case opc_sastore:
		case opc_pop: case opc_pop2:
		case opc_dup: case opc_dup_x1: case opc_dup_x2:
		case opc_dup2: case opc_dup2_x1: case opc_dup2_x2:
		case opc_swap:
		case opc_iadd: case opc_ladd: case opc_fadd: case opc_dadd:
		case opc_isub: case opc_lsub: case opc_fsub: case opc_dsub:
		case opc_imul: case opc_lmul: case opc_fmul: case opc_dmul:
		case opc_idiv: case opc_ldiv: case opc_fdiv: case opc_ddiv:
		case opc_irem: case opc_lrem: case opc_frem: case opc_drem:
		case opc_ineg: case opc_lneg: case opc_fneg: case opc_dneg:
		case opc_ishl: case opc_lshl:
		case opc_ishr: case opc_lshr:
		case opc_iushr: case opc_lushr: 
		case opc_iand: case opc_land:
		case opc_ior: case opc_lor: 
		case opc_ixor: case opc_lxor:
		case opc_i2l: case opc_i2f: case opc_i2d:
		case opc_l2i: case opc_l2f: case opc_l2d:
		case opc_f2i: case opc_f2l: case opc_f2d:
		case opc_d2i: case opc_d2l: case opc_d2f:
		case opc_i2b: case opc_i2c: case opc_i2s:
		case opc_lcmp: case opc_fcmpl: case opc_fcmpg:
		case opc_dcmpl: case opc_dcmpg:
		case opc_ireturn: case opc_lreturn: 
		case opc_freturn: case opc_dreturn: case opc_areturn:
		case opc_return: 
		case opc_athrow:
		case opc_arraylength:
		case opc_monitorenter: case opc_monitorexit:
		    instr.replaceInstruction(opcode);
		    instr.setLength(1);
		    break;
		default:
		    throw new ClassFormatError("Invalid opcode "+opcode);
		}
		if ((GlobalOptions.debuggingFlags
		     & GlobalOptions.DEBUG_BYTECODE) != 0) 
		    GlobalOptions.err.println();
		addr += instr.getLength();
	    }
	}
	for (Instruction instr = firstInstr; 
	     instr != null; instr = instr.getNextByAddr()) {
	    int addr = instr.getAddr();
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
		    = instrs[input.readUnsignedShort()].getPrevByAddr();
		exceptionHandlers[i].catcher
		    = instrs[input.readUnsignedShort()];
		int index = input.readUnsignedShort();
		exceptionHandlers[i].type = (index == 0) ? null
		    : cp.getClassName(index);
	    }
	}
	readAttributes(cp, input, FULLINFO);
    }

    public void dumpCode(java.io.PrintWriter output) {
	for (Instruction instr = firstInstr; 
	     instr != null; instr = instr.getNextByAddr()) {
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
	     instr != null; instr = instr.getNextByAddr()) {
	    if (instr.getOpcode() == opc_ldc) {
		Object constant = instr.getConstant();
		if (constant == null)
		    continue next_instr;
		for (int i=1; i < constants.length; i++) {
		    if (constant.equals(constants[i]))
			continue next_instr;
		}
		if (constant instanceof Integer) {
		    int value = ((Integer) constant).intValue();
		    if (value >= Short.MIN_VALUE
			&& value <= Short.MAX_VALUE)
			continue next_instr;
		}
		gcp.reserveConstant(constant);
	    }
	}
    }

    public void prepareWriting(GrowableConstantPool gcp) {
	/* Recalculate addr, length and add all constants to gcp */
	int addr = 0;
	for (Instruction instr = firstInstr; 
	     instr != null; instr = instr.getNextByAddr()) {
	    int opcode = instr.getOpcode();
	    instr.setAddr(addr);
	    int length;
	switch_opc:
	    switch (opcode) {
	    case opc_ldc:
	    case opc_ldc2_w: {
		Object constant = instr.getConstant();
		if (constant == null) {
		    length = 1;
		    break switch_opc;
		}
		for (int i=1; i < constants.length; i++) {
		    if (constant.equals(constants[i])) {
			length = 1;
			break switch_opc;
		    }
		}
		if (opcode == opc_ldc2_w) {
		    gcp.putLongConstant(constant);
		    length = 3;
		    break switch_opc;
		}
		if (constant instanceof Integer) {
		    int value = ((Integer) constant).intValue();
		    if (value >= Byte.MIN_VALUE
			&& value <= Byte.MAX_VALUE) {
			length = 2;
			break switch_opc;
		    } else if (value >= Short.MIN_VALUE
			       && value <= Short.MAX_VALUE) {
			length = 3;
			break switch_opc;
		    }
		}
		if (gcp.putConstant(constant) < 256) {
		    length = 2;
		} else {
		    length = 3;
		}
		break;
	    } 
	    case opc_iload: case opc_lload: 
	    case opc_fload: case opc_dload: case opc_aload:
	    case opc_istore: case opc_lstore: 
	    case opc_fstore: case opc_dstore: case opc_astore:
	    case opc_ret:
	    case opc_iinc: {
		int slot = instr.getLocalSlot();
		if (opcode == opc_iinc) {
		    if (slot < 256 
			&& instr.getIntData() >= Byte.MIN_VALUE 
			&& instr.getIntData() <= Byte.MAX_VALUE)
			length = 3;
		    else
			length = 6;
		} else {
		    if (opcode != opc_ret && slot < 4)
			length = 1;
		    else if (slot < 256)
			length = 2;
		    else 
			length = 4;
		}
		break;
	    } 
	    case opc_tableswitch: {
		length = 3-(addr % 4);
		length += 9 + 4 * instr.succs.length;
		break;
	    }
	    case opc_lookupswitch: {
		length = 3-(addr % 4);
		length += 1 + 8 * instr.succs.length;
		break;
	    }
	    case opc_goto: case opc_jsr: {
		int dist = instr.succs[0].getAddr() - instr.getAddr();
		if (dist < Short.MIN_VALUE || dist > Short.MAX_VALUE) {
		    /* wide goto / jsr */
		    length = 5;
		    break;
		} 
		/* fall through */
	    }
	    case opc_ifeq: case opc_ifne: 
	    case opc_iflt: case opc_ifge: 
	    case opc_ifgt: case opc_ifle:
	    case opc_if_icmpeq: case opc_if_icmpne:
	    case opc_if_icmplt: case opc_if_icmpge: 
	    case opc_if_icmpgt: case opc_if_icmple:
	    case opc_if_acmpeq: case opc_if_acmpne:
	    case opc_ifnull: case opc_ifnonnull:
		length = 3;
		break;
	    case opc_multianewarray: {
		if (instr.getIntData() == 1) {
		    String clazz = instr.getClazzType().substring(1);
		    if (newArrayTypes.indexOf(clazz.charAt(0)) != -1) {
			length = 2;
		    } else {
			gcp.putClassType(clazz);
			length = 3;
		    }
		} else {
		    gcp.putClassType(instr.getClazzType());
		    length = 4;
		}
		break;
	    }
	    case opc_getstatic:
	    case opc_getfield:
	    case opc_putstatic:
	    case opc_putfield:
		gcp.putRef(gcp.FIELDREF, instr.getReference());
		length = 3;
		break;
	    case opc_invokespecial:
	    case opc_invokestatic:
	    case opc_invokevirtual:
		gcp.putRef(gcp.METHODREF, instr.getReference());
		length = 3;
		break;
	    case opc_invokeinterface:
		gcp.putRef(gcp.INTERFACEMETHODREF, instr.getReference());
		length = 5;
		break;
	    case opc_new:
	    case opc_checkcast:
	    case opc_instanceof:
		gcp.putClassType(instr.getClazzType());
		length = 3;
		break;
	    case opc_nop:
	    case opc_iaload: case opc_laload: case opc_faload:
	    case opc_daload: case opc_aaload:
	    case opc_baload: case opc_caload: case opc_saload:
	    case opc_iastore: case opc_lastore: case opc_fastore:
	    case opc_dastore: case opc_aastore:
	    case opc_bastore: case opc_castore: case opc_sastore:
	    case opc_pop: case opc_pop2:
	    case opc_dup: case opc_dup_x1: case opc_dup_x2:
	    case opc_dup2: case opc_dup2_x1: case opc_dup2_x2:
	    case opc_swap:
	    case opc_iadd: case opc_ladd: case opc_fadd: case opc_dadd:
	    case opc_isub: case opc_lsub: case opc_fsub: case opc_dsub:
	    case opc_imul: case opc_lmul: case opc_fmul: case opc_dmul:
	    case opc_idiv: case opc_ldiv: case opc_fdiv: case opc_ddiv:
	    case opc_irem: case opc_lrem: case opc_frem: case opc_drem:
	    case opc_ineg: case opc_lneg: case opc_fneg: case opc_dneg:
	    case opc_ishl: case opc_lshl:
	    case opc_ishr: case opc_lshr:
	    case opc_iushr: case opc_lushr: 
	    case opc_iand: case opc_land:
	    case opc_ior: case opc_lor: 
	    case opc_ixor: case opc_lxor:
	    case opc_i2l: case opc_i2f: case opc_i2d:
	    case opc_l2i: case opc_l2f: case opc_l2d:
	    case opc_f2i: case opc_f2l: case opc_f2d:
	    case opc_d2i: case opc_d2l: case opc_d2f:
	    case opc_i2b: case opc_i2c: case opc_i2s:
	    case opc_lcmp: case opc_fcmpl: case opc_fcmpg:
	    case opc_dcmpl: case opc_dcmpg:
	    case opc_ireturn: case opc_lreturn: 
	    case opc_freturn: case opc_dreturn: case opc_areturn:
	    case opc_return: 
	    case opc_athrow:
	    case opc_arraylength:
	    case opc_monitorenter: case opc_monitorexit:
		length = 1;
		break;
	    default:
		throw new ClassFormatError("Invalid opcode "+opcode);
	    }
	    instr.setLength(length);
	    addr += length;
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
		output.writeShort(lvt[i].start.getAddr());
		output.writeShort(lvt[i].end.getAddr() + lvt[i].end.getLength()
				  - lvt[i].start.getAddr());
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
		output.writeShort(lnt[i].start.getAddr());
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
	     instr != null; instr = instr.getNextByAddr()) {
	    int opcode = instr.getOpcode();
        switch_opc:
	    switch (opcode) {
	    case opc_iload: case opc_lload: 
	    case opc_fload: case opc_dload: case opc_aload:
	    case opc_istore: case opc_lstore: 
	    case opc_fstore: case opc_dstore: case opc_astore: {
		int slot = instr.getLocalSlot();
		if (slot < 4) {
		    if (opcode < opc_istore)
			output.writeByte(opc_iload_0
					 + 4*(opcode-opc_iload)
					 + slot);
		    else
			output.writeByte(opc_istore_0
					 + 4*(opcode-opc_istore)
					 + slot);
		} else if (slot < 256) {
		    output.writeByte(opcode);
		    output.writeByte(slot);
		} else {
		    output.writeByte(opc_wide);
		    output.writeByte(opcode);
		    output.writeShort(slot);
		}
		break;
	    }		
	    case opc_ret: {
		int slot = instr.getLocalSlot();
		if (slot < 256) {
		    output.writeByte(opcode);
		    output.writeByte(slot);
		} else {
		    output.writeByte(opc_wide);
		    output.writeByte(opcode);
		    output.writeShort(slot);
		}
		break;
	    }
	    case opc_ldc:
	    case opc_ldc2_w: {
		Object constant = instr.getConstant();
		if (constant == null) {
		    output.writeByte(opc_aconst_null);
		    break switch_opc;
		}
		for (int i=1; i < constants.length; i++) {
		    if (constant.equals(constants[i])) {
			output.writeByte(opc_aconst_null + i);
			break switch_opc;
		    }
		}
		if (opcode == opc_ldc2_w) {
		    output.writeByte(opcode);
		    output.writeShort(gcp.putLongConstant(constant));
		} else {
		    if (constant instanceof Integer) {
			int value = ((Integer) constant).intValue();
			if (value >= Byte.MIN_VALUE
			    && value <= Byte.MAX_VALUE) {
			    
			    output.writeByte(opc_bipush);
			    output.writeByte(((Integer)constant)
					     .intValue());
			    break switch_opc;
			} else if (value >= Short.MIN_VALUE
				   && value <= Short.MAX_VALUE) {
			    output.writeByte(opc_sipush);
			    output.writeShort(((Integer)constant)
					      .intValue());
			    break switch_opc;
			}
		    }
		    if (instr.getLength() == 2) {
			output.writeByte(opc_ldc);
			output.writeByte(gcp.putConstant(constant));
		    } else {
			output.writeByte(opc_ldc_w);
			output.writeShort(gcp.putConstant(constant));
		    }
		}
		break;
	    }
	    case opc_iinc: {
		int slot = instr.getLocalSlot();
		int incr = instr.getIntData();
		if (instr.getLength() == 3) {
		    output.writeByte(opcode);
		    output.writeByte(slot);
		    output.writeByte(incr);
		} else {
		    output.writeByte(opc_wide);
		    output.writeByte(opcode);
		    output.writeShort(slot);
		    output.writeShort(incr);
		}
		break;
	    }
	    case opc_goto:
	    case opc_jsr:
		if (instr.getLength() == 5) {
		    /* wide goto or jsr */
		    output.writeByte(opcode + (opc_goto_w - opc_goto));
		    output.writeInt(instr.succs[0].getAddr()
				    - instr.getAddr());
		    break;
		}
		/* fall through */
	    case opc_ifeq: case opc_ifne: 
	    case opc_iflt: case opc_ifge: 
	    case opc_ifgt: case opc_ifle:
	    case opc_if_icmpeq: case opc_if_icmpne:
	    case opc_if_icmplt: case opc_if_icmpge: 
	    case opc_if_icmpgt: case opc_if_icmple:
	    case opc_if_acmpeq: case opc_if_acmpne:
	    case opc_ifnull: case opc_ifnonnull:
		output.writeByte(opcode);
		output.writeShort(instr.succs[0].getAddr() 
				  - instr.getAddr());
		break;

	    case opc_tableswitch: {
		output.writeByte(opcode);
		int align = 3-(instr.getAddr() % 4);
		int numcases = instr.succs.length - 1;
		output.write(new byte[align]);
		/* def */
		output.writeInt(instr.succs[numcases].getAddr() - instr.getAddr());
		/* low */
		output.writeInt(instr.getIntData()); 
		/* high */
		output.writeInt(instr.getIntData() + numcases - 1);
		for (int i=0; i < numcases; i++)
		    output.writeInt(instr.succs[i].getAddr() - instr.getAddr());
		break;
	    }
	    case opc_lookupswitch: {
		output.writeByte(opcode);
		int[] values = instr.getValues();
		int align = 3-(instr.getAddr() % 4);
		int numcases = values.length;
		output.write(new byte[align]);
		/* def */
		output.writeInt(instr.succs[numcases].getAddr() - instr.getAddr());
		output.writeInt(numcases);
		for (int i=0; i < numcases; i++) {
		    output.writeInt(values[i]);
		    output.writeInt(instr.succs[i].getAddr() - instr.getAddr());
		}
		break;
	    }

	    case opc_getstatic:
	    case opc_getfield:
	    case opc_putstatic:
	    case opc_putfield:
		output.writeByte(opcode);
		output.writeShort(gcp.putRef(gcp.FIELDREF, 
					     instr.getReference()));
		break;

	    case opc_invokespecial:
	    case opc_invokestatic:
	    case opc_invokeinterface:
	    case opc_invokevirtual: {
		Reference ref = instr.getReference();
		output.writeByte(opcode);
		if (opcode == opc_invokeinterface) {
		    output.writeShort(gcp.putRef(gcp.INTERFACEMETHODREF, ref));
		    output.writeByte
			(TypeSignature.getArgumentSize(ref.getType()) + 1);
		    output.writeByte(0);
		} else 
		    output.writeShort(gcp.putRef(gcp.METHODREF, ref));
		break;
	    }
	    case opc_new:
	    case opc_checkcast:
	    case opc_instanceof:
		output.writeByte(opcode);
		output.writeShort(gcp.putClassType(instr.getClazzType()));
		break;
	    case opc_multianewarray:
		if (instr.getIntData() == 1) {
		    String clazz = instr.getClazzType().substring(1);
		    int index = newArrayTypes.indexOf(clazz.charAt(0));
		    if (index != -1) {
			output.writeByte(opc_newarray);
			output.writeByte(index + 4);
		    } else {
			output.writeByte(opc_anewarray);
			output.writeShort(gcp.putClassType(clazz));
		    }
		} else {
		    output.writeByte(opcode);
		    output.writeShort(gcp.putClassType(instr.getClazzType()));
		    output.writeByte(instr.getIntData());
		}
		break;

	    case opc_nop:
	    case opc_iaload: case opc_laload: case opc_faload:
	    case opc_daload: case opc_aaload:
	    case opc_baload: case opc_caload: case opc_saload:
	    case opc_iastore: case opc_lastore: case opc_fastore:
	    case opc_dastore: case opc_aastore:
	    case opc_bastore: case opc_castore: case opc_sastore:
	    case opc_pop: case opc_pop2:
	    case opc_dup: case opc_dup_x1: case opc_dup_x2:
	    case opc_dup2: case opc_dup2_x1: case opc_dup2_x2:
	    case opc_swap:
	    case opc_iadd: case opc_ladd: case opc_fadd: case opc_dadd:
	    case opc_isub: case opc_lsub: case opc_fsub: case opc_dsub:
	    case opc_imul: case opc_lmul: case opc_fmul: case opc_dmul:
	    case opc_idiv: case opc_ldiv: case opc_fdiv: case opc_ddiv:
	    case opc_irem: case opc_lrem: case opc_frem: case opc_drem:
	    case opc_ineg: case opc_lneg: case opc_fneg: case opc_dneg:
	    case opc_ishl: case opc_lshl:
	    case opc_ishr: case opc_lshr:
	    case opc_iushr: case opc_lushr: 
	    case opc_iand: case opc_land:
	    case opc_ior: case opc_lor: 
	    case opc_ixor: case opc_lxor:
	    case opc_i2l: case opc_i2f: case opc_i2d:
	    case opc_l2i: case opc_l2f: case opc_l2d:
	    case opc_f2i: case opc_f2l: case opc_f2d:
	    case opc_d2i: case opc_d2l: case opc_d2f:
	    case opc_i2b: case opc_i2c: case opc_i2s:
	    case opc_lcmp: case opc_fcmpl: case opc_fcmpg:
	    case opc_dcmpl: case opc_dcmpg:
	    case opc_ireturn: case opc_lreturn: 
	    case opc_freturn: case opc_dreturn: case opc_areturn:
	    case opc_return: 
	    case opc_athrow:
	    case opc_arraylength:
	    case opc_monitorenter: case opc_monitorexit:
		output.writeByte(opcode);
		break;
	    default:
		throw new ClassFormatError("Invalid opcode "+opcode);
	    }
	}

	output.writeShort(exceptionHandlers.length);
	for (int i=0; i< exceptionHandlers.length; i++) {
	    output.writeShort(exceptionHandlers[i].start.getAddr());
	    output.writeShort(exceptionHandlers[i].end.getNextByAddr().getAddr());
	    output.writeShort(exceptionHandlers[i].catcher.getAddr());
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

    public Collection getInstructions() {
	return new AbstractCollection() {
	    public int size() {
		return instructionCount;
	    }

	    public Iterator iterator() {
		return new Iterator() {
		    Instruction instr = firstInstr;

		    public boolean hasNext() {
			return instr != null;
		    }

		    public Object next() {
			if (instr == null)
			    throw new NoSuchElementException();
			Instruction result = instr;
			instr = instr.getNextByAddr();
			return result;
		    }

		    public void remove() {
			instr.getPrevByAddr().removeInstruction();
		    }
		};
	    }
	};
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

    public String toString() {
        return "Bytecode "+methodInfo;
    }
}
