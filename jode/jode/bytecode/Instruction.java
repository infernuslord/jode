/* Instruction Copyright (C) 1999 Jochen Hoenicke.
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
import java.util.Vector;
import java.util.Enumeration;

/**
 * This class represents an instruction in the byte code.
 *
 */
public final class Instruction implements Opcodes{
    private BytecodeInfo codeinfo;
    /**
     * The opcode of the instruction.  We map some opcodes, e.g.
     * <pre>
     * iload_[0-3] -> iload, ldc_w -> ldc, wide iinc -> iinc.
     * </pre>
     */
    private int opcode;
    /**
     * If this opcode uses a local this gives the slot.  This info is
     * used when swapping locals.  
     */
    private int localSlot = -1;
    /**
     * Optional object data for this opcode.  There are four different
     * usages of this field:
     * <dl>
     * <dt>opc_ldc / opc_ldc2_w</dt>
     * <dd>The constant of type Integer/Long/Float/Double/String. </dd>
     * <dt>opc_invokexxx / opc_xxxfield / opc_xxxstatic</dt>
     * <dd>The field/method Reference</dd>
     * <dt>opc_new / opc_checkcast / opc_instanceof / opc_multianewarray</dt>
     * <dd>The typesignature of the class/array</dd>
     * <dt>opc_lookupswitch</dt>
     * <dd>The array of values of type int[]</dd>
     * </dl>
     */
    private Object objData;
    /**
     * Optional integer data for this opcode.  There are various uses
     * for this:
     * <dl>
     * <dt>opc_iinc</dt>
     * <dd>The value by which the constant is increased/decreased. (short)</dd>
     * <dt>opc_tableswitch</dt>
     * <dd>The start value of the table</dd>
     * <dt>opc_multianewarray</dt>
     * <dd>The number of dimensions (1..255)</dd>
     * <dt>opc_lookupswitch</dt>
     * <dd>The array of values of type int[]</dd>
     * </dl>
     */
    private int intData;
    /**
     * The address of this opcode.
     */
    private int addr;
    /**
     * The length of this opcode.  You shouldn't touch it, nor rely on
     * it, since the length of some opcodes may change automagically
     * (e.g. when changing localSlot  iload_0 <-> iload 5)
     */
    private int length;
    /**
     * The successors of this opcodes, where flow may lead to
     * (except that nextByAddr is implicit if !alwaysJump).  The
     * value null is equivalent to an empty array.
     */
    Instruction[] succs;
    /**
     * The predecessors of this opcode, orthogonal to the succs array.
     * This must be null or a non empty array.
     */
    Instruction[] preds;
    /**
     * The next instruction in code order.
     */
    private Instruction nextByAddr;
    /**
     * The previous instruction in code order, useful when changing
     * the order.
     */
    private Instruction prevByAddr;

    /**
     * You can use this field to add some info to each instruction.
     * After using, you must set it to null again.
     */
    private Object tmpInfo;

    public Instruction(BytecodeInfo ci) {
	this.codeinfo = ci;
    }

    /**
     * Returns the opcode of the instruction.  We map some opcodes, e.g.
     * <pre>
     * iload_0   -&gt; iload
     * ldc_w     -&gt; ldc
     * wide iinc -&gt; iinc
     * </pre>
     */
    public final int getOpcode() {
	return opcode;
    }

    /**
     * Returns the address of this opcode.  As long as you don't remove
     * or insert instructions, you can be sure, that the addresses of the
     * opcodes are unique, and that 
     * <pre>
     * instr.getAddr() + instr.getLength() == instr.getNextByAddr().getAddr()
     * <pre>
     *
     * If you insert/remove Instructions, you should be aware that the
     * above property is not guaranteed anymore.
     */
    public final int getAddr() {
	return addr;
    }

