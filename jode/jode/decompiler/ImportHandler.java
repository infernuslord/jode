/* 
 * JodeEnvironment (c) 1998 Jochen Hoenicke
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
import sun.tools.util.*;
import java.util.*;

public class JodeEnvironment extends LoadEnvironment {
    Hashtable imports = new Hashtable();
    BinaryClass main;
    Identifier pkg;

    JodeEnvironment() {
        super(null);
	MyType.setEnvironment(this);
        path = new ClassPath(System.getProperty("java.class.path"));
    }

    public BinaryConstantPool getConstantPool() {
        return main.getConstants();
    }

    public Object getConstant(int i) {
        return main.getConstants().getConstant(i, this);
    }

    public Type getConstantType(int i) 
         throws ClassFormatError
    {
        int t = main.getConstants().getConstantType(i);
        switch(t) {
        case 3: return Type.tInt   ;
        case 4: return Type.tFloat ;
        case 5: return Type.tLong  ;
        case 6: return Type.tDouble;
        case 8: return Type.tString;
        default:
            throw new ClassFormatError("invalid constant type: "+t);
        }
    }

    public String getTypeString(Type type) {
        return type.toString();
    }

    public String getTypeString(Identifier clazz) {
        return clazz.toString();
    }

    public String getTypeString(Type type, Identifier name) {
        return type.typeString(name.toString(), false, false);
    }

    public ClassDefinition getClassDefinition() {
        return main;
    }

    public void dumpHeader(TabbedPrintWriter writer) 
         throws java.io.IOException
    {
        writer.println("/* Decompiled by JoDe (Jochen's Decompiler) */");
        if (pkg != null && pkg != Constants.idNull)
            writer.println("package "+pkg+";");
        Enumeration enum = imports.keys();
        while (enum.hasMoreElements()) {
            Identifier packageName = (Identifier) enum.nextElement();
            Integer vote = (Integer) imports.get(packageName);
            if (vote.intValue() > 3)
                writer.println("import "+packageName+";");
        }
        writer.println("");
    }

    public void error(String message) {
        System.err.println(message);
    }

    public void doClass(String className) 
    {
        try {
            Identifier ident = Identifier.lookup(className);
            error(ident.toString());
            if (!classExists(ident)) {
                error("`"+ident+"' not found");
                return;
            }
            pkg = ident.getQualifier();
            main = (BinaryClass)getClassDefinition(ident);
            ClassAnalyzer a = new ClassAnalyzer(main, this);
            a.analyze();
            TabbedPrintWriter writer = 
                new TabbedPrintWriter(System.out, "    ");
            a.dumpSource(writer);
        } catch (ClassNotFound e) {
            error(e.toString());
        } catch (java.io.IOException e) {
            error(e.toString());
        }
    }

    protected int loadFileFlags()
    {
        return 1;
    }
}
