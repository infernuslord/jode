/* 
 * CombineCatchLocal (c) 1998 Jochen Hoenicke
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
import sun.tools.java.Identifier;

public class CombineCatchLocal implements Transformation{

//     static Identifier idException = Identifier.lookup("exception");

    public InstructionHeader transform(InstructionHeader ih) {
        CatchInstructionHeader catchIH;
        LocalInfo local;
        try {
            catchIH = (CatchInstructionHeader)ih;
            ih = ih.nextInstruction;
            if (ih.getPredecessors().size() != 1)
                return null;
            Instruction instr = ih.getInstruction();
            if (instr instanceof PopOperator) {
                local = new LocalInfo(99);
            } else if (instr instanceof LocalStoreOperator) {
                local = ((LocalStoreOperator) instr).getLocalInfo();
            } else
                return null;
        } catch (ClassCastException ex) {
            return null;
        } catch (NullPointerException ex) {
            return null;
        }
        if (!catchIH.combineWithLocal(local))
            return null;
        if(Decompiler.isVerbose)
            System.err.print("c");
        return catchIH;
    }
}
