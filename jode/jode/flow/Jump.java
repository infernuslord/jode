/* Jump Copyright (C) 1998-1999 Jochen Hoenicke.
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

package jode.flow;

/**
 * This class represents an unconditional jump.
 */
public class Jump {
    /**
     * The structured block that precedes this jump.
     */
    StructuredBlock prev;
    /**
     * The destination block of this jump, null if not known, or illegal.
     */
    FlowBlock destination;

    /**
     * The jumps in a flow block, that have the same destination, are
     * in a link list.  This field points to the next jump in this link.
     */
    Jump next;

    /**
     * The kill locals.  This are the slots, which must be overwritten
     * in this block on every path to this jump.  That means, that all
     * paths form the start of the current flow block to this jump
     * contain (unconditional) assignments to this slot.
     */
    VariableSet kill;

    /**
     * The gen locals.  This are the locals, which can be overwritten
     * in this block on a path to this jump.  That means, that there
     * exists a path form the start of the current flow block to this
     * jump that contains an (unconditional) assignments to this
     * local, and that is not overwritten afterwards.  
     */
    VariableSet gen;

    /**
     * The stack map.  This tells how many objects are on stack at
     * begin of the flow block, and to what locals they are maped.
     * @see FlowBlock.mapStackToLocal
     */
    VariableStack stackMap;

    public Jump (FlowBlock dest) {
        this.destination = dest;
	kill = new VariableSet();
	gen = new VariableSet();
    }

    public Jump (Jump jump) {
	destination = jump.destination;
	next = jump.next;
	jump.next = this;
	gen = (VariableSet) jump.gen.clone();
	kill = (VariableSet) jump.kill.clone();
    }

    /**
     * Print the source code for this structured block.  This handles
     * everything that is unique for all structured blocks and calls
     * dumpInstruction afterwards.
     * @param writer The tabbed print writer, where we print to.
     */
    public void dumpSource(jode.decompiler.TabbedPrintWriter writer)
        throws java.io.IOException
    {
        if (jode.Decompiler.debugInOut) {
            writer.println("gen : "+ gen.toString());
            writer.println("kill: "+ kill.toString());
        }
        if (destination == null)
            writer.println ("GOTO null-ptr!!!!!");
        else
            writer.println("GOTO "+destination.getLabel());
    }
}

