package jode;
import sun.tools.java.*;
import java.lang.reflect.Modifier;

public class Decompiler {
    public static void main(String[] params) {
        JodeEnvironment env = new JodeEnvironment();
        for (int i=0; i<params.length; i++) {
            env.doClass(params[i]);
        }
    }
}
