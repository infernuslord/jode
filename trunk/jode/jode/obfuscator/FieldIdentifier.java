/* 
 * jode.obfuscator.FieldIdentifier (c) 1998 Jochen Hoenicke
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
import jode.bytecode.*;
import java.io.*;

public class FieldIdentifier extends Identifier{
    FieldInfo info;
    ClassIdentifier clazz;

    public FieldIdentifier(ClassIdentifier clazz, FieldInfo info) {
	super(info.getName());
	this.info = info;
	this.clazz = clazz;
    }

    public void setSingleReachable() {
	super.setSingleReachable();
	String type = getType();
	int index = type.indexOf('L');
	if (index != -1) {
	    int end = type.indexOf(';', index);
	    clazz.bundle.reachableIdentifier(type.substring(index+1, end)
					     , false);
	}
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

    public String toString() {
	return "MethodIdentifier "+getFullName()+"."+getType();
    }

    public boolean conflicting(String newAlias, boolean strong) {
	if (strong) {
	    return clazz.containFieldAlias(newAlias, getType());
	} else {
	    return clazz.containFieldAlias(newAlias, "");
	}
    }

    int nameIndex;
    int descriptorIndex;
    int constvalIndex;
    int constvalcontentIndex;

    public void fillConstantPool(GrowableConstantPool gcp) 
	throws ClassFormatException {
	nameIndex = gcp.putUTF(getAlias());
	descriptorIndex = gcp.putUTF(clazz.bundle.getTypeAlias(getType()));
	constvalIndex = 0;
        AttributeInfo attribute = info.findAttribute("ConstantValue");
	if (attribute != null) {
	    byte[] contents = attribute.getContents();
	    if (contents.length != 2)
		throw new ClassFormatError("ConstantValue attribute"
					   + " has wrong length");
	    int index = (contents[0] & 0xff) << 8 | (contents[1] & 0xff);
	    constvalIndex = gcp.putUTF("ConstantValue");
	    constvalcontentIndex = 
		gcp.copyConstant(clazz.info.getConstantPool(), index);
	}
    }

    public void write(DataOutputStream out) throws IOException {
	out.writeShort(info.getModifiers());
	out.writeShort(nameIndex);
	out.writeShort(descriptorIndex);
	if (constvalIndex != 0) {
	    out.writeShort(1); // number of Attributes
	    out.writeShort(constvalIndex);
	    out.writeInt(2);   // length of Attribute
	    out.writeShort(constvalcontentIndex);
	} else
	    out.writeShort(0);
    }
}

