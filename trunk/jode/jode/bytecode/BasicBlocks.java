/* BasicBlocks Copyright (C) 1999 Jochen Hoenicke.
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
import java.io.IOException;
import java.io.PrintWriter;

///#def COLLECTIONS java.util
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
///#enddef
///#def COLLECTIONEXTRA java.lang
import java.lang.UnsupportedOperationException;
///#enddef

/**
 * <p>This class gives another representation of the byte code of a
 * method.  The instructions are splitted into BasicBlocks, each an
 * array of consecutive Instructions.  It is not allowed that a goto
 * jumps inside a basic block.</p>
 *
 * <p>All jumps must be at the end of the block.  A conditional jump
 * may be followed by a single goto, but there must not be any other
 * jumps.  If there is now unconditional jump at the end, the block
 * implicitely flows into the next one. </p>
 *
 * <p>Try block must span over some consecutive BasicBlocks and there
 * catches must jump to the start of an basic block. </p>
 *
 * <p>Deadcode will not be included in the BasicBlock, also
 * BasicBlocks consisting of a single jump will be optimized away.</p>
 *
 * @see jode.bytecode.Instruction */
public class BasicBlocks extends BinaryInfo {
    
    /**
     * The method info which contains the basic blocks.
     */
    private MethodInfo methodInfo;
    /**
     * The maximal number of stack entries, that may be used in this
     * method.  
     */
    int maxStack;
    /**
     * The maximal number of local slots, that may be used in this
     * method.  
     */
    int maxLocals;

    /**
     * This is an array of blocks, which are arrays
     * of Instructions.
     */
    private Block[] blocks;

    /**
     * The start block. Normally the first block, but differs if method start
     * with a goto, e.g a while.  This may be null, if this method is empty.
     */
    private Block startBlock;

    /**
     * The local variable infos for the method parameters.
     */
    private LocalVariableInfo[] paramInfos;

    /**
     * The array of exception handlers.
     */
    private Handler[] exceptionHandlers;

