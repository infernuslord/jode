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
    Type classType;

    public PutFieldOperator(CodeAnalyzer codeAnalyzer, boolean staticFlag, 
                            CpoolRef field) {
        super(Type.tType(field.getNameAndType().getType().getString()), ASSIGN_OP);
        this.codeAnalyzer = codeAnalyzer;
        this.staticFlag = staticFlag;
        this.field = field;
        classType = Type.tClass(field.getCpoolClass().getName().getString());
        if (staticFlag)
            classType.useType();
    }

    public boolean matches(Operator loadop) {
        return loadop instanceof GetFieldOperator &&
            ((GetFieldOperator)loadop).field == field;
    }

    public int getLValueOperandCount() {
        return staticFlag?0:1;
    }

    public int getLValueOperandPriority(int i) {
        return 900;
    }

    public Type getLValueOperandType(int i) {
        return classType;
    }

    public void setLValueOperandType(Type[] t) {
    }

    public String getLValueString(String[] operands) {
        String fieldName = field.getNameAndType().getName().getString();
        return staticFlag
            ? (classType.equals(Type.tType(codeAnalyzer.getClazz()))
               ? fieldName 
               : classType.toString() + "." + fieldName)
            : (operands[0].equals("this")
               ? fieldName
               : operands[0] + "." + fieldName);
    }

    public boolean equals(Object o) {
	return (o instanceof PutFieldOperator) &&
	    ((PutFieldOperator)o).field == field;
    }
}
