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

package jode.expr;
import jode.Type;
import jode.decompiler.CodeAnalyzer;

public class PutFieldOperator extends StoreInstruction {
    CodeAnalyzer codeAnalyzer;
    boolean staticFlag;
    String fieldName;
    Type fieldType;
    Type classType;

    public PutFieldOperator(CodeAnalyzer codeAnalyzer, boolean staticFlag, 
                            Type classType, Type type, String fieldName) {
        super(type, ASSIGN_OP);
        this.codeAnalyzer = codeAnalyzer;
        this.staticFlag = staticFlag;
        this.fieldName = fieldName;
	this.fieldType = type;
        this.classType = classType;
        if (staticFlag)
            classType.useType();
    }

    public boolean isStatic() {
        return staticFlag;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Type getFieldType() {
        return fieldType;
    }

    public boolean matches(Operator loadop) {
        return loadop instanceof GetFieldOperator
	    && ((GetFieldOperator)loadop).classType.equals(classType)
	    && ((GetFieldOperator)loadop).fieldName.equals(fieldName);
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
        return staticFlag
            ? (classType.equals(Type.tClass(codeAnalyzer.getClazz().getName()))
               && codeAnalyzer.findLocal(fieldName) == null
               ? fieldName 
               : classType.toString() + "." + fieldName)
            : (operands[0].equals("this")
               && codeAnalyzer.findLocal(fieldName) == null
               ? fieldName
               : operands[0] + "." + fieldName);
    }

    public boolean equals(Object o) {
	return o instanceof PutFieldOperator
	    && ((PutFieldOperator)o).classType.equals(classType)
	    && ((PutFieldOperator)o).fieldName.equals(fieldName);
    }
}
