package jode;
import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class JodeWindow 
    implements ActionListener, Runnable
{
    TextField classpathField, classField;
    TextArea  sourcecodeArea, errorArea;
    Checkbox  verboseCheck, prettyCheck;
    Button startButton, saveButton;
    String lastClassName;
    Frame frame;

    Thread decompileThread;
    
    public JodeWindow(Container window) {
	buildComponents(window);
    }

    private void buildComponents(Container window) {
	if (window instanceof Frame)
	    frame = (Frame) window;
	classpathField = new TextField(50);
	classField = new TextField(50);
	sourcecodeArea = new TextArea(20, 80);
	errorArea = new TextArea(3, 80);
	verboseCheck = new Checkbox("verbose", false);
	prettyCheck = new Checkbox("pretty", false);
	startButton = new Button("start");
	saveButton = new Button("save");
	saveButton.setEnabled(false);

	sourcecodeArea.setEditable(false);
	errorArea.setEditable(false);

	window.setLayout(new GridBagLayout());
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

	window.add(new Label("class path: "), labelConstr);
	window.add(classpathField, textConstr);
	window.add(new Label("class name: "), labelConstr);
	window.add(classField, textConstr);
	window.add(verboseCheck, checkConstr);
	window.add(prettyCheck, checkConstr);
	labelConstr.weightx = 1.0;
	window.add(new Label(), labelConstr);
	window.add(startButton, buttonConstr);
	buttonConstr.gridwidth = GridBagConstraints.REMAINDER;
	window.add(saveButton, buttonConstr);
	window.add(sourcecodeArea, areaConstr);
	areaConstr.gridheight = GridBagConstraints.REMAINDER;
	areaConstr.weighty = 0.0;
	window.add(errorArea, areaConstr);

	startButton.addActionListener(this);
	saveButton.addActionListener(this);
	Decompiler.err = new PrintStream(new AreaOutputStream(errorArea));
    }

    public void setClasspath(String cp) {
	classpathField.setText(cp);
    }
    public void setClass(String cls) {
	classField.setText(cls);
    }
	
    public synchronized void actionPerformed(ActionEvent e) {
	if (e.getSource() == startButton) {
	    startButton.setEnabled(false);
	    decompileThread = new Thread(this);
	    sourcecodeArea.setText("Please wait, while decompiling...\n");
	    decompileThread.start();
	} else if (e.getSource() == saveButton) {
	    if (frame == null)
		frame = new Frame(); //XXX
	    FileDialog fd = new FileDialog(frame, 
					   "Save decompiled code", 
					   FileDialog.SAVE);
	    fd.setFile(lastClassName.substring
		       (lastClassName.lastIndexOf('.')+1).concat(".java"));
	    fd.show();
	    String fileName = fd.getFile();
	    if (fileName == null)
		return;
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
    }

    public class AreaOutputStream extends OutputStream { 
	private TextArea area;

	public AreaOutputStream(TextArea a) {
	    area = a;
	}

	public void write(int b) throws IOException {
	    area.append(String.valueOf((byte)b));
	}

	public void write(byte[] b, int off, int len) throws IOException {
	    area.append(new String(b, off, len));
	}
    }

    public void run() {
	Decompiler.isVerbose = verboseCheck.getState();
	Decompiler.prettyLocals = prettyCheck.getState();
	errorArea.setText("");
	saveButton.setEnabled(false);

	lastClassName = classField.getText();
	String cp = classpathField.getText();
	cp = cp.replace(':', jode.bytecode.SearchPath.protocolSeparator);
	cp = cp.replace(',', File.pathSeparatorChar);
	JodeEnvironment env = new JodeEnvironment(cp);
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	try {
	    TabbedPrintWriter writer = new TabbedPrintWriter(out);
	    env.doClass(lastClassName, writer);
	    sourcecodeArea.setText(out.toString());
	    saveButton.setEnabled(true);
	} catch (Throwable t) {
	    sourcecodeArea.setText("Didn't succeed.\n"
				   +"Check the below area for more info.");
	    t.printStackTrace();
	} finally {
	    synchronized(this) {
		decompileThread = null;
		startButton.setEnabled(true);
	    }
	}
    }

    public static void main(String argv[]) {
	Frame frame = new Frame(Decompiler.copyright);
	JodeWindow win = new JodeWindow(frame);

	String cp = System.getProperty("java.class.path");
	if (cp != null)
	    win.setClasspath(cp.replace(File.pathSeparatorChar, ','));
	String cls = win.getClass().getName();
	win.setClass(cls);

	frame.addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent e) {
		System.exit(0);
	    }
	});
	frame.pack();
	frame.show();
    }
}
