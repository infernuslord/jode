/* OuterValues Copyright (C) 1998-1999 Jochen Hoenicke.
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

package jode.decompiler;
import jode.GlobalOptions;
import jode.expr.Expression;
import jode.expr.ThisOperator;
import jode.expr.LocalLoadOperator;
import jode.expr.OuterLocalOperator;
import jode.util.SimpleMap;
import jode.type.Type;

import java.util.Vector;
import java.util.Enumeration;

/**
 * A list of local variables that a method scoped class inherits
 * from its declaring method.
 *
 * A method scoped class is a class that is declared in a method and
 * it can access other (final) local variables declared earlier.  To
 * realize this the java compiler adds hidden parameters to the
 * constructor of the method scoped class, where it passes the values
 * of the local varaiables.  If a method scoped class has more than
 * one constructor, each gets this hidden parameters.  These hidden
 * parameters are the outerValues, because they are used to transport
 * a value of a local variable from an outer method.
 *
 * Unfortunately there is no definite way to distinguish this outer
 * value parameters from the real parameters, so jode has to do a
 * guess: It first assumes that everything is an outer value parameter
 * added by the compiler and if this leads to contradiction shrinks
 * the count of these parameters.  A contradiction can occur, because
 * the constructor is called two times with different values.
 *
 * On the other hand the TransformConstructor class assumes at some
 * point that some parameters are outer values.  If later a
 * contradiction occurs, jode has to give up and complain loudly.
 *
 * Every class interested in outer values, may register itself as
 * OuterValueListener.  It will then be notified every time the outer
 * values shrink.  Sometimes there are real listener queues: if
 * another method scoped class creates instances of the first in its
 * constructor by passing some of its own outer value parameter, it
 * may first seem that all parameters of the first class's constructor
 * are outer values.  Because we can't be sure that the parameter from
 * the second class's constructor is really an outer value, we have to
 * add a listener.  If later a constructor invokation for the second
 * class is found, where a parameter does not have the right outer
 * value, the listener will also shrink the outer values list of the 
 * first class.
 *
 * A non static _class_ scoped class (i.e. a normal inner class) also
 * has a hidden parameter, namely the instance of its outer class.
 * This hidden parameter is not considered as outer value though.
 * Note that you can even explicitly invoke the constructor with a
 * different outer class instance, by using the
 * <code>outerInstance.new InnerClass()</code> construct.  This
 * exception doesn't apply to method scoped classes, though.
 *
 * Anonymous classes can of course also extend class or method scoped
 * classes.  If they are compiled by jikes the constructor takes as
 * last parameter the outer instance of its super class.  This should
 * really be the first parameter just after the outerValues, as it
 * is under javac.  We mark such classes as jikesAnonymousInner.  This
 * is done in the initialize() pass.
 *
 * @see #shrinkOuterValues
 * @see #addOuterValueListener
 * @since 1.0.93 
 */
public class OuterValues 
{
    private ClassAnalyzer clazzAnalyzer;

    /**
     * The outer values.  An outer value is either a
     * LocalLoadOperator, a ThisOperator, or a OuterLocalOperator.
     */
    private Expression[] head;
    private Vector ovListeners;
    private boolean jikesAnonymousInner;

    /**
     * The maximal number of parameters used for outer values.
     */
    private int headCount;
    /**
     * The minimal number of parameters used for outer values.
     */
    private int headMinCount; 


    public OuterValues(ClassAnalyzer ca, Expression[] head) {
	this.clazzAnalyzer = ca;
	this.head = head;
	this.headMinCount = 0;
	this.headCount = head.length;
	if ((GlobalOptions.debuggingFlags
	     & GlobalOptions.DEBUG_CONSTRS) != 0)
	    GlobalOptions.err.println("Created OuterValues: "+this);
    }

    public Expression getValue(int i) {
	/** require i < getCount() **/
	return head[i];
    }

    public int getCount() {
	return headCount;
    }

    private int getNumberBySlot(int slot) {
	slot--;  // skip this parameter (not an outer value)
	for (int i=0; slot >= 0 && i < headCount; i++) {
	    if (slot == 0)
		return i;
	    slot -= head[i].getType().stackSize();
	}
	return -1;
    }

