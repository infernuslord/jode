package jode.test;

public class OptimizeTest {

    public final int getInlined(String str, int i) {
	return str.charAt(i);
    }

    public final int getInlined(String str, int i, OptimizeTest t) {
	return str.charAt(i) + t.getInlined(str, i) + i;
    }


    public final int complexInline(String str, int i) {
	System.err.println("");
	return str.charAt(i)+str.charAt(-i);
    }

    public int notInlined(String str, int i, OptimizeTest t) {
	return str.charAt(i) + t.getInlined(str, i);
    }
    
    public final void longInline(String str, int i) {
	str.replace('a','b');
	System.err.println(str.concat(String.valueOf(str.charAt(i))));
    }

    public int g;
   
    /**
     * This is a really brutal test.  It shows that side effects can
     * make the handling of inlined methods really, really difficult.
     */ 
    public final int sideInline(int a) {
	return g++ + a;
    }

    public void main(String[] param) {
	OptimizeTest ot = new OptimizeTest();
	
	System.err.println(ot.getInlined("Hallo", ot.notInlined(param[1], 10 - ot.getInlined(param[0], 0, new OptimizeTest()), ot)));
	System.err.println(ot.complexInline("ollah", param.length));
	System.err.println("result: "+(g++ + sideInline(g) + g++) + "g: "+g);
	longInline("Hallo", 3);
	System.err.println("result:"+ 
			   (g++ + jode.test.InlineTest
			    .difficultSideInline(this, g) 
			    + g++) + "g: "+g);
	// This was a check which methods are inlined. The result:
	// Only methods in the same package or in sub packages.
// 	System.err.println("result:"+ 
// 			   (g++ + jode.test.inline.InlineTest
// 			    .difficultSideInline(this, g) 
// 			    + g++) + "g: "+g);
// 	System.err.println("result:"+ 
// 			   (g++ + jode.InlineTest
// 			    .difficultSideInline(this, g) 
// 			    + g++) + "g: "+g);
    }
}
