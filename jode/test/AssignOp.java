package jode.test;

public class AssignOp {
    static short  static_short;
    static int    static_int;
    static double static_double;
    static String static_String;
    static long   static_long;

    short  obj_short;
    int    obj_int;
    long   obj_long;
    double obj_double;
    String obj_String;

    short[]  arr_short;
    int []   arr_int;
    long[]   arr_long;
    double[] arr_double;
    String[] arr_String;

    void assop() {
	short local_short = 0;
        int local_int = 0;
        double local_double = 1.0;
        String local_String = null;

        local_short -= 25 * local_int;
	static_short /= 5;
	obj_short += 100 - local_int;
	arr_short[local_int] >>= 25;

        local_int |= 25 | local_int;
        static_int <<= 3;
        obj_int *= 17 + obj_int;
        arr_int[local_int] /= (obj_int+=7);

        local_double /= 3.0;
        static_double *= obj_int;
        obj_double -= 25;
        arr_double[local_int] /= (local_double+=7.0);

        local_String += "Hallo";
        static_String += "Hallo";
        obj_String += "Hallo";
        arr_String[0] += local_double + static_String + "Hallo" + obj_int;
    }

    void prepost() {
        int local_int= -1;
        long local_long= 4;
        
        local_long = local_int++;
        obj_long = ++obj_int;
        arr_long[static_int] = static_long = (arr_long[--static_int] = (static_int--))+1;
    }

    void iinc() {
        int local_int = 0;
        local_int += 5;
        obj_int = (local_int -= 5);

        static_int = local_int++;
        obj_int = --local_int;
    }
}
