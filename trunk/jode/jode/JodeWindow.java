package jode;
import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class JodeWindow 
    implements ActionListener, Runnable
{
    TextField classpathField;
    TextField classField;
    TextArea  sourcecodeArea;
    TextArea  errorArea;

    Thread decompileThread;
    
    public JodeWindow(Container frame) {
	buildComponents(frame);
    }

    private void buildComponents(Container frame) {
	classpathField = new TextField(50);
	classField = new TextField(50);
	sourcecodeArea = new TextArea(20, 80);
	errorArea = new TextArea(3, 80);

	sourcecodeArea.setEditable(false);
	errorArea.setEditable(false);

	frame.setLayout(new GridBagLayout());
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

	frame.add(new Label("class path: "), labelConstr);
	frame.add(classpathField, textConstr);
	frame.add(new Label("class name: "), labelConstr);
	frame.add(classField, textConstr);
	frame.add(sourcecodeArea, areaConstr);
	areaConstr.gridheight = GridBagConstraints.REMAINDER;
	areaConstr.weighty = 0.0;
	frame.add(errorArea, areaConstr);

	classField.addActionListener(this);

	String cp = System.getProperty("java.class.path");
	if (cp != null)
	    classpathField.setText(cp);
	String cls = "jode.JodeWindow";
	classField.setText(cls);

	Decompiler.err = new PrintStream(new AreaOutputStream(errorArea));
    }

    public void setClasspath(String cp) {
	classpathField.setText(cp);
    }
    public void setClass(String cls) {
	classField.setText(cls);
    }
	
    public synchronized void actionPerformed(ActionEvent e) {
	if (decompileThread == null) {
	    decompileThread = new Thread(this);
	    sourcecodeArea.setText("Please wait, while decompiling...\n");
	    decompileThread.start();
	} else
	    sourcecodeArea.append("Be a little bit more patient, please.\n");
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
	JodeEnvironment env = new JodeEnvironment(classpathField.getText());
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	errorArea.setText("");
	try {
	    TabbedPrintWriter writer = new TabbedPrintWriter(out, "    ");
	    env.doClass(classField.getText(), writer);
	    sourcecodeArea.setText(out.toString());
	} catch (Throwable t) {
	    sourcecodeArea.setText("Didn't succeed.\n"
				   +"Check the below area for more info.");
	    t.printStackTrace();
	} finally {
	    synchronized(this) {
		decompileThread = null;
	    }
	}
    }

    public static void main(String argv[]) {
	Frame frame = new Frame(Decompiler.copyright);
	new JodeWindow(frame);
	frame.addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent e) {
		System.exit(0);
	    }
	});
	frame.pack();
	frame.show();
    }
}
