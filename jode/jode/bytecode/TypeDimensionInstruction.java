/* TypeDimensionInstruction Copyright (C) 1999 Jochen Hoenicke.
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
public class TypeDimensionInstruction extends TypeInstruction {
    /**
     * The dimension of this multianewarray operation.
     */
    private int dimension;

    /**
     * Standard constructor: creates an opcode with parameter and
     * lineNr.  
     */
    public TypeDimensionInstruction(int opcode, String type, int dimension,
				     int lineNr) {
	super(opcode, type, lineNr);
	if (opcode != opc_multianewarray)
	    throw new IllegalArgumentException("Instruction has no dimension");
	this.dimension = dimension;
    }

    /**
     * Creates a simple opcode, without any parameters.
     */
    public TypeDimensionInstruction(int opcode, String type, int dimension) {
	this(opcode, type, dimension, -1);
    }

    /**
     * Get the dimensions for an opc_anewarray opcode.
     */
    public final int getDimensions()
    {
	return dimension;
    }

    /**
     * Get the dimensions for an opc_anewarray opcode.
     */
    public final void setDimensions(int dim)
    {
	dimension = dim;
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
	poppush[0] = dimension;
	poppush[1] = 1;
    }

    public String toString() {
	return super.toString()+' '+dimension;
    }
}
