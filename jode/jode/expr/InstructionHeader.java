/* 
 * InstructionHeader (c) 1998 Jochen Hoenicke
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

package jode;
import java.util.Vector;
import sun.tools.java.Type;

/**
 * This class maintains the connections between the
 * InstructionHeaders.  They are connected in a doubly linked list
 * (but a instruction may have multiple successors and predecessors).
 * @see JumpInstructionHeader
 * @see SwitchInstructionHeader
 * @author Jochen Hoenicke
 */
public class InstructionHeader {
    public final static int METHOD   =-1;
    public final static int NORMAL   = 0;
    public final static int GOTO     = 1;
    public final static int IFGOTO   = 2;
    public final static int SWITCH   = 3;
    public final static int JSR      = 4;
    public final static int RET      = 5;
    public final static int RETURN   = 6;
    public final static int TRY      = 7;
    public final static int CATCH    = 8;
    public final static int IFSTATEMENT      = 10;
    public final static int WHILESTATEMENT   = 11;
    public final static int DOWHILESTATEMENT = 12;
    public final static int FORSTATEMENT     = 13;
    public final static int SWITCHSTATEMENT  = 14;
    public final static int TRYCATCHBLOCK    = 15;
    public final static int CASESTATEMENT    = 16;
    public final static int BREAK      = 20;
    public final static int CONTINUE   = 21;
    public final static int VOIDRETURN = 22;

    public final static int EMPTY    = 99;

    /** 
     * The flow type of the instruction header, this is one of the
     * above constants.  
     */
    int flowType;
    /**
     * The address of this and the following instruction header.
     */
    int addr, nextAddr;
    /**
     * The underlying instruction.  This is null for some special
     * instructions (depends on flowType).
     */
    Instruction  instr;
    /**
     * The instruction header representing the surrounding block.
     */
    InstructionHeader outer = null;
    /**
     * A simple doubly linked list of instructions in code order
     * (without caring about jump instructions).
     */
    InstructionHeader nextInstruction, prevInstruction;
    /** 
     * This should be implemented for those blocks, that is headers
     * which are outer of other headers.  This gives the instruction
     * where the control flows after this block.
     * @return the first instruction after this block.  
     */
    InstructionHeader getEndBlock() {
        if (nextInstruction != null) {
            return nextInstruction.getShadow();
        }
        return outer.getEndBlock();
    }

    /** 
     * Get the instruction header where this jumps to if this is a 
     * unconditional goto.  Otherwise returns this header.
     */
    InstructionHeader getShadow() {
        return (flowType == GOTO)? successors[0].getShadow() : this;
    }

    /**
     * A more complex doubly linked list of instructions.  The
     * successors are the possible destinations of jump and switch
     * instructions.  The predecessors are the reverse things.
     * The predeccors sits in a vector to make it easier to add
     * or remove one, when creating or manipulating other instruction
     * headers.
     */
    InstructionHeader[] successors;
    Vector predecessors = new Vector();

    /**
     * The addresses of the successors of this header.  This are
     * resolved by resolveSuccessors and then deleted.  
     */
    int[] succs = null;

    /**
     * Create a new InstructionHeader, that must be resolved later.
     * @param flowType The type of this instruction header.
     * @param addr     The address of this instruction header.
     * @param nextAddr The address of the next instruction header.
     * @param instr    The underlying Instruction.
     * @param succs    The successors of this instruction header 
     *                 (the addresses, are resolved later).
     */
    protected InstructionHeader(int flowType, int addr, int nextAddr, 
                                Instruction instr, int[] succs) {
        this.flowType = flowType;
	this.addr = addr;
	this.nextAddr = nextAddr;
	this.instr = instr;
        this.succs = succs;
    }

    /**
     * Create a new InstructionHeader.
     * @param flowType The type of this instruction header.
     * @param addr     The address of this instruction header.
     * @param nextAddr The address of the next instruction header.
     * @param successors The successors of this instruction header.
     * @param outer    The instruction header of the surrounding block.
     */
    protected InstructionHeader(int flowType, int addr, int nextAddr, 
                                InstructionHeader[] successors,
                                InstructionHeader outer) {
        this.flowType = flowType;
	this.addr = addr;
	this.nextAddr = nextAddr;
	this.instr = instr;
        this.successors = successors;
        this.outer = outer;
    }

