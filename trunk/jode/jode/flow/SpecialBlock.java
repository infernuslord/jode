/* SpecialBlock (c) 1998 Jochen Hoenicke
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
 * This is the structured block for atomic instructions.
 */
public class SpecialBlock extends StructuredBlock {

    public static int DUP  = 0;
    public static int SWAP = 1;
    private static String[] output = { "DUP", "SWAP" };

    /**
     * The type, one of DUP or SWAP
     */
    int type;
    /**
     * The count of stack entries that are transformed. 
     * This is 1 for swap, and 1 or 2 for dup.
     */
    int count;
    /**
     * The depth that the dupped element should be put to (0,1 or 2).
     * For swap this is zero.
     */
    int depth;

    public SpecialBlock(int type, int count, int depth, Jump jump) {
        this.type = type;
        this.count = count;
        this.depth = depth;
        setJump(jump);
    }

    public void dumpInstruction(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
        writer.println(output[type] 
                       + ((count == 1) ? "" : "2")
                       + ((depth == 0) ? "" : "_X"+depth));
    }
}
