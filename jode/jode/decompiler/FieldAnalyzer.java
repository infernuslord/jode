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
import gnu.bytecode.Spy;

public class FieldAnalyzer implements Analyzer {
    ClassAnalyzer clazz;
    JodeEnvironment env;
    int modifiers;
    Type type;
    String fieldName;
    ConstOperator constant;
    
    public FieldAnalyzer(ClassAnalyzer cla, Field fd, JodeEnvironment e)
    {
        clazz = cla;
        env  = e;

        modifiers = fd.getModifiers();
        type = Type.tType(fd.getType());
        fieldName = fd.getName();
        constant = null;

        Attribute attribute = 
            Attribute.get(clazz.classType.getField(fieldName), 
                          "ConstantValue");
        if (attribute != null) {
            try {
                int index = Spy.getAttributeStream((MiscAttr)attribute)
                    .readUnsignedShort();
                constant = new ConstOperator
                    (type.intersection(cla.getConstantType(index)),
                     cla.getConstantString(index));

            } catch (java.io.IOException ex) {
                throw new AssertError("attribute too small");
            }
        }
    }

    public void analyze() {
        type.useType();
    }

    public void dumpSource(TabbedPrintWriter writer) 
         throws java.io.IOException 
    {
	String modif = Modifier.toString(modifiers);
	if (modif.length() > 0)
	    writer.print(modif+" ");

        writer.print(type.toString() + " " + fieldName);
        if (constant != null) {
            writer.print(" = " + constant.toString());
        }
        writer.println(";");
    }
}