    public BasicBlocks(MethodInfo mi) {
	methodInfo = mi;
	int paramSize = (mi.isStatic() ? 0 : 1)
	    + TypeSignature.getArgumentSize(mi.getType());
	paramInfos = new LocalVariableInfo[paramSize];
	for (int i=0; i< paramSize; i++)
	    paramInfos[i] = LocalVariableInfo.getInfo(i);
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

    public Block getStartBlock() {
	return startBlock;
    }
   
    public Block[] getBlocks() {
	return blocks;
    }

    public Iterator getAllInstructions() {
	return new Iterator() {
	    int blockNr = 0;
	    Iterator blockIter = getNextIterator();
	    
	    public boolean hasNext() {
		return blockIter != null;
	    }
	    
	    public Iterator getNextIterator() {
		if (blockNr < blocks.length)
		    return blocks[blockNr++].getInstructions().iterator();
		return null;
	    }
	    
	    public Object next() {
		Object instr;
		try {
		    instr = blockIter.next();
		} catch (NullPointerException ex) {
		    throw new NoSuchElementException();
		}
		if (!blockIter.hasNext())
		    blockIter = getNextIterator();
		return instr;
	    }
	    
	    public void remove() {
		throw new UnsupportedOperationException();
	    }
	};
    }

    /**
     * @return the exception handlers, or null if the method has no
     * exception handlers.
     */
    public Handler[] getExceptionHandlers() {
	return exceptionHandlers;
    }

    public LocalVariableInfo getParamInfo(int i) {
	return paramInfos[i];
    }

    public int getParamCount() {
	return paramInfos.length;
    }

    public void setMaxStack(int ms) {
        maxStack = ms;
    }

    public void setMaxLocals(int ml) {
        maxLocals = ml;
    }

    public void setBlocks(Block[] blocks, Block startBlock) {
	for (int i = 0; i < blocks.length; i++)
	    blocks[i].blockNr = i;
	this.blocks = blocks;
	this.startBlock = startBlock;
	this.exceptionHandlers = null;
    }

    public void setExceptionHandlers(Handler[] handlers) {
	exceptionHandlers = handlers.length == 0 ? Handler.EMPTY : handlers;
	ArrayList activeHandlers = new ArrayList();
	for (int i = 0; i < blocks.length; i++) {
	    for (int j = 0; j < handlers.length; j++) {
		if (handlers[j].getStart() == blocks[i])
		    activeHandlers.add(handlers[j]);
		if (handlers[j].getEnd() == blocks[i])
		    activeHandlers.remove(handlers[j]);
	    }
	    if (activeHandlers.size() == 0)
		blocks[i].catchers = Handler.EMPTY;
	    else
		blocks[i].catchers = 
		    (Handler[]) activeHandlers.toArray(Handler.EMPTY);
	}
    }

    /**
     * Sets the name and type of a method parameter. This overwrites
     * any previously set parameter info for this slot.
     * @param info a local variable info mapping a slot nr to a name
     * and a type.
     */
    public void setParamInfo(LocalVariableInfo info) {
	paramInfos[info.getSlot()] = info;
    }

    private BasicBlockReader reader;
    public void read(ConstantPool cp, 
		     DataInputStream input, 
		     int howMuch) throws IOException {
	if ((GlobalOptions.debuggingFlags
	     & GlobalOptions.DEBUG_BYTECODE) != 0)
	    GlobalOptions.err.println("Reading "+methodInfo);
	reader = new BasicBlockReader(this);
	reader.readCode(cp, input);
	readAttributes(cp, input, howMuch);
  	reader.convert();
	reader = null;
	if ((GlobalOptions.debuggingFlags
	     & GlobalOptions.DEBUG_BYTECODE) != 0)
	    dumpCode(GlobalOptions.err);
    }

    protected void readAttribute(String name, int length, ConstantPool cp,
				 DataInputStream input, 
				 int howMuch) throws IOException {
	if (howMuch >= ClassInfo.ALMOSTALL
	    && name.equals("LocalVariableTable")) {
	    reader.readLVT(length, cp, input);
	} else if (howMuch >= ClassInfo.ALMOSTALL
		   && name.equals("LineNumberTable")) {
	    reader.readLNT(length, cp, input);
	} else
	    super.readAttribute(name, length, cp, input, howMuch);
    }


    void reserveSmallConstants(GrowableConstantPool gcp) {
	for (int i=0; i < blocks.length; i++) {
	next_instr:
	    for (Iterator iter = blocks[i].getInstructions().iterator(); 
		 iter.hasNext(); ) {
		Instruction instr = (Instruction) iter.next();
		if (instr.getOpcode() == Opcodes.opc_ldc) {
		    Object constant = instr.getConstant();
		    if (constant == null)
			continue next_instr;
		    for (int j=1; j < Opcodes.constants.length; j++) {
			if (constant.equals(Opcodes.constants[j]))
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
    }

    BasicBlockWriter bbw;
    void prepareWriting(GrowableConstantPool gcp) {
	bbw = new BasicBlockWriter(this, gcp);
    }

    int getKnownAttributeCount() {
	return bbw.getAttributeCount();
    }

    void writeKnownAttributes(GrowableConstantPool gcp,
			      DataOutputStream output)
	throws IOException {
	bbw.writeAttributes(gcp, output);
    }

    void write(GrowableConstantPool gcp, 
	       DataOutputStream output) throws IOException {
	output.writeInt(bbw.getSize() + getAttributeSize());
	bbw.write(gcp, output);
	writeAttributes(gcp, output);
	bbw = null;
    }
    
    public void dumpCode(PrintWriter output) {
	output.println(methodInfo.getName()+methodInfo.getType()+":");
	if (startBlock == null)
	    output.println("\treturn");
	else if (startBlock != blocks[0])
	    output.println("\tgoto "+startBlock);

	for (int i=0; i< blocks.length; i++) {
	    blocks[i].dumpCode(output);
	}
	for (int i=0; i< exceptionHandlers.length; i++) {
	    output.println("catch " + exceptionHandlers[i].type 
			   + " from " + exceptionHandlers[i].start
			   + " to " + exceptionHandlers[i].end
			   + " catcher " + exceptionHandlers[i].catcher);
	}
    }

    public String toString() {
        return "BasicBlocks["+methodInfo+"]";
    }
}
