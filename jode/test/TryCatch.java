package jode.test;

/**
 * This tests everything that has to do with a ExceptionHandler, that
 * is try, catch, finally and synchronized.
 */
class TryCatch {
    
    int simple() {
        TryCatch i = null;
        try {
            foo();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            return 0;
        } finally {
            simple();
        }
        synchronized(this) {
            System.gc();
            if (i != null)
                return -1;
        }
        synchronized(i) {
            foo();
        }
        return 1;
    }

    int localsInCatch() {
        int a = 0;
        try {
            a = 1;
            foo();
            a = 2;
            simple();
	    a = (a=4) / (a=0);
            a = 3;
            localsInCatch();
            a = 4;
            return 5;
        } catch (Exception ex) {
            return a;
        }
    }

    int finallyBreaks() {
        try {
            simple();
            throw new Exception();
        } catch (Exception ex) {
            return 3;
        } finally {
            simple();
            return 3;
        }
    }

    int whileInTry() {
        int a=1;
        try {
            while (true) {
                a= 4;
                whileInTry();
            }
        } finally {
            synchronized(this) {
                while (true) {
                    foo();
                    if (a == 5)
                        break;
                    finallyBreaks();
                }
            } 
            return a;
        }
    }

    void foo() {
        TryCatch local = null;
        while (true) {
            foo();
            synchronized (this) {
                System.gc();
                try {
                    System.err.println();
                } catch (RuntimeException ex) {
                    ex.printStackTrace();
                    for (int i=0; i<4; i++)
                        foo();
                    break;
                } finally {
                    System.out.println();
                    for (int i=0; i<4; i++)
                        foo();
                    System.out.println();
                }
            }
        }
        synchronized (local) {
            local.foo();
        }
        if (true) {
            synchronized (this) {
                try {
                    System.err.println();
                } finally {
                    Thread.dumpStack();
                    return;
                }
            }
        }
        System.out.println();
    }

    void oneEntryFinally() {
        try {
            throw new RuntimeException("ex");
        } finally {
            System.err.println("hallo");
        }
    }

    void finallyMayBreak() {
        while(true) {
            try {
                System.err.println();
            } finally {
                if (simple() < 2)
                    break;
                else if (simple() < 3)
                    foo();
		else
		    return;
            }
            System.out.println();
        }
        System.err.println();
    }
}
