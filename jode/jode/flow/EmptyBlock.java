/* 
 * EmptyBlock  (c) 1998 Jochen Hoenicke
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
import jode.TabbedPrintWriter;

/**
 * This is the structured block for an empty block.
 */
public class EmptyBlock extends StructuredBlock {
    public EmptyBlock() {
    }

    public EmptyBlock(Jump jump) {
        setJump(jump);
    }

    /**
     * Tells if this block is empty and only changes control flow.
     */
    public boolean isEmpty() {
        return true;
    }

    /**
     * Appends a block to this block.
     * @return the new combined block.
     */
    public StructuredBlock appendBlock(StructuredBlock block) {
	if (outer instanceof ConditionalBlock) {
	    IfThenElseBlock ifBlock = 
		new IfThenElseBlock(((ConditionalBlock)outer).
				    getInstruction());
	    ifBlock.replace(outer, this);
	    ifBlock.moveJump(outer.jump);
	    ifBlock.setThenBlock(this);
	}
	block.replace(this, null);
	return block;
    }

    public void dumpInstruction(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
        writer.println("/* empty */");
    }
}
