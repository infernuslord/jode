/* LoopBlock Copyright (C) 1998-1999 Jochen Hoenicke.
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
import jode.decompiler.TabbedPrintWriter;
import jode.type.Type;
import jode.decompiler.LocalInfo;
import jode.expr.Expression;
import jode.expr.ConstOperator;
import jode.expr.LocalStoreOperator;
import jode.expr.CombineableOperator;

/**
 * This is the structured block for an Loop block.
 */
public class LoopBlock extends StructuredBlock implements BreakableBlock {

    public static final int WHILE = 0;
    public static final int DOWHILE = 1;
    public static final int FOR = 2;
    public static final int POSSFOR = 3;

    public static final Expression TRUE = 
        new ConstOperator(Boolean.TRUE);
    public static final Expression FALSE = 
        new ConstOperator(Boolean.FALSE);

    /**
     * The condition.  Must be of boolean type.
     */
    Expression cond;
    /**
     * The stack the condition eats.
     */
    VariableStack condStack;
    /**
     * The init instruction, only valid if type == FOR or POSSFOR
     */
    InstructionBlock init;
    /**
     * The increase instruction, only valid if type == FOR or POSSFOR.
     */
    InstructionBlock incr;
    
    /**
     * True, if the initializer is a declaration.
     */
    boolean isDeclaration;

    /**
     * The type of the loop.  This must be one of DOWHILE, WHILE or FOR.
     */
    int type;

    /**
     * The body of this loop.  This is always a valid block and not null.
     */
    StructuredBlock bodyBlock;

    /**
     * The stack after the break.
     */
    VariableStack breakedStack;

    /**
     * The stack at begin of the loop.
     */
    VariableStack continueStack;

    /*{ invariant { type != POSSFOR ||
                    (incr != null
		     && incr.getInstruction().getOperator() 
		     instanceof CombineableOperator) 
		    :: "(possible) for with invalid init/incr";
		    init == null || 
		    (init.getInstruction().getOperator() 
		     instanceof CombinableOperator)
		    :: "Initializer is not combinableOperator";
		    type == POSSFOR || type == FOR ||
		    (init == null && incr == null)
		    :: "(while/do while) with init or incr";
		    cond != null && cond.getType() == Type.tBoolean
		    :: "invalid condition type";
		    type != POSSFOR || bodyBlock.contains(incr)
		    :: "incr is not in body of possible for" } }*/

    /**
     * Returns the block where the control will normally flow to, when
     * the given sub block is finished (<em>not</em> ignoring the jump
     * after this block). (This is overwritten by SequentialBlock and
     * SwitchBlock).  If this isn't called with a direct sub block,
     * the behaviour is undefined, so take care.  
     * @return null, if the control flows to another FlowBlock.  */
    public StructuredBlock getNextBlock(StructuredBlock subBlock) {
        return this;
    }

    public FlowBlock getNextFlowBlock(StructuredBlock subBlock) {
        return null;
    }
    
    public LoopBlock(int type, Expression cond) {
        this.type = type;
        this.cond = cond;
        this.mayChangeJump = (cond == TRUE);
    }

    public void setBody(StructuredBlock body) {
        bodyBlock = body;
        bodyBlock.outer = this;
        body.setFlowBlock(flowBlock);
    }

    public void setInit(InstructionBlock init) {
        this.init = init;
        if (type == FOR)
            init.removeBlock();
    }

    public boolean conditionMatches(InstructionBlock instr) {
        return (type == POSSFOR ||
                cond.containsMatchingLoad(instr.getInstruction()));
    }


    public Expression getCondition() {
        return cond;
    }

