package jode;

public interface LocalVarOperator {
    public boolean isRead();
    public boolean isWrite();
    public int getSlot();
    public LocalInfo getLocalInfo();
    public void setLocalInfo(LocalInfo li);
}
         
         
