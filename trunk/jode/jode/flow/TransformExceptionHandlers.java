/* TransformExceptionHandlers Copyright (C) 1997-1998 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id$
 */
package jode.flow;
import jode.AssertError;
import jode.Type;
import jode.LocalInfo;
import jode.ComplexExpression;
import jode.LocalStoreOperator;
import jode.NopOperator;
import jode.MonitorExitOperator;
import jode.LocalLoadOperator;
import jode.Expression;
import jode.PopOperator;
import java.util.Enumeration;
import java.util.Stack;

/**
 * 
 * @author Jochen Hoenicke
 */
public class TransformExceptionHandlers {
    FlowBlock[] flows;
    int count;
    int[] startPCs = new int[4];
    int[] endPCs = new int[4];
    int[] handlerPCs = new int[4];
    Type[] types = new Type[4];
    
    public TransformExceptionHandlers(FlowBlock[] flowBlocks) {
        flows = flowBlocks;
        count = 0;
    }

    /**
     * Add an exception Handler.
     * @param start The start address of the exception range.
     * @param end The end address of the exception range + 1.
     * @param handler The address of the handler.
     * @param type The type of the exception, null for ALL.
     */
    public void addHandler(int start, int end, int handler, Type type) {
        int offset = 0;
        /* First sort by start offsets, highest address first...*/
        while (offset < count && start < startPCs[offset])
            offset++;
        /* ...Second sort by end offsets, lowest address first...
         * this will move the innermost blocks to the beginning. */
        while (offset < count && start == startPCs[offset] 
               && end > endPCs[offset])
            offset++;
        /* ...Last sort by handler offsets, lowest first */
        while (offset < count && start == startPCs[offset]
               && end == endPCs[offset] && handler > handlerPCs[offset])
            offset++;

        if (count++ >= startPCs.length) {
            /* We grow the arrays by 50 % */
            int newSize = startPCs.length * 3 / 2;
            int[] newStartPCs = new int[newSize];
            int[] newEndPCs = new int[newSize];
            int[] newHandlerPCs = new int[newSize];
            Type[] newTypes = new Type[newSize];
            System.arraycopy(startPCs, 0, newStartPCs, 0, offset);
            System.arraycopy(endPCs, 0, newEndPCs, 0, offset);
            System.arraycopy(handlerPCs, 0, newHandlerPCs, 0, offset);
            System.arraycopy(types, 0, newTypes, 0, offset);

            if (offset+1 < count) {
                System.arraycopy(startPCs, offset, newStartPCs, offset+1, 
                                 count-offset-1);
                System.arraycopy(endPCs, offset, newEndPCs, offset+1, 
                                 count-offset-1);
                System.arraycopy(handlerPCs, offset, newHandlerPCs, offset+1, 
                                 count-offset-1);
                System.arraycopy(types, offset, newTypes, offset+1, 
                                 count-offset-1);
            }
            startPCs = newStartPCs;
            endPCs = newEndPCs;
            handlerPCs = newHandlerPCs;
            types = newTypes;
        } else if (offset+1 < count) {
            /* Move the tailing data one place below
             */
            System.arraycopy(startPCs, offset, startPCs, offset+1, 
                             count-offset-1);
            System.arraycopy(endPCs, offset, endPCs, offset+1, 
                             count-offset-1);
            System.arraycopy(handlerPCs, offset, handlerPCs, offset+1, 
                             count-offset-1);
            System.arraycopy(types, offset, types, offset+1, 
                             count-offset-1);
        }
        /* Insert the new handler */
        startPCs[offset] = start;
        endPCs[offset] = end;
        handlerPCs[offset] = handler;
        types[offset] = type;
    }

