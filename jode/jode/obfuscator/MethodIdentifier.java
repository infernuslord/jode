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

public class MethodIdentifier extends Identifier implements Opcodes {
    ClassIdentifier clazz;
    MethodInfo info;
    CodeInfo codeinfo;
    /**
     * The exceptions that can be thrown by this method
     */
    String[] exceptions;

    public MethodIdentifier(ClassIdentifier clazz, MethodInfo info) {
	super(info.getName());
	this.clazz = clazz;
	this.info  = info;
    }

    /**
     * Reads the opcodes out of the code info and determine its 
     * references
     * @return an enumeration of the references.
     */
    public void analyzeCode() throws IOException{
	ConstantPool cp = clazz.info.getConstantPool();
	byte[] code = codeinfo.getCode();
	DataInputStream stream = 
	    new DataInputStream(new ByteArrayInputStream(code));
	int addr = 0;
	while (stream.available() > 0) {
	    int opcode = stream.readUnsignedByte();
	    switch (opcode) {
	    case opc_wide: {
		switch (opcode = stream.readUnsignedByte()) {
		case opc_iload: case opc_lload: 
		case opc_fload: case opc_dload: case opc_aload:
		case opc_istore: case opc_lstore: 
		case opc_fstore: case opc_dstore: case opc_astore:
		case opc_ret:
		    stream.skip(2);
		    addr+=4;
		    break;
				
		case opc_iinc:
		    stream.skip(4);
		    addr+=6;
		    break;
		default:
		    throw new ClassFormatError("Invalid wide opcode "+opcode);
		}
	    }
	    case opc_ret:
		stream.skip(1);
		addr+=2;
		break;
	    case opc_sipush:
	    case opc_ldc_w:
	    case opc_ldc2_w:
	    case opc_iinc:
	    case opc_ifnull: case opc_ifnonnull:
	    case opc_putstatic:
	    case opc_putfield:
		stream.skip(2);
		addr+=3;
		break;
	    case opc_jsr_w:
	    case opc_goto_w:
		stream.skip(4);
		addr+=5;
		break;
	    case opc_tableswitch: {
		int length = 7-(addr % 4);
		stream.skip(length);
		int low  = stream.readInt();
		int high = stream.readInt();
		stream.skip(4*(high-low+1));
		addr += 9 + length + 4*(high-low+1);
		break;
	    }
	    case opc_lookupswitch: {
		int length = 7-(addr % 4);
		stream.skip(length);
		int npairs = stream.readInt();
		stream.skip(8*npairs);
		addr += 5 + length + 8*npairs;
		break;
	    }
			
	    case opc_getstatic:
	    case opc_getfield: {
		int ref = stream.readUnsignedShort();
		String[] names = cp.getRef(ref);
		clazz.bundle.reachableIdentifier
		    (names[0].replace('/','.')+"."+names[1]+"."+names[2],
		     false);
		addr += 3;
		break;
	    }
	    case opc_new:
	    case opc_anewarray:
	    case opc_checkcast:
	    case opc_instanceof:
	    case opc_multianewarray: {
		int ref = stream.readUnsignedShort();
		String clName = cp.getClassName(ref).replace('/','.');
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
		addr += 3;
		if (opcode == opc_multianewarray) {
		    stream.skip(1);
		    addr ++;
		}
		break;
	    }
	    case opc_invokespecial:
	    case opc_invokestatic:
	    case opc_invokeinterface:
	    case opc_invokevirtual: {
		int ref = stream.readUnsignedShort();
		String[] names = cp.getRef(ref);
		clazz.bundle.reachableIdentifier
		    (names[0].replace('/','.')+"."+names[1]+"."+names[2],
		     opcode == opc_invokevirtual 
		     || opcode == opc_invokeinterface);
		addr += 3;

		if (opcode == opc_invokeinterface) {
		    stream.skip(2);
		    addr += 2;
		}
		break;
	    }

	    default:
		if (opcode == opc_newarray
		    || (opcode >= opc_bipush && opcode <= opc_aload)
		    || (opcode >= opc_istore && opcode <= opc_astore)) {
		    stream.skip(1);
		    addr += 2;
		} else if (opcode >= opc_ifeq && opcode <= opc_jsr) {
		    stream.skip(2);
		    addr += 3;
		} else if (opcode == opc_xxxunusedxxx
			   || opcode >= opc_breakpoint)
		    throw new ClassFormatError("Invalid opcode "+opcode);
		else
		    addr++;
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
	    clazz.bundle.reachableIdentifier(exceptions[i], false);
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

        AttributeInfo codeattr = info.findAttribute("Code");
        AttributeInfo exceptionsattr = info.findAttribute("Exceptions");

	try {
	    if (codeattr != null) {
		DataInputStream stream = new DataInputStream
		    (new ByteArrayInputStream(codeattr.getContents()));
		codeinfo = new CodeInfo();
		codeinfo.read(clazz.info.getConstantPool(), stream);
		analyzeCode();
	    }
	    if (exceptionsattr != null)
		readExceptions(exceptionsattr);
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
	if (strong) {
	    return clazz.getMethod(newAlias, getType()) != null;
	} else {
	    String type = getType();
	    String paramType = type.substring(0, type.indexOf(')')+1);
	    return clazz.getMethod(newAlias, paramType) != null;
	}
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

    public void transformCode(GrowableConstantPool gcp) {
	ConstantPool cp = clazz.info.getConstantPool();
	byte[] origcode = codeinfo.getCode();
	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	DataOutputStream output = new DataOutputStream(baos);
	DataInputStream input = new DataInputStream
	    (new ByteArrayInputStream(origcode));
	try {
	    output.writeShort(codeinfo.getMaxStack());
	    output.writeShort(codeinfo.getMaxLocals());
	    output.writeInt(origcode.length);
	    int addr = 0;
	    while (input.available() > 0) {
		int opcode = input.readUnsignedByte();
		switch (opcode) {
		case opc_wide: {
		    output.writeByte(opcode);
		    int wideopcode = input.readUnsignedByte();
		    switch (wideopcode) {
		    case opc_iload: case opc_lload: 
		    case opc_fload: case opc_dload: case opc_aload:
		    case opc_istore: case opc_lstore: 
		    case opc_fstore: case opc_dstore: case opc_astore:
		    case opc_ret:
			output.writeByte(wideopcode);
			copy(input, output, 2);
			addr+=4;
			break;
			
		    case opc_iinc:
			output.writeByte(wideopcode);
			copy(input, output, 4);
			addr+=6;
			break;
		    default:
			throw new ClassFormatError("Invalid wide opcode "
						   +wideopcode);
		    }
		    break;
		}
		case opc_ret:
		    output.writeByte(opcode);
		    copy(input, output, 1);
		    addr+=2;
		    break;

		case opc_ldc: {
		    int index = input.readUnsignedByte();
		    int newIndex = gcp.copyConstant(cp, index);
		    output.writeByte(opcode);
		    output.writeByte(newIndex);
		    addr+=2;
		    break;
		}
		case opc_ldc_w:
		case opc_ldc2_w: {
		    int index = input.readUnsignedShort();
		    int newIndex = gcp.copyConstant(cp, index);
		    output.writeByte(opcode);
		    output.writeShort(newIndex);
		    addr += 3;
		    break;
		}
		
		case opc_sipush:
		case opc_iinc:
		case opc_ifnull: case opc_ifnonnull:
		    output.writeByte(opcode);
		    copy(input, output, 2);
		    addr+=3;
		    break;
		case opc_jsr_w:
		case opc_goto_w:
		    output.writeByte(opcode);
		    copy(input, output, 4);
		    addr+=5;
		    break;
		case opc_tableswitch: {
		    output.writeByte(opcode);
		    int length = 7-(addr % 4);
		    copy(input, output, length);
		    int low  = input.readInt();
		    output.writeInt(low);
		    int high = input.readInt();
		    output.writeInt(high);
		    copy(input, output, 4*(high-low+1));
		    addr += 9 + length + 4*(high-low+1);
		    break;
		}
		case opc_lookupswitch: {
		    output.writeByte(opcode);
		    int length = 7-(addr % 4);
		    copy(input, output, length);
		    int npairs = input.readInt();
		    output.writeInt(npairs);
		    copy(input, output, 8*npairs);
		    addr += 5 + length + 8*npairs;
		    break;
		}
		
		case opc_getstatic:
		case opc_getfield:
		case opc_putstatic:
		case opc_putfield: {
		    int ref = input.readUnsignedShort();
		    String[] names = cp.getRef(ref);
		    ClassIdentifier ci = (ClassIdentifier)
			clazz.bundle.getIdentifier(names[0].replace('/','.'));

		    if (ci != null) {
			names[0] = ci.getFullAlias();
			Identifier fi =  ci.getIdentifier(names[1], names[2]);
			if (fi instanceof FieldIdentifier) {
			    if (!((FieldIdentifier)fi).isReachable()) {
				if (opcode != opc_putfield
				    && opcode != opc_putstatic)
				    throw new jode.AssertError
					("reading not reachable field");
				int stacksize = 
				    (opcode == opc_putstatic) ? 0 : 1;
				stacksize += jode.Type.tType
				    (names[2]).stackSize();
				if (stacksize == 3) {
				    output.writeByte(opc_pop2);
				    output.writeByte(opc_pop);
				    output.writeByte(opc_nop);
				} else if (stacksize == 2) {
				    output.writeByte(opc_pop2);
				    output.writeByte(opc_nop);
				    output.writeByte(opc_nop);
				} else {
				    output.writeByte(opc_pop);
				    output.writeByte(opc_nop);
				    output.writeByte(opc_nop);
				}
				addr += 3;
				break;
			    }
			    names[1] = ((FieldIdentifier) fi).getAlias();
			}
		    }
		    names[2] = clazz.bundle.getTypeAlias(names[2]);
		    output.writeByte(opcode);
		    output.writeShort(gcp.putRef(cp.getTag(ref), names));
		    addr += 3;
		    break;
		}
		case opc_new:
		case opc_anewarray:
		case opc_checkcast:
		case opc_instanceof:
		case opc_multianewarray: {
		    int ref = input.readUnsignedShort();
		    String clName = cp.getClassName(ref).replace('/','.');
		    if (clName.charAt(0) == '[') {
			clName = clazz.bundle.getTypeAlias(clName);
		    } else {
			ClassIdentifier ci = (ClassIdentifier)
			    clazz.bundle.getIdentifier(clName);
			if (ci != null)
			    clName = ci.getFullAlias();
		    }
		    int newRef = gcp.putClassRef(clName);
		    output.writeByte(opcode);
		    output.writeShort(newRef);
		    addr += 3;
		    if (opcode == opc_multianewarray) {
			copy(input, output, 1);
			addr ++;
		    }
		    break;
		}
		case opc_invokespecial:
		case opc_invokestatic:
		case opc_invokeinterface:
		case opc_invokevirtual: {
		    int ref = input.readUnsignedShort();
		    String[] names = cp.getRef(ref);
		    ClassIdentifier ci = (ClassIdentifier)
			clazz.bundle.getIdentifier(names[0].replace('/','.'));

		    if (ci != null) {
			names[0] = ci.getFullAlias();
			Identifier mi =  ci.getIdentifier(names[1], names[2]);
			if (mi instanceof MethodIdentifier) {
			    names[1] = ((MethodIdentifier)mi).getAlias();
			}
		    }
		    names[2] = clazz.bundle.getTypeAlias(names[2]);
		    output.writeByte(opcode);
		    output.writeShort(gcp.putRef(cp.getTag(ref), names));
		    addr += 3;
		    if (opcode == opc_invokeinterface) {
			copy(input, output, 2);
			addr += 2;
		    }
		    break;
		}
		
		default:
		    output.writeByte(opcode);
		    if (opcode == opc_newarray
			|| (opcode >= opc_bipush && opcode <= opc_aload)
			|| (opcode >= opc_istore && opcode <= opc_astore)) {
			copy(input, output, 1);
			addr += 2;
		    } else if (opcode >= opc_ifeq && opcode <= opc_jsr) {
			copy(input, output, 2);
			addr += 3;
		    } else if (opcode == opc_xxxunusedxxx
			       || opcode >= opc_breakpoint)
			throw new ClassFormatError("Invalid opcode "+opcode);
		    else
			addr++;
		}
	    }

	    int[] handlers = codeinfo.getExceptionHandlers();
	    output.writeShort(handlers.length / 4);
	    for (int i=0; i< handlers.length; i += 4) {
		output.writeShort(handlers[i]);
		output.writeShort(handlers[i+1]);
		output.writeShort(handlers[i+2]);
		String clName 
		    = cp.getClassName(handlers[i+3]).replace('/','.');
		ClassIdentifier ci = (ClassIdentifier)
		    clazz.bundle.getIdentifier(clName);
		if (ci != null)
		    clName = ci.getFullAlias();
		output.writeShort(gcp.putClassRef(clName));
	    }
	    output.writeShort(0); // No Attributes;
	    output.close();
	} catch (IOException ex) {
	    ex.printStackTrace();
	    code = null;
	    return;
	}
	code = baos.toByteArray();
    }

    public void reserveSmallConstants(GrowableConstantPool gcp) {
        if (codeinfo != null) {
	    ConstantPool cp = clazz.info.getConstantPool();
	    byte[] code = codeinfo.getCode();
	    DataInputStream stream = 
		new DataInputStream(new ByteArrayInputStream(code));
	    try {
		int addr = 0;
		while (stream.available() > 0) {
		    int opcode = stream.readUnsignedByte();
		    switch (opcode) {

		    case opc_ldc: {
			int index = stream.readUnsignedByte();
			gcp.copyConstant(cp, index);
			break;
		    }

		    case opc_wide: {
			switch (opcode = stream.readUnsignedByte()) {
			case opc_iload: case opc_lload: 
			case opc_fload: case opc_dload: case opc_aload:
			case opc_istore: case opc_lstore: 
			case opc_fstore: case opc_dstore: case opc_astore:
			case opc_ret:
			    stream.skip(2);
			    addr+=4;
			    break;
				
			case opc_iinc:
			    stream.skip(4);
			    addr+=6;
			    break;
			default:
			    throw new ClassFormatError("Invalid wide opcode "+opcode);
			}
		    }
		    case opc_tableswitch: {
			int length = 7-(addr % 4);
			stream.skip(length);
			int low  = stream.readInt();
			int high = stream.readInt();
			stream.skip(4*(high-low+1));
			addr += 9 + length + 4*(high-low+1);
			break;
		    }
		    case opc_lookupswitch: {
			int length = 7-(addr % 4);
			stream.skip(length);
			int npairs = stream.readInt();
			stream.skip(8*npairs);
			addr += 5 + length + 8*npairs;
			break;
		    }
		    case opc_ret:
			stream.skip(1);
			addr+=2;
			break;
		    case opc_sipush:
		    case opc_ldc2_w:
		    case opc_iinc:
		    case opc_ifnull: case opc_ifnonnull:
		    case opc_new:
		    case opc_anewarray:
		    case opc_checkcast:
		    case opc_instanceof:
			stream.skip(2);
			addr+=3;
			break;
		    case opc_multianewarray:
			stream.skip(3);
			addr += 4;
			break;
		    case opc_jsr_w:
		    case opc_goto_w:
		    case opc_invokeinterface:
			stream.skip(4);
			addr+=5;
			break;
		    default:
			if (opcode == opc_newarray
			    || (opcode >= opc_bipush && opcode <= opc_aload)
			    || (opcode >= opc_istore && opcode <= opc_astore)) {
			    stream.skip(1);
			    addr += 2;
			} else if (opcode >= opc_ifeq && opcode <= opc_jsr
				   || opcode >= opc_getstatic && opcode <= opc_invokestatic) {
			    stream.skip(2);
			    addr += 3;
			} else if (opcode == opc_xxxunusedxxx
				   || opcode >= opc_breakpoint)
			    throw new ClassFormatError("Invalid opcode "+opcode);
			else
			    addr++;
		    }
		}
	    } catch (IOException ex) {
		ex.printStackTrace();
	    }
	}
    }

    public void fillConstantPool(GrowableConstantPool gcp) {
	nameIndex = gcp.putUTF(getAlias());
	descriptorIndex = gcp.putUTF(clazz.bundle.getTypeAlias(getType()));
        AttributeInfo codeattr = info.findAttribute("Code");
	codeIndex = 0;
        if (codeinfo != null) {
	    transformCode(gcp);
	    if (code != null)
		codeIndex = gcp.putUTF("Code");
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