    /**
     * Returns the length of this opcode.  See getAddr() for some
     * notes.  Note that the length doesn't necessarily reflect the
     * real length, when this bytecode is written again, since the
     * length of an ldc instruction depends on the number of entries
     * in constant pool, and the order they are allocated.  
     */
    public final int getLength() {
	return length;
    }

    final void setAddr(int addr) {
	this.addr = addr;
    }
    final void setLength(int length) {
	this.length = length;
    }

    public final int getLocalSlot() {
	return localSlot;
    }

    public final void setLocalSlot(int slot) {
	localSlot = slot;
    }

    public final int getIntData()
    /*{ require { opcode == opc_iinc || opcode == opc_multianewarray
                  || opcode == opc_tableswitch
                  :: "Instruction has no int data" } }*/
    {
	return intData;
    }

    public final void setIntData(int data)
    /*{ require { opcode == opc_iinc || opcode == opc_multianewarray
                  || opcode == opc_tableswitch
                  :: "Instruction has no int data" } }*/
    {
	this.intData = data;
    }

    public final Object getConstant() 
    /*{ require { opcode == opc_ldc || opcode == opc_ldc2_w
                  :: "Instruction has no constant" } }*/
    {
	return objData;
    }

    public final void setConstant(Object constant) 
    /*{ require { opcode == opc_ldc || opcode == opc_ldc2_w
                  :: "Instruction has no constant" } }*/
    {
	objData = constant;
    }

    public final Reference getReference()
    /*{ require { opcode >= opc_getstatic && opcode <= opc_invokeinterface
                  :: "Instruction has no reference" } }*/
    {
	return (Reference) objData;
    }

    public final void setReference(Reference ref)
    /*{ require { opcode >= opc_getstatic && opcode <= opc_invokeinterface
                  :: "Instruction has no reference" } }*/
    {
	objData = ref;
    }

    public final String getClazzType() 
    /*{ require { opcode == opc_new 
                  || opcode == opc_checkcast
                  || opcode == opc_instanceof
                  || opcode == opc_multianewarray
		  :: "Instruction has no typesig" } }*/
    {
	return (String) objData;
    }

    public final void setClazzType(String type)
    /*{ require { opcode == opc_new 
                  || opcode == opc_checkcast
                  || opcode == opc_instanceof
                  || opcode == opc_multianewarray
		  :: "Instruction has no typesig" } }*/
    {
	objData = type;
    }

    public final int[] getValues()
    /*{ require { opcode == opc_lookupswitch
                  :: "Instruction has no values" } }*/
    {
	return (int[]) objData;
    }

    public final void setValues(int[] values) 
    /*{ require { opcode == opc_lookupswitch
                  :: "Instruction has no values" } }*/
    {
	objData = values;
    }

    public final boolean doesAlwaysJump() {
	switch (opcode) {
	case opc_ret:
	case opc_goto:
	case opc_jsr:
	case opc_tableswitch:
	case opc_lookupswitch:
	case opc_ireturn: 
	case opc_lreturn: 
	case opc_freturn: 
	case opc_dreturn: 
	case opc_areturn:
	case opc_return: 
	case opc_athrow:
	    return true;
	default:
	    return false;
	}
    }

    public final Instruction[] getPreds() {
	return preds;
    }

    public final Instruction[] getSuccs() {
	return succs;
    }

    public final Instruction getPrevByAddr() {
	return prevByAddr;
    }

    public final Instruction getNextByAddr() {
	return nextByAddr;
    }

    public final Object getTmpInfo() {
	return tmpInfo;
    }

    public final void setTmpInfo(Object info) {
	tmpInfo = info;
    }

    public final void replaceInstruction(int newOpcode) {
	replaceInstruction(newOpcode, null);
    }

    public final void replaceInstruction(int newOpcode, 
					 Instruction[] newSuccs) {
	if (succs != null && succs != newSuccs) {
	    for (int i = 0; i< succs.length; i++)
		succs[i].removePredecessor(this);
	}

	opcode = newOpcode;
	localSlot = -1;
	objData = null;
	intData = 0;
	if (succs != newSuccs)
	    setSuccs(newSuccs);
    }

