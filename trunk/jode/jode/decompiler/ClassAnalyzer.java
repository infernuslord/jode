/* 
 * ClassAnalyzer (c) 1998 Jochen Hoenicke
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
import java.io.IOException;
import sun.tools.java.*;

public class ClassAnalyzer implements Analyzer {
    BinaryClass cdef;
    JodeEnvironment env;
    Analyzer fields[];
    
    public ClassAnalyzer(BinaryClass bc, JodeEnvironment e)
    {
        cdef = bc;
        env  = e;
    }
    
    public void analyze() {
        int numFields = 0, i=0;
        
        FieldDefinition f;
        for (f= cdef.getInnerClassField(); f != null; f = f.getNextField())
            numFields++;
        for (f= cdef.getFirstField(); f != null; f = f.getNextField())
            numFields++;
        fields = new Analyzer[numFields];
        for (f= cdef.getInnerClassField(); f != null; f = f.getNextField()) {
            System.err.println("analyzing inner: "+f.getName());
            fields[i] = new ClassAnalyzer((BinaryClass) f.getInnerClass(), env);
            fields[i++].analyze();
        }
        for (f= cdef.getFirstField(); f != null; f = f.getNextField()) {
            if (f.getType().getTypeCode() == Constants.TC_METHOD) {
                fields[i] = new MethodAnalyzer(f, env);
            } else {
                fields[i] = new FieldAnalyzer(f, env);
            }
            fields[i++].analyze();
        }
    }

    public void dumpSource(TabbedPrintWriter writer) throws IOException
    {
        if (cdef.getSource() != null)
            writer.println("/* Original source: "+cdef.getSource()+" */");
        String modif = Modifier.toString(cdef.getModifiers());
        if (modif.length() > 0)
            writer.print(modif + " ");
        writer.print((cdef.isInterface())?"interface ":"class ");
	writer.println(cdef.getName().getName().toString());
	writer.tab();
	if (cdef.getSuperClass() != null)
	    writer.println("extends "+cdef.getSuperClass().getName().toString());
        ClassDeclaration interfaces[] = cdef.getInterfaces();
	if (interfaces.length > 0) {
	    writer.print("implements ");
	    for (int i=0; i < interfaces.length; i++) {
		if (i > 0)
		    writer.print(", ");
		writer.print(interfaces[i].getName().toString());
	    }
            writer.println("");
	}
	writer.untab();
	writer.println("{");
	writer.tab();

	for (int i=0; i< fields.length; i++)
	    fields[i].dumpSource(writer);
	writer.untab();
	writer.println("}");
    }
}


