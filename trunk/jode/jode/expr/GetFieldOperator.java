/* 
 * GetFieldOperator (c) 1998 Jochen Hoenicke
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

public class GetFieldOperator extends Operator {
    boolean staticFlag;
    CpoolRef field;
    CodeAnalyzer codeAnalyzer;
    Type classType;

    public GetFieldOperator(CodeAnalyzer codeAnalyzer, boolean staticFlag, 
                            CpoolRef field) {
        super(Type.tType(field.getNameAndType().getType().getString()), 0);
        this.codeAnalyzer = codeAnalyzer;
        this.staticFlag = staticFlag;
        this.field = field;
        classType = Type.tClass(field.getCpoolClass().getName().getString());
        if (staticFlag)
            classType.useType();
    }

    public int getPriority() {
        return 950;
    }

    public int getOperandCount() {
        return staticFlag?0:1;
    }

    public int getOperandPriority(int i) {
        return 900;
    }

    public Type getOperandType(int i) {
        return classType;
    }

    public void setOperandType(Type types[]) {
    }

    public String toString(String[] operands) {
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
	return (o instanceof GetFieldOperator) &&
	    ((GetFieldOperator)o).field == field;
    }
}
