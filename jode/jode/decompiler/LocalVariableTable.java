/* 
 * LocalVariableTable (c) 1998 Jochen Hoenicke
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

package jode.decompiler;
import java.io.*;
import jode.Decompiler;
import jode.Type;
import jode.bytecode.AttributeInfo;
import jode.bytecode.ConstantPool;

public class LocalVariableTable {
    LocalVariableRangeList[] locals;

    public LocalVariableTable(int size, ConstantPool constantPool, 
			      AttributeInfo attr) {

        locals = new LocalVariableRangeList[size];
        for (int i=0; i<size; i++)
            locals[i] = new LocalVariableRangeList(i);

        DataInputStream stream = new DataInputStream
            (new ByteArrayInputStream(attr.getContents()));
        try {
            int count = stream.readUnsignedShort();
            for (int i=0; i < count; i++) {
                int start  = stream.readUnsignedShort();
                int end    = start + stream.readUnsignedShort();
		int nameIndex = stream.readUnsignedShort();
		int typeIndex = stream.readUnsignedShort();
		if (nameIndex == 0 || typeIndex == 0
		    || constantPool.getTag(nameIndex) != constantPool.UTF8
		    || constantPool.getTag(typeIndex) != constantPool.UTF8) {
		    // This is probably an evil lvt as created by HashJava
		    // simply ignore it.
		    if (Decompiler.showLVT) 
			Decompiler.err.println("Illegal entry, ignoring LVT");
		    return;
		}
                String name = constantPool.getUTF8(nameIndex);
                Type type = Type.tType(constantPool.getUTF8(typeIndex));
                int slot   = stream.readUnsignedShort();
                locals[slot].addLocal(start, end-start, name, type);
                if (Decompiler.showLVT)
                    Decompiler.err.println("\t"+name + ": " + type
                                       +" range "+start+" - "+end
                                       +" slot "+slot);
            }
        } catch (IOException ex) {
            ex.printStackTrace(Decompiler.err);
        }
    }

    public LocalVariableRangeList getLocal(int slot) 
         throws ArrayIndexOutOfBoundsException
    {
        return locals[slot];
    }
}
