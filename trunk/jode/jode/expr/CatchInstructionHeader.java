/* 
 * CatchInstructionHeader (c) 1998 Jochen Hoenicke
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
import sun.tools.java.Type;
import java.util.Vector;

public class CatchInstructionHeader extends InstructionHeader {

    Type type;
    String typeString;
    LocalInfo local;

    CatchInstructionHeader(Type type, String typeString, 
                           InstructionHeader firstInstr,
                           InstructionHeader outer) {
        super(CATCH, firstInstr.addr, firstInstr.addr, 
              new InstructionHeader[1], outer);

        movePredecessors(firstInstr);
        successors[0] = firstInstr;
        firstInstr.predecessors = new Vector();
        firstInstr.predecessors.addElement(this);

        prevInstruction = firstInstr.prevInstruction;
        if (prevInstruction != null)
            prevInstruction.nextInstruction = this;

        nextInstruction = firstInstr;
        firstInstr.prevInstruction = this;

        this.type = type;
        this.typeString = typeString;
    }

    public LocalInfo getLocal() {
        return local;
    }

    /**
     * Combines this catch instruction header with the next instruction
     * header which should containt only a LocalStoreOperator or a pop
     * according to the parameter.  (This is where the exception gets
     * stored.
     * @param local  The local where the exception is stored.
     * @return true if the operation succeeded.
     */
    public boolean combineWithLocal(LocalInfo local) {
        InstructionHeader next = nextInstruction;
        if (this.local != null || successors[0] != next)
            return false;

        this.local = local;
        local.setType(type);
        successors = next.successors;
        nextInstruction = next.nextInstruction;
        if (nextInstruction != null)
            nextInstruction.prevInstruction = this;

        for (int i=0; i< successors.length; i++) {
            successors[i].predecessors.removeElement(next);
            successors[i].predecessors.addElement(this);
        }
        return true;
    }

    public void dumpSource(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
	if (Decompiler.isDebugging)
            dumpDebugging(writer);

        writer.println("} catch ("+typeString+" "+
                       (local != null ? local.getName()
                        : (Object) "stack_0") + ") {");
    }
}


