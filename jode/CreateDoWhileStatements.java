/* 
 * CreateDoWhileStatements (c) 1998 Jochen Hoenicke
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

public class CreateDoWhileStatements extends FlowTransformation
implements Transformation {

    public InstructionHeader transform(InstructionHeader head) {

        if (head.predecessors.size() == 0 ||
            head.flowType == head.DOWHILESTATEMENT)
            return null;

        InstructionHeader end = head;
        Enumeration enum = head.predecessors.elements();
        while (enum.hasMoreElements()) {
            InstructionHeader pre = (InstructionHeader) enum.nextElement();
            if (pre.outer == head.outer && pre.addr > end.addr)
                end = pre;
        }

        if (end != head)
            if (end.flowType == end.IFGOTO || end.flowType == end.GOTO) {
                if(Decompiler.isVerbose)
                    System.err.print("d");
                return new DoWhileInstructionHeader(head, end);
            }
        return null;
    }
}
