package jode;

public class CreateIfStatements extends FlowTransformation
                                implements Transformation {

    public InstructionHeader transform(InstructionHeader ifgoto) {

        if (ifgoto.getFlowType() != ifgoto.IFGOTO ||
            ifgoto.nextInstruction == null ||
            ifgoto.getSuccessors()[1].getAddress() <=
            ifgoto.getSuccessors()[0].getAddress())
            return null;

        /* elseBlock points to the first instruction of the
         *   elseBlock or is null if the else block is empty.
         * next points to the next instruction after this block
         *   or is null, if this if is the last instruction in
         *   this block.
         */

        InstructionHeader elseBlock = null, next = null;

        if (ifgoto.outer != ifgoto.successors[1].outer) {
            if (ifgoto.outer.getEndBlock() != ifgoto.successors[1].getShadow())
                /* This doesn't seem to be an if but a if-break
                 */
                return null;

            /* This is an if without else that goes to the end
             * of the current block.  May be this was a continue,
             * but who would know it?
             *
             * If you like breaks and continues more than ifs, 
             * simply do the break transformation before the
             * if transformation.
             */
            next = null;
        } else {
            /* This is a normal if, let us first assume, that there is
             * no else part.  Then end is successors[1], but that may
             * be a while loop, so unoptimized it...  
             */
            next = UnoptimizeWhileLoops(ifgoto.successors[1]);
            if (next != ifgoto.successors[1]) {
                ifgoto.successors[1].predecessors.removeElement(ifgoto);
                ifgoto.successors[1] = next;
                ifgoto.successors[1].predecessors.addElement(ifgoto);
            }

            /* next.prevInstruction is the end of the `then' block.
             * If this a goto statement that jumps downward, this is
             * probably an `if-then-else'.  
             */
            InstructionHeader thenEnd = next.prevInstruction;
            if (thenEnd.flowType == thenEnd.GOTO) {
                if (thenEnd.successors[0].outer == next.outer &&
                    thenEnd.successors[0].addr >= next.addr) {

                    /* this is a normal if-then-else that is
                     * fully contained in this block.
                     */
                    elseBlock = next;
                    next = UnoptimizeWhileLoops(thenEnd.successors[0]);

                } else if (thenEnd.successors[0].getShadow() == 
                           ifgoto.outer.getEndBlock()) {
                    /* this is a normal if-then-else that goes
                     * to the end of the block.
                     * 
                     * Again this may also be a continue/break statement,
                     * but again,  who knows.
                     */
                    elseBlock = next;
                    next = null;
                }
            }
        }

        /* This was all,  the rest is done in the constructor of 
         * IfInstructionHeader.
         */
        if(Decompiler.isVerbose)
            System.err.print("i");
        return new IfInstructionHeader
            (ifgoto, elseBlock, next);
    }
}