    /**
     * Create a new InstructionHeader of zero length.
     * @param flowType The type of this instruction header.
     * @param addr     The address of this Instruction.
     * @param outer    The instruction header of the surrounding block.
     */
    protected InstructionHeader(int flowType, int addr, 
                                InstructionHeader outer) {
        this(flowType, addr, addr, new InstructionHeader[0], outer);
    }

    /**
     * Create an InstructionHeader for a normal instruction.
     * @param addr   The address of this instruction.
     * @param length The length of this instruction.
     * @param instr  The underlying Instruction.
     */
    public static InstructionHeader createNormal(int addr, int length, 
                                                 Instruction instr) {
        int[] succs = { addr + length };
        return new InstructionHeader(NORMAL, addr, addr + length, 
                                     instr, succs);
    }

    /**
     * Create an InstructionHeader for a return
     * @param addr   The address of this instruction.
     * @param length The length of this instruction.
     * @param instr  The underlying Instruction.
     */
    public static InstructionHeader createReturn(int addr, int length, 
                                                 Instruction instr) 
    {
         return new InstructionHeader(RETURN, addr, addr + length, 
                                      instr, new int[0]);
    }

    /**
     * Create an InstructionHeader for an unconditional jump.  
     * @param addr   The address of this instruction.
     * @param length The length of this instruction.
     * @param instr  The underlying Instruction.
     * @param dest   The destination address of the jump.
     */
    public static InstructionHeader createGoto(int addr, int length, int dest) 
    {
         int [] succs = { dest };
         return new InstructionHeader(GOTO, addr, addr + length, null, succs);
    }

    /**
     * Create an InstructionHeader for a conditional jump.  
     * @param addr   The address of this instruction.
     * @param length The length of this instruction.
     * @param instr  The underlying Instruction.
     * @param dest   The destination address of the jump.
     */
    public static InstructionHeader createIfGoto(int addr, int length, 
                                                 int dest, Instruction instr) {
        int[] succs = { addr+length , dest };
        return new InstructionHeader (IFGOTO, addr, addr + length, 
                                      instr, succs);
    }

    /**
     * Create an InstructionHeader for a switch.
     * @param addr   The address of this Instruction.
     * @param length The length of this Instruction.
     * @param instr  The underlying Instruction.
     * @param cases  The possible cases
     * @param succs  The destinations (one longer for default)
     */
    public static InstructionHeader createSwitch(int addr, int length, 
                                                 Instruction instr,
                                                 int[] cases, int[] succs) {
        InstructionHeader ih = 
            new SimpleSwitchInstructionHeader(addr, addr+length, instr, 
                                              cases, succs);
        return ih;
    }

    static int ids =0;
    int id = -1;
    public String toString() {
        if (id == -1) id = ids++;
        return addr+"__"+id;
    }

    /**
     * Returns the InstructionHeader where a break of this instruction
     * would jump to. Does only make sense for do/while/for-loops and
     * switch instructions.
     */
    public InstructionHeader getBreak() {
        return null;
    }

    /**
     * Returns the InstructionHeader where a continue of this instruction
     * would jump to. Does only make sense for do/while/for-loops.
     */
    public InstructionHeader getContinue() {
        return null;
    }

    /**
     * Get the flow type of this instruction.
     * @return the flow type.
     */
    public int getFlowType() {
        return flowType;
    }

    static int serialnr = 0;
    String label = null;

    /**
     * Get the label of this instruction.  It is created on the fly
     * if it didn't exists before.
     * @return the label.
     */
    public String getLabel() {
        if (label == null)
            label = "label_" + serialnr++;
        return label;
    }

    /**
     * Get the address of this instruction.  This is probably only useful
     * for debugging purposes.
     * @return the address.
     */
    public int getAddress() {
        return addr;
    }

    /**
     * Get the next address in code order.
     * @return the next instruction
     */
    public int getNextAddr() {
	return nextAddr;
    }

    /**
     * Get the underlying instruction.
     * @return the underlying instruction.
     */
    public Instruction getInstruction() {
	return instr;
    }