    private final void setSuccs(Instruction[] newSuccs) {
	succs = newSuccs;
	if (succs != null) {
	    for (int i = 0; i< succs.length; i++)
		succs[i].addPredecessor(this);
	}
    }

    public void addPredecessor(Instruction pred) {
	if (preds == null) {
	    preds = new Instruction[] { pred };
	    return;
	}
	int predsLength = preds.length;
	Instruction[] newPreds = new Instruction[predsLength+1];
	System.arraycopy(preds, 0, newPreds, 0, predsLength);
	newPreds[predsLength] = pred;
	preds = newPreds;
    }

    public void removePredecessor(Instruction pred) {
	/* Hopefully it doesn't matter if this is slow */
	int predLength = preds.length;
	if (predLength == 1) {
	    if (preds[0] != pred)
		throw new jode.AssertError
		    ("removing not existing predecessor");
	    preds = null;
	} else {
	    Instruction[] newPreds = new Instruction[predLength-1];
	    int j;
	    for (j = 0; preds[j] != pred; j++)
		newPreds[j] = preds[j];
	    System.arraycopy(preds, j+1, newPreds, j, predLength - j - 1);
	    preds = newPreds;
	}
    }

    public final Instruction insertInstruction(int opc) {
	codeinfo.instructionCount++;
	Instruction newInstr = new Instruction(codeinfo);
	newInstr.opcode = opc;
	newInstr.addr = addr;

	newInstr.prevByAddr = prevByAddr;
	if (prevByAddr != null)
	    prevByAddr.nextByAddr = newInstr;
	else
	    codeinfo.firstInstr = newInstr;
	newInstr.nextByAddr = this;
	prevByAddr = newInstr;

	/* promote the predecessors to newInstr */
	if (preds != null) {
	    for (int j=0; j < preds.length; j++)
		for (int i=0; i < preds[j].succs.length; i++)
		    if (preds[j].succs[i] == this)
			preds[j].succs[i] = newInstr;
	    newInstr.preds = preds;
	    preds = null;
	}
	return newInstr;
    }

    public final Instruction insertInstruction(int opc, 
					       Instruction[] newSuccs) {
	Instruction newInstr = insertInstruction(opc);
	if (newSuccs != null)
	    newInstr.setSuccs(newSuccs);
	return newInstr;
    }

    public Instruction appendInstruction(int opc) {
	codeinfo.instructionCount++;
	Instruction newInstr = new Instruction(codeinfo);
	newInstr.opcode = opc;
	newInstr.addr = addr + length;
	newInstr.length = 0;
	newInstr.nextByAddr = nextByAddr;
	if (nextByAddr != null)
	    nextByAddr.prevByAddr = newInstr;
	newInstr.prevByAddr = this;

	nextByAddr = newInstr;
	return newInstr;
    }

    public Instruction appendInstruction(int opc, Instruction[] newSuccs) {
	Instruction newInstr = appendInstruction(opc);
	if (newSuccs != null)
	    newInstr.setSuccs(newSuccs);
	return newInstr;
    }

