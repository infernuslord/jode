package jode.flow;
import jode.decompiler.LocalInfo;
import jode.expr.ComplexExpression;
import jode.expr.Expression;
import jode.expr.LocalLoadOperator;
import jode.expr.Operator;

/**
 * This class represents the state of the stack at various points in
 * the program.  Each entry is a anonymous local, which is used instead
 * of the PUSH / stack_i statements. <p>
 *
 * This class is immutable, but note, that the local infos can get merged.
 * @see FlowBlock.mapStackToLocal
 * @see FlowBlock.removePush
 */
public class VariableStack {
    public final static VariableStack EMPTY = 
	new VariableStack(new LocalInfo[0]);

    final LocalInfo[] stackMap; 

    private VariableStack(LocalInfo[] stack) {
	stackMap = stack;
    }

    public boolean isEmpty() {
	return stackMap.length == 0;
    }

    public VariableStack pop(int count) {
	LocalInfo[] newStack = new LocalInfo[stackMap.length - count];
	System.arraycopy(stackMap, 0, newStack, 0, stackMap.length - count);
	return new VariableStack(newStack);
    }
    
    public VariableStack push(LocalInfo li) {
	return poppush(0, li);
    }
    
    public VariableStack poppush(int count, LocalInfo li) {
	LocalInfo[] newStack = new LocalInfo[stackMap.length - count + 1];
	System.arraycopy(stackMap, 0, newStack, 0, stackMap.length - count);
	newStack[stackMap.length - count] = li;
	return new VariableStack(newStack);
    }
    
    public VariableStack peek(int count) {
	LocalInfo[] peeked = new LocalInfo[count];
	System.arraycopy(stackMap, stackMap.length - count, peeked, 0, count);
	return new VariableStack(peeked);
    }

    public void merge(VariableStack other) {
	if (stackMap.length != other.stackMap.length)
	    throw new IllegalArgumentException("stack length differs");
	for (int i=0; i<stackMap.length; i++) {
	    if (stackMap[i].getType().stackSize()
		!= other.stackMap[i].getType().stackSize())
		throw new IllegalArgumentException
		    ("stack element length differs at "+i);
	    stackMap[i].combineWith(other.stackMap[i]);
	}
    }

    /**
     * Merge to VariableStacks.  Either one may be null, in which case
     * the other is returned.
     */
    public static VariableStack merge(VariableStack first, 
				      VariableStack second) {
	if (first == null)
	    return second;
	else if (second == null)
	    return first;
	first.merge(second);
	return first;
    }

    public Expression mergeIntoExpression(Expression expr, VariableSet used) {
	/* assert expr.getOperandCount() == stackMap.length */

	ComplexExpression parent = null;
	Expression inner = expr;
	while (inner instanceof ComplexExpression) {
	    parent = (ComplexExpression)inner;
	    inner = parent.getSubExpressions()[0];
	}
	Expression[] loads = new Expression[stackMap.length];
	for (int i=0; i< stackMap.length; i++) {
	    used.addElement(stackMap[i]);
	    loads[i] = new LocalLoadOperator(stackMap[i].getType(),
					     stackMap[i]);
	}
	Expression newExpr = new ComplexExpression((Operator)inner, loads);
	if (parent != null)
	    parent.setSubExpressions(0, newExpr);
	else
	    expr = newExpr;
	return expr;
    }

    public VariableStack executeSpecial(SpecialBlock special) {
	if (special.type == special.POP) {
	    int popped = 0;
	    int newLength = stackMap.length;
	    while (popped < special.count) {
		newLength--;
		popped += stackMap[newLength].getType().stackSize();
	    }
	    if (popped != special.count)
		throw new IllegalArgumentException("wrong POP");
	    LocalInfo[] newStack = new LocalInfo[newLength];
	    System.arraycopy(stackMap, 0, newStack, 0, newLength);
	    return new VariableStack(newStack);
	} else if (special.type == special.DUP) {
	    int popped = 0;
	    int numDup = 0;
	    int startDup = stackMap.length;
	    while (popped < special.count) {
		startDup--;
		numDup++;
		popped += stackMap[startDup].getType().stackSize();
	    }
	    if (popped != special.count)
		throw new IllegalArgumentException("wrong DUP");
	    int destDup = startDup;
	    int depth = 0;
	    while (depth < special.depth) {
		destDup--;
		depth += stackMap[destDup].getType().stackSize();
	    }
	    if (depth != special.depth)
		throw new IllegalArgumentException("wrong DUP");
	    LocalInfo[] newStack = new LocalInfo[stackMap.length + numDup];
	    System.arraycopy(stackMap, 0, newStack, 0, destDup);
	    System.arraycopy(stackMap, startDup, newStack, destDup, numDup);
	    System.arraycopy(stackMap, destDup, newStack, destDup + numDup,
			     startDup - destDup);
	    System.arraycopy(stackMap, startDup, newStack, startDup + numDup,
			     numDup);
	    return new VariableStack(newStack);
	} else if (special.type == special.SWAP) {
	    LocalInfo[] newStack = new LocalInfo[stackMap.length];
	    System.arraycopy(stackMap, 0, newStack, 0, stackMap.length - 2);
	    if (stackMap[stackMap.length-2].getType().stackSize() != 1
		|| stackMap[stackMap.length-1].getType().stackSize() != 1)
		throw new IllegalArgumentException("wrong SWAP");
	    newStack[stackMap.length-2] = stackMap[stackMap.length-1];
	    newStack[stackMap.length-1] = stackMap[stackMap.length-2];
	    return new VariableStack(newStack);
	} else
	    throw new jode.AssertError("Unknown SpecialBlock");
    }
}
