/* BreakableBlock (c) 1998 Jochen Hoenicke
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
import jode.decompiler.TabbedPrintWriter;

/**
 * This is a structured block, that supports break.  
 */
public interface BreakableBlock {

    /**
     * Returns the label of this block and creates a new label, if
     * there wasn't a label previously.
     */
    public String getLabel();

    /**
     * Is called by BreakBlock, to tell us that this block is breaked.
     */
    public void setBreaked();

    /**
     * Is called by BreakBlock, to tell us what the stack can be after a
     * break.
     * @param stack the stack at the break.
     */
    public void mergeBreakedStack(VariableStack stack);
}
