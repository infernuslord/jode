/* 
 * MethodIdentifier (c) 1998 Jochen Hoenicke
 *
 * You may distribute under the terms of the GNU General Public License.
 *
 * IN NO EVENT SHALL JOCHEN HOENICKE BE LIABLE TO ANY PARTY FOR DIRECT,
 * INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT OF
 * THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF JOCHEN HOENICKE 
 * HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * JOCHEN HOENICKE SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS ON AN "AS IS"
 * BASIS, AND JOCHEN HOENICKE HAS NO OBLIGATION TO PROVIDE MAINTENANCE,
 * SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * $Id$
 */
package jode.obfuscator;
import jode.Obfuscator;
import jode.bytecode.*;
import java.io.*;
import java.util.Vector;
import java.util.Enumeration;

public class MethodIdentifier extends Identifier {
    ClassIdentifier clazz;
    MethodInfo info;

    public MethodIdentifier(ClassIdentifier clazz, MethodInfo info) {
	super(info.getName());
	this.clazz = clazz;
	this.info  = info;
    }

    public Vector analyzeCode(CodeInfo codeinfo) {
	Vector references = new Vector();
	byte[] code = codeinfo.getCode();
        try {
            DataInputStream stream = 
                new DataInputStream(new ByteArrayInputStream(code));
	    Opcodes.getReferences(stream, references);
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new ClassFormatError(ex.getMessage());
        }
	return references;
    }

    public void setSingleReachable() {
	super.setSingleReachable();

	if (Obfuscator.isDebugging)
	    Obfuscator.err.println("Reachable: "+this);

	ConstantPool cp = clazz.info.getConstantPool();
        AttributeInfo codeattr = info.findAttribute("Code");
        if (codeattr == null)
	    return;

	DataInputStream stream = new DataInputStream
	    (new ByteArrayInputStream(codeattr.getContents()));
	CodeInfo codeinfo = new CodeInfo();
	try {
	    codeinfo.read(clazz.info.getConstantPool(), stream);
	    Vector references = analyzeCode(codeinfo);
	    Enumeration enum = references.elements();
	    while (enum.hasMoreElements()) {
		int[] ref = (int[]) enum.nextElement();
		int tag = cp.getTag(ref[0]);
		switch (tag) {
		case ConstantPool.FIELDREF:
		case ConstantPool.METHODREF:
		case ConstantPool.INTERFACEMETHODREF: {
		    String[] names = cp.getRef(ref[0]);
		    clazz.bundle.reachableIdentifier
			(names[0].replace('/','.')+"."+names[1]+"."+names[2],
			 ref[1] == 1);
		    break;
		}
		case ConstantPool.CLASS: {
		    String clName = cp.getClassName(ref[0]).replace('/','.');
		    if (clName.charAt(0) == '[') {
			int i;
			for (i=0; i< clName.length(); i++)
			    if (clName.charAt(i) != '[')
				break;
			if (i >= clName.length() || clName.charAt(i) != 'L')
			    break;
			int index = clName.indexOf(';', i);
			if (index != clName.length()-1)
			    break;
			clName = clName.substring(i+1, index);
		    }
		    clazz.bundle.reachableIdentifier(clName, false);
		    break;
		}
		default:
		    throw new jode.AssertError
			("Unknown reference: "+ref+" tag: "+tag);
		}
	    }
	} catch (IOException ex) {
	    ex.printStackTrace();
	}
    }

    public void writeTable(PrintWriter out) throws IOException {
	if (getName() != getAlias())
	    out.println("" + getFullAlias()
			+ "." + clazz.bundle.getTypeAlias(getType())
			+ " = " + getName());
    }

    public String getFullName() {
	return clazz.getFullName() + "." + getName();
    }

    public String getFullAlias() {
	return clazz.getFullAlias() + "." + getAlias();
    }

    public String getName() {
	return info.getName();
    }

    public String getType() {
	return info.getType().getTypeSignature();
    }

    public boolean conflicting(String newAlias) {
	String type = getType();
	String paramType = type.substring(0, type.indexOf(')')+1);
	return clazz.containsMethod(newAlias, paramType)
	    || clazz.containsField(newAlias);
    }

    public String toString() {
	return "MethodIdentifier "+getFullName()+"."+getType();
    }
}
