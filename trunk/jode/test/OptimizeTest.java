public class OptimizeTest {

    public final int getInlined(String str, int i) {
	return str.charAt(i);
    }

    public final int getInlined(String str, int i, OptimizeTest t) {
	return str.charAt(i) + t.getInlined(str, i) + i;
    }


    public int complexInline(String str, int i) {
	System.err.println("");
	return str.charAt(i)+str.charAt(-i);
    }

    public int notInlined(String str, int i, OptimizeTest t) {
	return str.charAt(i) + t.getInlined(str, i);
    }

    public void main(String[] param) {
	OptimizeTest ot = new OptimizeTest();
	
	System.err.println(ot.getInlined("Hallo", ot.notInlined(param[1], 10 - ot.getInlined(param[0], 0, new OptimizeTest()), ot)));
	System.err.println(ot.complexInline("ollah", param.length));
    }
}








