package jode.test;

public class InnerCompat {
    int x;

    private class privateNeedThis {
	void a() { x = 5; }
    }
    protected class protectedNeedThis {
	void a() { x = 5; }
    }
    class packageNeedThis {
	void a() { x = 5; }
    }
    public class publicNeedThis {
	void a() { x = 5; }
    }
    private class privateNeedNotThis {
	int x;
	void a() { x = 5; }
    }
    protected class protectedNeedNotThis {
	int x;
	void a() { x = 5; }
    }
    class packageNeedNotThis {
	int x;
	void a() { x = 5; }
    }
    public class publicNeedNotThis {
	int x;
	void a() { x = 5; }
    }

}
