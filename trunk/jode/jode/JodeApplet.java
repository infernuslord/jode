package jode;
import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class JodeApplet extends Applet implements ActionListener, Runnable {
    TextField classpathField;
    TextField classField;
    TextArea  sourcecodeField;

    Thread decompileThread;
    
    public JodeApplet() {
	setLayout(new BorderLayout());
	Panel optionPanel = new Panel();
	optionPanel.setLayout(new GridLayout(2,2));
	optionPanel.add(new Label("class path: "));
	classpathField = new TextField();
	optionPanel.add(classpathField);
	optionPanel.add(new Label("class name: "));
	classField = new TextField();
	optionPanel.add(classField);
	add(optionPanel, BorderLayout.NORTH);

	sourcecodeField = new TextArea();
	add(sourcecodeField, BorderLayout.CENTER);

	classField.addActionListener(this);
    }
	
    public synchronized void actionPerformed(ActionEvent e) {
	if (decompileThread == null) {
	    decompileThread = new Thread(this);
	    sourcecodeField.setText("Please wait, while decompiling...\n");
	    decompileThread.start();
	} else
	    sourcecodeField.append("Be a little bit more patient, please.\n");
    }

    public void init() {
	String cp = getParameter("classpath");
	if (cp != null)
	    classpathField.setText(cp);
	String cls = getParameter("class");
	if (cls != null)
	    classField.setText(cls);
    }

    public void run() {
	JodeEnvironment env = new JodeEnvironment(classpathField.getText());
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	try {
	    TabbedPrintWriter writer = new TabbedPrintWriter(out, "    ");
	    env.doClass(classField.getText(), writer);
	    sourcecodeField.setText(out.toString());
	} catch (Throwable t) {
	    sourcecodeField.setText("Didn't succeed.\n"
				    +"Check the java console for more info.");
	    t.printStackTrace();
	} finally {
	    synchronized(this) {
		decompileThread = null;
	    }
	}
    }
}