    /**
     * Removes this instruction (as if it would be replaced by a nop).
     */
    public void removeInstruction() {
	codeinfo.instructionCount--;

	/* remove from chained list and adjust addr / length */
	if (prevByAddr != null) {
	    prevByAddr.nextByAddr = nextByAddr;
	    prevByAddr.length += length;
	} else {
	    if (nextByAddr == null)
		/* Mustn't happen, each method must include a return */
		throw new IllegalArgumentException
		    ("Removing the last instruction of a method!");
	    codeinfo.firstInstr = nextByAddr;
	    nextByAddr.addr = 0;
	    nextByAddr.length += length;
	}

	if (nextByAddr != null)
	    nextByAddr.prevByAddr = prevByAddr;

	/* remove predecessors of successors */
	if (succs != null) {
	    for (int i=0; i < succs.length; i++)
		succs[i].removePredecessor(this);
	    succs = null;
	}

	Instruction alternative = nextByAddr != null ? nextByAddr : prevByAddr;
	/* remove the predecessors to alternative */
	if (preds != null) {
	    for (int j=0; j < preds.length; j++)
		for (int i=0; i < preds[j].succs.length; i++)
		    if (preds[j].succs[i] == this)
			preds[j].succs[i] = alternative;
	    if (alternative.preds == null)
		alternative.preds = preds;
	    else {
		Instruction[] newPreds
		    = new Instruction[alternative.preds.length + preds.length];
		System.arraycopy(preds, 0, newPreds, 0, preds.length);
		System.arraycopy(alternative.preds, 0, newPreds, preds.length, 
				 alternative.preds.length);
		alternative.preds = newPreds;
	    }
	    preds = null;
	}

	/* adjust exception handlers */
	Handler[] handlers = codeinfo.getExceptionHandlers();
	for (int i=0; i< handlers.length; i++) {
	    if (handlers[i].start == this && handlers[i].end == this) {
		/* Remove the handler.
		 * This is very seldom, so we can make it slow */
		Handler[] newHandlers = new Handler[handlers.length - 1];
		System.arraycopy(handlers, 0, newHandlers, 0, i);
		System.arraycopy(handlers, i+1, newHandlers, i, 
				 handlers.length - (i+1));
		handlers = newHandlers;
		codeinfo.setExceptionHandlers(newHandlers);
		i--;
	    } else {
		if (handlers[i].start == this)
		    handlers[i].start = nextByAddr;
		if (handlers[i].end == this)
		    handlers[i].end = prevByAddr;
		if (handlers[i].catcher == this)
		    handlers[i].catcher = nextByAddr;
	    }
	}

	/* adjust local variable table and line number table */
	LocalVariableInfo[] lvt = codeinfo.getLocalVariableTable();
	if (lvt != null) {
	    for (int i=0; i< lvt.length; i++) {
		if (lvt[i].start == this && lvt[i].end == this) {
		    /* Remove the local variable info.
		     * This is very seldom, so we can make it slow
		     */
		    LocalVariableInfo[] newLVT = 
			new LocalVariableInfo[lvt.length - 1];
		    System.arraycopy(lvt, 0, newLVT, 0, i);
		    System.arraycopy(lvt, i+1, newLVT, i, 
				     newLVT.length - i);
		    lvt = newLVT;
		    codeinfo.setLocalVariableTable(newLVT);
		    i--;
		} else {
		    if (lvt[i].start == this)
			lvt[i].start = nextByAddr;
		    if (lvt[i].end == this)
			lvt[i].end = prevByAddr;
		}
	    }
	}
	LineNumber[] lnt = codeinfo.getLineNumberTable();
	if (lnt != null) {
	    for (int i=0; i< lnt.length; i++) {
		if (lnt[i].start == this) {
		    if (nextByAddr == null
			|| (i+1 < lnt.length 
			    && lnt[i+1].start == nextByAddr)) {
			/* Remove the line number.
			 * This is very seldom, so we can make it slow */
			LineNumber[] newLNT = 
			    new LineNumber[lnt.length - 1];
			System.arraycopy(lnt, 0, newLNT, 0, i);
			System.arraycopy(lnt, i+1, newLNT, i, 
					 newLNT.length - i);
			lnt = newLNT;
			codeinfo.setLineNumberTable(newLNT);
			i--;
		    } else
			lnt[i].start = nextByAddr;
		}
	    }
	}
    }

