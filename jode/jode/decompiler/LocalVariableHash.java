package jode;
import sun.tools.java.Type;
import sun.tools.java.Identifier;

public class LocalVariableHash implements LocalVariable {
    Hashtable locals;

    private find(int addr) {
        LocalInfo li = (LocalInfo) locals.get(new Integer(addr));
        if (li == null) {
            li = new LocalInfo();
            locals.put(new Integer(addr), li);
        }
        return li;
    }

    public Identifier getName(int addr) {
        LocalInfo li = find(addr);
        return li.name;
    }

    public Type getType(int addr) {
        LocalInfo li = find(addr);
        return li.type;
    }

    public Type setType(int addr, Type type) {
        LocalInfo li = find(addr);
        li.type = UnknownType.commonType(li.type, type);
        return li.type;
    }
        
    public void combine(int addr1, int addr2) {
        LocalInfo li1 = find(addr1);
        LocalInfo li2 = find(addr2);
        li1.type = UnknownType.commonType(li1.type, li2.type);
        Enumeration keys = locals.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (locals.get(key) == li2)
                locals.put(key, li1);
        }
    }
}
