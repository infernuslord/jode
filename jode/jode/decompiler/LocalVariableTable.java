package jode;
import sun.tools.java.*;
import java.io.*;
import java.util.Vector;

public class LocalVariableTable {
    Vector locals;
    boolean readfromclass;

    public LocalVariableTable(int size) {
        locals = new Vector(); 
        locals.setSize(size);
        readfromclass = false;
    }

    public boolean isReadFromClass() {
        return readfromclass;
    }

    public void read(JodeEnvironment env, DataInputStream stream)
         throws IOException
    {
        int count = stream.readUnsignedShort();
        for (int i=0; i<count; i++) {
            int start  = stream.readUnsignedShort();
            int length = stream.readUnsignedShort();
            int name_i = stream.readUnsignedShort();
            int desc_i = stream.readUnsignedShort();
            int slot   = stream.readUnsignedShort();
            LocalVariableRangeList lv = 
                (LocalVariableRangeList)locals.elementAt(slot);
            if (lv == null) {
                lv = new LocalVariableRangeList(slot);
                locals.setElementAt(lv, slot);
            }
            lv.addLocal(start, length, 
                        Identifier.lookup((String)
                                          env.getConstantPool().
                                          getValue(name_i)),
                        env.getConstantPool().getType(desc_i));
        }
        readfromclass = true;
    }

    public LocalVariable getLocal(int slot) 
         throws ArrayOutOfBoundsException
    {
        LocalVariable lv = (LocalVariable)locals.elementAt(slot);
        if (lv == null) {
            lv = new LocalVariable(slot);
            locals.setElementAt(lv, slot);
        }
        return lv;
    }
}
