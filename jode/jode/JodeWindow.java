package jode;
import java.applet.*;
import java.awt.*;
///#ifndef AWT10
import java.awt.event.*;
///#endif
import java.io.*;
import jode.decompiler.TabbedPrintWriter;

public class JodeWindow 
    implements Runnable
///#ifndef AWT10
    , ActionListener
///#endif

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
///#ifdef AWT10
///	saveButton.disable();
///#else
	saveButton.setEnabled(false);
///#endif

	sourcecodeArea.setEditable(false);
	errorArea.setEditable(false);
	Font monospaced = new Font("monospaced", Font.PLAIN, 10);
	sourcecodeArea.setFont(monospaced);
	errorArea.setFont(monospaced);

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

///#ifdef AWT10
///	Label label = new Label("class path: ");
///	gbl.setConstraints(label, labelConstr);
///	window.add(label);
///	gbl.setConstraints(classpathField, textConstr);
///	window.add(classpathField);
///	label = new Label("class name: ");
///	gbl.setConstraints(label, labelConstr);
///	window.add(label);
///	gbl.setConstraints(classField, textConstr);
///	window.add(classField);
///	gbl.setConstraints(verboseCheck, checkConstr);
///	window.add(verboseCheck);
///	gbl.setConstraints(prettyCheck, checkConstr);
///	window.add(prettyCheck);
///	labelConstr.weightx = 1.0;
///	label = new Label();
///	gbl.setConstraints(label, labelConstr);
///	window.add(label);
///	gbl.setConstraints(startButton, buttonConstr);
///	window.add(startButton);
///	buttonConstr.gridwidth = GridBagConstraints.REMAINDER;
///	gbl.setConstraints(saveButton, buttonConstr);
///	window.add(saveButton);
///	gbl.setConstraints(sourcecodeArea, areaConstr);
///	window.add(sourcecodeArea);
///	areaConstr.gridheight = GridBagConstraints.REMAINDER;
///	areaConstr.weighty = 0.0;
///	gbl.setConstraints(errorArea, areaConstr);
///	window.add(errorArea);
///#else
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
///#endif
	Decompiler.err = new PrintStream(new AreaOutputStream(errorArea));
    }

    public void setClasspath(String cp) {
	classpathField.setText(cp);
    }
    public void setClass(String cls) {
	classField.setText(cls);
    }
	
///#ifdef AWT10
///    public synchronized void action(Object target) {
///#else
    public synchronized void actionPerformed(ActionEvent e) {
	Object target = e.getSource();
///#endif
	if (target == startButton) {

///#ifdef AWT10
///	    startButton.disable();
///#else
	    startButton.setEnabled(false);
///#endif
	    decompileThread = new Thread(this);
	    sourcecodeArea.setText("Please wait, while decompiling...\n");
	    decompileThread.start();
	} else if (target == saveButton) {
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
	    } catch (SecurityException ex) {
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
///#ifdef AWT10
///	    area.appendText(String.valueOf((char)b));
///#else
	    area.append(String.valueOf((char)b));
///#endif
	}

	public void write(byte[] b, int off, int len) throws IOException {
///#ifdef AWT10
///	    area.appendText(new String(b, off, len));
///#else
	    area.append(new String(b, off, len));
///#endif
	}
    }

    public void run() {
	Decompiler.isVerbose = verboseCheck.getState();
	Decompiler.prettyLocals = prettyCheck.getState();
	errorArea.setText("");
///#ifdef AWT10
///	saveButton.disable();
///#else
	saveButton.setEnabled(false);
///#endif

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
///#ifdef AWT10
///	    saveButton.enable();
///#else
	    saveButton.setEnabled(true);
///#endif
	} catch (Throwable t) {
	    sourcecodeArea.setText("Didn't succeed.\n"
				   +"Check the below area for more info.");
	    t.printStackTrace();
	} finally {
	    synchronized(this) {
		decompileThread = null;
///#ifdef AWT10
///		startButton.enable();
///#else
		startButton.setEnabled(true);
///#endif
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

///#ifndef AWT10
	frame.addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent e) {
		System.exit(0);
	    }
	});
///#endif
	frame.pack();
	frame.show();
    }
}