    /**
     * Set the underlying instruction.
     * @param instr the underlying instruction.
     */
    public void setInstruction(Instruction instr) {
	this.instr = instr;
    }

    /**
     * Get the next instruction in code order.  This function mustn't
     * be called before resolveSuccessors is executed for this
     * InstructionHeaders.  
     * @return the next instruction
     */
    public InstructionHeader getNextInstruction() {
	return nextInstruction;
    }
    
    /**
     * Get the successors of this instructions.  This function mustn't
     * be called before resolveSuccessors is executed for this
     * InstructionHeaders.  
     * @return Array of successors.  
     */
    public InstructionHeader[] getSuccessors() {
	return successors;
    }

    /**
     * Returns true if this instruction header needs a label.
     */
    protected boolean needsLabel() {
        if (label != null)
            return true;
        /* An instruction my have only one prevInstruction, but
         * may have more then one predecessor that has its
         * nextInstruction pointing to us.
         */
        for (int i=0; i<predecessors.size(); i++) {
            InstructionHeader ih = 
                (InstructionHeader)predecessors.elementAt(i);
            if ((ih.flowType == GOTO || ih.flowType == IFGOTO) && 
                ih.getEndBlock() != this)
                return true;
        }
        return false;
    }

    /**
     * Get the unique predecessor or null if there isn't a 
     * unique predecessor.
     */
    public InstructionHeader getUniquePredecessor() {
        return (predecessors.size() == 1 &&
                predecessors.elementAt(0) == prevInstruction) ? 
            prevInstruction : null;
    }

    /**
     * Get the unique predecessor which mustn't be a conditional jump
     * @return the predecessor or null if there isn't a such a thing
     */
    public InstructionHeader getSimpleUniquePredecessor() {
        InstructionHeader pre = getUniquePredecessor();
        return (pre.getSuccessors().length != 1) ? null : pre;
    }
    
    /**
     * Get the predecessors of this instruction.  This function mustn't
     * be called before resolveSuccessors is executed for all
     * InstructionHeaders.  
     * @return Vector of predecessors.
     */
    public Vector getPredecessors() {
	return predecessors;
    }

    /**
     * Resolve the successors and predecessors and build a doubly
     * linked list.  
     * @param instHeaders an array of the InstructionHeaders, indexed
     * by addresses.
     */
    public void resolveSuccessors(InstructionHeader[] instHeaders) {
        if (nextAddr < instHeaders.length) {
            nextInstruction = instHeaders[nextAddr];
            instHeaders[nextAddr].prevInstruction = this;
        } else
            nextInstruction = null;
        successors = new InstructionHeader[succs.length];
        for (int i=0; i< succs.length; i++) {
            successors[i] = instHeaders[succs[i]];
            successors[i].predecessors.addElement(this);
        }
        succs = null;
    }

    public void dumpDebugging(TabbedPrintWriter writer)
	throws java.io.IOException
    {
        writer.println("");
        writer.print(""+toString()+
                     ": <"+addr + " - "+(nextAddr-1)+">  preds: ");
        for (int i=0; i<predecessors.size(); i++) {
            if (i>0) writer.print(", ");
            writer.print(""+predecessors.elementAt(i));
        }
        writer.println("");
        writer.print("out: "+outer + 
                     " prev: "+prevInstruction+", next: "+ nextInstruction +
                     "  succs: ");
        for (int i=0; i<successors.length; i++) {
            if (i>0) writer.print(", ");
            writer.print(""+successors[i]);
        }
        writer.println("");
    }


    public void dumpSource(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
	if (Decompiler.isDebugging) {
            dumpDebugging(writer);
	    writer.tab();
	}
                         
	if (needsLabel()) {
            writer.untab();
	    writer.println(getLabel()+": ");
            writer.tab();
        }

        if (flowType == IFGOTO) {

            writer.println("if ("+instr.toString()+")");
            writer.tab();
            if (successors[1] != getEndBlock())
                writer.println("goto "+successors[1].getLabel());
            else
                writer.println("/*empty*/;");
            writer.untab();

        } else if (flowType == GOTO) {

            if (successors[0] != getEndBlock())
                writer.println("goto "+successors[0].getLabel());

        } else if (flowType == RETURN) { 

            writer.println((instr != null ? instr.toString() : 
                            "return") + ";");

        } else {

            if (flowType != EMPTY) {
                if (!(instr instanceof NopOperator)) {
                    if (instr.getType() != MyType.tVoid)
                        writer.print("push ");
                    writer.println(instr.toString()+";");
                }
            }

        }
	if (Decompiler.isDebugging)
	    writer.untab();
    }
    
