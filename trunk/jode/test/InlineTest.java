// You may put this in different packages, to check package wide
// inlining (see jode.test.OptimizeTest)
package jode.test;

/**
 * Check if inlines are allowed over package borders.
 */
public class InlineTest {

    public static final int 
	difficultSideInline(jode.test.OptimizeTest ot, int a) {
	return ot.g++ + a;
    }
}
