/* 
 * TryInstructionHeader (c) 1998 Jochen Hoenicke
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

public class TryInstructionHeader extends InstructionHeader {

    TryInstructionHeader(InstructionHeader firstInstr,
                         InstructionHeader outer) {
        super(TRY, firstInstr.addr, firstInstr.addr, 
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
    }

    public void addHandler(InstructionHeader endInstr,
                           InstructionHeader catchInstr) {
        InstructionHeader[] newSuccessors =
            new InstructionHeader[successors.length+2];
        System.arraycopy(successors, 0, newSuccessors, 0, successors.length);
        successors = newSuccessors;
        successors[successors.length-2] = endInstr;
        successors[successors.length-1] = catchInstr;
        endInstr.predecessors.addElement(this);
        catchInstr.predecessors.addElement(this);
    }

    public void dumpSource(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
	if (Decompiler.isDebugging)
            dumpDebugging(writer);

        if (successors.length > 1) {
            writer.print ("try: ");
            for (int i=1; i<successors.length; i+=2)
                writer.print("to "+successors[i].getLabel()+
                             " catch "+successors[i+1].getLabel()+"; ");
            writer.println("");
        }
    }
}




