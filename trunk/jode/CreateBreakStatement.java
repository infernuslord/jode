/* 
 * CreateBreakStatement (c) 1998 Jochen Hoenicke
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

public class CreateBreakStatement implements Transformation {
    public InstructionHeader transform(InstructionHeader ih) {
        InstructionHeader breakDest;
        if (ih.getFlowType() == ih.GOTO)
            breakDest = ih.successors[0].getShadow();
        else if (ih.getFlowType() == ih.IFGOTO)
            breakDest = ih.successors[1].getShadow();
        else
            return null;

        if (breakDest == ih.getEndBlock()) {
            /* This is an unnecessary goto, remove it.
             */
            if (ih.prevInstruction != null)
                ih.prevInstruction.nextInstruction = ih.nextInstruction;
            if (ih.nextInstruction != null)
                ih.nextInstruction.prevInstruction = ih.prevInstruction;
            breakDest.addPredecessors(ih);
            breakDest.predecessors.removeElement(ih);
            if (Decompiler.isVerbose)
                System.err.print("g");
            return null;
        }

        boolean needBreakLabel = false, needContLabel = false;
        InstructionHeader outer = ih.outer;
        while (outer != null) {
            if (outer.getBreak() == breakDest) {
                if (Decompiler.isVerbose)
                    System.err.print("b");
                return new BreakInstructionHeader
                    (ih.BREAK, ih, needBreakLabel?outer.getLabel(): null);
            }
            if (outer.getContinue() == breakDest) {
                if (Decompiler.isVerbose)
                    System.err.print("c");
                return new BreakInstructionHeader
                    (ih.CONTINUE, ih, needContLabel?outer.getLabel(): null);
            }
            if (outer.outer == null && outer.getEndBlock() == breakDest) {
                if (Decompiler.isVerbose)
                    System.err.print("r");
                return new BreakInstructionHeader
                    (ih.VOIDRETURN, ih, null);
            }
            if (outer.getBreak() != null)
                needBreakLabel = true;
            if (outer.getContinue() != null)
                needContLabel = true;
            outer = outer.outer;
        }
        return null;
    }
}

