/* TransformExceptionHandlers Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.Decompiler;
import jode.type.Type;
import jode.decompiler.LocalInfo;
import jode.expr.*;

import java.util.Enumeration;

/**
 * 
 * @author Jochen Hoenicke
 */
public class TransformExceptionHandlers {
    int count;
    FlowBlock[] trys    = new FlowBlock[4];
    FlowBlock[] catches = new FlowBlock[4];
    int[] endPCs = new int[4];
    Type[] types = new Type[4];
    
    public TransformExceptionHandlers() {
        count = 0;
    }

    /**
     * Add an exception Handler.
     * @param start The start address of the exception range.
     * @param end The end address of the exception range + 1.
     * @param handler The address of the handler.
     * @param type The type of the exception, null for ALL.
     */
    public void addHandler(FlowBlock tryBlock, int end, 
			   FlowBlock catchBlock, Type type) {
        int offset = 0;
	int start = tryBlock.addr;
	int handler = catchBlock.addr;
        /* First sort by start offsets, highest address first...*/
        while (offset < count && start < trys[offset].addr)
            offset++;
        /* ...Second sort by end offsets, lowest address first...
         * this will move the innermost blocks to the beginning. */
        while (offset < count && start == trys[offset].addr 
               && end > endPCs[offset])
            offset++;
        /* ...Last sort by handler offsets, lowest first */
        while (offset < count && start == trys[offset].addr
               && end == endPCs[offset] && handler > catches[offset].addr)
            offset++;

        if (count++ >= trys.length) {
            /* We grow the arrays by 50 % */
            int newSize = trys.length * 3 / 2;
            FlowBlock[] newTrys = new FlowBlock[newSize];
            int[] newEndPCs = new int[newSize];
            FlowBlock[] newCatches = new FlowBlock[newSize];
            Type[] newTypes = new Type[newSize];
            System.arraycopy(trys, 0, newTrys, 0, offset);
            System.arraycopy(endPCs, 0, newEndPCs, 0, offset);
            System.arraycopy(catches, 0, newCatches, 0, offset);
            System.arraycopy(types, 0, newTypes, 0, offset);

            if (offset+1 < count) {
                System.arraycopy(trys, offset, newTrys, offset+1, 
                                 count-offset-1);
                System.arraycopy(endPCs, offset, newEndPCs, offset+1, 
                                 count-offset-1);
                System.arraycopy(catches, offset, newCatches, offset+1, 
                                 count-offset-1);
                System.arraycopy(types, offset, newTypes, offset+1, 
                                 count-offset-1);
            }
            trys = newTrys;
            endPCs = newEndPCs;
            catches = newCatches;
            types = newTypes;
        } else if (offset+1 < count) {
            /* Move the tailing data one place below
             */
            System.arraycopy(trys, offset, trys, offset+1, 
                             count-offset-1);
            System.arraycopy(endPCs, offset, endPCs, offset+1, 
                             count-offset-1);
            System.arraycopy(catches, offset, catches, offset+1, 
                             count-offset-1);
            System.arraycopy(types, offset, types, offset+1, 
                             count-offset-1);
        }
        /* Insert the new handler */
        trys[offset] = tryBlock;
        endPCs[offset] = end;
        catches[offset] = catchBlock;
        types[offset] = type;
    }

