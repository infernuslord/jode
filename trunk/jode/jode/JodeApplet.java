package jode;
import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class JodeApplet extends Applet {
    JodeWindow jodeWin;

    public JodeApplet() {
	jodeWin = new JodeWindow(this);
    }


///#ifdef AWT10
///    public boolean action(Event e, Object arg) {
///	jodeWin.action(e);
///	return true;
///    }
///#endif
    	
    public void init() {
	String cp = getParameter("classpath");
	if (cp != null)
	    jodeWin.setClasspath(cp);
	String cls = getParameter("class");
	if (cls != null)
	    jodeWin.setClass(cls);
    }
}

