package jode.test;

public class For {

    boolean nested() {
    outer:
        for (int i=0; i< 100; i++) {
            for (int j=0; j< 100; j++) {
                if (i < j)
                    continue outer;
            }
            return false;
        }
        return true;
    }

}
