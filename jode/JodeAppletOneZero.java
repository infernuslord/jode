package jode;
import java.applet.*;
import java.awt.*;
import java.io.*;

public class JodeAppletOneZero extends Applet implements Runnable {

    TextField classpathField;
    TextField classField;
    TextArea  sourcecodeArea;
    TextArea  errorArea;

    Thread decompileThread;
    
    public JodeAppletOneZero() {
	buildComponents(this);
    }
    	
    public void init() {
	String cp = getParameter("classpath");
	if (cp != null)
	    setClasspath(cp);
	String cls = getParameter("class");
	if (cls != null)
	    setClass(cls);
    }

    private void buildComponents(Container frame) {
	classpathField = new TextField(50);
	classField = new TextField(50);
	sourcecodeArea = new TextArea(20, 80);
	errorArea = new TextArea(3, 80);

	sourcecodeArea.setEditable(false);
	errorArea.setEditable(false);

	GridBagLayout gbl = new GridBagLayout();
	frame.setLayout(gbl);
	GridBagConstraints labelConstr = new GridBagConstraints();
	GridBagConstraints textConstr = new GridBagConstraints();
	GridBagConstraints areaConstr = new GridBagConstraints();
	labelConstr.fill = GridBagConstraints.NONE;
	textConstr.fill = GridBagConstraints.HORIZONTAL;
	areaConstr.fill = GridBagConstraints.BOTH;
	textConstr.gridwidth = GridBagConstraints.REMAINDER;
	textConstr.weightx = 1.0;
	areaConstr.gridwidth = GridBagConstraints.REMAINDER;
	areaConstr.weightx = 1.0;
	areaConstr.weighty = 1.0;

	Label label = new Label("class path: ");
	gbl.setConstraints(label, labelConstr);
	frame.add(label);
	gbl.setConstraints(classpathField, textConstr);
	frame.add(classpathField);
	label = new Label("class name: ");
	gbl.setConstraints(label, labelConstr);
	frame.add(label);
	gbl.setConstraints(classField, textConstr);
	frame.add(classField);
	gbl.setConstraints(sourcecodeArea, areaConstr);
	frame.add(sourcecodeArea);
	areaConstr.gridheight = GridBagConstraints.REMAINDER;
	areaConstr.weighty = 0.0;
	gbl.setConstraints(errorArea, areaConstr);
	frame.add(errorArea);

	Decompiler.err = new PrintStream(new AreaOutputStream(errorArea));
    }

    public void setClasspath(String cp) {
	classpathField.setText(cp);
    }
    public void setClass(String cls) {
	classField.setText(cls);
    }
	
    public boolean action(Event e, Object arg) {
	if (e.target == classField) {
	    if (decompileThread == null) {
		decompileThread = new Thread(this);
		sourcecodeArea.setText("Please wait, while decompiling...\n");
		decompileThread.start();
	    } else
		sourcecodeArea
		    .appendText("Be a little bit more patient, please.\n");
	}
	return true;
    }
	
    public class AreaOutputStream extends OutputStream { 
	private TextArea area;

	public AreaOutputStream(TextArea a) {
	    area = a;
	}

	public void write(int b) throws IOException {
	    area.appendText(String.valueOf((byte)b));
	}

	public void write(byte[] b, int off, int len) throws IOException {
	    area.appendText(new String(b, off, len));
	}
    }

    public void run() {
	errorArea.setText("");
	String cp = classpathField.getText();
	cp = cp.replace(':', jode.bytecode.SearchPath.protocolSeparator);
	cp = cp.replace(',', File.pathSeparatorChar);
	JodeEnvironment env = new JodeEnvironment(cp);
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	try {
	    TabbedPrintWriter writer = new TabbedPrintWriter(out, "    ");
	    env.doClass(classField.getText(), writer);
	    sourcecodeArea.setText(out.toString());
	} catch (Throwable t) {
	    sourcecodeArea.setText("Didn't succeed.\n"
				   +"Check the below area for more info.");
	    t.printStackTrace(Decompiler.err);
	} finally {
	    synchronized(this) {
		decompileThread = null;
	    }
	}
    }
}
