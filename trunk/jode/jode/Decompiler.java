package jode;
import sun.tools.java.*;
import java.lang.reflect.Modifier;

public class Decompiler {
    public static boolean isVerbose = false;
    public static boolean isDebugging = false;

    public static void main(String[] params) {
        JodeEnvironment env = new JodeEnvironment();
        for (int i=0; i<params.length; i++) {
            if (params[i].equals("-v"))
                isVerbose = true;
            else if (params[i].equals("-debug"))
                isDebugging = true;
            else
                env.doClass(params[i]);
        }
    }
}
