package jode;
import java.applet.*;
import java.awt.*;
import java.io.*;

public class JodeAppletOneZero extends Applet implements Runnable {

    TextField classpathField, classField;
    TextArea  sourcecodeArea, errorArea;
    Checkbox  verboseCheck, prettyCheck;
    Button startButton, saveButton;
    String lastClassName;

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

    private void buildComponents(Container window) {
	classpathField = new TextField(50);
	classField = new TextField(50);
	sourcecodeArea = new TextArea(20, 80);
	errorArea = new TextArea(3, 80);
	verboseCheck = new Checkbox("verbose");
	prettyCheck = new Checkbox("pretty");
	startButton = new Button("start");
	saveButton = new Button("save");
	saveButton.disable();

	sourcecodeArea.setEditable(false);
	errorArea.setEditable(false);

	GridBagLayout gbl = new GridBagLayout();
	window.setLayout(gbl);
	GridBagConstraints labelConstr = new GridBagConstraints();
	GridBagConstraints textConstr = new GridBagConstraints();
	GridBagConstraints areaConstr = new GridBagConstraints();
	GridBagConstraints checkConstr = new GridBagConstraints();
	GridBagConstraints buttonConstr = new GridBagConstraints();
	labelConstr.fill = GridBagConstraints.NONE;
	textConstr.fill = GridBagConstraints.HORIZONTAL;
	areaConstr.fill = GridBagConstraints.BOTH;
	checkConstr.fill = GridBagConstraints.NONE;
	buttonConstr.fill = GridBagConstraints.NONE;
	labelConstr.anchor = GridBagConstraints.EAST;
	textConstr.anchor = GridBagConstraints.CENTER;
	checkConstr.anchor = GridBagConstraints.WEST;
	buttonConstr.anchor = GridBagConstraints.CENTER;
	labelConstr.anchor = GridBagConstraints.EAST;
	textConstr.gridwidth = GridBagConstraints.REMAINDER;
	textConstr.weightx = 1.0;
	areaConstr.gridwidth = GridBagConstraints.REMAINDER;
	areaConstr.weightx = 1.0;
	areaConstr.weighty = 1.0;

	Label label = new Label("class path: ");
	gbl.setConstraints(label, labelConstr);
	window.add(label);
	gbl.setConstraints(classpathField, textConstr);
	window.add(classpathField);
	label = new Label("class name: ");
	gbl.setConstraints(label, labelConstr);
	window.add(label);
	gbl.setConstraints(classField, textConstr);
	window.add(classField);
	gbl.setConstraints(verboseCheck, checkConstr);
	window.add(verboseCheck);
	gbl.setConstraints(prettyCheck, checkConstr);
	window.add(prettyCheck);
	labelConstr.weightx = 1.0;
	label = new Label();
	gbl.setConstraints(label, labelConstr);
	window.add(label);
	gbl.setConstraints(startButton, buttonConstr);
	window.add(startButton);
	buttonConstr.gridwidth = GridBagConstraints.REMAINDER;
	gbl.setConstraints(saveButton, buttonConstr);
	window.add(saveButton);

	gbl.setConstraints(sourcecodeArea, areaConstr);
	window.add(sourcecodeArea);
	areaConstr.gridheight = GridBagConstraints.REMAINDER;
	areaConstr.weighty = 0.0;
	gbl.setConstraints(errorArea, areaConstr);
	window.add(errorArea);

	Decompiler.err = new PrintStream(new AreaOutputStream(errorArea));
    }

    public void setClasspath(String cp) {
	classpathField.setText(cp);
    }
    public void setClass(String cls) {
	classField.setText(cls);
    }
	
    public boolean action(Event e, Object arg) {
	if (e.target == startButton) {
	    startButton.disable();
	    Thread decompileThread = new Thread(this);
	    sourcecodeArea.setText("Please wait, while decompiling...\n");
	    decompileThread.start();
	} else if (e.target == saveButton) {
	    FileDialog fd = new FileDialog(new Frame(), 
					   "Save decompiled code", 
					   FileDialog.SAVE);
	    fd.setFile(lastClassName.substring
		       (lastClassName.lastIndexOf('.')+1).concat(".java"));
	    fd.show();
	    String fileName = fd.getFile();
	    if (fileName == null)
		return true;
	    try {
		File f = new File(new File(fd.getDirectory()), fileName);
		FileWriter out = new FileWriter(f);
		out.write(sourcecodeArea.getText());
		out.close();
	    } catch (IOException ex) {
		errorArea.setText("");
		Decompiler.err.println("Couldn't write to file " 
				       + fileName + ": ");
		ex.printStackTrace(Decompiler.err);
	    }
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
	Decompiler.isVerbose = verboseCheck.getState();
	Decompiler.prettyLocals = prettyCheck.getState();
	errorArea.setText("");
	saveButton.disable();
	lastClassName = classField.getText();

	String cp = classpathField.getText();
	cp = cp.replace(':', jode.bytecode.SearchPath.protocolSeparator);
	cp = cp.replace(',', File.pathSeparatorChar);
	JodeEnvironment env = new JodeEnvironment(cp);
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	try {
	    TabbedPrintWriter writer = new TabbedPrintWriter(out, "    ");
	    env.doClass(classField.getText(), writer);
	    sourcecodeArea.setText(out.toString());
	    saveButton.enable();
	} catch (Throwable t) {
	    sourcecodeArea.setText("Didn't succeed.\n"
				   +"Check the below area for more info.");
	    t.printStackTrace(Decompiler.err);
	} finally {
	    startButton.enable();
	}
    }
}
