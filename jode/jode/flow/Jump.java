/* Jump (c) 1998 Jochen Hoenicke
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
     * The destination address, in case the destination block is not yet
     * known.  
     */
    int destAddr;

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

    public Jump (int destAddr) {
        this.destAddr = destAddr;
	kill = new VariableSet();
	gen = new VariableSet();
    }

    public Jump (FlowBlock dest) {
        this.destination = dest;
	kill = new VariableSet();
	gen = new VariableSet();
    }

    public Jump (Jump jump) {
	destAddr = jump.destAddr;
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

