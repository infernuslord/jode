/* LogicalDeadCodeOptimizer Copyright (C) 1999 Jochen Hoenicke.
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

package jode.obfuscator;

/**
 * This class analyzes the method to detect and remove logical dead
 * code.  If a field, local or stack entry, is tested to have a
 * certain value, that is remembered and if a similar test occurs
 * again, we may know, that it always evaluates to true
 * resp. false.<br>
 *
 * Each field/local/stack entry has a Constraint, that tells e.g. if
 * that value is always a constant, always not zero, always zero, or
 * always equal to another stack entry etc.  These constraints can be
 * propagated, and if the condition of a opc_if is always the same,
 * that opc_if can be removed resp. replaces by a opc_goto. <br>
 *
 * @author Jochen Hoenicke
 */
public class LogicalDeadCodeOptimizer {
    class Constraint {
	/* The constant are aligned so that the operation ^1 negates the
	 * comparison and like the VM opcodes.
	 */
	public int EQ =  0;
	public int NE =  1;
	public int LT =  2;
	public int GE =  3; 
	public int GT =  4;
	public int LE =  5;

	/**
	 * This gives the compareOps for this constraint (one of
	 * EQ/NE/GE/GT/LE/LT).  */
	int compareOp;
	/**
	 * The reference value to which is compared (this is the
	 * second argument of the compare).
	 */
	ConstrainedValue reference;

    }

    class ConstrainedValue {
	boolean isConstant;
	Object constant;

	/**
	 * If this is not constant, this are all constraints.
	 */
	Constraint[] constraints;



	public boolean implies(Constraint constraint) {
	    if (isConstant) {
		// check if always
		// value.constant compareOp reference
		return constraint.reference.compareToConstant
		    (value.constant, compareOp) == 1;
	    } else {
		//
	    }
	}

	/**
	 * Compares this object with other.
	 * @param compareOp one of Constraint.EQ/NE/LT/LE/GT/GE.
	 * @param other a constant, null, a String or a Number.
	 * @return 1, if (other compareOp this) always true, <br>
	 *         0, if (other compareOp this) always false, <br>
	 *        -1, otherwise
	 */
	public int compareToConstant(Object other, int compareOp) {
	    if (isConstant) {
		switch (compareOp) {
		case EQ:
		    return other.equals(constant) ? 1 : 0;
		case NE:
		    return other.equals(constant) ? 0 : 1;
		case LE:
		case LT:
		case GE:
		case GT: {
		    /* This must be a number */
		    int cmp = ((Number)other).compareTo((Number)constant);
		    if (compareOp == LE && cmp <= 0
			|| compareOp == LT && cmp < 0
			|| compareOp == GE && cmp >= 0
			|| compareOp == GT && cmp > 0)
			return 1;
		    return 0;
		}
		}
	    } else {
		/* Not a constant, try the constraints. */

		/* First we find all equal references */
		Vector equalRefs = new Vector();
		equalRefs.add(this);
		for (int i=0; i < equalRefs.size(); i++) {
		    Constraint[] cs = ((ConstrainedValue) 
				       equalRefs.elementAt(i)).constraints;
		    for (int j=0; j < cs.count; j++) {
			if (cs[j].compareOp == EQ
			    && !equalRefs.contains(cs[j].reference))
			    equalRefs.addElement(cs[j].reference);
		    }
		}
		/* If we wanted to only check for EQ or NE we can do this now.
		 */
		if (compareOp == EQ || compareOp == NE) {
		    for (int i=0; i < equalRefs.size(); i++) {
			Constraint[] cs = ((ConstrainedValue) 
					   equalRefs.elementAt(i)).constraints;
			for (int j=0; j < cs.count; j++) {
			    if ((1 << cs[j].compareOp
				 & (1<<NE | 1<<LT | 1<<GT)) != 0
				&& cs[j].reference.isConstant
				&& cs[j].reference.constant.equals(other)) {
				/* Yeah, we are not equal to that constant. */
				return (compareOp == NE) ? 1 : 0;
			    }
			}
		    }
		    /* No helpful constraints found */
		    return -1;
		}

		/* Check if we are greater / greater or equal */
		
		if (cs[j])
		    /* This is a constant, check if constraint
		     * is okay and compare.
		     */
		    if (cmpOp == cs[j].compareOp
			&& cs[j].compareOp == cmpOp
				 || cs[j].compareOp ^ 7 == cmpOp)
				/* We are lucky, this is a constant
				 * (can this happen?)
				 */
			return cs[j].reference.compareToConstant
				    (other, compareOp);
		
		/* Now try to prove that always greater */
		    

		Stack stack = new Stack();
		Stack cmpopStack = new Stack();
		stack.push(this);
		cmpopStack.push(new Integer(compareOp));
		while (!stack.isEmpty()) {
		    ConstrainedValue cv = (ConstrainedValue) stack.pop();
		    Constraint[] cs = cv.constraints;
		    int cmpop = ((Integer) cmpopStack.pop()).intValue();
		    for (int i=0; i < cs.count; i++) {
			if (/* always consider equals. */
			    cs.compareOp == EQ    
			    /* never consider nonequals */
			    || (cs.compareOp >= NE
				/* consider same compares */
				&& cs.compareOp == cmpop
				/* and consider same compares except equal */
				&& cs.compareOp ^ 7 == cmpop)) {
			    
			    /* if cs.compareOp is greater or lower 
			     * (not equal), we can allow equal in cmpop */
			    if (cs.compareOp != EQ
				&& (cs.compareOp & 1) == 0
				&& (cmpop & 1) == 0)
				cmpop ^= 7;
			    if (cs.reference.isConstant()) {
				if (other.compareToConstant
				    (cmpop, cs.reference.isConstant()) == 1)
				    return 1;
			    } else if (!stack.contains(cs.reference)) {
				stack.push(cs.reference);
				cmpopStack.push(new Integer(cmpop));
			    }
			}
		    }
		}
		return -1;
	    }
	}
    }
    
    /**
     * OPEN QUESTIONS: 
     *   we have a variable whose value is > 0.
     *   is after an increase the value still >0?  
     *    (not necessarily, e.g. value == Integer.MAX_VALUE)
     */

    /* Every local/stack has a list of Constraint.
     * Operations:
     *   -when two control flows flow together, we need to intersect 
     *    Constraints: All constraints are considered, a constraint
     *    is taken, if it is implied by a constraint in the other list.
     *
     *   -load operations copy local Constraint to stack Constraint.
     *   -store operations copy stack Constraint to local Constraint.
     */
}



