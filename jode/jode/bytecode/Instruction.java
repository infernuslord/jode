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

/**
 * This class represents an instruction in the byte code.
 *
 * We only allow a subset of opcodes.  Other opcodes are mapped to
 * their simpler version.  When writing the bytecode the shortest
 * possible bytecode is produced.
 *
 * The opcodes we map are:
 * <pre>
 * [iflda]load_x           -&gt; [iflda]load
 * [iflda]store_x          -&gt; [iflda]store
 * [ifa]const_xx, ldc_w    -&gt; ldc
 * [dl]const_xx            -&gt; ldc2_w
 * wide opcode             -&gt; opcode
 * tableswitch             -&gt; lookupswitch
 * [a]newarray             -&gt; multianewarray
 * </pre> 
 */
public class Instruction implements Opcodes{
    /**
     * The opcode and lineNr of the instruction.  
     * opcode is <code>(lineAndOpcode &amp; 0xff)</code>, while 
     * lineNr is <code>(lineAndOpcode &gt;&gt; 8)</code>.
     * If line number is not known or unset, it is -1.
     */
    private int lineAndOpcode;

    /**
     * Creates a new simple Instruction with no parameters.  We map
     * some opcodes, so you must always use the mapped opcode.
     * @param opcode the opcode of this instruction.
     * @exception IllegalArgumentException if opcode is not in our subset
     * or if opcode needs a parameter.  */
    public static Instruction forOpcode(int opcode) {
	switch (opcode) {
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
	    return new Instruction(opcode);
	default:
	    throw new IllegalArgumentException("Instruction has a parameter");
	}
    }


    /**
     * Creates a new ldc Instruction.
     * @param opcode the opcode of this instruction.
     * @param constant the constant parameter.
     * @exception IllegalArgumentException if opcode is not opc_ldc or
     * opc_ldc2_w.
     */
    public static Instruction forOpcode(int opcode, Object constant) {
	if (opcode == opc_ldc || opcode == opc_ldc2_w)
	    return new ConstantInstruction(opcode, constant);
	throw new IllegalArgumentException("Instruction has no constant");
    }

    /**
     * Creates a new Instruction with a local variable as parameter.
     * @param opcode the opcode of this instruction.
     * @param lvi the local variable parameter.
     * @exception IllegalArgumentException if opcode is not in our subset
     * or if opcode doesn't need a single local variable as parameter.
     */
    public static Instruction forOpcode(int opcode, LocalVariableInfo lvi) {
	if (opcode == opc_ret
	    || opcode >= opc_iload && opcode <= opc_aload
	    || opcode >= opc_istore && opcode <= opc_astore)
	    return new SlotInstruction(opcode, lvi);
	throw new IllegalArgumentException("Instruction has no slot");
    }

    /**
     * Creates a new Instruction with reference as parameter.
     * @param opcode the opcode of this instruction.
     * @param reference the reference parameter.
     * @exception IllegalArgumentException if opcode is not in our subset
     * or if opcode doesn't need a reference as parameter.
     */
    public static Instruction forOpcode(int opcode, Reference reference) {
	if (opcode >= opc_getstatic && opcode <= opc_invokeinterface)
	    return new ReferenceInstruction(opcode, reference);
	throw new IllegalArgumentException("Instruction has no reference");
    }

    /**
     * Creates a new Instruction with type signature as parameter.
     * @param opcode the opcode of this instruction.
     * @param typeSig the type signature parameter.
     * @exception IllegalArgumentException if opcode is not in our subset
     * or if opcode doesn't need a type signature as parameter.
     */
    public static Instruction forOpcode(int opcode, String typeSig) {
	switch (opcode) {
	case opc_new: 
	case opc_checkcast:
	case opc_instanceof:
	    return new TypeInstruction(opcode, typeSig);
	default:
	    throw new IllegalArgumentException("Instruction has no type");
	}
    }

    /**
     * Creates a new switch Instruction.
     * @param opcode the opcode of this instruction must be opc_lookupswitch.
     * @param values an array containing the different cases.
     * @exception IllegalArgumentException if opcode is not opc_lookupswitch.
     */
    public static Instruction forOpcode(int opcode, int[] values) {
	if (opcode == opc_lookupswitch)
	    return new SwitchInstruction(opcode, values);
	throw new IllegalArgumentException("Instruction has no values");
    }

    /**
     * Creates a new increment Instruction.
     * @param opcode the opcode of this instruction.
     * @param lvi the local variable parameter.
     * @param increment the increment parameter.
     * @exception IllegalArgumentException if opcode is not opc_iinc.
     */
    public static Instruction forOpcode(int opcode, 
					LocalVariableInfo lvi, int increment) {
	if (opcode == opc_iinc)
	    return new IncInstruction(opcode, lvi, increment);
	throw new IllegalArgumentException("Instruction has no increment");
    }

