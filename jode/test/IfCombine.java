package jode.test;

public class IfCombine {
    boolean a,b,c;
    int i,j,k;

    public void foo() {
        if ( a && (b || c) && (i<k)) {
            if (a != b)
                i = 1;
            else
                i = 2;
        }
    }
}
