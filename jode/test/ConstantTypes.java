package jode.test;

public class ConstantTypes {
    static boolean bool = true;
    static byte    b  = (byte) 0x80;
    static char    c  = '\u0080';
    static short   s  = (short)'\u8234';
    static int     i  = '\uffff';
    
    static void intFunc(int i){}
    static void shortFunc(short s){}
    static void charFunc(char c){}
    static void byteFunc(byte b){}

    static {
	/* All casts are necessaray */
	intFunc(25);
	shortFunc((short) 25);
	charFunc((char) 25);
	byteFunc((byte) 25);
	intFunc('\u0019');
	shortFunc((short) '\u0019');
	charFunc('\u0019');
	byteFunc((byte) '\u0019');
	intFunc(b);
	intFunc(c);
	intFunc(s);
	intFunc(i);
	shortFunc(b);
	shortFunc((short)c);
	shortFunc(s);
	shortFunc((short)i);
	charFunc((char)b);
	charFunc(c);
	charFunc((char)s);
	charFunc((char)i);
	byteFunc(b);
	byteFunc((byte)c);
	byteFunc((byte)s);
	byteFunc((byte)i);
	b = 42;
	c = 42;
	s = 42;
	i = 42;
	i = c;
	s = b;
	i = s;
	c = (char) s;
	s = (short) c;
	c = (char) b;
	b = (byte) c;
    }
}
