package jode.test;

public class NestedAnon {
    public NestedAnon(int maxdepth) {
	class NestMyself {
	    int depth;
	    NestMyself son;

	    public NestMyself(int d) {
		depth = d;
		if (d > 0)
		    son = new NestMyself(d-1);
	    }
	}
	new NestMyself(maxdepth);
    }

}
