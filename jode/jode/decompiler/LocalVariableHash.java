package jode;
import sun.tools.java.Type;
import sun.tools.java.Identifier;
import java.util.Hashtable;
import java.util.Enumeration;

public class LocalVariableHash implements LocalVariable {
    Hashtable locals;
    int slot;
    
    public LocalVariableHash(int slot) {
	locals = new Hashtable();
	this.slot = slot;
    }

    public LocalInfo getInfo(int addr) {
        LocalInfo li = (LocalInfo) locals.get(new Integer(addr));
        if (li == null) {
	    System.err.println("creating "+slot+": "+addr);
            li = new LocalInfo(slot);
            locals.put(new Integer(addr), li);
        }
        return li;
    }
        
    public void combine(int addr1, int addr2) {
	System.err.println("combining "+slot+": "+addr1+" and "+addr2);
        LocalInfo li1 = getInfo(addr1);
	LocalInfo li2 = (LocalInfo) locals.get(new Integer(addr2));
	if (li2 != null) {
	    li1.type = UnknownType.commonType(li1.type, li2.type);
	    Enumeration keys = locals.keys();
	    while (keys.hasMoreElements()) {
		Object key = keys.nextElement();
		if (locals.get(key) == li2)
		    locals.put(key, li1);
	    }
	} else
	    locals.put(new Integer(addr2), li1);
    }
}
