/* TypeInstruction Copyright (C) 1999 Jochen Hoenicke.
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
public class TypeInstruction extends Instruction {
    /**
     * The typesignature of the class/array.
     */
    private String typeSig;

    /**
     * Standard constructor: creates an opcode with parameter and
     * lineNr.  
     */
    public TypeInstruction(int opcode, String typeSig, int lineNr) {
	super(opcode, lineNr);
	if (opcode != opc_new && opcode != opc_checkcast
	    && opcode != opc_instanceof && opcode != opc_multianewarray)
	    throw new IllegalArgumentException("Instruction has no typesig");
	this.typeSig = typeSig;
    }

    /**
     * Creates a simple opcode, without any parameters.
     */
    public TypeInstruction(int opcode, String typeSig) {
	this(opcode, typeSig, -1);
    }

    public final String getClazzType() 
    {
	return typeSig;
    }

    public final void setClazzType(String type)
    {
	typeSig = type;
    }

    public String toString() {
	return super.toString()+' '+typeSig;
    }
}
