/* 
 * CreateWhileStatements (c) 1998 Jochen Hoenicke
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

public class CreateWhileStatements extends FlowTransformation
implements Transformation {

    public InstructionHeader transform(InstructionHeader gotoIH) {

        if (gotoIH.flowType == gotoIH.IFGOTO &&
            gotoIH.successors[1] == gotoIH)
            /* This is an empty while loop
             */
            return new WhileInstructionHeader(gotoIH, gotoIH);

        if (gotoIH.flowType != gotoIH.GOTO || 
            gotoIH.nextInstruction == null ||
            gotoIH.successors[0].addr < gotoIH.nextInstruction.addr ||
            gotoIH.outer != gotoIH.successors[0].outer)
            return null;

        InstructionHeader ifgoto = gotoIH.successors[0];

        if (ifgoto.getFlowType() != ifgoto.IFGOTO ||
            ifgoto.outer != ifgoto.successors[1].outer)
            return null;

        InstructionHeader next = UnoptimizeWhileLoops(ifgoto.successors[1]);
        if (next != gotoIH.nextInstruction)
            return null;

        if (next != ifgoto.successors[1]) {
            ifgoto.successors[1].predecessors.removeElement(ifgoto);
            ifgoto.successors[1] = next;
            ifgoto.successors[1].predecessors.addElement(ifgoto);
        }

        if(Decompiler.isVerbose)
            System.err.print("w");
        return new WhileInstructionHeader(gotoIH, ifgoto);
    }
}
