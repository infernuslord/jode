package jode;
import sun.tools.java.*;

public class LocalInfo {
    static int serialnr = 0;
    Identifier name;
    Type type;

    public LocalInfo() {
        name = Identifier.lookup("__"+serialnr);
        type = Type.tUnknown;
        serialnr++;
    }
}

