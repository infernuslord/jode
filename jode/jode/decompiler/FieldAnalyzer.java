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
import java.lang.reflect.Modifier;
import jode.bytecode.FieldInfo;
import jode.bytecode.AttributeInfo;
import jode.bytecode.ClassFormatException;

public class FieldAnalyzer implements Analyzer {
    ClassAnalyzer clazz;
    JodeEnvironment env;
    int modifiers;
    Type type;
    String fieldName;
    Expression constant;
    
    public FieldAnalyzer(ClassAnalyzer cla, FieldInfo fd, 
                         JodeEnvironment e)
    {
        clazz = cla;
        env  = e;

        modifiers = fd.getModifiers();
        type = fd.getType();
        fieldName = fd.getName();
        constant = null;

        AttributeInfo attribute = fd.findAttribute("ConstantValue");

        if (attribute != null) {
            try {
                byte[] contents = attribute.getContents();
                if (contents.length != 2)
                    throw new AssertError("ConstantValue attribute"
                                          + " has wrong length");
                int index = (contents[0] & 0xff) << 8 | (contents[1] & 0xff);
                constant = new ConstOperator
                    (type.intersection(cla.getConstantPool()
                                       .getConstantType(index)),
                     cla.getConstantPool().getConstantString(index));
                constant.makeInitializer();
            } catch (ClassFormatException ex) {
                ex.printStackTrace();
                throw new AssertError("ClassFormatException");
            }
        }
    }

    public String getName() {
        return fieldName;
    }

    public boolean setInitializer(Expression expr) {
        expr.makeInitializer();
        if (constant != null)
            return constant.equals(expr);
        constant = expr;
        return true;
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
            writer.print(" = " + constant.simplify().toString());
        }
        writer.println(";");
    }
}
