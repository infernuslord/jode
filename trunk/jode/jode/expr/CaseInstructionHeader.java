/* 
 * CaseInstructionHeader (c) 1998 Jochen Hoenicke
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

public class CaseInstructionHeader extends InstructionHeader {

    int label;
    boolean isDefault;

    public CaseInstructionHeader(int label, boolean isDefault,
                                 //Type type,
                                 int addr,
                                 InstructionHeader parent) {
        super(CASESTATEMENT, addr, addr,
              new InstructionHeader[1], parent);
        this.label = label;
        this.isDefault = isDefault;
    }

    public void dumpSource(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
	if (Decompiler.isDebugging) {
            dumpDebugging(writer);
	    writer.tab();
	}
        if (isDefault) {
            if (nextInstruction == null && successors[0] == null)
                return;
            writer.println("default:");
        } else
            writer.println("case "+label+":" );

        writer.tab();
        for (InstructionHeader ih = successors[0]; ih != null; 
             ih = ih.nextInstruction)
                ih.dumpSource(writer);
        writer.untab();

	if (Decompiler.isDebugging)
            writer.untab();            
    }

    /** 
     * Get the instruction header where the next instruction is.
     */
    InstructionHeader getShadow() {
        return (successors[0] != null ? successors[0].getShadow() : 
                getEndBlock());
    }

    public InstructionHeader doTransformations(Transformation[] trafo) {
        InstructionHeader next;
        if (successors[0] != getEndBlock()) {
            for (InstructionHeader ih = successors[0]; ih != null; ih = next) {
                if ((next = ih.doTransformations(trafo)) == null)
                    next = ih.getNextInstruction();
            }
        }
        return super.doTransformations(trafo);
    }
}
