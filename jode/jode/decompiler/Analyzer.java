package jode;

public interface Analyzer {
    public void analyze();
    public void dumpSource(TabbedPrintWriter writer) throws java.io.IOException;
}