    /**
     * This returns the number of stack entries this instruction
     * pushes and pops from the stack.  The result fills the given
     * array.
     *
     * @param poppush an array of two ints.  The first element will
     * get the number of pops, the second the number of pushes.  
     */
    public void getStackPopPush(int[] poppush)
    /*{ require { poppush != null && poppush.length == 2
        :: "poppush must be an array of two ints" } } */
    {
	byte delta = (byte) stackDelta.charAt(opcode);
	if (delta < 0x40) {
	    poppush[0] = delta & 7;
	    poppush[1] = delta >> 3;
	} else {
	    switch (opcode) {
	    case opc_invokevirtual:
	    case opc_invokespecial:
	    case opc_invokestatic:
	    case opc_invokeinterface: {
		Reference ref = (Reference) objData;
		String typeSig = ref.getType();
		poppush[0] = opcode != opc_invokestatic ? 1 : 0;
		poppush[0] += TypeSignature.getArgumentSize(typeSig);
		poppush[1] = TypeSignature.getReturnSize(typeSig);
		break;
	    }
	    
	    case opc_putfield:
	    case opc_putstatic: {
		Reference ref = (Reference) objData;
		poppush[1] = 0;
		poppush[0] = TypeSignature.getTypeSize(ref.getType());
		if (opcode == opc_putfield)
		    poppush[0]++;
		break;
	    }
	    case opc_getstatic:
	    case opc_getfield: {
		Reference ref = (Reference) objData;
		poppush[1] = TypeSignature.getTypeSize(ref.getType());
		poppush[0] = opcode == opc_getfield ? 1 : 0;
		break;
	    }
	    
	    case opc_multianewarray: {
		poppush[1] = 1;
		poppush[0] = prevByAddr.intData;
		break;
	    }
	    default:
		throw new jode.AssertError("Unknown Opcode: "+opcode);
	    }
	}
    }
    
    public Instruction findMatchingPop() {
	int poppush[] = new int[2];
	getStackPopPush(poppush);	

	int count = poppush[1];
	Instruction instr = this;
	while (true) {
	    if (instr.succs != null || instr.doesAlwaysJump())
		return null;
	    instr = instr.nextByAddr;
	    if (instr.preds != null)
		return null;
	    
	    instr.getStackPopPush(poppush);	
	    if (count == poppush[0])
		return instr;
	    count += poppush[1] - poppush[0];
	}
    }

    public Instruction findMatchingPush() {
	int count = 0;
	Instruction instr = this;
	int poppush[] = new int[2];
	while (true) {
	    if (instr.preds != null)
		return null;
	    instr = instr.prevByAddr;
	    if (instr == null || instr.succs != null || instr.doesAlwaysJump())
		return null;

	    instr.getStackPopPush(poppush);
	    if (count < poppush[1]) {
		return count == 0 ? instr : null;
	    }
	    count += poppush[0] - poppush[1];
	}
    }

    public String getDescription() {
	StringBuffer result = new StringBuffer(String.valueOf(addr))
	    .append('_').append(Integer.toHexString(hashCode()))
	    .append(": ").append(opcodeString[opcode]);
	if (localSlot != -1)
	    result.append(" ").append(localSlot);
	if (succs != null && succs.length == 1)
	    result.append(" ").append(succs[0].addr);
	switch (opcode) {
	case opc_iinc:
	    result.append(" ").append(intData);
	    break;
	case opc_ldc: case opc_ldc2_w:    
	case opc_getstatic: case opc_getfield:
	case opc_putstatic: case opc_putfield:
	case opc_invokespecial:	case opc_invokestatic: case opc_invokevirtual:
	case opc_new: 
	case opc_checkcast: 
	case opc_instanceof:
	    result.append(" ").append(objData);
	    break;
	case opc_multianewarray:
	case opc_invokeinterface:
	    result.append(" ").append(objData).append(" ").append(intData);
	    break;
	}
	return result.toString();
    }

    public String toString() {
        return "" + addr + "_" + Integer.toHexString(hashCode());
    }

