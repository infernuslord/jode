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
import java.lang.reflect.Modifier;
import jode.Obfuscator;
import jode.bytecode.*;
import java.io.*;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Hashtable;

public class MethodIdentifier extends Identifier implements Opcodes {
    ClassIdentifier clazz;
    MethodInfo info;
    /**
     * The exceptions that can be thrown by this method
     */
    String[] exceptions;

    /**
     * The byte code of this method, or null if there isn't any.
     */
    BytecodeInfo bytecode;

    public MethodIdentifier(ClassIdentifier clazz, MethodInfo info) {
	super(info.getName());
	this.clazz = clazz;
	this.info  = info;

        AttributeInfo codeattr = info.findAttribute("Code");
        AttributeInfo exceptionsattr = info.findAttribute("Exceptions");

	try {
	    if (codeattr != null) {
		DataInputStream stream = new DataInputStream
		    (new ByteArrayInputStream(codeattr.getContents()));
		bytecode = new BytecodeInfo();
		bytecode.read(clazz.info.getConstantPool(), stream);
	    }
	    if (exceptionsattr != null)
		readExceptions(exceptionsattr);
	} catch (IOException ex) {
	    ex.printStackTrace(Obfuscator.err);
	}
    }

    public void applyPreserveRule(int preserveRule) {
	if ((preserveRule & (info.getModifiers() ^ Modifier.PRIVATE)) != 0) {
	    setReachable();
	    setPreserved();
	}
    }

