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
import java.lang.reflect.*;
import gnu.bytecode.Attribute;
import gnu.bytecode.MiscAttr;

public class FieldAnalyzer implements Analyzer {
    ClassAnalyzer clazz;
    int constantValue;
    Field field;
    JodeEnvironment env;
    
    public FieldAnalyzer(ClassAnalyzer cla, Field fd, JodeEnvironment e)
    {
        clazz = cla;
        field = fd;
        env  = e;
    }

    public void analyze() {
        Type.tType(field.getType()).useType();
        constantValue = 0;
        Attribute attribute = 
            Attribute.get(clazz.classType.getField(field.getName()), 
                          "ConstantValue");
        if (attribute != null) {
            byte[] data = gnu.bytecode.Spy.getAttribute((MiscAttr)attribute);
            constantValue = (unsigned(data[0]) << 8) | unsigned(data[1]);
        }
    }

    private final int unsigned(byte value) {
        if (value < 0)
            return value + 256;
        else
            return value;
    }

    public void dumpSource(TabbedPrintWriter writer) 
         throws java.io.IOException 
    {
	String modif = Modifier.toString(field.getModifiers());
	if (modif.length() > 0)
	    writer.print(modif+" ");

        writer.print(Type.tType(field.getType()).toString()
                     + " " + field.getName());
        if (constantValue != 0) {
            writer.print(" = "+clazz.getConstantString(constantValue));
        }
        writer.println(";");
    }
}