    /** 
     * Updates the in/out-Vectors of the structured block of the
     * successing flow block for a try catch block.  The main difference
     * to updateInOut in FlowBlock is, that this function works, as if
     * every instruction would have a jump.  This is because every
     * instruction can throw an exception and thus enter the catch block.
     *
     * @param successor The flow block which is unified with this flow
     * block.  
     * @return The variables that must be defined in this block.
     */
    static void updateInOutCatch (FlowBlock tryFlow, FlowBlock catchFlow) {
        VariableSet gens = ((TryBlock)tryFlow.block).gen;

        /* Merge the locals used in the catch block with those written
         * by the try block
         */
        catchFlow.in.merge(gens);
        
        /* The gen/kill sets must be updated for every jump 
         * in the catch block */
        Enumeration stacks = catchFlow.successors.elements();
        while (stacks.hasMoreElements()) {
            Enumeration enum = ((Stack) stacks.nextElement()).elements();
            while (enum.hasMoreElements()) {

                Jump jump = (Jump) enum.nextElement();
                if (jump != null) {
                    jump.gen.mergeGenKill(gens, jump.kill);
                }
            }
        }
        tryFlow.in.unionExact(catchFlow.in);
        tryFlow.gen.unionExact(catchFlow.gen);
    
        if (jode.Decompiler.debugInOut) {
            System.err.println("UpdateInOutCatch: gens : "+gens);
            System.err.println("                  s.in : "+catchFlow.in);
            System.err.println("                  in   : "+tryFlow.in);
        }
    }


    /* simple try catch block:
     *
     *   try-header
     *      |- first instruction
     *      |  ...
     *      |  last instruction
     *      |- optional jump (last+1)
     *      |  ...
     *      `- catch block
     */
    static int serialno=0;

    static void analyzeCatchBlock(Type type, 
                                  FlowBlock tryFlow, FlowBlock catchFlow) {

        StructuredBlock catchBlock = catchFlow.block;
        LocalInfo local = null;
        StructuredBlock firstInstr = (catchBlock instanceof SequentialBlock)
            ? catchBlock.getSubBlocks()[0] : catchBlock;

        if (firstInstr instanceof InstructionBlock) {
            Expression instr = 
                ((InstructionBlock) firstInstr).getInstruction();
            if (instr instanceof PopOperator
                && ((PopOperator) instr).getCount() == 1) {
                /* The exception is ignored.  Create a dummy local for it */
                local = new LocalInfo(-1);
                local.setName("exception_"+(serialno++)+"_");
                firstInstr.removeBlock();

            } else if (instr instanceof jode.LocalStoreOperator) {
                /* The exception is stored in a local variable */
                local = 
                    ((jode.LocalStoreOperator) instr).getLocalInfo();
                firstInstr.removeBlock();
            }
        }

        if (local == null) {
            local = new LocalInfo(-1);
            local.setName("ERROR!!!");
        }
        local.setType(type);

        CatchBlock newBlock = new CatchBlock(type, local);
        ((TryBlock)tryFlow.block).addCatchBlock(newBlock);
        newBlock.setCatchBlock(catchFlow.block);
        tryFlow.mergeSuccessors(catchFlow);
        tryFlow.length += catchFlow.length;
    }

    /* And now the complicated parts. */
    
    /**
     * This transforms a sub routine, that is checks if the beginning
     * local assignment matches the final ret and then returns.
     */
    boolean transformSubRoutine(FlowBlock subRoutine) {
        try {
            SequentialBlock sequBlock = (SequentialBlock) subRoutine.block;
            LocalStoreOperator store = (LocalStoreOperator)
                ((InstructionBlock)sequBlock.subBlocks[0]).instr.getOperator();
            while (sequBlock.subBlocks[1] instanceof SequentialBlock)
                sequBlock = (SequentialBlock) sequBlock.subBlocks[1];
            RetBlock retBlock = (RetBlock)sequBlock.subBlocks[1];
            if (! retBlock.local.equals(store.getLocalInfo()))
                /* Ret doesn't match */
                return false;
            subRoutine.block.getSubBlocks()[0].removeBlock();
            retBlock.removeBlock();
            return true;
        } catch (ClassCastException ex) {
            return false;
        }
    }