    /**
     * Creates a new Instruction with type signature and a dimension
     * as parameter.
     * @param opcode the opcode of this instruction.
     * @param typeSig the type signature parameter.
     * @param dimension the array dimension parameter.
     * @exception IllegalArgumentException if opcode is not
     * opc_multianewarray.  
     */
    public static Instruction forOpcode(int opcode, 
					String typeSig, int dimension) {
	if (opcode == opc_multianewarray)
	    return new TypeDimensionInstruction(opcode, typeSig, dimension);
	throw new IllegalArgumentException("Instruction has no dimension");
    }

    /**
     * Creates a simple opcode, without any parameters.
     */
    Instruction(int opcode) {
	this.lineAndOpcode = (-1 << 8) | opcode;
    }

    /**
     * Returns the opcode of the instruction.  
     */
    public final int getOpcode() {
	return lineAndOpcode & 0xff;
    }

    public final boolean hasLineNr() {
	return lineAndOpcode >= 0;
    }

    public final int getLineNr() {
	return lineAndOpcode >> 8;
    }

    public final void setLineNr(int nr) {
	lineAndOpcode = (nr << 8) | (lineAndOpcode & 0xff);
    }

    public boolean isStore() {
	return false;
    }

    public boolean hasLocal() {
	return false;
    }
	    
    public int getLocalSlot()
    {
	throw new IllegalArgumentException();
	// UnsupportedOperationException would be more appropriate
    }

    public LocalVariableInfo getLocalInfo()
    {
	throw new IllegalArgumentException();
    }

    public void setLocalInfo(LocalVariableInfo info) 
    {
	throw new IllegalArgumentException();
    }

    public void setLocalSlot(int slot) 
    {
	throw new IllegalArgumentException();
    }

    /**
     * Get the increment for an opc_iinc instruction.
     */
    public int getIncrement()
    {
	throw new IllegalArgumentException();
    }

    /**
     * Set the increment for an opc_iinc instruction.
     */
    public void setIncrement(int incr)
    {
	throw new IllegalArgumentException();
    }

    /**
     * Get the dimensions for an opc_anewarray opcode.
     */
    public int getDimensions()
    {
	throw new IllegalArgumentException();
    }

    /**
     * Set the dimensions for an opc_anewarray opcode.
     */
    public void setDimensions(int dims)
    {
	throw new IllegalArgumentException();
    }

    public Object getConstant() 
    {
	throw new IllegalArgumentException();
    }

    public void setConstant(Object constant) 
    {
	throw new IllegalArgumentException();
    }

    public Reference getReference()
    {
	throw new IllegalArgumentException();
    }

    public void setReference(Reference ref)
    {
	throw new IllegalArgumentException();
    }

    public String getClazzType() 
    {
	throw new IllegalArgumentException();
    }

    public void setClazzType(String type)
    {
	throw new IllegalArgumentException();
    }

    public int[] getValues()
    {
	throw new IllegalArgumentException();
    }

    public void setValues(int[] values) 
    {
	throw new IllegalArgumentException();
    }

    public final boolean doesAlwaysJump() {
	switch (getOpcode()) {
	case opc_ret:
	case opc_goto:
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
	byte delta = (byte) stackDelta.charAt(getOpcode());
	poppush[0] = delta & 7;
	poppush[1] = delta >> 3;
    }

    /**
     * Gets a printable representation of the opcode with its
     * parameters.  This will not include the destination for jump
     * instructions, since this information is not stored inside the
     * instruction.  
     */
    public final String getDescription() {
	return toString();
    }

    public String toString() {
	return opcodeString[getOpcode()];
    }

    /**
     * stackDelta contains \100 if stack count of opcode is variable
     * \177 if opcode is illegal, or 8*stack_push + stack_pop otherwise
     * The string is created by scripts/createStackDelta.pl
     */
    final static String stackDelta = 
	"\000\010\010\010\010\010\010\010\010\020\020\010\010\010\020\020\010\010\010\010\020\010\020\010\020\010\010\010\010\010\020\020\020\020\010\010\010\010\020\020\020\020\010\010\010\010\012\022\012\022\012\012\012\012\001\002\001\002\001\001\001\001\001\002\002\002\002\001\001\001\001\002\002\002\002\001\001\001\001\003\004\003\004\003\003\003\003\001\002\021\032\043\042\053\064\022\012\024\012\024\012\024\012\024\012\024\012\024\012\024\012\024\012\024\012\024\011\022\011\022\012\023\012\023\012\023\012\024\012\024\012\024\000\021\011\021\012\012\022\011\021\021\012\022\012\011\011\011\014\012\012\014\014\001\001\001\001\001\001\002\002\002\002\002\002\002\002\000\000\000\001\001\001\002\001\002\001\000\100\100\100\100\100\100\100\100\177\010\011\011\011\001\011\011\001\001\177\100\001\001\000\000";
}
