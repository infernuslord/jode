/* 
 * CreateSwitchStatements (c) 1998 Jochen Hoenicke
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
import java.util.Enumeration;

public class CreateSwitchStatements extends FlowTransformation
implements Transformation {

    public InstructionHeader transform(InstructionHeader ih) {
        if (ih.getFlowType() != ih.SWITCH)
            return null;

        SimpleSwitchInstructionHeader switchIH = 
            (SimpleSwitchInstructionHeader) ih;

        int defaultCase = switchIH.successors.length - 1;
        int addr = switchIH.nextInstruction.addr;
        int count = 1;
        for (int i=0; i < switchIH.successors.length; i++) {
            if (switchIH.successors[i] != switchIH.successors[defaultCase])
                count ++;
        }

        int[] cases  = new int[count];
        InstructionHeader[] sorted = new InstructionHeader[count];
        count = 0;
        for (int i=0; i < switchIH.successors.length; i++) {
            if (i != defaultCase &&
                switchIH.successors[i] == switchIH.successors[defaultCase])
                continue;

            InstructionHeader next = 
                UnoptimizeWhileLoops(switchIH.successors[i]);
            if (next != switchIH.successors[i]) {
                switchIH.successors[i].predecessors.removeElement(switchIH);
                switchIH.successors[i] = next;
                switchIH.successors[i].predecessors.addElement(switchIH);
            }

            int insert;
            for (insert = 0; insert < count; insert++) {
                if (sorted[insert].addr > switchIH.successors[i].addr) 
                    break;
            }
            if (insert < count) {
                System.arraycopy(cases, insert, 
                                 cases, insert+1, count-insert);
                System.arraycopy(sorted, insert, 
                                 sorted, insert+1, count-insert);
            }
            if (i == defaultCase)
                defaultCase = insert;
            else
                cases[insert]  = switchIH.cases[i];
            sorted[insert] = switchIH.successors[i];
            count++;
        }

        InstructionHeader endBlock = switchIH.outer.getEndBlock();
        int lastBlock;
        for (lastBlock = count-1; lastBlock>= 0; lastBlock--)
            if (sorted[lastBlock].outer == switchIH.outer)
                break;

        if (lastBlock >= 0) {
        EndSearch:
            ih = sorted[lastBlock];
            while (ih != null) {
                Enumeration enum;
                if (ih.flowType == ih.GOTO)
                    enum = ih.successors[0].getPredecessors().elements();
                else
                    enum = ih.getPredecessors().elements();
                while (enum.hasMoreElements()) {
                    InstructionHeader pred = 
                        (InstructionHeader)enum.nextElement();
                    if (pred.addr < sorted[lastBlock].addr &&
                        pred.outer == switchIH.outer &&
                        (pred.flowType == ih.GOTO ||
                         (pred.flowType == ih.IFGOTO && 
                          pred.successors[1] == ih)) &&
                        ih == UnoptimizeWhileLoops(ih)) {
                        endBlock = ih;
                        /* search further down, if there are other
                         * more suitible instructions.
                         */
                    }
                }
                ih = ih.nextInstruction;
            }
        } 

        for (int i=0; i< sorted.length; i++) {
            if (sorted[i].outer != switchIH.outer) {
                if (sorted[i].getShadow() != endBlock) {
                    /* Create a new goto at the beginning of 
                     * the switch statement, jumping to the right
                     * successor.
                     */
                    InstructionHeader[] successors = { sorted[i] };
                    InstructionHeader dummyGoto = 
                        new InstructionHeader(switchIH.GOTO, 
                                              switchIH.addr, switchIH.addr,
                                              successors, switchIH.outer);
                    sorted[i].predecessors.addElement(dummyGoto);
                    
                    /* Connect it in the prev/next Instruction chain.
                     */
                    dummyGoto.nextInstruction = switchIH.nextInstruction;
                    if (dummyGoto.nextInstruction != null)
                        dummyGoto.nextInstruction.prevInstruction = dummyGoto;
                    switchIH.nextInstruction  = dummyGoto;
                    dummyGoto.prevInstruction = switchIH;
                    
                    /* Search all instructions that jump to this point and
                     * stack them together.
                     */
                    int length = 1;
                    while (i+length < sorted.length &&
                           sorted[i+length] == sorted[i])
                        length++;
                    
                    /* Move them to the beginning of this array.
                     */
                    System.arraycopy(sorted, 0, sorted, length, i);

                    int[] tmp = new int[length];
                    System.arraycopy(cases, i, tmp, 0, length);
                    System.arraycopy(cases, 0, cases, length, i);
                    System.arraycopy(tmp, 0, cases, 0, length);
                    
                    if (defaultCase < i)
                        defaultCase += length;
                    else if (defaultCase <= i+length)
                        defaultCase -= i;
                    for (int j=0; j<length; j++)
                        sorted[j] = dummyGoto;
                    
                    i += length - 1;
                } else {
                    /* Search all instructions that jump to this point and
                     * stack them together.
                     */
                    int length = 1;
                    while (i+length < sorted.length &&
                           sorted[i+length] == sorted[i])
                        length++;
                    
                    /* Move them to the end of this array, if they
                     * aren't already there.
                     */
                    if (i+length < sorted.length) {
                        System.arraycopy(sorted, i + length, sorted, i,
                                         sorted.length - i - length);
                        for (int j=length; j>0; j--)
                            sorted[sorted.length-j] = endBlock;

                        int[] tmp = new int[length];
                        System.arraycopy(cases, i, tmp, 0, length);
                        System.arraycopy(cases, i + length, cases, i,
                                         cases.length - i - length);
                        System.arraycopy(tmp, 0, cases, 
                                         sorted.length - length, length);
                        if (defaultCase >= i + length)
                            defaultCase -= length;
                        else if (defaultCase >=i)
                            defaultCase += cases.length-i-length;
                    }
                }
            }
        }

        if(Decompiler.isVerbose)
            System.err.print("s");
        return new SwitchInstructionHeader
            (switchIH, cases, sorted, defaultCase, endBlock);
    }
}


