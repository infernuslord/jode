package jode;
import sun.tools.java.*;

public class LocalVariableRangeList implements LocalVariable {

    class MyLocalInfo extends LocalInfo {
        int start;
        int length;
        MyLocalInfo next;
        
        MyLocalInfo(int slot, int s, int l, Identifier n, Type t) {
            super (slot);
            start = s;
            length = l;
            setName(n);
            setType(t);
            next = null;
        }
    }

    MyLocalInfo list = null;
    int slot;

    LocalVariableRangeList(int slot) {
        this.slot = slot;
    }

    private void add(MyLocalInfo li) {
        MyLocalInfo before = null;
        MyLocalInfo after = list;
        while (after != null && after.start < li.start) {
            before = after;
            after = after.next;
        }
        if (after != null && li.start + li.length > after.start) 
            throw new AssertError("non disjoint locals");
        li.next = after;
        if (before == null)
            list = li;
        else
            before.next = li;
    }

    private LocalInfo find(int addr) {
        MyLocalInfo li = list;
        while (li != null && addr > li.start+li.length)
            li = li.next;
        if (li == null || li.start > addr) {
            LocalInfo temp = new LocalInfo(slot);
            return temp;
        }
        return li;
    }

    public void addLocal(int start, int length, 
                         Identifier name, Type type) {
        MyLocalInfo li = new MyLocalInfo(slot,start,length,name,type);
        add (li);
    }

    public LocalInfo getInfo(int addr) {
        return find(addr);
    }

    public void combine(int addr1, int addr2) {
        throw new AssertError("combine called on RangeList");
    }
}
