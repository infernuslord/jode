package jode.test;

public class TriadicExpr {
    int a,b,c,d,e,f,g,h,i,j;
    boolean bool;

    void test() {
        if( (a< b ? bool : (a<b)))
            a=b;
        else
            b=a;

        if ((a<b?b<a:i<j) ? (c<d ? e<f : g < h) : (j<i ? h<g:f<e))
            a=(b<a ? g : h);
        else
            b=a;
    }
}