    /**
     * Moves the predecessors from the InstructionHeader <em>from</em> to
     * the current instruction.  The current predecessors are overwritten
     * and you must make sure that no live InstructionHeader points to
     * the current. <p>
     * The predecessors of <em>from</em> are informed about this change.
     * @param from The instruction header which predecessors are moved.
     */
    public void movePredecessors(InstructionHeader from) {
        if (this == from)
            return;
        addr = from.addr;
        prevInstruction = from.prevInstruction;
        if (prevInstruction != null) {
            prevInstruction.nextInstruction = this;
        }
        predecessors = from.predecessors;
        for (int i=0; i < predecessors.size(); i++) {
            InstructionHeader pre = 
                (InstructionHeader)predecessors.elementAt(i);

            for (int j=0; j<pre.successors.length; j++)
                if (pre.successors[j] == from)
                    pre.successors[j] = this;
        }
        from.predecessors = new Vector();
    }

    /**
     * Moves the predecessors from the InstructionHeader <em>from</em> to
     * the current instruction. <p>
     * The predecessors of <em>from</em> are informed about this change.
     * @param from The instruction header which predecessors are moved.
     */
    public void addPredecessors(InstructionHeader from) {
        for (int i=0; i < from.predecessors.size(); i++) {
            InstructionHeader pre = 
                (InstructionHeader)from.predecessors.elementAt(i);

            predecessors.addElement(pre);
            for (int j=0; j < pre.successors.length; j++)
                if (pre.successors[j] == from)
                    pre.successors[j] = this;
        }
        from.predecessors.removeAllElements();
    }

    /**
     * Moves the successors from the InstructionHeader <em>from</em> to
     * the current instruction.  The current successors are overwritten
     * and you must make sure that is has no live InstructionHeaders.
     * Also the <em>from</em> InstructionHeader musnt't be used any more.<p>
     *
     * The successors of <em>from</em> are informed about this change.
     * @param from The instruction header which successors are moved.
     */
    public void moveSuccessors(InstructionHeader from) {
        successors = from.successors;
        from.successors = null;
        for (int i=0; i < successors.length; i++) {
            successors[i].predecessors.removeElement(from);
            successors[i].predecessors.addElement(this);
        }
    }

    /**
     * This method replaces multiple InstructionHeaders by a single one.
     * The next count Instructions must be unique.
     * @param count the number of InstructionHeaders that should be replaced.
     * @param instr the new instruction; this should be equivalent to the
     *              old <em>count</em instructions.
     * @return the InstructionHeader representing the combined instructions.
     */
    public InstructionHeader combine(int count, Instruction newInstr) {
        InstructionHeader ih = this;
        for (int i=1; i < count; i++) {
            ih = ih.nextInstruction;
        }
        ih.instr = newInstr;
        ih.movePredecessors(this);
        return ih;
    }

    public InstructionHeader doTransformations(Transformation[] trafo) {
        for (int i=0; i<trafo.length; i++) {
            InstructionHeader newInstr = trafo[i].transform(this);
            if (newInstr != null) 
                return newInstr;
        }
        return null;
    }

    /**
     * This method replaces two conditional InstructionHeaders by a
     * single one.  You must make sure that this and the next instruction
     * are both conditional Instructions and the destinations matches.
     *
     * @param newCondition the new instruction; this should be equivalent
     *              to the old two conditions.
     */
    public InstructionHeader combineConditional(Instruction newCondition) {
        successors[1].predecessors.removeElement(this);
        InstructionHeader next = nextInstruction;
        next.instr        = newCondition;
        next.movePredecessors(this);
        return next;
    }
}
