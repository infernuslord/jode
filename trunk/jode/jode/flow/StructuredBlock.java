/* 
 * StructuredBlock  (c) 1998 Jochen Hoenicke
 *
 * You may distribute under the terms of the GNU General Public License.
 *
 * IN NO EVENT SHALL JOCHEN HOENICKE BE LIABLE TO ANY PARTY FOR DIRECT,
 * INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT OF
 * THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF JOCHEN HOENICKE 
 * HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * JOCHEN HOENICKE SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS"
 * BASIS, AND JOCHEN HOENICKE HAS NO OBLIGATION TO PROVIDE MAINTENANCE,
 * SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * $Id$
 */

package jode.flow;
import jode.TabbedPrintWriter;
import jode.LocalInfo;

/**
 * A structured block is the building block of the source programm.
 * For every program construct like if, while, try, or blocks there is
 * a corresponding structured block.
 *
 * Some of these Block are only intermediate representation, that get
 * converted to another block later.
 *
 * Every block has to handle the local variables that it contains.
 * This is done by the in/out vectors and the local variable structure
 * themself.  Every local variable used in this structured block is
 * either in or out.
 *
 * There are following types of structured blocks: 
 * <ul>
 * <li>if-then-(else)-block  (IfThenElseBlock)
 * <li>(do)-while/for-block  (LoopBlock)
 * <li>switch-block          (SwitchBlock)
 * <li>try-catch-block       (CatchBlock)
 * <li>try-finally-block     (FinallyBlock)
 * <li>synchronized-block    (SynchronizedBlock)
 * <li>one-instruction       (InstructionBlock)
 * <li>empty-block           (EmptyBlock)
 * <li>multi-blocks-block    (SequentialBlock)
 * </ul>
 */

public abstract class StructuredBlock {
    /* Invariants:
     * in.intersection(out) = empty
     * outer != null => flowBlock = outer.flowBlock
     * outer == null => flowBlock.block = this
     * jump  == null => outer != null
     * either getNextBlock() != null 
     *     or getNextFlowBlock() != null or outer == null
     * either outer.getNextBlock(this) != null 
     *     or outer.getNextFlowBlock(this) != null
     */

    /**
     * The variable set containing all variables that are used in
     * this block.
     */
    VariableSet used = new VariableSet();

    /**
     * The variable set containing all variables we must declare.
     * The analyzation is done in makeDeclaration
     */
    VariableSet declare;

    /**
     * The surrounding structured block.  If this is the outermost
     * block in a flow block, outer is null.  */
    StructuredBlock outer;

    /**
     * The flow block in which this structured block lies.
     */
    FlowBlock flowBlock;
    
    /** 
     * The jump that follows on this block, or null if there is no
     * jump, but the control flows normal (only allowed if
     * getNextBlock != null).  
     */
    Jump jump;

    /**
     * Returns the block where the control will normally flow to, when
     * this block is finished (not ignoring the jump after this block).
     */
    public StructuredBlock getNextBlock() {
        if (jump != null)
            return null;
        if (outer != null)
            return outer.getNextBlock(this);
        return null;
    }

    public void setJump(Jump jump) {
        this.jump = jump;
        jump.prev = this;
    }

    /**
     * Returns the flow block where the control will normally flow to,
     * when this block is finished (not ignoring the jump after this
     * block).  
     * @return null, if the control flows into a non empty structured
     * block or if this is the outermost block.
     */
    public FlowBlock getNextFlowBlock() {
        if (jump != null)
            return jump.destination;
        if (outer != null)
            return outer.getNextFlowBlock(this);
        return null;
    }

    /**
     * Checks if the jump to the outside has correct monitorexit and
     * jsr instructions attached.
     * @return null, if everything is okay, and a diagnostic message that
     * should be put in a comment otherwise.
     */
    public String checkJump(Jump jump) {
        if (outer != null)
            return outer.checkJump(jump);
        if (jump.hasAttachments())
            return "Unknown attachments: "+jump.describeAttachments();
        return null;
    }

    /**
     * Returns the block where the control will normally flow to, when
     * the given sub block is finished (<em>not</em> ignoring the jump
     * after this block). (This is overwritten by SequentialBlock and
     * SwitchBlock).  If this isn't called with a direct sub block,
     * the behaviour is undefined, so take care.  
     * @return null, if the control flows to another FlowBlock.  */
    public StructuredBlock getNextBlock(StructuredBlock subBlock) {
        return getNextBlock();
    }

    public FlowBlock getNextFlowBlock(StructuredBlock subBlock) {
        return getNextFlowBlock();
    }

