/* TransformExceptionHandlers Copyright (C) 1998-2001 Jochen Hoenicke.
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
import jode.GlobalOptions;
import jode.type.Type;
import jode.decompiler.LocalInfo;
import jode.expr.*;

///#def COLLECTIONS java.util
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.Set;
import java.util.Map;
import java.util.Iterator;
///#enddef
///#def COLLECTIONEXTRA java.lang
import java.lang.Comparable;
///#enddef

/**
 * 
 * @author Jochen Hoenicke
 */
public class TransformExceptionHandlers {
    SortedSet handlers;
    FlowBlock[] flowBlocks;
    
    static class Handler implements Comparable {
	FlowBlock start;
	FlowBlock end;
	FlowBlock handler;
	Type type;

	public Handler(FlowBlock tryBlock, FlowBlock endBlock, 
		       FlowBlock catchBlock, Type type) {
	    this.start = tryBlock;
	    this.end = endBlock;
	    this.handler = catchBlock;
	    this.type = type;
	}

	public int compareTo (Object o) {
	    Handler second = (Handler) o;

	    /* First sort by start offsets, highest block number first...*/
	    if (start.getBlockNr() != second.start.getBlockNr())
		/* this subtraction is save since block numbers are only 16 bit */
		return second.start.getBlockNr() - start.getBlockNr();

	    /* ...Second sort by end offsets, lowest block number first...
	     * this will move the innermost blocks to the beginning. */
	    if (end.getBlockNr() != second.end.getBlockNr())
		return end.getBlockNr() - second.end.getBlockNr();

	    /* ...Last sort by handler offsets, lowest first */
	    if (handler.getBlockNr() != second.handler.getBlockNr())
		return handler.getBlockNr() - second.handler.getBlockNr();
	    
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

    public TransformExceptionHandlers(FlowBlock[] flowBlocks) {
	handlers = new TreeSet();
	this.flowBlocks = flowBlocks;
    }

    /**
     * Add an exception Handler.
     * @param start The start block number of the exception range.
     * @param end The end block number of the exception range + 1.
     * @param handler The block number of the handler.
     * @param type The type of the exception, null for ALL.
     */
    public void addHandler(FlowBlock tryBlock, FlowBlock endBlock, 
			   FlowBlock catchBlock, Type type) {
	handlers.add(new Handler(tryBlock, endBlock, catchBlock, type));
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
    static void analyzeCatchBlock(Type type, FlowBlock tryFlow, 
				  StructuredBlock catchBlock) {
        CatchBlock newBlock = new CatchBlock(type);
        ((TryBlock)tryFlow.block).addCatchBlock(newBlock);
        newBlock.setCatchBlock(catchBlock);
    }

    /* And now the complicated parts. */
    
    /**
     * This transforms a sub routine, that is checks if the beginning
     * local assignment matches the final ret and then returns.
     */
    boolean transformSubRoutine(StructuredBlock subRoutine) {
        if (!(subRoutine instanceof SequentialBlock))
	    return false;
	SequentialBlock sequBlock = (SequentialBlock) subRoutine;
	StructuredBlock firstBlock = sequBlock.getSubBlocks()[0];

	LocalInfo local = null;
	if (firstBlock instanceof SpecialBlock) {
	    SpecialBlock popBlock
		= (SpecialBlock) firstBlock;
	    if (popBlock.type != SpecialBlock.POP
		|| popBlock.count != 1)
		return false;
	} else if (firstBlock instanceof InstructionBlock) {
	    Expression expr
		= ((InstructionBlock) firstBlock).getInstruction();
	    if (expr instanceof StoreInstruction
		&& ((StoreInstruction) 
		    expr).getLValue() instanceof LocalStoreOperator) {
		LocalStoreOperator store = (LocalStoreOperator) 
		    ((StoreInstruction)expr).getLValue();
		local = store.getLocalInfo();
		expr = ((StoreInstruction) expr).getSubExpressions()[1];
	    }
	    if (!(expr instanceof NopOperator))
		return false;
	} else
	    return false;

	/* We are now committed.  Remove the first Statement which
	 * stores/removes the return address.
	 */
	firstBlock.removeBlock();
        
	/* XXX - Replace any RET with a jump to end of this flow block.
	 *
	 * This is a complicated task which isn't needed for javac nor
	 * jikes.  We just check if the last instruction is a ret and
	 * replace this.  This will never produce code with wrong semantic,
	 * as long as the bytecode was verified correctly.
	 */
        while (sequBlock.subBlocks[1] instanceof SequentialBlock)
            sequBlock = (SequentialBlock) sequBlock.subBlocks[1];

        if (sequBlock.subBlocks[1] instanceof RetBlock
            && (((RetBlock) sequBlock.subBlocks[1]).local.equals(local))) {
	    sequBlock.subBlocks[1].removeBlock();
	}
        return true;
    }

    /**
     * Remove the locale that javac introduces to temporary store the return
     * value, when it executes a finally block resp. monitorexit
     * @param ret the ReturnBlock.
     */
    private void removeReturnLocal(ReturnBlock ret) {
	StructuredBlock pred = getPredecessor(ret);
	if (!(pred instanceof InstructionBlock))
	    return;
	Expression instr = ((InstructionBlock) pred).getInstruction();
	if (!(instr instanceof StoreInstruction))
	    return;

	Expression retInstr = ret.getInstruction();
	if (!(retInstr instanceof LocalLoadOperator
	      && ((StoreInstruction) instr).lvalueMatches
	      ((LocalLoadOperator) retInstr)))
	    return;

	Expression rvalue = ((StoreInstruction) instr).getSubExpressions()[1];
	ret.setInstruction(rvalue);
	ret.replace(ret.outer);
    }

    /**
     * Remove the wrongly placed JSRs jumping to the specified
     * subRoutine. The right JSRs are already removed, but we have to
     * replace the wrong ones with a warning.
     * @param tryFlow the FlowBLock of the try block.
     * @param subRoutine the FlowBlock of the sub routine.
     */
    private void removeJSR(FlowBlock tryFlow, StructuredBlock catchBlock,
			   FlowBlock subRoutine) {
	Jump nextJump;
        for (Jump jumps = tryFlow.getJumps(subRoutine); 
	     jumps != null; jumps = nextJump) {

            StructuredBlock prev = jumps.prev;
	    nextJump = jumps.next;
            if (prev instanceof EmptyBlock
                && prev.outer instanceof JsrBlock) {
		JsrBlock jsr = (JsrBlock) prev.outer;
		if (prev.outer == catchBlock) {
		    /* This is the mandatory jsr in the catch block */
		    continue;
		}

		tryFlow.removeSuccessor(jumps);
		prev.removeJump();
		if (jsr.isGood()) {
		    StructuredBlock next = jsr.getNextBlock();
		    jsr.removeBlock();
		    if (next instanceof ReturnBlock)
			removeReturnLocal((ReturnBlock) next);
		} else {
		    /* We have a JSR to the subroutine, which is badly placed.
		     * We complain here.
		     */
		    DescriptionBlock msg 
			= new DescriptionBlock("ERROR: JSR FINALLY BLOCK!");
		    msg.replace(prev.outer);
		}
            } else {
		/* We have a jump to the subroutine, that is wrong.
		 * We complain here.
		 */
		DescriptionBlock msg 
		    = new DescriptionBlock("ERROR: GOTO FINALLY BLOCK!");
		tryFlow.removeSuccessor(jumps);
		prev.removeJump();
		prev.appendBlock(msg);
	    }
        }
    }
    
    private static StructuredBlock getPredecessor(StructuredBlock stmt)
    {
	if (stmt.outer instanceof SequentialBlock) {
	    SequentialBlock seq = (SequentialBlock) stmt.outer;
	    if (seq.subBlocks[1] == stmt)
		return seq.subBlocks[0];
	    else if (seq.outer instanceof SequentialBlock)
		return seq.outer.getSubBlocks()[0];
	}
	return null;
    }

    /**
     * Gets the slot of the monitorexit instruction instr in the
     * stmt, or -1 if stmt isn't a InstructionBlock with a
     * monitorexit instruction.
     * @param stmt the stmt, may be null.
     */
    private static int getMonitorExitSlot(StructuredBlock stmt) {
	if (stmt instanceof InstructionBlock) {
	    Expression instr = ((InstructionBlock) stmt).getInstruction();
	    if (instr instanceof MonitorExitOperator) {
		MonitorExitOperator monExit = (MonitorExitOperator)instr;
		if (monExit.getFreeOperandCount() == 0
		    && (monExit.getSubExpressions()[0] 
			instanceof LocalLoadOperator))
		return ((LocalLoadOperator) monExit.getSubExpressions()[0])
                    .getLocalInfo().getSlot();
            }
        }
        return -1;
    }
    
    private boolean isMonitorExitSubRoutine(FlowBlock subRoutine, 
					    LocalInfo local) {
	if (transformSubRoutine(subRoutine.block)
	    && getMonitorExitSlot(subRoutine.block) == local.getSlot())
	    return true;
	return false;
    }

    private static StructuredBlock skipFinExitChain(StructuredBlock block)
    {
	StructuredBlock pred, result;
	if (block instanceof ReturnBlock)
	    pred = getPredecessor(block);
	else
	    pred = block;
	result = null;

	while (pred instanceof JsrBlock
	       || getMonitorExitSlot(pred) >= 0) {
	    result = pred;
	    pred = getPredecessor(pred);
	} 
	return result;
    }
					
				    
    private void checkAndRemoveJSR(FlowBlock tryFlow, 
				   StructuredBlock catchBlock,
				   FlowBlock subRoutine,
				   int startOutExit, int endOutExit) {
        Iterator iter = tryFlow.getSuccessors().iterator();
    dest_loop:
        while (iter.hasNext()) {
	    FlowBlock dest = (FlowBlock) iter.next();
            if (dest == subRoutine)
                continue dest_loop;

	    boolean isFirstJump = true;
            for (Jump jumps = tryFlow.getJumps(dest);
		 jumps != null; jumps = jumps.next, isFirstJump = false) {

                StructuredBlock prev = jumps.prev;
                if (prev instanceof EmptyBlock
                    && prev.outer instanceof JsrBlock) {
		    /* This jump is really a jsr, since it doesn't
		     * leave the block forever, we can ignore it.
		     */
		    continue;
                }

		StructuredBlock pred = skipFinExitChain(prev);
		if (pred instanceof JsrBlock) {
		    JsrBlock jsr = (JsrBlock) pred;
		    StructuredBlock jsrInner = jsr.innerBlock;
		    if (jsrInner instanceof EmptyBlock
			&& jsrInner.jump != null
			&& jsrInner.jump.destination == subRoutine) {
			/* The jump is preceeded by the right jsr.  Mark the
			 * jsr as good.
			 */
			jsr.setGood(true);
			continue;
		    }
		}

		if (pred == null && isFirstJump) {
		    /* Now we have a jump that is not preceded by any
		     * jsr.  There's a last chance: the jump jumps
		     * directly to a correct jsr instruction, which
		     * lies outside the try/catch block.  
		     */
		    if (jumps.destination.predecessors.size() == 1
			&& jumps.destination.getBlockNr() >= startOutExit
			&& jumps.destination.getNextBlockNr() <= endOutExit) {
			jumps.destination.analyze(startOutExit, endOutExit);
		    
			StructuredBlock sb = jumps.destination.block;
			if (sb instanceof SequentialBlock)
			    sb = sb.getSubBlocks()[0];
			if (sb instanceof JsrBlock
			    && sb.getSubBlocks()[0] instanceof EmptyBlock
			    && (sb.getSubBlocks()[0].jump.destination
				== subRoutine)) {
			    StructuredBlock jsrInner = sb.getSubBlocks()[0];
			    jumps.destination.removeSuccessor(jsrInner.jump);
			    jsrInner.removeJump();
			    sb.removeBlock();
			    continue dest_loop;
			}
		    }
		}
		
                /* Now we have a jump with a wrong destination.
                 * Complain!
                 */
                DescriptionBlock msg 
                    = new DescriptionBlock("ERROR: NO JSR TO FINALLY");
		if (pred != null)
		    pred.prependBlock(msg);
		else {
		    prev.appendBlock(msg);
		    msg.moveJump(prev.jump);
		}
            }
        }
	if (tryFlow.getSuccessors().contains(subRoutine))
	    removeJSR(tryFlow, catchBlock, subRoutine);
    }

    private void checkAndRemoveMonitorExit(FlowBlock tryFlow, 
					   StructuredBlock catchBlock, 
					   LocalInfo local, 
					   int start, int end) {
        FlowBlock subRoutine = null;
        Iterator succs = tryFlow.getSuccessors().iterator();
    dest_loop:
        while (succs.hasNext()) {
	    boolean isFirstJump = true;
	    FlowBlock successor = (FlowBlock) succs.next();
            for (Jump jumps = tryFlow.getJumps(successor);
                 jumps != null; jumps = jumps.next, isFirstJump = false) {

                StructuredBlock prev = jumps.prev;
                if (prev instanceof EmptyBlock
                    && prev.outer instanceof JsrBlock) {
                    /* This jump is really a jsr, since it doesn't
		     * leave the block forever, we can ignore it.
		     */
		    continue;
                }
		StructuredBlock pred = skipFinExitChain(prev);
		if (pred instanceof JsrBlock) {
		    JsrBlock jsr = (JsrBlock) pred;
		    StructuredBlock jsrInner = jsr.innerBlock;
		    if (jsrInner instanceof EmptyBlock
			&& jsrInner.jump != null) {
			FlowBlock dest = jsrInner.jump.destination;

			if (subRoutine == null
			    && dest.getBlockNr() >= start
			    && dest.getNextBlockNr() <= end) {
			    dest.analyze(start, end);
			    if (isMonitorExitSubRoutine(dest, local))
				subRoutine = dest;
			}

			if (dest == subRoutine) {
			    /* The jump is preceeded by the right jsr.
			     * Mark it as good.
			     */
			    jsr.setGood(true);
			    continue;
			}
		    }
		} else if (getMonitorExitSlot(pred) == local.getSlot()) {
		    /* The jump is preceeded by the right monitor
		     * exit instruction.
		     */
		    pred.removeBlock();
		    if (prev instanceof ReturnBlock)
			removeReturnLocal((ReturnBlock) prev);
		    continue;
		}

		if (pred == null && isFirstJump) {
		    /* Now we have a jump that is not preceded by a
		     * monitorexit.  There's a last chance: the jump
		     * jumps directly to the correct monitorexit
		     * instruction, which lies outside the try/catch
		     * block.  
		     */
		    if (successor.predecessors.size() == 1
			&& successor.getBlockNr() >= start
			&& successor.getNextBlockNr() <=  end) {
			successor.analyze(start, end);
		    
			StructuredBlock sb = successor.block;
			if (sb instanceof SequentialBlock)
			    sb = sb.getSubBlocks()[0];
			if (sb instanceof JsrBlock
			    && sb.getSubBlocks()[0] instanceof EmptyBlock) {
			    StructuredBlock jsrInner = sb.getSubBlocks()[0];
			    FlowBlock dest = jsrInner.jump.destination;
			    if (subRoutine == null
				&& dest.getBlockNr() >= start
				&& dest.getNextBlockNr() <= end) {
				dest.analyze(start, end);
				if (isMonitorExitSubRoutine(dest, local))
				    subRoutine = dest;
			    }

			    if (subRoutine == dest) {
				successor.removeSuccessor(jsrInner.jump);
				jsrInner.removeJump();
				sb.removeBlock();
				continue dest_loop;
			    }
			}
			if (getMonitorExitSlot(sb) == local.getSlot()) {
			    sb.removeBlock();
			    continue dest_loop;
			}
		    }
		}
		
		/* Complain!
                 */
                DescriptionBlock msg 
                    = new DescriptionBlock("ERROR: NO MONITOREXIT");
                prev.appendBlock(msg);
                msg.moveJump(jumps);
            }  
        }

	if (subRoutine != null) {
	    if (tryFlow.getSuccessors().contains(subRoutine))
		removeJSR(tryFlow, catchBlock, subRoutine);
	    tryFlow.mergeBlockNr(subRoutine);
	}
    }

    private StoreInstruction getExceptionStore(StructuredBlock catchBlock) {
        if (!(catchBlock instanceof SequentialBlock)
            || !(catchBlock.getSubBlocks()[0] instanceof InstructionBlock))
            return null;
        
        Expression instr = 
            ((InstructionBlock)catchBlock.getSubBlocks()[0]).getInstruction();
	if (!(instr instanceof StoreInstruction))
	    return null;

	StoreInstruction store = (StoreInstruction) instr;
	if (!(store.getLValue() instanceof LocalStoreOperator
	      && store.getSubExpressions()[1] instanceof NopOperator))
	    return null;
    
	return store;
    }

    private boolean analyzeSynchronized(FlowBlock tryFlow, 
                                        StructuredBlock catchBlock,
                                        int endHandler) {
	StoreInstruction excStore = getExceptionStore(catchBlock);
	if (excStore != null)
	    catchBlock = catchBlock.getSubBlocks()[1];

        if (!(catchBlock instanceof SequentialBlock
              && catchBlock.getSubBlocks()[0] 
              instanceof InstructionBlock))
            return false;
            
        Expression instr = 
            ((InstructionBlock)catchBlock.getSubBlocks()[0]).getInstruction();
        
        if (!(instr instanceof MonitorExitOperator
	      && instr.getFreeOperandCount() == 0
	      && (((MonitorExitOperator)instr).getSubExpressions()[0] 
		  instanceof LocalLoadOperator)
	      && catchBlock.getSubBlocks()[1] instanceof ThrowBlock))
	    return false;

        Expression throwInstr = 
	    ((ThrowBlock)catchBlock.getSubBlocks()[1]).getInstruction();
	
	if (excStore != null) {
	    if (!(throwInstr instanceof Operator
		  && excStore.lvalueMatches((Operator)throwInstr)))
		return false;
	} else {
	    if (!(throwInstr instanceof NopOperator))
		return false;
	}

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
	 *  catchBlock:                                               |
	 *      local_n = stack                                      |
	 *      monitorexit local_x                                  |
	 *      throw local_n                                        |
	 *   [OR ALTERNATIVELY:]                                     |
	 *      monitorexit local_x                                  |
	 *      throw stack                                          |
	 *  optional subroutine: <-----------------------------------'
	 *    astore_n
	 *    monitorexit local_x
	 *    return_n
	 */
	
	MonitorExitOperator monexit = (MonitorExitOperator)
	    ((InstructionBlock) catchBlock.getSubBlocks()[0]).instr;
	LocalInfo local = 
	    ((LocalLoadOperator)monexit.getSubExpressions()[0])
	    .getLocalInfo();
	
	if ((GlobalOptions.debuggingFlags
	     & GlobalOptions.DEBUG_ANALYZE) != 0)
	    GlobalOptions.err.println
		("analyzeSynchronized(" + tryFlow.getBlockNr()
		 + "," + tryFlow.getNextBlockNr() + "," + endHandler + ")");
	
	checkAndRemoveMonitorExit
	    (tryFlow, catchBlock, local, tryFlow.getNextBlockNr(), endHandler);
	
	SynchronizedBlock syncBlock = new SynchronizedBlock(local);
	TryBlock tryBlock = (TryBlock) tryFlow.block;
	syncBlock.replace(tryBlock);
	syncBlock.moveJump(tryBlock.jump);
	syncBlock.setBodyBlock(tryBlock.subBlocks.length == 1
			       ? tryBlock.subBlocks[0] : tryBlock);
	tryFlow.lastModified = syncBlock;
	return true;
    }

    private boolean analyzeFinally(FlowBlock tryFlow, 
				   StructuredBlock catchBlock, int end) {

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
         *   catchBlock:                      |
         *       local_n = stack              v
         *       jsr finally ---------------->|
         *       throw local_n;               |
         *   finally: <-----------------------'
         *      astore_n
         *      ...
         *      return_n
         */
        
	StoreInstruction excStore = getExceptionStore(catchBlock);
	if (excStore == null)
	    return false;

	catchBlock = catchBlock.getSubBlocks()[1];
        if (!(catchBlock instanceof SequentialBlock))
            return false;
        
        StructuredBlock finallyBlock = null;

        if (catchBlock.getSubBlocks()[0] instanceof LoopBlock) {
            /* In case the try block has no exit (that means, it throws
             * an exception), the finallyBlock was already merged with
             * the catchBlock.  We have to check for this case separately:
             *
             * do {
             *    JSR
             *       break;
             *    throw local_x
             * } while(false);
             * finallyBlock; (starts with POP / local_y = POP)
             */
            LoopBlock doWhileFalse = (LoopBlock)catchBlock.getSubBlocks()[0];
            if (doWhileFalse.type == LoopBlock.DOWHILE
                && doWhileFalse.cond == LoopBlock.FALSE
                && doWhileFalse.bodyBlock instanceof SequentialBlock) {
		if (transformSubRoutine(catchBlock.getSubBlocks()[1])) {
		    finallyBlock = catchBlock.getSubBlocks()[1];
		    catchBlock = (SequentialBlock) doWhileFalse.bodyBlock;
		}
            }
        }

        if (!(catchBlock instanceof SequentialBlock
	      && catchBlock.getSubBlocks()[0] instanceof JsrBlock
	      && catchBlock.getSubBlocks()[1] instanceof ThrowBlock))

	    return false;
	
	JsrBlock jsrBlock = (JsrBlock)catchBlock.getSubBlocks()[0];
	ThrowBlock throwBlock = (ThrowBlock) catchBlock.getSubBlocks()[1];


	if (!(throwBlock.getInstruction() instanceof Operator
	      && excStore.lvalueMatches((Operator)
					throwBlock.getInstruction())))
	    return false;

	FlowBlock subRoutine;
	if (finallyBlock != null) {
	    /* Check if the jsr breaks (see two comments above). We don't 
	     * need to check if it breaks to the right block, because
	     * we know that there is only one Block around the jsr.
	     */
	    if (!(jsrBlock.innerBlock instanceof BreakBlock))
		return false;
	    
	    /* Check if the try block has no exit
	     * XXX - Unfortunately the try block already has the
	     * successors of catch block.
	     */
// 	    if (tryFlow.getSuccessors().size() > 0)
// 		return false;

	    catchBlock = finallyBlock;
	    subRoutine = null;
	} else {
	    if (!(jsrBlock.innerBlock instanceof EmptyBlock))
		return false;
	    catchBlock = jsrBlock;
	    subRoutine = jsrBlock.innerBlock.jump.destination;
	    checkAndRemoveJSR(tryFlow, catchBlock, subRoutine,
			      tryFlow.getNextBlockNr(), 
			      subRoutine.getBlockNr());
	}

	/* Wow that was complicated :-)
	 * But now we know that the catch block looks
	 * exactly like it should:
	 *
	 *   local_n = POP
	 * catchBlock:
	 *   JSR 
	 *       finally
	 *   throw local_n
	 */

	TryBlock tryBlock = (TryBlock) tryFlow.block;
	if (tryBlock.getSubBlocks()[0] instanceof TryBlock) {
	    /* remove the surrounding tryBlock */
	    TryBlock innerTry = (TryBlock)tryBlock.getSubBlocks()[0];
	    innerTry.gen = tryBlock.gen;
	    innerTry.replace(tryBlock);
	    tryBlock = innerTry;
	    tryFlow.lastModified = tryBlock;
	    tryFlow.block = tryBlock;
	}
	FinallyBlock newBlock = new FinallyBlock();
	newBlock.setCatchBlock(catchBlock);
	tryBlock.addCatchBlock(newBlock);

	if (subRoutine != null) {
	    while (subRoutine.analyze(tryFlow.getNextBlockNr(), end));

	    /* Now check if the subroutine is correct and has only the
	     * catchFlow as predecessor.
	     */
	    if (subRoutine.predecessors.size() == 1
		&& transformSubRoutine(subRoutine.block)) {

		tryFlow.removeSuccessor(jsrBlock.innerBlock.jump);
		tryFlow.mergeBlockNr(subRoutine);
		tryFlow.mergeSuccessors(subRoutine);
		subRoutine.block.replace(catchBlock);
		tryFlow.updateInOutCatch(subRoutine);
	    }
	}
	return true;
    }

    private boolean analyzeSpecialFinally(FlowBlock tryFlow, 
					  StructuredBlock catchBlock, 
					  int end) {
        StructuredBlock firstInstr = 
            catchBlock instanceof SequentialBlock 
            ? catchBlock.getSubBlocks()[0]: catchBlock;

        if (!(firstInstr instanceof SpecialBlock 
	      && ((SpecialBlock)firstInstr).type == SpecialBlock.POP
	      && ((SpecialBlock)firstInstr).count == 1))
	    return false;

	/* This may be a special try/finally-block, where
	 * the finally block ends with a break, return or
	 * similar.
	 */
	FlowBlock succ = null;

	/* remove the pop now */
	if (catchBlock instanceof SequentialBlock)
	    catchBlock = catchBlock.getSubBlocks()[1];
	else {
	    catchBlock = new EmptyBlock();
	    catchBlock.moveJump(firstInstr.jump);

	    succ = firstInstr.jump.destination;
	}

// 	Set trySuccs = tryFlow.getSuccessors();
// 	if (trySuccs.size() > 1
// 	    || (trySuccs.size() == 1
// 		&& trySuccs.iterator().next() != succ))
// 	    return false;

	if (succ != null) {
	    /* Handle the jumps in the tryFlow.
	     */
	    Jump jumps = tryFlow.removeJumps(succ);
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
	newBlock.setCatchBlock(catchBlock);
	return true;
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
		int start = exc.start.getBlockNr();
		int end = exc.end.getBlockNr();
		int handler = exc.handler.getBlockNr();
		if (start > end || handler <= end)
		    throw new InternalError
			("ExceptionHandler order failed: not "
			 + start + " < " + end + " <= " + handler);
		if (last != null
		    && (last.start.getBlockNr() != start
			|| last.end.getBlockNr() != end)) {
		    /* The last handler does catch another range. 
		     * Due to the order:
		     *  start < last.start.getBlockNr()
		     *  || end > last.end.getBlockNr()
		     */
		    if (end >= last.start.getBlockNr() 
			&& end < last.end.getBlockNr())
			throw new InternalError
			    ("Exception handlers ranges are intersecting: ["
			     + last.start.getBlockNr()+", "
			     + last.end.getBlockNr()+"] and ["
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
		int startNr = exc.start.getBlockNr();
		int endNr   = exc.end.getBlockNr();
		next = i.hasNext() ? (Handler) i.next() : null;
		int endHandler = Integer.MAX_VALUE;
		/* If the next exception handler catches a bigger range
		 * it must surround the handler completely.
		 */
		if (next != null
		    && next.end.getBlockNr() > endNr)
		    endHandler = next.end.getBlockNr() + 1;

		FlowBlock tryFlow = exc.start;
		tryFlow.checkConsistent();

		if (last == null || exc.type == null
		    || last.start.getBlockNr() != startNr
		    || last.end.getBlockNr() != endNr) {
		    /* The last handler does catch another range. 
		     * Create a new try block.
		     */
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_ANALYZE) != 0)
			GlobalOptions.err.println
			    ("analyzeTry(" + startNr + ", " + endNr+")");
		    while(true) {
			while (tryFlow.analyze(startNr, 
					       endNr+1));
			int nextNr = tryFlow.getNextBlockNr();
			if (nextNr > endNr)
			    break;
			tryFlow = flowBlocks[nextNr];
		    }
		    if (tryFlow.getBlockNr() != startNr)
			GlobalOptions.err.println
			    ("Warning: Can't completely analyze try.");
		    TryBlock tryBlock = new TryBlock(tryFlow);
		} else if (!(tryFlow.block instanceof TryBlock))
			throw new InternalError("no TryBlock");

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
		    FlowBlock newFlow = new FlowBlock
			(catchFlow.method, catchFlow.getBlockNr(),
			 catchFlow.prevByCodeOrder);
		    newFlow.setSuccessors(new FlowBlock[] { catchFlow });
		    newFlow.nextByCodeOrder = catchFlow;
		    catchFlow.prevByCodeOrder = newFlow;
		    catchFlow = newFlow;
		} else {
		    if ((GlobalOptions.debuggingFlags
			 & GlobalOptions.DEBUG_ANALYZE) != 0)
			GlobalOptions.err.println
			    ("analyzeCatch("
			     + catchFlow.getBlockNr() + ", " + endHandler + ")");
		    while (catchFlow.analyze(catchFlow.getBlockNr(), 
					     endHandler));
		}
		    
		/* Merge the try-block with the catch-block now */
		tryFlow.updateInOutCatch(catchFlow);
		tryFlow.mergeSuccessors(catchFlow);
		tryFlow.mergeBlockNr(catchFlow);
		if (exc.type != null)
		    analyzeCatchBlock(exc.type, tryFlow, catchFlow.block);
		
		else if (!analyzeSynchronized(tryFlow, catchFlow.block, 
					      endHandler)
			 && ! analyzeFinally(tryFlow, catchFlow.block, 
					     endHandler)
			 && ! analyzeSpecialFinally(tryFlow, catchFlow.block, 
						    endHandler))
		    
		    analyzeCatchBlock(Type.tObject, tryFlow, catchFlow.block);
		
		tryFlow.checkConsistent();
		if ((GlobalOptions.debuggingFlags
		     & GlobalOptions.DEBUG_ANALYZE) != 0)
		    GlobalOptions.err.println
			("analyzeTryCatch(" + tryFlow.getBlockNr() + ", "
			 + tryFlow.getNextBlockNr() + ") done.");
	    }
	}
    }
}
