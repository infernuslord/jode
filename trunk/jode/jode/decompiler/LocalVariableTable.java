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
import java.util.Enumeration;
import gnu.bytecode.LocalVarsAttr;
import gnu.bytecode.Variable;
import gnu.bytecode.Spy;

public class LocalVariableTable {
    LocalVariableRangeList[] locals;

    public LocalVariableTable(int size, 
                              ClassAnalyzer cla, LocalVarsAttr attr) {

        locals = new LocalVariableRangeList[size];
        for (int i=0; i<size; i++)
            locals[i] = new LocalVariableRangeList(i);
        
        Enumeration vars = attr.allVars();
        while (vars.hasMoreElements()) {
            Variable var = (Variable) vars.nextElement();
            
            int start  = Spy.getStartPC(var);
            int end    = Spy.getEndPC(var);
            int slot = Spy.getSlot(var);
            String name = var.getName();
            Type type = Type.tType(var.getType().getSignature());
            locals[slot].addLocal(start, end-start, name, type);
	    if (Decompiler.showLVT)
		System.err.println(name + ": " + type
				   +" range "+start+" - "+end
				   +" slot "+slot);
        }
    }

    public LocalVariableRangeList getLocal(int slot) 
         throws ArrayIndexOutOfBoundsException
    {
        return locals[slot];
    }
}