    /** 
     * Updates the in/out-Vectors of the structured block of the
     * successing flow block for a try catch block.  The main difference
     * to updateInOut in FlowBlock is, that this function works, as if
     * every instruction would have a jump.  This is because every
     * instruction can throw an exception and thus enter the catch block.<br>
     *
     * For example this code prints <code>0</code>:
     * <pre>
     *   int a=3;
     *   try {
     *     a = 5 / (a=0);
     *   } catch (DivideByZeroException ex) {
     *     System.err.println(a);
     *   }
     * </pre>
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
        Enumeration succs = catchFlow.successors.elements();
        while (succs.hasMoreElements()) {
            for (Jump succJumps = (Jump) succs.nextElement();
                 succJumps != null; succJumps = succJumps.next) {
                succJumps.gen.mergeGenKill(gens, succJumps.kill);
            }
        }
        tryFlow.in.unionExact(catchFlow.in);
        tryFlow.gen.unionExact(catchFlow.gen);
    
        if (Decompiler.debugInOut) {
            Decompiler.err.println("UpdateInOutCatch: gens : "+gens);
            Decompiler.err.println("                  s.in : "+catchFlow.in);
            Decompiler.err.println("                  in   : "+tryFlow.in);
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
    static void analyzeCatchBlock(Type type, 
                                  FlowBlock tryFlow, FlowBlock catchFlow) {

        StructuredBlock catchBlock = catchFlow.block;
        LocalInfo local = null;
        StructuredBlock firstInstr = (catchBlock instanceof SequentialBlock)
            ? catchBlock.getSubBlocks()[0] : catchBlock;

	if (firstInstr instanceof SpecialBlock
	    && ((SpecialBlock) firstInstr).type == SpecialBlock.POP
	    && ((SpecialBlock) firstInstr).count == 1) {
	    /* The exception is ignored.  Create a dummy local for it */
	    local = new LocalInfo();
	    local.setName("exception");
	    firstInstr.removeBlock();

	} else if (firstInstr instanceof InstructionBlock) {
            Expression instr = 
                ((InstructionBlock) firstInstr).getInstruction();
            if (instr instanceof LocalStoreOperator) {
                /* The exception is stored in a local variable */
                local = ((LocalStoreOperator) instr).getLocalInfo();
                firstInstr.removeBlock();
            }
        }

        if (local == null) {
            local = new LocalInfo();
            local.setName("ERROR!!!");
        }
        local.setType(type);

        CatchBlock newBlock = new CatchBlock(type, local);
        ((TryBlock)tryFlow.block).addCatchBlock(newBlock);
        newBlock.setCatchBlock(catchFlow.block);
        tryFlow.mergeSuccessors(catchFlow);
	tryFlow.mergeAddr(catchFlow);
    }

    /* And now the complicated parts. */
    
    /**
     * This transforms a sub routine, that is checks if the beginning
     * local assignment matches the final ret and then returns.
     */
    boolean transformSubRoutine(StructuredBlock subRoutine) {
        if (!(subRoutine instanceof SequentialBlock)
            || !(subRoutine.getSubBlocks()[0] instanceof InstructionBlock))
            return false;
        SequentialBlock sequBlock = (SequentialBlock) subRoutine;
        InstructionBlock instr = (InstructionBlock)sequBlock.subBlocks[0];
        
        if (! (instr.getInstruction() instanceof LocalStoreOperator))
            return false;
        LocalStoreOperator store = (LocalStoreOperator) instr.getInstruction();
        
        while (sequBlock.subBlocks[1] instanceof SequentialBlock)
            sequBlock = (SequentialBlock) sequBlock.subBlocks[1];
        
	/* XXX - Check that the local isn't used for any other purposes 
	 * than RET and replace any RET with a flow control to end of 
	 * flow block.
	 *
	 * This is a complicated task which isn't needed for javac nor jikes.
	 */
	if (sequBlock.subBlocks[1].jump != null
	    && sequBlock.subBlocks[1].jump.destination
	    == FlowBlock.END_OF_METHOD) {
	    instr.removeBlock();
	    return true;
	}
        if (! (sequBlock.subBlocks[1] instanceof RetBlock)
            || !(((RetBlock)sequBlock.subBlocks[1])
                 .local.equals(store.getLocalInfo())))
                return false;
        
        instr.removeBlock();
        sequBlock.subBlocks[1].removeBlock();
        return true;
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
        for (Jump jumps = (Jump)tryFlow.successors.remove(subRoutine);
             jumps != null; jumps = jumps.next) {

            StructuredBlock prev = jumps.prev;
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
                    if (seq.subBlocks[1] instanceof JsrBlock
                        || (seq.subBlocks[1] instanceof SequentialBlock
                            && seq.subBlocks[1].getSubBlocks()[0] 
                            instanceof JsrBlock)) {
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
        Enumeration succs = tryFlow.successors.elements();
        while (keys.hasMoreElements()) {
            Jump jumps = (Jump) succs.nextElement();
            if (keys.nextElement() == subRoutine)
                continue;

            for ( ; jumps != null; jumps = jumps.next) {

                StructuredBlock prev = jumps.prev;
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
                DescriptionBlock msg 
                    = new DescriptionBlock("ERROR: NO JSR TO FINALLY");
                prev.appendBlock(msg);
                msg.moveJump(jumps);
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
        Enumeration succs = tryFlow.successors.elements();
    dest_loop:
        while (succs.hasMoreElements()) {
            for (Jump jumps = (Jump) succs.nextElement();
                 jumps != null; jumps = jumps.next) {

                StructuredBlock prev = jumps.prev;

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

                /* If the block is a jsr or a return block, check if
                 * it is preceeded by another jsr.
                 */
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
                        if (pred instanceof JsrBlock)
                            /* The jump is preceeded by another jsr, okay.
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

                /* The block is a jsr that is not preceeded by another jsr.
                 * This must be the monitorexit subroutine.
                 */
                if (prev instanceof JsrBlock && subRoutine == null) {
                    
                    subRoutine = jumps.destination;
                    subRoutine.analyze(startMonExit, endMonExit);
                    transformSubRoutine(subRoutine.block);
                
                    if (subRoutine.block instanceof InstructionBlock) {
                        Expression instr = 
                            ((InstructionBlock)subRoutine.block)
                            .getInstruction();
                        if (isMonitorExit(instr, local)) {
                            tryFlow.mergeAddr(subRoutine);
                            continue dest_loop;
                        }
                    }
                }

                /* Now we have a jump that is not preceded by a monitorexit.
                 * Complain!
                 */
                DescriptionBlock msg 
                    = new DescriptionBlock("ERROR: NO MONITOREXIT");
                prev.appendBlock(msg);
                msg.moveJump(jumps);
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
             *   |- synchronized block
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
             *  optional subroutine: <-----------------------------------'
             *    astore_n
             *    monitorexit local_x
             *    return_n
             */

            /* Now remove the jump (after the throw) from the
             * catch block so that we can forget about it.  
             */
            
            catchFlow.removeSuccessor(catchBlock.subBlocks[1].jump);
            ComplexExpression monexit = (ComplexExpression)
                ((InstructionBlock) catchBlock.subBlocks[0]).instr;
            LocalInfo local = 
                ((LocalLoadOperator)monexit.getSubExpressions()[0])
                    .getLocalInfo();
            tryFlow.mergeAddr(catchFlow);

            checkAndRemoveMonitorExit
                (tryFlow, local, catchFlow.getNextAddr(), endHandler);
            
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

        /* Layout of a try-finally block:  
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
        
        if (!(catchFlow.block instanceof SequentialBlock)
            || !(catchFlow.block.getSubBlocks()[0] 
                 instanceof InstructionBlock)
            || !(catchFlow.block.getSubBlocks()[1]
                 instanceof SequentialBlock))
            return false;
        
        StructuredBlock finallyBlock = null;
        SequentialBlock catchBlock = (SequentialBlock) catchFlow.block;
        Expression instr = 
            ((InstructionBlock)catchBlock.subBlocks[0]).getInstruction();
        catchBlock = (SequentialBlock)catchBlock.subBlocks[1];

        if (catchBlock.subBlocks[0] instanceof LoopBlock) {
            /* In case the try block has no exit (that means, it throws
             * an exception), the finallyBlock was already merged with
             * the catchBlock.  We have to check for this case separately:
             *
             * do {
             *    JSR
             *       break;
             *    throw local_x
             * } while(false);
             * finallyBlock;
             */
            LoopBlock doWhileFalse = (LoopBlock)catchBlock.subBlocks[0];
            if (doWhileFalse.type == LoopBlock.DOWHILE
                && doWhileFalse.cond == LoopBlock.FALSE
                && doWhileFalse.bodyBlock instanceof SequentialBlock) {
                finallyBlock = catchBlock.subBlocks[1];
                catchBlock = (SequentialBlock) doWhileFalse.bodyBlock;
            }
        }

        if (catchBlock instanceof SequentialBlock
            && catchBlock.getSubBlocks()[0] instanceof JsrBlock
            && instr instanceof LocalStoreOperator
            && catchBlock.getSubBlocks()[1] instanceof ThrowBlock
            && (((ThrowBlock)catchBlock.getSubBlocks()[1]).instr
                instanceof LocalLoadOperator)
            && (((LocalStoreOperator) instr).matches
                ((LocalLoadOperator)
                 ((ThrowBlock)catchBlock.getSubBlocks()[1]).instr))) {

            /* Wow that was complicated :-)
             * But now we know that the catch block looks
             * exactly like it should:
             *
             * catchBlock:
             *   JSR 
             *       finally
             *   throw local_n  <- matches the local in instr.
             */

            if (finallyBlock != null) {
                /* Check if the jsr breaks (see two comments above). We don't 
                 * need to check if it breaks to the right block, because
                 * we know that there is only one Block around the jsr.
                 */
                if (!(((JsrBlock)catchBlock.getSubBlocks()[0]).innerBlock
                      instanceof BreakBlock))
                    return false;

                /* Check if the try block has no exit (except throws)
                 */
                Jump throwJumps = (Jump) 
                    tryFlow.successors.get(FlowBlock.END_OF_METHOD);
                if (tryFlow.successors.size() > 1
                    || (tryFlow.successors.size() > 0 && throwJumps == null))
                    return false;

                for (/**/; throwJumps != null; throwJumps = throwJumps.next) {
                    if (!(throwJumps.prev instanceof ThrowBlock)) 
                        /* There is a return exit in the try block */
                        return false;
                }
                /* Remove the jump of the throw instruction.
                 */
                catchFlow.removeSuccessor(catchBlock.getSubBlocks()[1].jump);
		catchBlock.getSubBlocks()[1].removeJump();

                /* Replace the catchBlock with the finallyBlock.
                 */
                finallyBlock.replace(catchFlow.block);
                transformSubRoutine(finallyBlock);

                updateInOutCatch(tryFlow, catchFlow);
                tryFlow.mergeAddr(catchFlow);
                finallyBlock = catchFlow.block;
                tryFlow.mergeSuccessors(catchFlow);

            } else {
                FlowBlock subRoutine = 
                    ((JsrBlock)catchBlock.getSubBlocks()[0])
                    .innerBlock.jump.destination;

                subRoutine.analyze(catchFlow.getNextAddr(), end);
                if (!transformSubRoutine(subRoutine.block))
                    return false;

                tryFlow.mergeAddr(catchFlow);

                checkAndRemoveJSR(tryFlow, subRoutine);

                updateInOutCatch(tryFlow, subRoutine);                
                tryFlow.mergeAddr(subRoutine);
                tryFlow.mergeSuccessors(subRoutine);
                finallyBlock = subRoutine.block;

                /* Now remove the jump to the JSR from the catch block
                 * and the jump of the throw instruction.
                 */
                catchBlock.getSubBlocks()[0].getSubBlocks()[0]
                    .jump.destination.predecessors.removeElement(catchFlow);
                catchBlock.getSubBlocks()[1]
                    .jump.destination.predecessors.removeElement(catchFlow);
            }
                
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
            newBlock.setCatchBlock(finallyBlock);
            tryBlock.addCatchBlock(newBlock);
            return true;
        }
        return false;
    }

    private boolean analyzeSpecialFinally(FlowBlock tryFlow, 
                                          FlowBlock catchFlow, int end) {
        
        StructuredBlock firstInstr = 
            catchFlow.block instanceof SequentialBlock 
            ? catchFlow.block.getSubBlocks()[0]: catchFlow.block;

        if (firstInstr instanceof SpecialBlock 
            && ((SpecialBlock)firstInstr).type == SpecialBlock.POP
            && ((SpecialBlock)firstInstr).count == 1) {

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
                if (key != FlowBlock.END_OF_METHOD) {
                    /* There is another exit in the try block, bad */
                    return false;
                }
                for (Jump throwJumps = (Jump) tryFlow.successors.get(key);
                     throwJumps != null; throwJumps = throwJumps.next) {
                    if (!(throwJumps.prev instanceof ThrowBlock)) {
                        /* There is a return exit in the try block */
                        return false;
                    }
                }
            }

            /* remove the pop now */
            firstInstr.removeBlock();
            tryFlow.mergeAddr(catchFlow);

            if (succ != null) {
                Jump jumps = (Jump) tryFlow.successors.remove(succ);
                succ.predecessors.removeElement(tryFlow);
                /* Handle the jumps in the tryFlow.
                 */
                jumps = tryFlow.resolveSomeJumps(jumps, succ);
                tryFlow.resolveRemaining(jumps);
            }

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
                while (succ.analyze(catchFlow.getNextAddr(), end));
		tryFlow.mergeAddr(succ);
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
         * optimized classes (or generated by other compilers) will fail.
         */
        for (int i=0; i<count; i++) {
            int start = trys[i].addr;
            int end = endPCs[i];
            int handler = catches[i].addr;
            if (start >= end || handler < end)
                throw new AssertError("ExceptionHandler order failed: not "
                                      + start + " < " + end + " <= " + 
                                      handler);
            if (i == 0 
                || trys[i-1].addr != start || endPCs[i-1] != end) {
                /* The last handler does catch another range. */
                if ( /*handler > end + 1 || */
                    (i > 0 && end > trys[i-1].addr && end < endPCs[i-1]))
                    throw new AssertError("ExceptionHandler"
                                          + " at wrong position: "
                                          + "end: "+end + " handler: "+handler
                                          + (i>0 ? " last: ("+trys[i-1].addr
                                             +", "+endPCs[i-1]+", "
                                             +catches[i-1].addr+")"
                                             :""));
            }
        }

        for (int i=0; i<count; i++) {
            int endHandler = (i< count-1 && endPCs[i+1] > catches[i].addr) 
                ? endPCs[i+1]
                : Integer.MAX_VALUE;
            if (Decompiler.debugAnalyze)
                Decompiler.err.println("analyzeCatch(" + trys[i].addr + ", "
                                   + endPCs[i] + ", " +catches[i].addr + ")");
            FlowBlock tryFlow = trys[i];
            tryFlow.checkConsistent();
            while (tryFlow.analyze(tryFlow.addr, catches[i].addr));

            if (i == 0
                || trys[i-1].addr != trys[i].addr || endPCs[i-1] != endPCs[i]) {
                /* The last handler does catch another range. 
                 * Create a new try block.
                 */
                TryBlock tryBlock = new TryBlock(tryFlow);
            } else if (! (tryFlow.block instanceof TryBlock))
                throw new AssertError("no TryBlock");

            FlowBlock catchFlow = catches[i];
            while (catchFlow.analyze(catchFlow.addr, endHandler));

            updateInOutCatch(tryFlow, catchFlow);
            if (types[i] != null)
                analyzeCatchBlock(types[i], tryFlow, catchFlow);

            else if (!analyzeSynchronized(tryFlow, catchFlow, endHandler)
                     && ! analyzeFinally(tryFlow, catchFlow, endHandler)
                     && ! analyzeSpecialFinally(tryFlow, catchFlow, 
                                                endHandler))

                analyzeCatchBlock(Type.tObject, tryFlow, catchFlow);

            tryFlow.checkConsistent();
            if (Decompiler.debugAnalyze)
                Decompiler.err.println("analyzeCatch(" + tryFlow.addr + ", "
				       + (tryFlow.addr + tryFlow.length) + 
				       ") done.");
        }
    }
}