    public void setCondition(Expression cond) {
        this.cond = cond;
        if (type == POSSFOR) {
            /* We can now say, if this is a for block or not.
             */
            if (cond.containsMatchingLoad(incr.getInstruction())) {
                type = FOR;
                incr.removeBlock();
                if (init != null) {
                    if (cond.containsMatchingLoad(init.getInstruction()))
                        init.removeBlock();
                    else
                        init = null;
                }
            } else {
                /* This is not a for block, as it seems first.  Make
                 * it a while block again, and forget about init and
                 * incr.  */
                type = WHILE;
                init = incr = null;
            }
        }
        mayChangeJump = false;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public VariableSet propagateUsage() {
        if (type == FOR && init != null)
            used.unionExact(init.used);
        if (type == FOR && incr != null)
            used.unionExact(incr.used);
        VariableSet allUse = (VariableSet) used.clone();
        allUse.unionExact(bodyBlock.propagateUsage());
        return allUse;
    }

    /**
     * Replaces the given sub block with a new block.
     * @param oldBlock the old sub block.
     * @param newBlock the new sub block.
     * @return false, if oldBlock wasn't a direct sub block.
     */
    public boolean replaceSubBlock(StructuredBlock oldBlock, 
                                   StructuredBlock newBlock) {
        if (bodyBlock == oldBlock)
            bodyBlock = newBlock;
        else
            return false;
        return true;
    }

    /**
     * Returns all sub block of this structured block.
     */
    public StructuredBlock[] getSubBlocks() {
        return new StructuredBlock[] { bodyBlock };
    }

    public void dumpDeclaration(TabbedPrintWriter writer, LocalInfo local)
	throws java.io.IOException
    {
        if (type == FOR && init != null
            && (init.getInstruction().getOperator() 
                instanceof LocalStoreOperator)
            && (((LocalStoreOperator) 
                 init.getInstruction().getOperator()).getLocalInfo() 
                == local.getLocalInfo()))
            isDeclaration = true;
        else
            super.dumpDeclaration(writer, local);
    }

    public void dumpSource(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
        isDeclaration = false;
        super.dumpSource(writer);
    }

    public void dumpInstruction(TabbedPrintWriter writer) 
	throws java.io.IOException
    {
        if (label != null) {
            writer.untab();
            writer.println(label+":");
            writer.tab();
        }
        boolean needBrace = bodyBlock.needsBraces();
        switch (type) {
        case POSSFOR:
            /* a possible for is now treated like a WHILE */
        case WHILE:
            if (cond == TRUE)
                /* special syntax for endless loops: */
                writer.print("for (;;)");
            else
                writer.print("while ("+cond.simplify().toString()+")");
            break;
        case DOWHILE:
            writer.print("do");
            break;
        case FOR:
            writer.print("for (");
            if (init != null) {
                if (isDeclaration)
                    writer.print(((LocalStoreOperator) 
                                  init.getInstruction().getOperator())
                                 .getLocalInfo().getType().getHint()
                                 + " ");
                writer.print(init.getInstruction().simplify().toString());
            } else
                writer.print("/**/");
            writer.print("; "+cond.simplify().toString()+"; "
                         +incr.getInstruction().simplify().toString()+")");
            break;
        }
	if (needBrace)
	    writer.openBrace();
	else
	    writer.println();
        writer.tab();
        bodyBlock.dumpSource(writer);
        writer.untab();
        if (type == DOWHILE) {
	    if (needBrace)
		writer.closeBraceContinue();
            writer.println("while ("+cond.simplify().toString()+");");
        } else if (needBrace)
            writer.closeBrace();
    }


    boolean mayChangeJump = true;

    /**
     * The serial number for labels.
     */
    static int serialno = 0;

    /**
     * The label of this instruction, or null if it needs no label.
     */
    String label = null;

    /**
     * Returns the label of this block and creates a new label, if
     * there wasn't a label previously.
     */
    public String getLabel() {
        if (label == null)
            label = "while_"+(serialno++)+"_";
        return label;
    }

    /**
     * Is called by BreakBlock, to tell us that this block is breaked.
     */
    public void setBreaked() {
	mayChangeJump = false;
    }

    /** 
     * This is called after the analysis is completely done.  It
     * will remove all PUSH/stack_i expressions, (if the bytecode
     * is correct).
     * @param stack the stack at begin of the block
     * @return null if there is no way to the end of this block,
     * otherwise the stack after the block has executed.  
     */
    public VariableStack mapStackToLocal(VariableStack stack) {
	if (type == DOWHILE) {
	    VariableStack afterBody = bodyBlock.mapStackToLocal(stack);
	    if (afterBody != null)
		mergeContinueStack(afterBody);

	    if (continueStack != null) {
		VariableStack newStack;
		int params = cond.getOperandCount();
		if (params > 0) {
		    condStack = continueStack.peek(params);
		    newStack = continueStack.pop(params);
		} else
		    newStack = continueStack;

		if (cond != TRUE)
		    mergeBreakedStack(newStack);
		if (cond != FALSE)
		    stack.merge(newStack);
	    }	    
	} else {
	    continueStack = stack;
	    VariableStack newStack;
	    int params = cond.getOperandCount();
	    if (params > 0) {
		condStack = stack.peek(params);
		newStack = stack.pop(params);
	    } else
		newStack = stack;
	    if (cond != TRUE)
		breakedStack = newStack;
	    VariableStack afterBody = bodyBlock.mapStackToLocal(newStack);
	    if (afterBody != null)
		mergeContinueStack(afterBody);
	}
	return breakedStack;
    }

    /**
     * Is called by BreakBlock, to tell us what the stack can be after a
     * break.
     * @return false if the stack is inconsistent.
     */
    public void mergeContinueStack(VariableStack stack) {
	if (continueStack == null)
	    continueStack = stack;
	else
	    continueStack.merge(stack);
    }

    /**
     * Is called by BreakBlock, to tell us what the stack can be after a
     * break.
     * @return false if the stack is inconsistent.
     */
    public void mergeBreakedStack(VariableStack stack) {
	if (breakedStack != null)
	    breakedStack.merge(stack);
	else
	    breakedStack = stack;
    }

    public void removePush() {
	if (condStack != null)
	    cond = condStack.mergeIntoExpression(cond, used);
	bodyBlock.removePush();
    }

    /** 
     * This method should remove local variables that are only written
     * and read one time directly after another.  <br>
     *
     * This is especially important for stack locals, that are created
     * when there are unusual swap or dup instructions, but also makes
     * inlined functions more pretty (but not that close to the
     * bytecode).  
     */
    public void removeOnetimeLocals() {
	cond = cond.removeOnetimeLocals();
	if (type == FOR) {
	    if (init != null)
		init.removeOnetimeLocals();
	    incr.removeOnetimeLocals();
	}
	super.removeOnetimeLocals();
    }

    /**
     * Replace all breaks to block with a continue to this.
     * @param block the breakable block where the breaks originally 
     * breaked to (Have a break now, if you didn't understand that :-).
     */
    public void replaceBreakContinue(BreakableBlock block) {
        java.util.Stack todo = new java.util.Stack();
        todo.push(block);
        while (!todo.isEmpty()) {
            StructuredBlock[] subs = 
                ((StructuredBlock)todo.pop()).getSubBlocks();
            for (int i=0; i<subs.length; i++) {
                if (subs[i] instanceof BreakBlock) {
                    BreakBlock breakblk = (BreakBlock) subs[i];
                    if (breakblk.breaksBlock == block) {
                        new ContinueBlock(this, breakblk.label != null)
                            .replace(breakblk);
                    }
                }
                todo.push(subs[i]);
            }
        }
    }

    /**
     * Determines if there is a sub block, that flows through to the end
     * of this block.  If this returns true, you know that jump is null.
     * @return true, if the jump may be safely changed.
     */
    public boolean jumpMayBeChanged() {
        return mayChangeJump;
    }

    public boolean doTransformations() {
        return init == null && (type == FOR || type == POSSFOR)
            && CreateForInitializer.transform(this, flowBlock.lastModified);
    }
}
