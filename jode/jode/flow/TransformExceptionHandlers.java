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
import jode.GlobalOptions;
import jode.type.Type;
import jode.decompiler.LocalInfo;
import jode.expr.*;

///#ifdef JDK12
///import java.util.TreeSet;
///import java.util.SortedSetSet;
///import java.util.Map;
///import java.util.Iterator;
///#else
import jode.util.TreeSet;
import jode.util.SortedSet;
import jode.util.Map;
import jode.util.Iterator;
import jode.util.Comparable;
///#endif

/**
 * 
 * @author Jochen Hoenicke
 */
public class TransformExceptionHandlers {
    SortedSet handlers;
    
    static class Handler implements Comparable {
	FlowBlock start;
	int endAddr;
	FlowBlock handler;
	Type type;

	public Handler(FlowBlock tryBlock, int end, 
		       FlowBlock catchBlock, Type type) {
	    this.start = tryBlock;
	    this.endAddr = end;
	    this.handler = catchBlock;
	    this.type = type;
	}

	public int compareTo (Object o) {
	    Handler second = (Handler) o;

	    /* First sort by start offsets, highest address first...*/
	    if (start.addr != second.start.addr)
		/* this subtraction is save since addresses are only 16 bit */
		return second.start.addr - start.addr;

	    /* ...Second sort by end offsets, lowest address first...
	     * this will move the innermost blocks to the beginning. */
	    if (endAddr != second.endAddr)
		return endAddr - second.endAddr;

	    /* ...Last sort by handler offsets, lowest first */
	    if (handler.addr != second.handler.addr)
		return handler.addr - second.handler.addr;
	    
	    /* ...Last sort by typecode signature.  Shouldn't happen to often.
	     */
	    if (type == second.type)
		return 0;
	    if (type == null)
		return -1;
	    if (second.type == null)
		return 1;
	    return type.getTypeSignature()
		.compareTo(second.type.getTypeSignature());
	}
    }