    /**
     * Remove the JSR's jumping to the specified subRoutine. It
     * is checked if the next block is a leaving instruction, and
     * otherwise the JsrBlock is not removed (to give the user a
     * hint that something went wrong).  This will also remove the
     * local javac generates for returns.
     * @param tryFlow the FlowBLock of the try block.
     * @param subRoutine the FlowBlock of the sub routine.
     */
    private void removeJSR(FlowBlock tryFlow, FlowBlock subRoutine) {
        Stack jumps = (Stack)tryFlow.successors.remove(subRoutine);
        if (jumps == null)
            return;
        Enumeration enum = jumps.elements();
        while (enum.hasMoreElements()) {
            Jump jump = (Jump)enum.nextElement();

            StructuredBlock prev = jump.prev;
            prev.removeJump();
            if (prev instanceof EmptyBlock
                && prev.outer instanceof JsrBlock) {
                if (prev.outer.getNextFlowBlock() != null) {
                    /* The jsr is directly before a jump, okay. */
                    prev.outer.removeBlock();
                    continue;
                }
                if (prev.outer.outer instanceof SequentialBlock
                    && prev.outer.outer.getSubBlocks()[0] == prev.outer) {
                    SequentialBlock seq = (SequentialBlock) prev.outer.outer;
                    if (seq.subBlocks[1] instanceof JsrBlock) {
                        /* The jsr is followed by a jsr, okay. */
                        prev.outer.removeBlock();
                        continue;
                    }
                    if (seq.subBlocks[1] instanceof ReturnBlock
                        && !(seq.subBlocks[1] instanceof ThrowBlock)) {

                        /* The jsr is followed by a return, okay. */
                        ReturnBlock ret = (ReturnBlock) seq.subBlocks[1];
                        prev.outer.removeBlock();

                        if (ret.outer != null 
                            && ret.outer instanceof SequentialBlock) {
                            /* Try to eliminate the local that javac uses
                             * in this case.
                             */
                            try {
                                ComplexExpression expr = (ComplexExpression)
                                    ((InstructionBlock)
                                     ret.outer.getSubBlocks()[0]).instr;
                                LocalStoreOperator store = 
                                    (LocalStoreOperator) expr.getOperator();
                                if (store.matches((LocalLoadOperator)
                                                  ret.getInstruction())) {
                                    ret.setInstruction(expr.
                                                       getSubExpressions()[0]);
                                    ret.replace(ret.outer);
                                    ret.used.removeElement
                                        (store.getLocalInfo());
                                }
                            } catch(ClassCastException ex) {
                                /* didn't succeed */
                            }
                        }
                        continue;
                    }
                } 
            }
            /* Now we have a dangling JSR at the wrong place.
             * We don't do anything, so that JSR will show up in
             * the output.
             */
        }
    }
    
    public void checkAndRemoveJSR(FlowBlock tryFlow, FlowBlock subRoutine) {
        Enumeration keys = tryFlow.successors.keys();
        Enumeration stacks = tryFlow.successors.elements();
        while (keys.hasMoreElements()) {
            Stack jumps = (Stack) stacks.nextElement();
            if (keys.nextElement() == subRoutine)
                continue;

            Enumeration enum = jumps.elements();
            while (enum.hasMoreElements()) {
                Jump jump = (Jump)enum.nextElement();

                StructuredBlock prev = jump.prev;
                if (prev instanceof ThrowBlock) {
                    /* The jump is a throw.  We have a catch-all block
                     * that will do the finally.
                     */
                    continue;
                }
                if (prev instanceof JsrBlock) {
                    /* The jump is directly preceeded by a jsr.
                     * Everything okay.
                     */
                    continue;
                }
                
                if (prev instanceof EmptyBlock
                    && prev.outer instanceof JsrBlock) {
                    /* If jump is a jsr check the outer
                     * block instead.
                     */
                    prev = prev.outer;
                }
                if ((prev instanceof ReturnBlock
                     || prev instanceof JsrBlock)
                    && prev.outer instanceof SequentialBlock) {
                    SequentialBlock seq = (SequentialBlock) prev.outer;
                    if (seq.subBlocks[1] == prev
                        && (seq.subBlocks[0] instanceof JsrBlock)) {
                        /* The jump is preceeded by another jsr, okay.
                         */
                        continue;
                    }
                    if (seq.subBlocks[0] == prev
                        && seq.outer instanceof SequentialBlock
                        && (seq.outer.getSubBlocks()[0] instanceof JsrBlock)) {
                        /* Again the jump is preceeded by another jsr, okay.
                         */
                        continue;
                    }
                }
                /* Now we have a jump with a wrong destination.
                 * Complain!
                 */
                System.err.println("non well formed try-finally block");
            }
        }
        removeJSR(tryFlow, subRoutine);
    }

