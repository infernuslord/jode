/* 
 * CreateNewConstructor (c) 1998 Jochen Hoenicke
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
import jode.expr.*;

public class CreateNewConstructor {

    public static boolean transform(InstructionContainer ic,
                                    StructuredBlock last) {
        /* Situation:
         *
         *  new <object>
         *  (optional DUP)
         *  PUSH load_ops
         *  optionally:  <= used for "string1 += string2"
         *      DUP_X2
         *  [ n non void + some void expressions ]
         *  stack_n.<init>(stack_n-1,...,stack_0)
         *
         * transform it to
         *
         *  PUSH load_ops
         *  optionally:
         *      DUP_X1      <= remove the depth
         *  [ n non void + some void expressions ]
         *  (optional PUSH) new <object>(stack_n-1,...,stack_0)
         */

        if (!(last.outer instanceof SequentialBlock))
            return false;
        if (!(ic.getInstruction() instanceof InvokeOperator))
            return false;
        InvokeOperator constrCall = (InvokeOperator) ic.getInstruction();
        if (!constrCall.isConstructor())
            return false;

        /* The rest should probably succeed */
        int params = constrCall.getOperandCount() - 1;
        SpecialBlock optDupX2 = null;
        SequentialBlock sequBlock = (SequentialBlock) last.outer;
        while (params > 0) {
            if (!(sequBlock.subBlocks[0] instanceof InstructionBlock))
                return false;
            Expression expr 
                = ((InstructionBlock) sequBlock.subBlocks[0]).getInstruction();
            if (!expr.isVoid())
                params--;
            if (expr.getOperandCount() > 0) {
                if (params == 0
                    && sequBlock.outer instanceof SequentialBlock
                    && sequBlock.outer.getSubBlocks()[0] 
                    instanceof SpecialBlock) {
                    /* handle the optional dup */
                    sequBlock = (SequentialBlock) sequBlock.outer;
                    optDupX2 = (SpecialBlock) sequBlock.subBlocks[0];
                    if (optDupX2.type != SpecialBlock.DUP
                        || optDupX2.depth != 2)
                        return false;
                    params = optDupX2.count;
                } else
                    return false;
            }
            if (!(sequBlock.outer instanceof SequentialBlock))
                return false;
            sequBlock = (SequentialBlock) sequBlock.outer;
        }

        while (sequBlock.subBlocks[0] instanceof InstructionBlock
               && ((InstructionBlock)sequBlock.subBlocks[0])
               .getInstruction().isVoid()
               && sequBlock.outer instanceof SequentialBlock)
            sequBlock = (SequentialBlock) sequBlock.outer;
               
        SpecialBlock dup = null;
        if (sequBlock.outer instanceof SequentialBlock
            && sequBlock.subBlocks[0] instanceof SpecialBlock) {

            dup = (SpecialBlock) sequBlock.subBlocks[0];
            if (dup.type != SpecialBlock.DUP 
                || dup.count != 1 || dup.depth != 0)
                return false;
            sequBlock = (SequentialBlock)sequBlock.outer;
        }

        if (!(sequBlock.subBlocks[0] instanceof InstructionBlock))
            return false;
        InstructionBlock block = (InstructionBlock) sequBlock.subBlocks[0];
        if (!(block.getInstruction() instanceof NewOperator))
            return false;
        
        NewOperator op = (NewOperator) block.getInstruction();
        if (constrCall.getClassType() != op.getType())
            return false;
        
        block.removeBlock();
        if (dup != null)
            dup.removeBlock();
        if (optDupX2 != null)
            optDupX2.depth = 0;
        ic.setInstruction(new ConstructorOperator(constrCall.getClassType(), 
                                                  constrCall.getMethodType(),
                                                  dup == null));
        return true;
    }
}

