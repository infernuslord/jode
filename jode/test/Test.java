package jode.test;

class A {
}

interface I1 {
}

interface I2 {
}

interface I12 extends I1, I2 {
}

class B extends A implements I1 {
}

class C extends B implements I2, I12 {
}

class D extends A implements I12 {
}

class E extends A implements I2 {
}

public class Test {
    A a;
    B b;
    C c;
    D d;
    E e;

    I1 i1;
    I2 i2;
    I12 i12;

    boolean g_bo;
    byte g_by;
    short g_sh;
    int g_in;

    int z;
    int[]ain;

    void skip() {
        for (int i=0; i<3; i++) {
            if (z > 5) {
                for (int j=0; j<5; j++) {
                    z++;
                }
            }
            i--;
        }
    }
    public void arithTest() {
        int a=1,b=2;
        boolean x = true,y = false;
        int c=0;
        skip();
        if (x & y) {
            c = 5;
            skip();
            x &= y;
            skip();
            x = x | y;
            skip();
            x ^= y;
            skip();
            x = x && y;
            skip();
            b <<= a;
            b <<= c;
        }
        a&=b;
    }
            
            

    public void switchWhileTest() {
        int local_1__114 = g_in;
        int local_2__115 = 0;
        int local_3__116 = 0;
        int local_4__117 = 0;
        z = 5;
        switch (local_1__114) {
        case 1:
            while (local_4__117 == 0) {
                local_4__117 = 1;
                local_2__115 = g_in;
                local_3__116 = g_in;
                if (local_2__115 > 7)
                    local_2__115 = local_2__115 - 4;
                int local_5__118 = 0;
                while (local_5__118 < 4) {
                    int local_6__119 = 0;
                    while (local_6__119 < 5) {
                        if (ain[local_6__119] == local_2__115 + local_5__118 && ain[local_6__119] == local_3__116)
                            local_4__117 = 0;
                        local_6__119 += 1;
                    }
                    local_5__118 += 1;
                }
            }
            int local_5__120 = 0;
            while (local_5__120 < 5) {
                ain[z] = local_2__115 + local_5__120;
                ain[z] = local_3__116;
                z += 1;
                local_5__120 += 1;
            }
            break;
        case 2:
            while (local_4__117 == 0) {
                local_4__117 = 1;
                local_2__115 = g_in;
                local_3__116 = g_in;
                if (local_3__116 > 7)
                    local_3__116 = local_3__116 - 4;
                int local_5__121 = 0;
                while (local_5__121 < 4) {
                    int local_6__122 = 0;
                    while (local_6__122 < 4) {
                        if (ain[local_6__122] == local_2__115 && ain[local_6__122] == local_3__116 + local_5__121)
                            local_4__117 = 0;
                        local_6__122 += 1;
                    }
                    local_5__121 += 1;
                }
            }
            int local_5__123 = 0;
            while (local_5__123 < 4) {
                ain[z] = local_2__115;
                ain[z] = local_3__116 + local_5__123;
                z += 1;
                local_5__123 += 1;
            }
            break;
        }
    }
    
    public void intTypeTest() {
        boolean b = false;
        boolean abo[] = null;
        byte aby[] = null;
        byte by;
        int in;
        short sh;
        b = g_bo;
        in = g_sh;
        sh = (short)g_in;
        by = (byte)sh;
        sh = by;
        in = by;
        abo[0] = g_bo;
        abo[1] = false;
        abo[2] = true;
        aby[0] = g_by;
        aby[1] = 0;
        aby[2] = 1;
    }

    /**
     * This is an example where our flow analysis doesn't find an
     * elegant solution.  The reason is, that we try to make 
     * while(true)-loops as small as possible (you can't see the real
     * end of the loop, if it is breaked there like here).
     *
     * Look at the assembler code and you know why my Decompiler has
     * problems with this.  But the decompiler does produce compilable
     * code which produces the same assembler code.  
     *
     * A solution would be, to make switches as big as possible (like we
     * already do it with try-catch-blocks).
     */
    void WhileTrueSwitch() {
        int i = 1;
        while (true) {
            switch (i) {
            case 0:
                return;
            case 1:
                i = 5;
                continue;
            case 2:
                i = 6;
                continue;
            case 3:
                throw new RuntimeException();
            default:
                i = 7;
		return;
            }
        }
    }

    native void arrFunc(B[] b);
    
    /**
     * This is an example where it is really hard to know, which type
     * each local has.  
     */
    void DifficultType () {
        B myB = c;
	C myC = c;
        I2 myI2 = c;
        I12 myI12 = c;
	boolean bool = true;
        B[] aB = new C[3];
        arrFunc(new C[3]);

	while (bool) {
            if (bool) {
                i1 = myB;
                i2 = myC;
                i1 = aB[0];
            } else if (bool) {
                i1 = myI12;
                i2 = myI2;
            } else {
                i1 = myC;
                i2 = myI12;
            }
	    myB = b;
            if (bool)
                myI2 = i12;
            else {
                myI12 = d;
                myI2 = e;
            }
	}
    }

    /**
     * This is an example where it is really hard to know, which type
     * each local has.  
     */
    void DifficultArrayType () {
        boolean bool = true;
        B[] aB = new C[3];
        arrFunc(new C[3]);
        C[][][][][] x = new C[4][3][4][][];
        int[][][][][] y = new int[1][2][][][];

	while (bool) {
            if (bool) {
                i1 = aB[0];
                aB[0] = b;
            }
	}
    }
}