    static boolean isMonitorExit(Expression instr, LocalInfo local) {
        if (instr instanceof ComplexExpression) {
            ComplexExpression expr = (ComplexExpression)instr;
            if (expr.getOperator() instanceof MonitorExitOperator
                && expr.getSubExpressions()[0] instanceof LocalLoadOperator
                && (((LocalLoadOperator) expr.getSubExpressions()[0])
                    .getLocalInfo().getSlot() == local.getSlot())) {
                return true;
            }
        }
        return false;
    }

    public void checkAndRemoveMonitorExit(FlowBlock tryFlow, LocalInfo local, 
                                          int startMonExit, int endMonExit) {
        FlowBlock subRoutine = null;
        Enumeration stacks = tryFlow.successors.elements();
    dest_loop:
        while (stacks.hasMoreElements()) {
            Enumeration enum = ((Stack) stacks.nextElement()).elements();
            while (enum.hasMoreElements()) {
                Jump jump = (Jump)enum.nextElement();

                StructuredBlock prev = jump.prev;
                if (prev instanceof EmptyBlock
                    && prev.outer instanceof JsrBlock
                    && subRoutine == null) {
                    
                    subRoutine = jump.destination;
                    subRoutine.analyze(startMonExit, endMonExit);
                    transformSubRoutine(subRoutine);
                
                    if (subRoutine.block instanceof InstructionBlock) {
                        Expression instr = 
                            ((InstructionBlock)subRoutine.block)
                            .getInstruction();
                        if (isMonitorExit(instr, local)) {
                            tryFlow.length += subRoutine.length;
                            continue dest_loop;
                        }
                    }
                }

                if (prev instanceof ThrowBlock) {
                    /* The jump is a throw.  We have a catch all block
                     * that will do the monitorexit.
                     */
                    continue;
                }
                if (prev instanceof JsrBlock) {
                    /* The jump is directly preceeded by a jsr.
                     */
                    continue;
                }
                if (prev instanceof EmptyBlock
                    && prev.outer instanceof JsrBlock) {
                    /* If jump is a jsr check the outer
                     * block instead.
                     */
                    prev = prev.outer;
                }

                if ((prev instanceof JsrBlock
                     || prev instanceof ReturnBlock)
                    && prev.outer instanceof SequentialBlock) {
                    SequentialBlock seq = (SequentialBlock) prev.outer;
                    StructuredBlock pred = null;
                    if (seq.subBlocks[1] == prev)
                        pred = seq.subBlocks[0];
                    else if (seq.outer instanceof SequentialBlock)
                        pred = seq.outer.getSubBlocks()[0];
                    
                    if (pred != null) {
                        if (pred instanceof JsrBlock  || pred.jump != null)
                            /* The jump is preceeded by another jump
                             * or jsr and last in its block, okay.
                             */
                            continue;
                        
                        if (pred instanceof InstructionBlock) {
                            Expression instr = 
                                ((InstructionBlock)pred).getInstruction();
                            if (instr instanceof ComplexExpression
                                && ((ComplexExpression)instr)
                                .getOperator() instanceof MonitorExitOperator
                                && ((ComplexExpression)instr)
                                .getSubExpressions()[0] 
                                instanceof LocalLoadOperator
                                && (((LocalLoadOperator) 
                                     ((ComplexExpression)instr)
                                     .getSubExpressions()[0]).getLocalInfo()
                                    .getSlot() == local.getSlot())) 
                                continue;
                        }
                    }
                }

                if (prev instanceof InstructionBlock
                    && isMonitorExit(((InstructionBlock)prev).instr, local)) {
                    /* This is probably the last expression in the
                     * synchronized block, and has the right monitor exit
                     * attached.  Remove this block.
                     */
                    prev.removeBlock();
                    continue;
                }

                /* Now we have a jump that is not preceded by a monitorexit.
                 * Complain!
                 */
                System.err.println("non well formed synchronized block");
            }  
        }

        if (subRoutine != null)
            removeJSR(tryFlow, subRoutine);
    }