    /**
     * Reads the opcodes out of the code info and determine its 
     * references
     * @return an enumeration of the references.
     */
    public void analyzeCode() throws IOException {
	for (Instruction instr = bytecode.getFirstInstr();
	     instr != null; instr = instr.nextByAddr) {
	    switch (instr.opcode) {
	    case opc_new:
	    case opc_anewarray:
	    case opc_checkcast:
	    case opc_instanceof:
	    case opc_multianewarray: {
		String clName = (String) instr.objData;
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
	    case opc_getstatic:
	    case opc_getfield:
	    case opc_invokespecial:
	    case opc_invokestatic:
	    case opc_invokeinterface:
	    case opc_invokevirtual: {
		String[] names = (String[]) instr.objData;
		clazz.bundle.reachableIdentifier
		    (names[0].replace('/','.')+"."+names[1]+"."+names[2],
		     instr.opcode == opc_invokevirtual 
		     || instr.opcode == opc_invokeinterface);
		break;
	    }
	    }
	}
    }

    public void readExceptions(AttributeInfo exceptionsattr) 
	throws IOException {
	byte[] content = exceptionsattr.getContents();
	DataInputStream input = new DataInputStream
	    (new ByteArrayInputStream(content));
	ConstantPool cp = clazz.info.getConstantPool();
	
	int count = input.readUnsignedShort();
	exceptions = new String[count];
	for (int i=0; i< count; i++) {
	    exceptions[i] 
		= cp.getClassName(input.readUnsignedShort()).replace('/','.');
	}
    }

    public void setSingleReachable() {
	super.setSingleReachable();

	if (Obfuscator.isDebugging)
	    Obfuscator.err.println("Reachable: "+this);

	String type = getType();
	int index = type.indexOf('L');
	while (index != -1) {
	    int end = type.indexOf(';', index);
	    clazz.bundle.reachableIdentifier(type.substring(index+1, end)
					     , false);
	    index = type.indexOf('L', end);
	}

	if (bytecode != null) {
	    try {
		analyzeCode();
	    } catch (IOException ex) {
		ex.printStackTrace(Obfuscator.err);
		System.exit(0);
	    }
	}
	if (exceptions != null) {
	    for (int i=0; i< exceptions.length; i++)
		clazz.bundle.reachableIdentifier(exceptions[i], false);
	}
    }

    public void readTable(Hashtable table) {
	setAlias((String) table.get(getFullName() + "." + getType()));
    }

    public void writeTable(Hashtable table) {
	table.put(getFullAlias()
		  + "." + clazz.bundle.getTypeAlias(getType()), getName());
    }

    public Identifier getParent() {
	return clazz;
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

    public boolean conflicting(String newAlias, boolean strong) {
	String paramType = getType();
	if (!strong) {
	    paramType = paramType.substring(0, paramType.indexOf(')')+1);
	}
	if (clazz.getMethod(newAlias, paramType) != null)
	    return true;

	Enumeration enum = clazz.knownSubClasses.elements();
	while (enum.hasMoreElements()) {
	    ClassIdentifier ci = (ClassIdentifier) enum.nextElement();
	    if (ci.hasMethod(newAlias, paramType))
		return true;
	}
	return false;
    }

    public String toString() {
	return "MethodIdentifier "+getFullName()+"."+getType();
    }

    int nameIndex;
    int descriptorIndex;
    int codeIndex;
    byte[] code;
    int exceptionsIndex;
    int[] excIndices;

    static byte[] buff = new byte[10];
    static void copy(DataInputStream in, DataOutputStream out, int length) 
	throws IOException {
	if (buff.length < length)
	    buff = new byte[length];
	in.readFully(buff, 0, length);
	out.write(buff, 0, length);
    }

    /**
     * This method does the code transformation.  This include
     * <ul><li>new slot distribution for locals</li>
     *     <li>obfuscating transformation of flow</li>
     *     <li>renaming field, method and class references</li>
     * </ul>
     */
    public void doCodeTransformations(GrowableConstantPool gcp) {
        if (bytecode != null) {
	    /* XXX This should be in a if (Obfuscator.distributeLocals) */
	    LocalOptimizer localOpt = new LocalOptimizer(bytecode);
	    localOpt.calcLocalInfo();
	    localOpt.distributeLocals();
	    if (Obfuscator.isDebugging)
		localOpt.dumpLocals();

	    for (Instruction instr = bytecode.getFirstInstr(); 
		 instr != null; instr = instr.nextByAddr) {
		switch (instr.opcode) {
		case opc_ldc:
		    gcp.reserveConstant(instr.objData);
		    break;

		case opc_invokespecial:
		case opc_invokestatic:
		case opc_invokeinterface:
		case opc_invokevirtual: {
		    String[] names = (String[]) instr.objData;
		    ClassIdentifier ci = (ClassIdentifier)
			clazz.bundle.getIdentifier(names[0].replace('/','.'));
		    
		    if (ci != null) {
			names[0] = ci.getFullAlias();
			names[1] = 
			    ((MethodIdentifier)
			     ci.getIdentifier(names[1], names[2])).getAlias();
		    }
		    names[2] = clazz.bundle.getTypeAlias(names[2]);
		    break;
		}
		case opc_getstatic:
		case opc_getfield: {
		    String[] names = (String[]) instr.objData;
		    ClassIdentifier ci = (ClassIdentifier)
			clazz.bundle.getIdentifier(names[0].replace('/','.'));
		    if (ci != null) {
			FieldIdentifier fi = (FieldIdentifier) 
			    ci.getIdentifier(names[1], names[2]);
			names[0] = ci.getFullAlias();
			names[1] = fi.getAlias();
		    }
		    names[2] = clazz.bundle.getTypeAlias(names[2]);
		    break;
		}
		case opc_putstatic:
		case opc_putfield: {
		    String[] names = (String[]) instr.objData;
		    ClassIdentifier ci = (ClassIdentifier)
			clazz.bundle.getIdentifier(names[0].replace('/','.'));
		    if (ci != null) {
			FieldIdentifier fi = (FieldIdentifier) 
			    ci.getIdentifier(names[1], names[2]);
			if (Obfuscator.shouldStrip && !fi.isReachable()) {
			    /* Replace instruction with pop opcodes. */
			    int stacksize = 
				(instr.opcode 
				 == Instruction.opc_putstatic) ? 0 : 1;
			    stacksize += jode.Type.tType(names[2]).stackSize();
			    if (stacksize == 3) {
				/* Add a pop instruction after this opcode. */
				Instruction second = new Instruction();
				second.length = 1;
				second.opcode = Instruction.opc_pop;
				second.nextByAddr = instr.nextByAddr;
				instr.nextByAddr = second;
				second.nextByAddr.preds.removeElement(instr);
				second.nextByAddr.preds.addElement(second);
				stacksize--;
			    }
			    instr.objData = null;
			    instr.intData = 0;
			    instr.opcode = Instruction.opc_pop - 1 + stacksize;
			    instr.length = 1;
			} else {
			    names[0] = ci.getFullAlias();
			    names[1] = fi.getAlias();
			}
		    }
		    names[2] = clazz.bundle.getTypeAlias(names[2]);
		    break;
		}
		case opc_new:
		case opc_anewarray:
		case opc_checkcast:
		case opc_instanceof:
		case opc_multianewarray: {
		    String clName = (String) instr.objData;
		    if (clName.charAt(0) == '[') {
			clName = clazz.bundle.getTypeAlias(clName);
		    } else {
			ClassIdentifier ci = (ClassIdentifier) 
			    clazz.bundle.getIdentifier(clName);
			if (ci != null)
			    clName = ci.getFullAlias();
		    }
		    instr.objData = clName;
		    break;
		}
		}
	    }
	
	    Handler[] handlers = bytecode.getExceptionHandlers();
	    for (int i=0; i< handlers.length; i++) {
		if (handlers[i].type != null) {
		    ClassIdentifier ci = (ClassIdentifier) 
			clazz.bundle.getIdentifier(handlers[i].type);
		    if (ci != null)
		    handlers[i].type = ci.getFullAlias();
		}
	    }
	}
    }

    public void fillConstantPool(GrowableConstantPool gcp) {
	nameIndex = gcp.putUTF(getAlias());
	descriptorIndex = gcp.putUTF(clazz.bundle.getTypeAlias(getType()));

	codeIndex = 0;
        if (bytecode != null) {



	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    DataOutputStream output = new DataOutputStream(baos);
	    try {
		bytecode.writeCode(gcp, clazz.bundle, output);
		output.close();
		code = baos.toByteArray();
		codeIndex = gcp.putUTF("Code");
	    } catch (IOException ex) {
		code = null;
	    }
	}
	if (exceptions != null) {
	    exceptionsIndex = gcp.putUTF("Exceptions");
	    excIndices = new int[exceptions.length];
	    for (int i=0; i< exceptions.length; i++) {
		ClassIdentifier ci = (ClassIdentifier) 
		    clazz.bundle.getIdentifier(exceptions[i]);
		if (ci != null)
		    excIndices[i] = gcp.putClassRef(ci.getFullAlias());
		else
		    excIndices[i] = gcp.putClassRef(exceptions[i]);
	    }
	}
    }

    public void write(DataOutputStream out) throws IOException {
	out.writeShort(info.getModifiers());
	out.writeShort(nameIndex);
	out.writeShort(descriptorIndex);
	int attrCount = 0;
	if (code != null)
	    attrCount++;
	if (excIndices != null)
	    attrCount++;
	out.writeShort(attrCount);
	if (code != null) {
	    out.writeShort(codeIndex);
	    out.writeInt(code.length);
	    out.write(code);
	}
	if (excIndices != null) {
	    out.writeShort(exceptionsIndex);
	    out.writeInt(excIndices.length*2+2);
	    out.writeShort(excIndices.length);
	    for (int i=0; i< excIndices.length; i++)
		out.writeShort(excIndices[i]);
	}
    }
}
