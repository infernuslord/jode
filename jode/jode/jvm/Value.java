/* 
 * Value (c) 1998 Jochen Hoenicke
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
package jode.jvm;
import jode.bytecode.*;

/**
 * This class represents a stack value.
 *
 * @author Jochen Hoenicke
 */
public class Value {
    Object value;
    NewObject newObj;

    public void setObject(Object obj) {
	value = obj;
    }

    public Object objectValue() {
	if (newObj != null)
	    return newObj.objectValue();
	return value;
    }

    public void setInt(int i) {
	value = new Integer(i);
    }

    public int intValue() {
	return ((Integer)value).intValue();
    }

    public void setLong(long i) {
	value = new Long(i);
    }

    public long longValue() {
	return ((Long)value).longValue();
    }

    public void setFloat(float i) {
	value = new Float(i);
    }

    public float floatValue() {
	return ((Float)value).floatValue();
    }

    public void setDouble(double i) {
	value = new Double(i);
    }

    public double doubleValue() {
	return ((Double)value).doubleValue();
    }

    public void setNewObject(NewObject n) {
	newObj = n;
    }

    public NewObject getNewObject() {
	return newObj;
    }

    public void setValue(Value val) {
	value = val.value;
	newObj = val.newObj;
    }
}