    /**
     * Tells if this block is empty and only changes control flow.
     */
    public boolean isEmpty() {
        return false;
    }

    /**
     * Tells if the sub block is the single exit point of the current block.
     * @param subBlock the sub block.
     * @return true, if the sub block is the single exit point of the
     * current block.  
     */
    public boolean isSingleExit(StructuredBlock subBlock) {
	return false;
    }

    /**
     * Replaces the given sub block with a new block.
     * @param oldBlock the old sub block.
     * @param newBlock the new sub block.
     * @return false, if oldBlock wasn't a direct sub block.
     */
    public boolean replaceSubBlock(StructuredBlock oldBlock, 
                                   StructuredBlock newBlock) {
        return false;
    }

    /**
     * Returns all sub block of this structured block.
     */
    public StructuredBlock[] getSubBlocks() {
        return new StructuredBlock[0];
    }

    /**
     * Returns if this block contains the given block.
     * @param child the block which should be contained by this block.
     * @return false, if child is null, or is not contained in this block.
     */
    public boolean contains(StructuredBlock child) {
        while (child != this && child != null)
            child = child.outer;
        return (child == this);
    }

    /** 
     * Removes the jump.  This does also update the successors vector
     * of the flow block.  */
    public void removeJump() {
        if (jump != null) {
            jump.prev = null;
            flowBlock.removeSuccessor(jump);
            jump = null;
        }
    }

    /**
     * This will move the definitions of sb and childs to this block,
     * but only descend to sub and not further.  It is assumed that
     * sub will become a sub block of this block, but may not yet.  
     *
     * @param sb The structured block that should be replaced.  
     * @param sub The uppermost sub block of structured block, that
     * will be moved to this block (may be this).  
     */
    void moveDefinitions(StructuredBlock from, StructuredBlock sub) {
        if (from != sub && from != this) {
            /* define(...) will not move from blocks, that are not sub blocks,
             * so we do it by hand.
             */
            java.util.Enumeration enum = from.used.elements();
            while (enum.hasMoreElements()) {
                LocalInfo var = 
                    ((LocalInfo) enum.nextElement()).getLocalInfo();
                if (!used.contains(var))
                    used.addElement(var);
            }
            from.used.removeAllElements();
            StructuredBlock[] subs = from.getSubBlocks();
            for (int i=0; i<subs.length; i++)
                moveDefinitions(subs[i], sub);
        }
    }

    /**
     * This function replaces sb with this block.  It copies outer and
     * from sb, and updates the outer block, so it knows that sb was
     * replaced.  You have to replace sb.outer or mustn't use sb
     * anymore.  <p>
     * It will also move the definitions of sb and childs to this block,
     * but only descend to sub and not further.  It is assumed that
     * sub will become a sub block of this block.
     * @param sb The structured block that should be replaced.  
     * @param sub  The uppermost sub block of structured block, 
     *      that will be moved to this block (may be this).
     */
    public void replace(StructuredBlock sb, StructuredBlock sub) {
        moveDefinitions(sb, sub);
        outer = sb.outer;
        setFlowBlock(sb.flowBlock);
        if (outer != null) {
            outer.replaceSubBlock(sb, this);
        } else {
            flowBlock.block = this;
        }
    }

    /**
     * This function swaps the jump with another block.
     * @param block The block whose jump is swapped.
     */
    public void swapJump(StructuredBlock block) {
        Jump tmp = block.jump;
        block.jump = jump;
        jump = tmp;
        
        jump.prev = this;
        block.jump.prev = block;
    }

    /**
     * This function moves the jump to this block.  
     * The jump field of the previous owner is cleared afterwards.  
     * If the given jump is null, nothing bad happens.
     * @param jump The jump that should be moved, may be null.
     */
    public void moveJump(Jump jump) {
	removeJump();
        this.jump = jump;
        if (jump != null) {
            jump.prev.jump = null;
            jump.prev = this;
        }
    }

    /**
     * Appends a block to this block.
     * @return the new combined block.
     */
    public StructuredBlock appendBlock(StructuredBlock block) {
	SequentialBlock sequBlock = new SequentialBlock();
	sequBlock.replace(this, this);
	sequBlock.setFirst(this);
	sequBlock.setSecond(block);
	return sequBlock;
    }

    /**
     * Determines if there is a sub block, that flows through to the end
     * of this block.  If this returns true, you know that jump is null.
     * @return true, if the jump may be safely changed.
     */
    public boolean jumpMayBeChanged() {
        return false;
    }

