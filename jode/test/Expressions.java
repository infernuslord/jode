package jode.test;
public class Expressions {
    double  cd;
    float   cf;
    long    cl;
    int     ci;
    char    cc;
    short   cs;
    byte    cb;
    boolean cz;

    void postIncDecExpressions() {
	cd++;
	cf++;
	cl++;
	ci++;
	cs++;
	cb++;
	cc++;
	cd--;
	cf--;
	cl--;
	ci--;
	cs--;
	cb--;
	cc--;
	float f = 0.0F;
	double d = 0.0;
	long l = 0L;
	f++;
	f--;
	d++;
	d--;
	l++;
	l--;
    }
    

    void unary() {
	short s = 25;
	s = (short) ~s;
	boolean b = !true;
	s = b? s: cs;
	char c= 25;
	c = b ? c: cc;
    }

    void shift() {
	int i = 0;
	long l =0;
	l >>= i;
	l >>= i;
	i >>= i;
	l = l << l;
	l = l << i;
	l = i << l;
	l = i << i;
	i = (int) (l << l);
	i = (int) (l << i);
	i = i << l;
	i = i << i;
	cl >>= ci;
	ci <<= ci;
	cl = cl << cl;
	cl = cl << ci;
	cl = ci << cl;
	cl = ci << ci;
	ci = (int) (cl << cl);
	ci = (int) (cl << ci);
	ci = ci << cl;
	ci = ci << ci;
    }
}
