/* 
 * jode.obfuscator.FieldIdentifier (c) 1998 Jochen Hoenicke
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
package jode.obfuscator;
import jode.bytecode.FieldInfo;

public class FieldIdentifier extends Identifier{
    FieldInfo info;
    ClassIdentifier clazz;

    public FieldIdentifier(ClassIdentifier clazz, FieldInfo info) {
	super(info.getName());
	this.info = info;
	this.clazz = clazz;
    }

    public String getFullName() {
	return clazz.getFullName() + "." + getName();
    }

    public String getFullAlias() {
	return clazz.getFullAlias() + "." + getAlias();
    }

    public String getName() {
	return info.getName();
    }

    public String getType() {
	return info.getType().getTypeSignature();
    }

    public String toString() {
	return "MethodIdentifier "+getFullName()+"."+getType();
    }

    public boolean conflicting(String newAlias) {
	return clazz.containsField(newAlias)
	    || clazz.containsMethod(newAlias, "");
    }
}
