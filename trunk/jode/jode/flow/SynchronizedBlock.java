/* SynchronizedBlock Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.decompiler.LocalInfo;
import jode.decompiler.TabbedPrintWriter;
import jode.expr.Expression;
import jode.util.SimpleSet;

///#ifdef JDK12
///import java.util.Set;
///#else
import jode.util.Set;
///#endif

/**
 * This class represents a synchronized structured block.
 * 
 * @author Jochen Hoenicke
 */
public class SynchronizedBlock extends StructuredBlock {

    Expression object;
    LocalInfo local;
    boolean isEntered;

    StructuredBlock bodyBlock;

    public SynchronizedBlock(LocalInfo local) {
        this.local = local;
    }
    
    /**
     * Sets the body block.
     */
    public void setBodyBlock(StructuredBlock body) {
        bodyBlock = body;
        body.outer = this;
        body.setFlowBlock(flowBlock);
    }

    /**
     * Returns all sub block of this structured block.
     */
    public StructuredBlock[] getSubBlocks() {
	return new StructuredBlock[] { bodyBlock };
    }

    /**
     * Replaces the given sub block with a new block.
     * @param oldBlock the old sub block.
     * @param newBlock the new sub block.
     * @return false, if oldBlock wasn't a direct sub block.
     */
    public boolean replaceSubBlock(StructuredBlock oldBlock, 
                            StructuredBlock newBlock) {
        if (bodyBlock == oldBlock)
            bodyBlock = newBlock;
        else
            return false;
        return true;
    }

    public Set getDeclarables() {
	Set used = new SimpleSet();
	if (object != null)
	    object.fillDeclarables(used);
	else
	    used.add(local);
	return used;
    }

    public void dumpInstruction(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
        if (!isEntered)
            writer.println("MISSING MONITORENTER");
        writer.print("synchronized (");
	if (object != null)
	    object.dumpExpression(writer);
	else
	    writer.print(local.getName());
	writer.print(")");
	writer.openBrace();
        writer.tab();
        bodyBlock.dumpSource(writer);
        writer.untab();
	writer.closeBrace();
    }

    public void simplify() {
	if (object != null)
	    object = object.simplify();
	super.simplify();
    }
    

    public boolean doTransformations() {
        StructuredBlock last = flowBlock.lastModified;
        return (!isEntered && CompleteSynchronized.enter(this, last))
            || (isEntered && object == null 
                && CompleteSynchronized.combineObject(this, last));
    }
}
