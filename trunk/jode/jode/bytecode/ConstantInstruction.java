/* ConstantInstruction Copyright (C) 1999 Jochen Hoenicke.
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
import jode.util.StringQuoter;

/**
 * This class represents an instruction in the byte code.
 *
 */
public class ConstantInstruction extends Instruction {
    /**
     * The typesignature of the class/array.
     */
    private Object constant;

    /**
     * Standard constructor: creates an opcode with parameter and
     * lineNr.  
     */
    public ConstantInstruction(int opcode, Object constant, int lineNr) {
	super(opcode, lineNr);
	if (opcode != opc_ldc && opcode != opc_ldc2_w)
	    throw new IllegalArgumentException("Instruction has no typesig");
	this.constant = constant;
    }

    public ConstantInstruction(int opcode, Object constant) {
	this(opcode, constant, -1);
    }

    public final Object getConstant() 
    {
	return constant;
    }

    public final void setConstant(Object constant) 
    {
	this.constant = constant;
    }

    public String toString() {
	return super.toString() + ' ' +
	    (constant instanceof String
	     ? StringQuoter.quote((String) constant) : constant);
    }
}

