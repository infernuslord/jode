/* 
 * FieldAnalyzer (c) 1998 Jochen Hoenicke
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
import sun.tools.java.*;
import java.lang.reflect.Modifier;

public class FieldAnalyzer implements Analyzer {
    FieldDefinition fdef;
    JodeEnvironment env;
    
    public FieldAnalyzer(FieldDefinition fd, JodeEnvironment e)
    {
        fdef = fd;
        env  = e;
    }

    public void analyze() {
    }

    public int unsigned(byte value) {
        if (value < 0)
            return value + 256;
        else
            return value;
    }

    public void dumpSource(TabbedPrintWriter writer) 
         throws java.io.IOException 
    {
	String modif = Modifier.toString(fdef.getModifiers());
	if (modif.length() > 0)
	    writer.print(modif+" ");

        writer.print(env.getTypeString(Type.tType(fdef.getType()), 
                                       fdef.getName()));
        byte[] attrib = 
            ((BinaryField) fdef).getAttribute(Constants.idConstantValue);
        if (attrib != null) {
            int index = (unsigned(attrib[0]) << 8) | unsigned(attrib[1]);
            writer.print(" = "+env.getConstant(index).toString());
        }
        writer.println(";");
    }
}