    private final static String stackDelta = 
	"\000\010\010\010\010\010\010\010\010\020\020\010\010\010\020\020\010\010\010\010\020\010\020\010\020\010\010\010\010\010\020\020\020\020\010\010\010\010\020\020\020\020\010\010\010\010\012\022\012\022\012\012\012\012\001\002\001\002\001\001\001\001\001\002\002\002\002\001\001\001\001\002\002\002\002\001\001\001\001\003\004\003\004\003\003\003\003\001\002\021\032\043\042\053\064\022\012\024\012\024\012\024\012\024\012\024\012\024\012\024\012\024\012\024\012\024\011\022\011\022\012\023\012\023\012\023\012\024\012\024\012\024\000\021\011\021\012\012\022\011\021\021\012\022\012\011\011\011\014\012\012\014\014\001\001\001\001\001\001\002\002\002\002\002\002\002\002\000\010\000\001\001\001\002\001\002\001\000\100\100\100\100\100\100\100\100\177\010\011\011\011\001\011\011\001\001\177\100\001\001\000\010";

    /* stackDelta contains \100 if stack count of opcode is variable
     * \177 if opcode is illegal, or 8*stack_push + stack_pop otherwise
     * The above values are extracted from following list with: 
     *    perl -ne'/"(.*)"/ and print $1' 
     *
     * "\000"                             // nop
     * "\010\010\010\010\010\010\010\010" // aconst_null, iconst_m?[0-5]
     * "\020\020\010\010\010\020\020"     // [lfd]const_[0-2]
     * "\010\010\010\010\020"             // sipush bipush ldcx
     * "\010\020\010\020\010"             // [ilfda]load
     * "\010\010\010\010"
     * "\020\020\020\020"
     * "\010\010\010\010"
     * "\020\020\020\020"
     * "\010\010\010\010"
     * "\012\022\012\022\012\012\012\012" // [ilfdabcs]aload
     * "\001\002\001\002\001"             // [ilfda]store
     * "\001\001\001\001"
     * "\002\002\002\002"
     * "\001\001\001\001"
     * "\002\002\002\002"
     * "\001\001\001\001"
     * "\003\004\003\004\003\003\003\003" // [ilfdabcs]astore
     * "\001\002"                         // pop
     * "\021\032\043\042\053\064"         // dup2?(_x[12])?
     * "\022"                             // swap
     * "\012\024\012\024"                 // [ilfd]add
     * "\012\024\012\024"                 // [ilfd]sub
     * "\012\024\012\024"                 // [ilfd]mul
     * "\012\024\012\024"                 // [ilfd]div
     * "\012\024\012\024"                 // [ilfd]rem
     * "\011\022\011\022"                 // [ilfd]neg
     * "\012\023\012\023\012\023"         // [il]u?sh[lr]
     * "\012\024\012\024\012\024"         // [il](and|or|xor)
     * "\000"                             // opc_iinc
     * "\021\011\021"                     // i2[lfd]
     * "\012\012\022"                     // l2[ifd]
     * "\011\021\021"                     // f2[ild]
     * "\012\022\012"                     // d2[ilf]
     * "\011\011\011"                     // i2[bcs]
     * "\014\012\012\014\014"             // [lfd]cmp.?
     * "\001\001\001\001\001\001"         // if..
     * "\002\002\002\002\002\002"         // if_icmp..
     * "\002\002"                         // if_acmp..
     * "\000\010\000\001\001"             // goto,jsr,ret, .*switch
     * "\001\002\001\002\001\000"         // [ilfda]?return
     * "\100\100\100\100"                 // (get/put)(static|field)
     * "\100\100\100\100"                 // invoke.*
     * "\177\010\011\011\011"             // 186 - 190
     * "\001\011\011\001\001"             // 191 - 195
     * "\177\100\001\001"                 // 196 - 199
     * "\000\010"                         // goto_w, jsr_w
     */
}