    private boolean analyzeSynchronized(FlowBlock tryFlow, 
                                        FlowBlock catchFlow,
                                        int endHandler) {
        if (!(catchFlow.block instanceof SequentialBlock
              && catchFlow.block.getSubBlocks()[0] 
              instanceof InstructionBlock))
            return false;
            
        SequentialBlock catchBlock = (SequentialBlock) catchFlow.block;
        Expression instr = 
            ((InstructionBlock)catchBlock.subBlocks[0]).getInstruction();
        
        if (instr instanceof ComplexExpression
            && ((ComplexExpression)instr).getOperator() 
            instanceof MonitorExitOperator
            && ((ComplexExpression)instr).getSubExpressions()[0] 
            instanceof LocalLoadOperator
            && catchBlock.subBlocks[1] instanceof ThrowBlock
            && ((ThrowBlock)catchBlock.subBlocks[1]).instr 
            instanceof NopOperator) {

            /* This is a synchronized block:
             *
             *  local_x = monitor object;  // later
             *  monitorenter local_x       // later
             *  tryFlow:
             *   |- syncronized block
             *   |  ...
             *   |   every jump to outside is preceded by jsr subroutine-,
             *   |  ...                                                  |
             *   |- monitorexit local_x                                  |
             *   `  jump after this block (without jsr monexit)          |
             *                                                           |
             *  catchFlow:                                               |
             *      local_n = stack                                      |
             *      monitorexit local_x                                  |
             *      throw local_n                                        |
             *  oprtional subroutine: <----------------------------------'
             *    astore_n
             *    monitorexit local_x
             *    return_n
             */

            /* Now remove the jump (after the throw) from the
             * catch block so that we can forget about it.  
             */
             
            catchBlock.subBlocks[1]
                .jump.destination.predecessors.removeElement(catchFlow);
            
            ComplexExpression monexit = (ComplexExpression)
                ((InstructionBlock) catchBlock.subBlocks[0]).instr;
            LocalInfo local = 
                ((LocalLoadOperator)monexit.getSubExpressions()[0])
                    .getLocalInfo();
            tryFlow.length += catchFlow.length;

            checkAndRemoveMonitorExit
                (tryFlow, local, catchFlow.addr+catchFlow.length, endHandler);
            
            SynchronizedBlock syncBlock = new SynchronizedBlock(local);
            TryBlock tryBlock = (TryBlock) tryFlow.block;
            syncBlock.replace(tryBlock);
            syncBlock.moveJump(tryBlock.jump);
            syncBlock.setBodyBlock(tryBlock.subBlocks.length == 1
                                   ? tryBlock.subBlocks[0] : tryBlock);
            tryFlow.lastModified = syncBlock;
            return true;
        }
        return false;
    }

