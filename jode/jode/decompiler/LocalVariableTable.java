package jode;
import sun.tools.java.*;
import java.io.*;

public class LocalVariableTable {
    LocalVariableRangeList[] locals;
    boolean readfromclass;

    public LocalVariableTable(int size) {
        locals = new LocalVariableRangeList[size];
        readfromclass = false;
    }

    public int getSize() {
	return locals.length;
    }

    public boolean isReadFromClass() {
        return readfromclass;
    }

    public void read(JodeEnvironment env, DataInputStream stream)
         throws IOException
    {
        int count = stream.readUnsignedShort();
        for (int i=0; i<count; i++) {
            int start  = stream.readUnsignedShort()-2; /*XXX*/
            int length = stream.readUnsignedShort();
            int name_i = stream.readUnsignedShort();
            int desc_i = stream.readUnsignedShort();
            int slot   = stream.readUnsignedShort();
            LocalVariableRangeList lv = locals[slot];
            if (lv == null) {
                lv = new LocalVariableRangeList(slot);
                locals[slot] = lv;
            }
            lv.addLocal(start, length, 
                        Identifier.lookup((String)
                                          env.getConstantPool().
                                          getValue(name_i)),
                        env.getConstantPool().getType(desc_i));
        }
        readfromclass = true;
    }

    public LocalVariableRangeList getLocal(int slot) 
         throws ArrayIndexOutOfBoundsException
    {
        LocalVariableRangeList lv = locals[slot];
        if (lv == null)
            lv = new LocalVariableRangeList(slot);
        return lv;
    }
}
