/* Subroutine Copyright (C) 2000 Jochen Hoenicke.
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

package net.sf.jode.bytecode;

import java.io.PrintWriter;
///#def COLLECTIONS java.util
import java.util.Collection;
import java.util.Arrays;
import java.util.List;
import java.util.Iterator;
///#enddef

/**
 * <p>Represents a <code>jsr</code>-Subroutine.</p>
 *
 * <p>In my representation a subroutine consists of all blocks from
 * which the ret instruction is reachable.  Every subroutine must have
 * a reachable ret instruction, or the jsr is replaced by a simple goto.
 * </p>
 * 
 * @author Jochen Hoenicke
 * @see net.sf.jode.bytecode.BasicBlocks
 * @see net.sf.jode.bytecode.Block 
 */
public final class Subroutine {
    /**
     * Subroutines may be nested.  This points to the outer subroutine
     * or to null if this doesn't have an outer.
     */
    private Subroutine outer;

    /**
     * Each subroutine has exactly one ret instruction, which is the
     * last instruction in the retBlock.  The local of the ret
     * instruction must equal the local where the first instruction of
     * the subroutine stores to.
     */
    private Block retBlock;

    /**
     * The set of locals that are accessed inside this subroutine.
     */
    private BitSet accessedLocals;

    public Block getRetBlock() {
	return retBlock;
    }

    public Subroutine getOuter() {
	return outer;
    }

    public boolean isAccessed(int slot) {
	return accessedLocals.get(slot);
    }
}