    private boolean analyzeFinally(FlowBlock tryFlow, FlowBlock catchFlow,
                                   int end) {
        if (!(catchFlow.block instanceof SequentialBlock
              && catchFlow.block.getSubBlocks()[0] 
              instanceof InstructionBlock))
            return false;
        
        SequentialBlock catchBlock = (SequentialBlock) catchFlow.block;
        Expression instr = 
            ((InstructionBlock)catchBlock.subBlocks[0]).getInstruction();
        
        if (catchBlock.subBlocks[1] instanceof SequentialBlock
            && catchBlock.subBlocks[1].getSubBlocks()[0] 
            instanceof JsrBlock
            && instr instanceof LocalStoreOperator
            && catchBlock.subBlocks[1].getSubBlocks()[1]
            instanceof ThrowBlock
            && ((ThrowBlock)catchBlock.subBlocks[1]
                .getSubBlocks()[1]).instr
            instanceof LocalLoadOperator
            && ((LocalStoreOperator) instr)
            .matches((LocalLoadOperator) 
                     ((ThrowBlock)catchBlock.subBlocks[1]
                      .getSubBlocks()[1]).instr)) {

            /* Wow that was complicated :-)
             * But now we know that the catch block looks
             * exactly like an try finally block:
             *
             *   tryFlow:
             *    |- first instruction
             *    |  ...
             *    |  every jump to outside is preceded by jsr finally
             *    |  ...
             *    |  jsr finally -----------------,
             *    `- jump after finally           |
             *                                    |
             *   catchFlow: (already checked)     |
             *       local_n = stack              v
             *       jsr finally ---------------->|
             *       throw local_n;               |
             *   finally: <-----------------------'
             *      astore_n
             *      ...
             *      return_n
             */

            FlowBlock subRoutine = 
                ((JsrBlock)catchBlock.subBlocks[1].getSubBlocks()[0])
                .innerBlock.jump.destination;

            /* Now remove the two jumps of the catch block
             * so that we can forget about them.
             * This are the jsr and the throw.
             */
            catchBlock.subBlocks[1].getSubBlocks()[0].getSubBlocks()[0]
                .jump.destination.predecessors.removeElement(catchFlow);
            catchBlock.subBlocks[1].getSubBlocks()[1]
                .jump.destination.predecessors.removeElement(catchFlow);
                
            subRoutine.analyze(catchFlow.addr+catchFlow.length, end);
            if (!transformSubRoutine(subRoutine))
                return false;
            Stack jumps = (Stack)tryFlow.successors.get(subRoutine);
            if (jumps != null)
                /* jumps may be null, if tryFlow ends with a throw */
                tryFlow.updateInOut(subRoutine, true, jumps);

            tryFlow.length += catchFlow.length;
            checkAndRemoveJSR(tryFlow, subRoutine);
                
            TryBlock tryBlock = (TryBlock)tryFlow.block;
            if (tryBlock.getSubBlocks()[0] instanceof TryBlock) {
                /* remove the nested tryBlock */
                TryBlock innerTry = (TryBlock)tryBlock.getSubBlocks()[0];
                innerTry.gen = tryBlock.gen;
                innerTry.replace(tryBlock);
                tryBlock = innerTry;
                tryFlow.lastModified = innerTry;
            }
            FinallyBlock newBlock = new FinallyBlock();
            newBlock.setCatchBlock(subRoutine.block);
            tryBlock.addCatchBlock(newBlock);
            tryFlow.mergeSuccessors(subRoutine);
            tryFlow.length += subRoutine.length;
            return true;
        }
        return false;
    }

