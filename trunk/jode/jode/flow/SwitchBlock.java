/* 
 * SwitchBlock  (c) 1998 Jochen Hoenicke
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

/**
 * This is the structured block for an empty block.
 */
public class SwitchBlock extends InstructionContainer 
implements BreakableBlock {
    CaseBlock[] caseBlocks;

    public SwitchBlock(jode.Instruction instr,
		       int[] cases, int[] dests) {
	super(instr);
        this.caseBlocks = new CaseBlock[dests.length];
	int lastDest = -1;
	boolean lastBlock = true;
        for (int i=dests.length-1; i>=0; i--) {
	    /**
	     * Sort the destinations by finding the greatest destAddr
	     */
	    int index = 0;
	    for (int j=1; j<dests.length; j++) {
		if (dests[j] >= dests[index])
		    index = j;
	    }

	    int value;
	    if (index == cases.length)
		value = -1;
	    else
		value = cases[index];

	    if (dests[index] == lastDest)
		this.caseBlocks[i] = new CaseBlock(value);
	    else
		this.caseBlocks[i] = new CaseBlock(value, 
						   new Jump(dests[index]));
	    this.caseBlocks[i].outer = this;
	    this.caseBlocks[i].isLastBlock = lastBlock;
	    lastBlock = false;
	    lastDest = dests[index];
	    dests[index] = -1;
	    if (index == cases.length)
		this.caseBlocks[i].isDefault = true;
        }
        this.jump = null;
        isBreaked = false;
    }

    /**
     * Find the case that jumps directly to destination.
     * @return The sub block of the case block, which jumps to destination.
     */
    public StructuredBlock findCase(FlowBlock destination) {
	for (int i=0; i < caseBlocks.length; i++) {
	    if (caseBlocks[i].subBlock != null
		&& caseBlocks[i].subBlock instanceof EmptyBlock
		&& caseBlocks[i].subBlock.jump != null
		&& caseBlocks[i].subBlock.jump.destination == destination)
		
		return caseBlocks[i].subBlock;
	}
	return null;
    }

    /**
     * Find the case that precedes the given case.
     * @param block The sub block of the case, whose predecessor should
     * be returned.
     * @return The sub block of the case precedes the given case.
     */
    public StructuredBlock prevCase(StructuredBlock block) {
	for (int i=caseBlocks.length-1; i>=0; i--) {
	    if (caseBlocks[i].subBlock == block) {
		for (i--; i>=0; i--) {
		    if (caseBlocks[i].subBlock != null)
			return caseBlocks[i].subBlock;
		}
	    }
	}
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
	for (int i=0; i< caseBlocks.length-1; i++) {
	    if (subBlock == caseBlocks[i]) {
		return caseBlocks[i+1];
	    }
	}
        return getNextBlock();
    }

    public FlowBlock getNextFlowBlock(StructuredBlock subBlock) {
	for (int i=0; i< caseBlocks.length-1; i++) {
	    if (subBlock == caseBlocks[i]) {
		return null;
	    }
	}
        return getNextFlowBlock();
    }

    public void dumpInstruction(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
        if (label != null) {
            writer.untab();
            writer.println(label+":");
            writer.tab();
        }
        writer.println("switch ("+instr+") {");
	for (int i=0; i < caseBlocks.length; i++)
	    caseBlocks[i].dumpSource(writer);
	writer.println("}");
    }

    public void setInstruction(jode.Instruction instr) {
	super.setInstruction(instr);
	sun.tools.java.Type type = instr.getType();
	if (type != caseBlocks[0].type) {
	    for (int i=0; i < caseBlocks.length; i++)
		caseBlocks[i].type = type;
	}
    }

    /**
     * Returns all sub block of this structured block.
     */
    public StructuredBlock[] getSubBlocks() {
        return caseBlocks;
    }

    boolean isBreaked = false;

    /**
     * The serial number for labels.
     */
    static int serialno = 0;

    /**
     * The label of this instruction, or null if it needs no label.
     */
    String label = null;

    /**
     * Returns the label of this block and creates a new label, if
     * there wasn't a label previously.
     */
    public String getLabel() {
        if (label == null)
            label = "switch_"+(serialno++)+"_";
        return label;
    }

    /**
     * Is called by BreakBlock, to tell us that this block is breaked.
     */
    public void setBreaked() {
	isBreaked = true;
    }

    /**
     * Determines if there is a sub block, that flows through to the end
     * of this block.  If this returns true, you know that jump is null.
     * @return true, if the jump may be safely changed.
     */
    public boolean jumpMayBeChanged() {
        return !isBreaked 
            && (caseBlocks[caseBlocks.length-1].jump != null
                || caseBlocks[caseBlocks.length-1].jumpMayBeChanged());
    }
}