    public void define(VariableSet vars) {
        java.util.Enumeration enum = vars.elements();
        while (enum.hasMoreElements()) {
            LocalInfo var = ((LocalInfo) enum.nextElement()).getLocalInfo();
            used.addElement(var);
        }
    }

    public VariableSet propagateUsage() {
        StructuredBlock[] subs = getSubBlocks();
        VariableSet allUse = (VariableSet) used.clone();
        for (int i=0; i<subs.length; i++) {
            VariableSet childUse = subs[i].propagateUsage();
            /* All variables used in more than one sub blocks, are
             * used in this block, too.  
             */
            used.addExact(allUse.intersectExact(childUse));
            allUse.addExact(childUse);
        }
        return allUse;
    }

    /**
     * Make the declarations, i.e. initialize the declare variable
     * to correct values.  This will declare every variable that
     * is marked as used, but not done.
     * @param done The set of the already declare variables.
     */
    public void makeDeclaration(VariableSet done) {
	declare = new VariableSet();
	java.util.Enumeration enum = used.elements();
	while (enum.hasMoreElements()) {
	    LocalInfo local = ((LocalInfo) enum.nextElement()).getLocalInfo();
	    if (!declare.contains(local))
		declare.addElement(local);
	}
	declare.subtractExact(done);
	done.addExact(declare);

        StructuredBlock[] subs = getSubBlocks();
	for (int i=0; i<subs.length; i++)
	    subs[i].makeDeclaration(done);
    }

    public void checkConsistent() {
        StructuredBlock[] subs = getSubBlocks();
        for (int i=0; i<subs.length; i++) {
            if (subs[i].outer != this ||
                subs[i].flowBlock != flowBlock) {
                throw new RuntimeException("Inconsistency");
            }
            subs[i].checkConsistent();
        }
        if (jump != null && 
            (jump.prev != this || 
             !flowBlock.successors.contains(jump) ||
             !jump.destination.predecessors.contains(flowBlock))) {
                throw new RuntimeException("Inconsistency");
        }
    }

    /**
     * Set the flow block of this block and all sub blocks.
     * @param flowBlock the new flow block
     */
    public void setFlowBlock(FlowBlock flowBlock) {
        if (this.flowBlock != flowBlock) {
            this.flowBlock = flowBlock;
            StructuredBlock[] subs = getSubBlocks();
            for (int i=0; i<subs.length; i++) {
                if (subs[i] != null)
                    subs[i].setFlowBlock(flowBlock);
            }
        }
    }

    /**
     * Tells if this block needs braces when used in a if or while block.
     * @return true if this block should be sorrounded by braces.
     */
    public boolean needsBraces() {
        return true;
    }

    /**
     * Fill all in variables into the given VariableSet.
     * @param in The VariableSet, the in variables should be stored to.
     */
    public void fillInSet(VariableSet in) {
        /* overwritten by InstructionContainer */
    }

    /**
     * Put all the successors of this block and all subblocks into
     * the given vector.
     * @param succs The vector, the successors should be stored to.
     */
    public void fillSuccessors(java.util.Vector succs) {
        if (jump != null)
            succs.addElement(jump);
        StructuredBlock[] subs = getSubBlocks();
        for (int i=0; i<subs.length; i++) {
            subs[i].fillSuccessors(succs);
        }
    }

    /**
     * Print the source code for this structured block.  This handles
     * everything that is unique for all structured blocks and calls
     * dumpInstruction afterwards.
     * @param writer The tabbed print writer, where we print to.
     */
    public void dumpSource(jode.TabbedPrintWriter writer)
        throws java.io.IOException
    {
	if (declare != null) {
	    if (jode.Decompiler.isDebugging) {
		writer.println("declaring: "+declare);
		writer.println("using: "+used);
            }

	    java.util.Enumeration enum = declare.elements();
	    while (enum.hasMoreElements()) {
		LocalInfo local = (LocalInfo) enum.nextElement();
		dumpDeclaration(writer, local);
	    }
	}
        dumpInstruction(writer);

        if (jump != null)
            jump.dumpSource(writer);
    }

    /**
     * Print the code for the declaration of a local variable.
     * @param writer The tabbed print writer, where we print to.
     * @param local  The local that should be declared.
     */
    public void dumpDeclaration(jode.TabbedPrintWriter writer, LocalInfo local)
        throws java.io.IOException
    {
	writer.println(local.getType().toString() + " "
                       + local.getName().toString() + ";");
    }

    /**
     * Print the instruction expressing this structured block.
     * @param writer The tabbed print writer, where we print to.
     */
    public abstract void dumpInstruction(TabbedPrintWriter writer)
        throws java.io.IOException;
}

