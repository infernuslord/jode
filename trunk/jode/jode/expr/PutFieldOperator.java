/* 
 * PutFieldOperator (c) 1998 Jochen Hoenicke
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
import gnu.bytecode.CpoolRef;

public class PutFieldOperator extends StoreInstruction {
    CodeAnalyzer codeAnalyzer;
    boolean staticFlag;
    CpoolRef field;

    public PutFieldOperator(CodeAnalyzer codeAnalyzer, boolean staticFlag, 
                            CpoolRef field) {
        super(Type.tType(field.getNameAndType().getType().getString()), ASSIGN_OP);
        this.codeAnalyzer = codeAnalyzer;
        this.staticFlag = staticFlag;
        this.field = field;
    }

    public boolean matches(Operator loadop) {
        return loadop instanceof GetFieldOperator &&
            ((GetFieldOperator)loadop).field == field;
    }

    public int getLValueOperandCount() {
        return staticFlag?0:1;
    }

    public int getLValueOperandPriority(int i) {
        if (staticFlag) {
            /* shouldn't be called */
            throw new RuntimeException("Field is static");
        }
        return 900;
    }

    public Type getLValueOperandType(int i) {
        if (staticFlag) {
            /* shouldn't be called */
            throw new AssertError("Field is static");
        }
        return Type.tClass(field.getCpoolClass()
                           .getName().getString());
    }

    public void setLValueOperandType(Type[] t) {
        if (staticFlag) {
            /* shouldn't be called */
            throw new AssertError("Field is static");
        }
        return;
    }

    public String getLValueString(String[] operands) {
        String object;
        if (staticFlag) {
            if (field.getCpoolClass().getName().getString()
                .replace(java.io.File.separatorChar, '.')
                .equals(codeAnalyzer.getClazz().getName()))
                return field.getNameAndType().getName().getString();
            object = codeAnalyzer.getTypeString
                (Type.tClass(field.getCpoolClass().getName().getString()));
        } else {
            if (operands[0].equals("this"))
                return field.getNameAndType().getName().getString();
            object = operands[0];
        }
        return object + "." + field.getNameAndType().getName().getString();
    }

    public boolean equals(Object o) {
	return (o instanceof PutFieldOperator) &&
	    ((PutFieldOperator)o).field == field;
    }
}
