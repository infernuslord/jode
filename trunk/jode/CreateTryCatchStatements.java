/* 
 * CreateTryCatchStatements (c) 1998 Jochen Hoenicke
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

public class CreateTryCatchStatements extends FlowTransformation
implements Transformation {

    public InstructionHeader transform(InstructionHeader tryIH) {
        if (tryIH.getFlowType() != tryIH.TRY ||
            tryIH.successors.length == 1 ||
            tryIH.nextInstruction == null)
            return null;

        /* find handler with largest end address 
         * and count the catches.
         */
        InstructionHeader endIH = tryIH;
        int index = 0, count = 0;
        for (int i=1; i < tryIH.successors.length; i+=2) {
            if (tryIH.successors[i] == endIH)
                count++;
            else if (tryIH.successors[i].addr > endIH.addr) {
                endIH = tryIH.successors[i];
                count = 1;
            }
        }
        if (count == 0 || endIH.outer != tryIH.outer)
            return null;

        /* now find all corresponding catches */
        InstructionHeader[] catchIH = new InstructionHeader[count+1];
        InstructionHeader[] remaining = 
            new InstructionHeader[tryIH.successors.length-2*count];
        int index1 = 0, index2=0;
        remaining[index2++] = tryIH.successors[0];
        catchIH[index1++] = tryIH;
        for (int i=1; i < tryIH.successors.length; i+=2) {
            if (tryIH.successors[i] == endIH) {
                /* assume that the catches are already sorted */
                if (tryIH.successors[i+1].outer != tryIH.outer ||
                    tryIH.successors[i+1].flowType != InstructionHeader.CATCH)
                    return null;
                catchIH[index1] = tryIH.successors[i+1];
                index1++;
            } else {
                remaining[index2++] = tryIH.successors[i];
                remaining[index2++] = tryIH.successors[i+1];
            }
        }
        InstructionHeader endBlock;
        if (endIH != catchIH[1]) {
            if (endIH.flowType != endIH.GOTO)
                return null;

            endBlock = UnoptimizeWhileLoops(endIH.successors[0]);
        } else
            endBlock = tryIH.outer.getEndBlock();
        if (Decompiler.isVerbose)
            System.err.print("t");
        return new TryCatchInstructionHeader
            (catchIH, endIH, remaining, endBlock);
    }
}