    /**
     * Get the outer value corresponding to a given slot.  This will
     * also adjust the minSlot value.  This only considers head slots.
     * @return index into outerValues array or -1, if not matched.
     */
    public Expression getValueBySlot(int slot) {
	slot--;  // skip this parameter (not an outer value)
	for (int i=0; i < headCount; i++) {
	    if (slot == 0) {
		Expression expr = head[i];
		if (i >= headMinCount)
		    headMinCount = i;
		return expr;
	    }
	    slot -= head[i].getType().stackSize();
	}
	return null;
    }

    /**
     * If li is a local variable of a constructor, and it could be
     * an outer value, return this outer value and mark ourself as
     * listener.  If that outer value gets invalid later, we shrink
     * ourself to the given nr.
     * @param expr The expression to lift.
     * @param nr The nr of outer values we shrink to, if something 
     *           happens later.
     * @return the outer value if the above conditions are true, 
     * null otherwise.
     */
    private Expression liftOuterValue(LocalInfo li, final int nr) {
	MethodAnalyzer method = li.getMethodAnalyzer();

	if (!method.isConstructor() || method.isStatic())
	    return null;
	OuterValues ov = method.getClassAnalyzer().getOuterValues();
	if (ov == null)
	    return null;

	int ovNr = ov.getNumberBySlot(li.getSlot());
	if ((GlobalOptions.debuggingFlags
	     & GlobalOptions.DEBUG_CONSTRS) != 0)
	    GlobalOptions.err.println("  ovNr "+ovNr+","+ov);
	if (ovNr < 0 && ov.getCount() >= 1 && ov.isJikesAnonymousInner()) {
	    /* Second chance if this is a jikesAnonInner class:
	     * last parameter is this parameter. XXX
	     */
	    Type[] paramTypes = method.getType().getParameterTypes();
	    int lastSlot = 1;
	    for (int i=0; i < paramTypes.length - 1; i++)
		lastSlot += paramTypes[i].stackSize();

	    /* jikesAnonInner corresponds to the first outer value */
	    if (li.getSlot() == lastSlot)
		ovNr = 0;
	}
	if (ovNr < 0)
	    return null;
	if (ov != this || ovNr > nr) {
	    final int limit = ovNr;
	    ov.addOuterValueListener(new OuterValueListener() {
		public void shrinkingOuterValues
		    (OuterValues other, int newCount) {
		    if (newCount <= limit)
			setCount(nr);
		}
	    });
	}
	return ov.head[ovNr];
    }

    public boolean unifyOuterValues(int nr,
				    Expression otherExpr) {
	if ((GlobalOptions.debuggingFlags
	     & GlobalOptions.DEBUG_CONSTRS) != 0)
	    GlobalOptions.err.println("unifyOuterValues: "+this+","
				      +nr+","+otherExpr);
	/** require nr < getCount() **/
	Expression expr1 = otherExpr;
	Expression expr2 = head[nr];
	LocalInfo li1;

	/* Wow, unifying outer values of different constructors in
	 * different methods of different classes can get complicated.
	 * We have not committed the number of OuterValues.  So we
	 * can't say for sure, if the local load matches an outer
	 * local if this is a constructor.  Even worse: The previous
	 * outerValues may be a load of a constructor local, that
	 * should be used as outer value...
	 *
	 * See MethodScopeTest for examples.
	 *
	 * We look if there is a way to merge them and register an
	 * outer value listener to lots of classes.  
	 */

	if (expr1 instanceof ThisOperator) {
	    li1 = null;
	} else if (expr1 instanceof OuterLocalOperator) {
	    li1 = ((OuterLocalOperator) expr1).getLocalInfo();
	} else if (expr1 instanceof LocalLoadOperator) {
	    li1 = ((LocalLoadOperator) expr1).getLocalInfo();
	} else
	    return false;

	/* First lift expr1 until it is a parent of this class */
	while (li1 != null
	       && !li1.getMethodAnalyzer().isMoreOuterThan(clazzAnalyzer)) {
	    expr1 = liftOuterValue(li1, nr);
	    if ((GlobalOptions.debuggingFlags
		 & GlobalOptions.DEBUG_CONSTRS) != 0)
		GlobalOptions.err.println("  lift1 "+li1
					  +" in "+li1.getMethodAnalyzer()
					  +"  to "+expr1);

	    if (expr1 instanceof ThisOperator) {
		li1 = null;
	    } else if (expr1 instanceof OuterLocalOperator) {
		li1 = ((OuterLocalOperator) expr1).getLocalInfo();
	    } else
		return false;
	}
	/* Now lift expr2 until expr1 and expr2 are equal */
	while (!expr1.equals(expr2)) {
	    if (expr2 instanceof OuterLocalOperator) {
		LocalInfo li2 = ((OuterLocalOperator) expr2).getLocalInfo();

		/* if expr1 and expr2 point to same local, we have
		 * succeeded (note that expr1 may be an LocalLoadOperator)
		 */
		if (li2.equals(li1))
		    break;

		expr2 = liftOuterValue(li2, nr);
		if ((GlobalOptions.debuggingFlags
		     & GlobalOptions.DEBUG_CONSTRS) != 0)
		    GlobalOptions.err.println("  lift2 "+li2
					      +" in "+li2.getMethodAnalyzer()
					      +"  to "+expr2);

	    } else
		return false;
	}

	if ((GlobalOptions.debuggingFlags
	     & GlobalOptions.DEBUG_CONSTRS) != 0)
	    GlobalOptions.err.println("unifyOuterValues succeeded.");
	return true;
    }

