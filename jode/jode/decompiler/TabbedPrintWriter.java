package jode;
import java.io.*;

public class TabbedPrintWriter {
    boolean atbol;
    String tabstr;
    StringBuffer indent;
    PrintWriter pw;
    int verbosity=100;

    public TabbedPrintWriter (OutputStream os, String tabstr) {
	pw = new PrintWriter(os);
	this.tabstr=tabstr;
	indent = new StringBuffer();
	atbol = true;
    }

    public void tab() {
	indent.append(tabstr);
    }

    public void untab() {
	indent.setLength(indent.length()-tabstr.length());
    }

    public void println(String str) throws java.io.IOException {
	if (atbol) {
	    pw.print(indent);
	}
	pw.println(str);
        pw.flush();
	atbol = true;
    }

    public void print(String str) throws java.io.IOException {
	if (atbol) {
	    pw.print(indent);
	}
	pw.print(str);
	atbol = false;
    }
}
