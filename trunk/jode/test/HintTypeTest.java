package jode.test;

/**
 * The primitive types can give some headaches.  You almost never can say
 * if a local variable is of type int, char, short etc. <p>
 *
 * Most times this doesn't matter this much, but with int and character's
 * this can get ugly. <p>
 *
 * The solution is to give every variable a hint, which type it probably is.
 * The hint reset, when the type is not possible.  For integer types we try
 * to set it to the smallest explicitly assigned type. <p>
 *
 * Some operators will propagate this hint.<p>
 */
public class HintTypeTest {

    public void charLocal() {
	String s= "Hallo";
	for (int i=0; i< s.length(); i++) {
	    char c = s.charAt(i);
	    if (c == 'H')
		// The widening to int doesn't occur in byte code, but
		// is necessary.  This is really difficult.
		System.err.println("H is "+(int)c);
	    else
		System.err.println(""+c+" is "+(int)c);
	}
    }
}


