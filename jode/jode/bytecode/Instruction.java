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
 */
public class Instruction implements Opcodes{
    /**
     * The opcode and lineNr of the instruction.  
     * opcode is <code>(lineAndOpcode &amp; 0xff)</code>, while 
     * lineNr is <code>(lineAndOpcode &gt;&gt; 8)</code>.
     * If line number is not known or unset, it is -1.
     */
    private int lineAndOpcode;


//      /**
//       * Optional object data for this opcode.  There are six different
//       * usages of this field:
//       * <dl>
//       * <dt>opc_ldc / opc_ldc2_w</dt>
//       * <dd>The constant of type Integer/Long/Float/Double/String. </dd>
//       * <dt>opc_invokexxx / opc_xxxfield / opc_xxxstatic</dt>
//       * <dd>The field/method Reference</dd>
//       * <dt>opc_new / opc_checkcast / opc_instanceof</dt>
//       * <dd>The typesignature of the class/array</dd>
//       * <dt>opc_lookupswitch</dt>
//       * <dd>The array of values of type int[]</dd>
//       * <dt>opc_multianewarray</dt>
//       * <dd>A DoubleParam: intValue contains dimension, objValue contains
//       *     reference </dd>
//       * <dt>opc_[aildf]{load,store}</dt>
//       * <dd>The LocalVariableInfo</dd>
//       * <dt>opc_iinc</dt>
//       * <dd>A DoubleParam: intValue contains count, objValue contains
//       *     local variable info.</dd>
//       * </dl>
//       */
//      private Object param;

//      /**
//       * Create a new Instruction suitable for the given opcode.  We map
//       * some opcodes, so you must always use the mapped opcode.
//       * <pre>
//       * [iflda]load_x           -&gt; [iflda]load
//       * [iflda]store_x          -&gt; [iflda]store
//       * [ifa]const_xx, ldc_w    -&gt; ldc
//       * [dl]const_xx            -&gt; ldc2_w
//       * wide opcode             -&gt; opcode
//       * tableswitch             -&gt; lookupswitch
//       * [a]newarray             -&gt; multianewarray
//       * </pre> 
//       */
//      public static Instruction forOpcode(int opcode) {
//  	if (opcode == opc_iinc)
//  	    return new IncInstruction(opcode);
//  	else if (opcode == opc_ret
//  		 || opcode >= opc_iload && opcode <= opc_aload
//  		 || opcode >= opc_istore && opcode <= opc_astore)
//  	    return new SlotInstruction(opcode);
//  	else if (opcode >= opc_getstatic && opcode <= opc_invokeinterface)
//  	    return new ReferenceInstruction(opcode);
//  	else switch (opcode) {
//  	case opc_new: 
//  	case opc_checkcast:
//  	case opc_instanceof:
//  	    return new TypeInstruction(opcode);
//  	case opc_multianewarray:
//  	    return new TypeDimensionInstruction(opcode);
//  	default:
//  	    return new Instruction(opcode);
//  	}
//      }

    /**
     * Standard constructor: creates an opcode with parameter and
     * lineNr.  
     */
    public Instruction(int opcode, int lineNr) {
	if (stackDelta.charAt(opcode) == '\177')
	    throw new IllegalArgumentException("Unknown opcode: "+opcode);
	this.lineAndOpcode = (lineNr << 8) | opcode;
    }

    /**
     * Creates a simple opcode, without any parameters.
     */
    public Instruction(int opcode) {
	this(opcode, -1);
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
    protected final static String stackDelta = 
	"\000\010\010\010\010\010\010\010\010\020\020\010\010\010\020\020\010\010\010\010\020\010\020\010\020\010\010\010\010\010\020\020\020\020\010\010\010\010\020\020\020\020\010\010\010\010\012\022\012\022\012\012\012\012\001\002\001\002\001\001\001\001\001\002\002\002\002\001\001\001\001\002\002\002\002\001\001\001\001\003\004\003\004\003\003\003\003\001\002\021\032\043\042\053\064\022\012\024\012\024\012\024\012\024\012\024\012\024\012\024\012\024\012\024\012\024\011\022\011\022\012\023\012\023\012\023\012\024\012\024\012\024\000\021\011\021\012\012\022\011\021\021\012\022\012\011\011\011\014\012\012\014\014\001\001\001\001\001\001\002\002\002\002\002\002\002\002\000\000\000\001\001\001\002\001\002\001\000\100\100\100\100\100\100\100\100\177\010\011\011\011\001\011\011\001\001\177\100\001\001\000\000";
}