    public TransformExceptionHandlers() {
	handlers = new TreeSet();
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
	handlers.add(new Handler(tryBlock, end, catchBlock, type));
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
     *     System.out.println(a);
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
        Iterator succs = catchFlow.successors.values().iterator();
        while (succs.hasNext()) {
            for (Jump succJumps = (Jump) succs.next();
                 succJumps != null; succJumps = succJumps.next) {
                succJumps.gen.mergeGenKill(gens, succJumps.kill);
            }
        }
        tryFlow.in.unionExact(catchFlow.in);
        tryFlow.gen.unionExact(catchFlow.gen);
    
        if ((GlobalOptions.debuggingFlags & GlobalOptions.DEBUG_INOUT) != 0) {
            GlobalOptions.err.println("UpdateInOutCatch: gens : "+gens);
            GlobalOptions.err.println("                  s.in : "+catchFlow.in);
            GlobalOptions.err.println("                  in   : "+tryFlow.in);
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

        CatchBlock newBlock = new CatchBlock(type);
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
        
        if (!(instr.getInstruction() instanceof StoreInstruction)
	    || !(((StoreInstruction) instr.getInstruction()).getLValue()
		 instanceof LocalStoreOperator))
            return false;
        LocalStoreOperator store = (LocalStoreOperator) 
	    ((StoreInstruction)instr.getInstruction()).getLValue();
        
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
                                StoreInstruction store = (StoreInstruction)
                                    ((InstructionBlock)
                                     ret.outer.getSubBlocks()[0]).instr;
				LocalInfo local = 
				    ((LocalStoreOperator) store.getLValue())
				    .getLocalInfo();
				if (store.lvalueMatches
				    ((LocalLoadOperator)
				     ret.getInstruction())) {
				    Expression expr = 
					store.getSubExpressions()[1];
				    ret.setInstruction(expr);
                                    ret.replace(ret.outer);
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
        Iterator iter = tryFlow.successors.entrySet().iterator();
        while (iter.hasNext()) {
	    Map.Entry entry = (Map.Entry) iter.next();
            Jump jumps = (Jump) entry.getValue();
            if (entry.getKey() == subRoutine)
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
        if (instr instanceof MonitorExitOperator) {
            MonitorExitOperator monExit = (MonitorExitOperator)instr;
            if (monExit.getFreeOperandCount() == 0
                && monExit.getSubExpressions()[0] instanceof LocalLoadOperator
                && (((LocalLoadOperator) monExit.getSubExpressions()[0])
                    .getLocalInfo().getSlot() == local.getSlot())) {
                return true;
            }
        }
        return false;
    }

    public void checkAndRemoveMonitorExit(FlowBlock tryFlow, LocalInfo local, 
                                          int startMonExit, int endMonExit) {
        FlowBlock subRoutine = null;
        Iterator succs = tryFlow.successors.values().iterator();
    dest_loop:
        while (succs.hasNext()) {
            for (Jump jumps = (Jump) succs.next();
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
			    if (isMonitorExit(instr, local))
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
        
        if (instr instanceof MonitorExitOperator
	    && instr.getFreeOperandCount() == 0
            && (((MonitorExitOperator)instr).getSubExpressions()[0] 
		instanceof LocalLoadOperator)
            && catchBlock.subBlocks[1] instanceof ThrowBlock
            && (((ThrowBlock)catchBlock.subBlocks[1]).instr 
		instanceof NopOperator)) {

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
            MonitorExitOperator monexit = (MonitorExitOperator)
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
            && instr instanceof StoreInstruction
            && catchBlock.getSubBlocks()[1] instanceof ThrowBlock
            && (((ThrowBlock)catchBlock.getSubBlocks()[1]).instr
                instanceof LocalLoadOperator)
            && (((StoreInstruction) instr).lvalueMatches
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
            Iterator iter = tryFlow.successors.entrySet().iterator();
            while (iter.hasNext()) {
		Map.Entry entry = (Map.Entry) iter.next();
                Object key = entry.getKey();
                if (key == succ)
                    continue;
                if (key != FlowBlock.END_OF_METHOD) {
                    /* There is another exit in the try block, bad */
                    return false;
                }
                for (Jump throwJumps = (Jump) entry.getValue();
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
	{ 
	    Handler last = null;
	    for (Iterator i = handlers.iterator(); i.hasNext(); ) {
		Handler exc = (Handler) i.next();
		int start = exc.start.addr;
		int end = exc.endAddr;
		int handler = exc.handler.addr;
		if (start >= end || handler < end)
		    throw new AssertError
			("ExceptionHandler order failed: not "
			 + start + " < " + end + " <= " + handler);
		if (last != null
		    && (last.start.addr != start || last.endAddr != end)) {
		    /* The last handler does catch another range. 
		     * Due to the order:
		     *  start < last.start.addr || end > last.end.addr
		     */
		    if (end > last.start.addr && end < last.endAddr)
			throw new AssertError
			    ("Exception handlers ranges are intersecting: ["
			 + last.start.addr+", "+last.endAddr+"] and ["
			     + start+", "+end+"].");
		}
		last = exc;
	    }
	}

	{
	    Iterator i = handlers.iterator();
	    Handler exc = null;
	    Handler next = i.hasNext() ? (Handler) i.next() : null;
	    while(next != null) {
		Handler last = exc;
		exc = next;
		next = i.hasNext() ? (Handler) i.next() : null;
		int endHandler = Integer.MAX_VALUE;
		/* If the next exception handler catches a bigger range
		 * it must surround the handler completely.
		 */
		if (next != null && next.endAddr > exc.endAddr)
		    endHandler = next.endAddr;

		FlowBlock tryFlow = exc.start;
		tryFlow.checkConsistent();
		if ((GlobalOptions.debuggingFlags
		     & GlobalOptions.DEBUG_ANALYZE) != 0)
		    GlobalOptions.err.println
			("analyzeTry("
			 + exc.start.addr + ", " + exc.endAddr+")");
		while (tryFlow.analyze(tryFlow.addr, exc.endAddr));

		if (last == null
		    || last.start.addr != exc.start.addr
		    || last.endAddr != exc.endAddr) {
		    /* The last handler does catch another range. 
		     * Create a new try block.
		     */
		    TryBlock tryBlock = new TryBlock(tryFlow);
		} else if (! (tryFlow.block instanceof TryBlock))
		    throw new AssertError("no TryBlock");

		FlowBlock catchFlow = exc.handler;
		boolean isMultiUsed = catchFlow.predecessors.size() != 0;
		if (!isMultiUsed && next != null) {
		    for (Iterator j = handlers.tailSet(next).iterator(); 
			 j.hasNext();) {
			Handler h = (Handler) j.next();
			if (h.handler == catchFlow) {
			    isMultiUsed = true;
			    break;
			}
		    }
		}
			 
		if (isMultiUsed) {
		    /* If this exception is used in other exception handlers,
		     * create a new flow block, that jumps to the handler.
		     * This will be our new exception handler.
		     */
		    EmptyBlock jump = new EmptyBlock(new Jump(catchFlow));
		    FlowBlock newFlow = new FlowBlock(catchFlow.method,
						      catchFlow.addr, 0);
		    newFlow.setBlock(jump);
		    catchFlow.prevByAddr.setNextByAddr(newFlow);
		    newFlow.setNextByAddr(catchFlow);
		    catchFlow = newFlow;
		} else {
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_ANALYZE) != 0)
			GlobalOptions.err.println
			    ("analyzeCatch("
			     + catchFlow.addr + ", " + endHandler + ")");
		    while (catchFlow.analyze(catchFlow.addr, endHandler));
		}
		    
		updateInOutCatch(tryFlow, catchFlow);
		if (exc.type != null)
		    analyzeCatchBlock(exc.type, tryFlow, catchFlow);
		
		else if (!analyzeSynchronized(tryFlow, catchFlow, endHandler)
			 && ! analyzeFinally(tryFlow, catchFlow, endHandler)
			 && ! analyzeSpecialFinally(tryFlow, catchFlow, 
						    endHandler))
		    
		    analyzeCatchBlock(Type.tObject, tryFlow, catchFlow);
		
		tryFlow.checkConsistent();
		if ((GlobalOptions.debuggingFlags
		     & GlobalOptions.DEBUG_ANALYZE) != 0)
		    GlobalOptions.err.println
			("analyzeCatch(" + tryFlow.addr + ", "
			 + (tryFlow.addr + tryFlow.length) + ") done.");
	    }
	}
    }
}