    /**
     * Jikes gives the outer class reference in an unusual place (as last
     * parameter) for anonymous classes that extends an inner (or method
     * scope) class.  This method tells if this is such a class.
     */
    public boolean isJikesAnonymousInner() {
	return jikesAnonymousInner;
    }

    public void addOuterValueListener(OuterValueListener l) {
	if (ovListeners == null)
	    ovListeners = new Vector();
	ovListeners.addElement(l);
    }

    /**
     * Jikes gives the outer class reference in an unusual place (as last
     * parameter) for anonymous classes that extends an inner (or method
     * scope) class.  This method tells if this is such a class.
     */
    public void setJikesAnonymousInner(boolean value) {
	jikesAnonymousInner = value;
    }

    private static int countSlots(Expression[] exprs, int length) {
	int slots = 0;
	for (int i=0; i < length; i++)
	    slots += exprs[i].getType().stackSize();
	return slots;
    }
	    
    public void setMinCount(int newMin) {
	if (headCount < newMin) {
	    GlobalOptions.err.println
		("WARNING: something got wrong with scoped class "
		 +clazzAnalyzer.getClazz()+": " +newMin+","+headCount);
	    new Throwable().printStackTrace(GlobalOptions.err);
	    headMinCount = headCount;
	} else if (newMin > headMinCount)
	    headMinCount = newMin;
    }

    public void setCount(int newHeadCount) {
	if (newHeadCount >= headCount)
	    return;
	headCount = newHeadCount;

	if ((GlobalOptions.debuggingFlags
	     & GlobalOptions.DEBUG_CONSTRS) != 0) {
	    GlobalOptions.err.println("setCount: "+this+","+newHeadCount);
	    new Throwable().printStackTrace(GlobalOptions.err);
	}
	    
	if (newHeadCount < headMinCount) {
	    GlobalOptions.err.println
		("WARNING: something got wrong with scoped class "
		 +clazzAnalyzer.getClazz()+": "
		 +headMinCount+","+headCount);
	    new Throwable().printStackTrace(GlobalOptions.err);
	    headMinCount = newHeadCount;
	}

	if (ovListeners != null) {
	    for (Enumeration enum = ovListeners.elements();
		 enum.hasMoreElements();)
		((OuterValueListener) enum.nextElement()
		 ).shrinkingOuterValues(this, newHeadCount);
	}
    }

    public String toString() {
	StringBuffer sb = new StringBuffer()
	    .append(clazzAnalyzer.getClazz())
	    .append(".OuterValues[");
	String comma = "";
	int slot = 1;
	for (int i=0; i < headCount; i++) {
	    if (i == headMinCount)
		sb.append("<-");
	    sb.append(comma).append(slot).append(":").append(head[i]);
	    slot += head[i].getType().stackSize();
	    comma = ",";
	}
	if (jikesAnonymousInner)
	    sb.append("!jikesAnonymousInner");
	return sb.append("]").toString();
    }
}