    private boolean analyzeSpecialFinally(FlowBlock tryFlow, 
                                          FlowBlock catchFlow, int end) {
        
        StructuredBlock firstInstr = 
            catchFlow.block instanceof SequentialBlock 
            ? catchFlow.block.getSubBlocks()[0]: catchFlow.block;

        if (firstInstr instanceof InstructionBlock 
            && ((InstructionBlock) firstInstr).getInstruction()
            instanceof PopOperator
            && ((PopOperator) ((InstructionBlock) firstInstr)
                .getInstruction()).getCount() == 1) {

            /* This is a special try/finally-block, where
             * the finally block ends with a break, return or
             * similar.
             */

            FlowBlock succ = (firstInstr.jump != null) ?
                          firstInstr.jump.destination : null;
            boolean hasExit = false;
            Enumeration keys = tryFlow.successors.keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                if (key == succ)
                    continue;
                if (key != tryFlow.END_OF_METHOD) {
                    /* There is another exit in the try block, bad */
                    return false;
                }
                Enumeration enum = 
                    ((Stack)tryFlow.successors.get(key)).elements();
                while (enum.hasMoreElements()) {
                    Jump throwJump= (Jump)enum.nextElement();
                    if (!(throwJump.prev instanceof ThrowBlock)) {
                        /* There is a return exit in the try block */
                        return false;
                    }
                }
            }

            /* remove the pop now */
            firstInstr.removeBlock();

            if (succ != null) {
                Stack jumps = (Stack) tryFlow.successors.remove(succ);
                succ.predecessors.removeElement(tryFlow);
                /* Handle the jumps in the tryFlow.
                 */
                tryFlow.updateInOut(succ, true, jumps);
                tryFlow.optimizeJumps(jumps, succ);
                tryFlow.resolveRemaining(jumps);
            }

            tryFlow.length += catchFlow.length;
                
            TryBlock tryBlock = (TryBlock)tryFlow.block;
            if (tryBlock.getSubBlocks()[0] instanceof TryBlock) {
                /* remove the unnecessary tryBlock */
                TryBlock innerTry = (TryBlock)tryBlock.getSubBlocks()[0];
                innerTry.gen = tryBlock.gen;
                innerTry.replace(tryBlock);
                tryBlock = innerTry;
                tryFlow.lastModified = innerTry;
            }
            FinallyBlock newBlock = new FinallyBlock();
            tryBlock.addCatchBlock(newBlock);
            /* try block has no successors */
                
            if (succ != null && succ.predecessors.size() == 1) {
                while (succ.analyze(catchFlow.addr+catchFlow.length, end));
                tryFlow.length += succ.length;
                tryFlow.successors.remove(succ);
                newBlock.setCatchBlock(succ.block);
                tryFlow.mergeSuccessors(succ);
            } else {
                /* Put the catchBlock in instaed.
                 */
                newBlock.setCatchBlock(catchFlow.block);
                tryFlow.mergeSuccessors(catchFlow);
            }
            return true;
        }
        return false;
    }

    /**
     * Analyzes all exception handlers to try/catch/finally or
     * synchronized blocks.
     */
    public void analyze() { 
        /* Check if try/catch ranges are okay.  The following succeeds
         * for all classes generated by the sun java compiler, but hand
         * optimized classes will fail this.
         */
        for (int i=0; i<count; i++) {
            int start = startPCs[i];
            int end = endPCs[i];
            int handler = handlerPCs[i];
            if (start >= end || handler < end)
                throw new AssertError("ExceptionHandler order failed: not "
                                      + start + " < " + end + " <= " + 
                                      handler);
            if (i == 0 
                || startPCs[i-1] != start || endPCs[i-1] != end) {
                /* The last handler does catch another range. */
                if ( /*handler > end + 1 || */
                    (i > 0 && end > startPCs[i-1] && end < endPCs[i-1]))
                    throw new AssertError("ExceptionHandler"
                                          + " at wrong position: "
                                          + "end: "+end + " handler: "+handler
                                          + (i>0 ? " last: ("+startPCs[i-1]
                                             +", "+endPCs[i-1]+", "
                                             +handlerPCs[i-1]+")"
                                             :""));
            }
        }

        for (int i=0; i<count; i++) {
            int endHandler = (i< count-1 && endPCs[i+1] > handlerPCs[i]) 
                ? endPCs[i+1]
                : Integer.MAX_VALUE;
            if (jode.Decompiler.debugAnalyze)
                System.err.println("analyzeCatch(" + startPCs[i] + ", "
                                   + endPCs[i] + ", " +handlerPCs[i] + ")");
            FlowBlock tryFlow = flows[startPCs[i]];
            while (tryFlow.analyze(startPCs[i], handlerPCs[i]));

            if (i == 0
                || startPCs[i-1] != startPCs[i] || endPCs[i-1] != endPCs[i]) {
                /* The last handler does catch another range. 
                 * Create a new try block.
                 */
                TryBlock tryBlock = new TryBlock(tryFlow);
            } else if (! (tryFlow.block instanceof TryBlock))
                throw new AssertError("no TryBlock");

            FlowBlock catchFlow = flows[handlerPCs[i]];
            while (catchFlow.analyze(handlerPCs[i], endHandler));

            if (!catchFlow.predecessors.isEmpty())
                throw new AssertError("Handler has a predecessors");

            updateInOutCatch(tryFlow, catchFlow);
            if (types[i] != null) {
                analyzeCatchBlock(types[i], tryFlow, catchFlow);

            } else if (!analyzeSynchronized(tryFlow, catchFlow, endHandler)
                       && ! analyzeFinally(tryFlow, catchFlow, endHandler)
                       && ! analyzeSpecialFinally(tryFlow, catchFlow, 
                                                  endHandler))

                analyzeCatchBlock(jode.Type.tObject, tryFlow, catchFlow);

            tryFlow.checkConsistent();
            if (jode.Decompiler.debugAnalyze)
                System.err.println("analyzeCatch(" + tryFlow.addr + ", "
                                   + (tryFlow.addr + tryFlow.length) + 
                                   ") done.");
        }
    }
}
