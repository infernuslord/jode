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

    public LocalVariableTable(int size, 
                              ClassAnalyzer cla, AttributeInfo attr) {

        locals = new LocalVariableRangeList[size];
        for (int i=0; i<size; i++)
            locals[i] = new LocalVariableRangeList(i);

        ConstantPool constantPool = cla.getConstantPool();

        DataInputStream stream = new DataInputStream
            (new ByteArrayInputStream(attr.getContents()));
        try {
            int count = stream.readUnsignedShort();
            for (int i=0; i < count; i++) {
                int start  = stream.readUnsignedShort();
                int end    = start + stream.readUnsignedShort();
                String name = constantPool.getUTF8(stream.readUnsignedShort());
                Type type = Type.tType(constantPool.getUTF8(stream.readUnsignedShort()));
                int slot   = stream.readUnsignedShort();
                locals[slot].addLocal(start, end-start, name, type);
                if (Decompiler.showLVT)
                    Decompiler.err.println(name + ": " + type
                                       +" range "+start+" - "+end
                                       +" slot "+slot);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public LocalVariableRangeList getLocal(int slot) 
         throws ArrayIndexOutOfBoundsException
    {
        return locals[slot];
    }
}
