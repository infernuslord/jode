package jode.swingui;
import jode.Decompiler;
import jode.JodeEnvironment;
import jode.decompiler.TabbedPrintWriter;
import com.sun.java.swing.*;
import com.sun.java.swing.event.*;
import com.sun.java.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class MainWindow 
    implements ActionListener, Runnable, TreeSelectionListener {
    JTree classTree;
    PackagesTreeModel classModel;
    TextArea  sourcecodeArea, errorArea;
    String classpath, lastClassName;
    Thread decompileThread;

    public MainWindow(Container frame) {
	frame.setLayout(new GridBagLayout());
	GridBagConstraints c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	c.gridwidth = 1;
	c.gridheight = 2;
	c.weightx = 0.5;
	c.weighty = 1.0;
	c.fill = c.BOTH;
	classModel = new PackagesTreeModel();
	classTree = new JTree(classModel);
	classTree.setRootVisible(false);
	DefaultTreeSelectionModel selModel = new DefaultTreeSelectionModel();
	selModel.setSelectionMode(selModel.SINGLE_TREE_SELECTION);
	classTree.setSelectionModel(selModel);
	classTree.addTreeSelectionListener(this);
        JScrollPane sp = new JScrollPane();
        sp.getViewport().add(classTree);
	frame.add(sp, c);
	c.gridx = 1;
	c.gridy = 0;
	c.gridwidth = 1;
	c.gridheight = 1;
	c.weightx = 1.0;
	c.weighty = 1.0;
	c.fill = c.BOTH;
	sourcecodeArea = new TextArea(20, 80);
	frame.add(sourcecodeArea, c);
	c.gridx = 1;
	c.gridy = 1;
	c.gridwidth = 1;
	c.gridheight = 1;
	c.weightx = 1.0;
	c.weighty = 0.0;
	c.fill = c.BOTH;
	errorArea = new TextArea(3, 80);
	frame.add(errorArea, c);
	Decompiler.err = new PrintStream(new AreaOutputStream(errorArea));
    }

    public void setClasspath(String classpath) {
	this.classpath = classpath;
    }

    public synchronized void valueChanged(TreeSelectionEvent e) {
	if (decompileThread != null)
	    return;
	TreePath path = e.getNewLeadSelectionPath();
	if (path == null)
	    return;
	Object node = path.getLastPathComponent();
	if (node != null && classModel.isLeaf(node)) {
	    lastClassName = classModel.getFullName(node);
	    decompileThread = new Thread(this);
	    decompileThread.setPriority(Thread.MIN_PRIORITY);
	    sourcecodeArea.setText("Please wait, while decompiling...\n");
	    decompileThread.start();
	}
    }

    public synchronized void actionPerformed(ActionEvent e) {
	if (e.getSource() == classTree && decompileThread == null) {
//  	    startButton.setEnabled(false);
	    decompileThread = new Thread(this);
	    sourcecodeArea.setText("Please wait, while decompiling...\n");
	    decompileThread.start();
//  	} else if (e.getSource() == saveButton) {
//  	    if (frame == null)
//  		frame = new Frame(); //XXX
//  	    FileDialog fd = new FileDialog(frame, 
//  					   "Save decompiled code", 
//  					   FileDialog.SAVE);
//  	    fd.setFile(lastClassName.substring
//  		       (lastClassName.lastIndexOf('.')+1).concat(".java"));
//  	    fd.show();
//  	    String fileName = fd.getFile();
//  	    if (fileName == null)
//  		return;
//  	    try {
//  		File f = new File(new File(fd.getDirectory()), fileName);
//  		FileWriter out = new FileWriter(f);
//  		out.write(sourcecodeArea.getText());
//  		out.close();
//  	    } catch (IOException ex) {
//  		errorArea.setText("");
//  		Decompiler.err.println("Couldn't write to file " 
//  				       + fileName + ": ");
//  		ex.printStackTrace(Decompiler.err);
//  	    }
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
//  	Decompiler.isVerbose = verboseCheck.getState();
//  	Decompiler.prettyLocals = prettyCheck.getState();
	errorArea.setText("");
//  	saveButton.setEnabled(false);

	String cp = classpath;
	cp = cp.replace(':', jode.bytecode.SearchPath.protocolSeparator);
	cp = cp.replace(',', File.pathSeparatorChar);
	JodeEnvironment env = new JodeEnvironment(cp);
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	try {
	    TabbedPrintWriter writer = new TabbedPrintWriter(out);
	    env.doClass(lastClassName, writer);
	    sourcecodeArea.setText(out.toString());
//  	    saveButton.setEnabled(true);
	} catch (Throwable t) {
	    sourcecodeArea.setText("Didn't succeed.\n"
				   +"Check the below area for more info.");
	    t.printStackTrace();
	} finally {
	    synchronized(this) {
		decompileThread = null;
//  		startButton.setEnabled(true);
	    }
	}
    }

    public static void main(String[] params) {
	JFrame frame = new JFrame(Decompiler.copyright);
	String cp = System.getProperty("java.class.path");
	if (cp != null)
	    jode.bytecode.ClassInfo.setClassPath(cp);

	MainWindow win = new MainWindow(frame.getContentPane());
	if (cp != null)
	    win.setClasspath(cp.replace(File.pathSeparatorChar, ','));

	frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
	frame.addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent e) {
		System.exit(0);
	    }
	});
	frame.pack();
	frame.show();
    }
}
