/* 
 * MethodReachability (c) 1998 Jochen Hoenicke
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
import jode.bytecode.ConstantPool;
import jode.bytecode.MethodInfo;
import jode.MethodType;
import jode.Type;
import java.lang.reflect.Modifier;
import java.util.*;

public class MethodReachability {
    ClassReachability clazz;
    MethodInfo info;
    public ClassReachability inheritedFromClass;

    public boolean reachable = false;
    public boolean preserve = false;

    public Vector references;

    public MethodReachability(ClassReachability clazz, MethodInfo info) {
	this.clazz = clazz;
	this.info  = info;

        AttributeInfo codeattr = minfo.findAttribute("Code");
        if (codeattr != null) {
            DataInputStream stream = new DataInputStream
                (new ByteArrayInputStream(codeattr.getContents()));
            CodeInfo codeinfo = new CodeInfo();
            try {
                codeinfo.read(classAnalyzer.getConstantPool(), stream);
		analyzeCode(codeinfo);
            } catch (IOException ex) {
                ex.printStackTrace();
                code = null;
            }
        }
	
    }

    public void analyzeCode(CodeInfo codeinfo) {
	references = new Vector();
	byte[] code = codeinfo.getCode();
        try {
            DataInputStream stream = 
                new DataInputStream(new ByteArrayInputStream(code));
	    Opcodes.getReferences(stream, references);
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new ClassFormatError(ex.getMessage());
        }
    }

    public void markReachable() {
	ConstantPool cp = clazz.getClassInfo().getConstantPool();
	if (!reachable) {
	    reachable = true;
	    Enumeration enum = references.elements();
	    while (enum.hasMoreElements()) {
		int ref = ((Integer) enum.nextElement()).intValue();
		int tag = cp.getTag(ref);
		switch (tag) {
		case ConstantPool.FIELDREF:
		case ConstantPool.METHODREF:
		case ConstantPool.INTERFACEMETHODREF:
		    String[] refs = cp.getRef(ref);
		    if (tag == ConstantPool.FIELDREF)
			clazz.getBundle()
			    .markReachableField(refs[0], refs[1],
						new MethodType(refs[2]));
		    else
			clazz.getBundle()
			    .markReachableMethod(refs[0], refs[1],
						 new MethodTyp(refs[2]));
		    break;
		case ConstantPool.CLASS:
		    String clName = cp.getClassName(ref);
		    clazz.getBundle().markReachable(clName);
		    break;
		}
	    }
	}
    }

    public void markPreserved() {
	ConstantPool cp = clazz.getClassInfo().getConstantPool();
	if (!preserved) {
	    preserved = true;
	    Enumeration enum = references.elements();
	    while (enum.hasMoreElements()) {
		int ref = ((Integer) enum.nextElement()).intValue();
		int tag = cp.getTag(ref);
		switch (tag) {
		case ConstantPool.FIELDREF:
		case ConstantPool.METHODREF:
		case ConstantPool.INTERFACEMETHODREF:
		    String[] refs = cp.getRef(ref);
		    if (tag == ConstantPool.FIELDREF)
			clazz.getBundle()
			    .markPreservedField(refs[0], refs[1],
						new MethodType(refs[2]));
		    else
			clazz.getBundle()
			    .markPreservedMethod(refs[0], refs[1],
						 new MethodTyp(refs[2]));
		    break;
		case ConstantPool.CLASS:
		    String clName = cp.getClassName(ref);
		    clazz.getBundle().markPreserved(clName);
		    break;
		}
	    }
	}
    }
}
