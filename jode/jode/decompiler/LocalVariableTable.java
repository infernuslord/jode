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

package jode;
import sun.tools.java.Identifier;
import java.io.*;

public class LocalVariableTable {
    LocalVariableRangeList[] locals;
    boolean readfromclass;

    public LocalVariableTable(int size) {
        locals = new LocalVariableRangeList[size];
        readfromclass = false;
    }

    public int getSize() {
	return locals.length;
    }

    public boolean isReadFromClass() {
        return readfromclass;
    }

    public void read(JodeEnvironment env, DataInputStream stream)
         throws IOException
    {
        int count = stream.readUnsignedShort();
        for (int i=0; i<count; i++) {
            int start  = stream.readUnsignedShort();
            int length = stream.readUnsignedShort();
            int name_i = stream.readUnsignedShort();
            int desc_i = stream.readUnsignedShort();
            int slot   = stream.readUnsignedShort();
            LocalVariableRangeList lv = locals[slot];
            if (lv == null) {
                lv = new LocalVariableRangeList(slot);
                locals[slot] = lv;
            }
            lv.addLocal(start, length, 
                        Identifier.lookup((String)
                                          env.getConstantPool().
                                          getValue(name_i)),
                        Type.tType(env.getConstantPool().getType(desc_i)));
	    if (Decompiler.showLVT)
		System.err.println(""+env.getConstantPool().getValue(name_i)
				   +": "+env.getConstantPool().getType(desc_i)
				   +" range "+start+" - "+(start+length)
				   +" slot "+slot);
        }
        readfromclass = true;
    }

    public LocalVariableRangeList getLocal(int slot) 
         throws ArrayIndexOutOfBoundsException
    {
        LocalVariableRangeList lv = locals[slot];
        if (lv == null)
            lv = new LocalVariableRangeList(slot);
        return lv;
    }
}
