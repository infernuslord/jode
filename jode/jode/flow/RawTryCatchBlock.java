/* jode.flow.RawTryCatchBlock  (c) 1998 Jochen Hoenicke 
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

/**
 * A RawTryCatchBlock is created for each exception in the
 * ExceptionHandlers-Attribute. <p>
 *
 * For each catch block (there may be more than one catch block
 * appending a single try block) and for each finally and each
 * synchronized block such a RawTryCatchBlock is created.  The
 * finally/synchronized-blocks have a null exception type so that they
 * are easily distinguishable from the catch blocks. <p>
 *
 * A RawTryCatchBlock is an intermediate representation that gets
 * converted later to a CatchBlock, a FinallyBlock or a
 * SynchronizedBlock (after the body is parsed).
 *
 * @date 1998/09/16
 * @author Jochen Hoenicke
 * @see CatchBlock
 * @see FinallyBlock
 * @see SynchronizedBlock
 */

public RawTryCatchBlock extends StructuredBlock {

    /**
     * An unconditional jump to the EndBlock.
     */
    Jump EndBlock;

    /**
     * An unconditional jump to the CatchBlock.
     */
    Jump CatchBlock;

    /** 
     * The type of the exception that is catched. This is null for a
     * synchronized/finally block 
     */
    Type type;
}
