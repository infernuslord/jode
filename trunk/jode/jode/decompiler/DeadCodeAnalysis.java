package jode.decompiler;
import jode.bytecode.BytecodeInfo;
import jode.bytecode.Instruction;
import jode.bytecode.Handler;

public class DeadCodeAnalysis {

    private final static Object reachable = new Integer(1);
    private final static Object reachChanged = new Integer(2);

    private static void propagateReachability(Instruction firstInstr, 
					      Instruction reachInstr) {
	if (reachInstr.tmpInfo != null)
	    return;
	reachInstr.tmpInfo = reachChanged;
	boolean changed;
	do {
	    changed = false;
	    for (Instruction instr = firstInstr; 
		 instr != null; instr = instr.nextByAddr) {
		if (instr.tmpInfo == reachChanged) {
		    changed = true;
		    instr.tmpInfo = reachable;
		    if (instr.succs != null)
			for (int i=0; i< instr.succs.length; i++)
			    if (instr.succs[i].tmpInfo == null)
				instr.succs[i].tmpInfo = reachChanged;
		    if (!instr.alwaysJumps && instr.nextByAddr != null)
			if (instr.nextByAddr.tmpInfo == null)
			    instr.nextByAddr.tmpInfo = reachChanged;
		}
	    }
	} while (changed);
    }

    public static void removeDeadCode(BytecodeInfo code) {
	for (Instruction instr = code.getFirstInstr(); 
	     instr != null; instr = instr.nextByAddr)
	    instr.tmpInfo = null;
	
	propagateReachability(code.getFirstInstr(), code.getFirstInstr());
	Handler[] handlers = code.getExceptionHandlers();
	boolean changed;
	do {
	    changed = false;
	    for (int i=0; i < handlers.length; i++) {
		if (handlers[i].catcher.tmpInfo == null) {
		    /* check if the try block is somewhere reachable 
		     * and mark the catcher as reachable then.
		     */
		    for (Instruction instr = handlers[i].start; 
			 instr != null; instr = instr.nextByAddr) {
			if (instr.tmpInfo != null) {
			    propagateReachability(code.getFirstInstr(), 
						  handlers[i].catcher);
			    changed = true;
			    break;
			}
			if (instr == handlers[i].end)
			    break;
		    }
		}
	    }
	} while (changed);
	/* Now remove the dead code */
	for (Instruction instr = code.getFirstInstr(); 
	     instr != null; instr = instr.nextByAddr) {
	    if (instr.tmpInfo == null) {
		/* first block is always reachable, so prevByAddr != null */
		instr.prevByAddr.nextByAddr = instr.nextByAddr;
		instr.nextByAddr.prevByAddr = instr.prevByAddr;
		/* adjust length, since someon may rely on this */
		instr.prevByAddr.length += instr.length;
	    }
	}
	for (int i=0; i< handlers.length; i++) {
	    /* A handler is not reachable iff the catcher is not reachable */
	    if (handlers[i].catcher.tmpInfo == null) {
		/* This is very seldom, so we can make it slow */
		Handler[] newHandlers = new Handler[handlers.length - 1];
		System.arraycopy(handlers, 0, newHandlers, 0, i);
		System.arraycopy(handlers, i+1, newHandlers, i, 
				 handlers.length - (i+1));
		handlers = newHandlers;
		code.setExceptionHandlers(newHandlers);
		i--;
	    } else {
		/* This works! */
		while (handlers[i].start.tmpInfo == null) 
		    handlers[i].start = handlers[i].start.nextByAddr;
		while (handlers[i].end.tmpInfo == null) 
		    handlers[i].end = handlers[i].end.prevByAddr;
	    }
	}
    }
}

