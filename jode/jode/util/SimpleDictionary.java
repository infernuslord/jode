/* jode.flow.SimpleDictionary  (c) 1998 Jochen Hoenicke 
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

package jode.flow;
import java.util.Dictionary;
import java.util.Enumeration;

public class SimpleDictionary extends Dictionary {
    Object[] keyObjects = new Object[2];
    Object[] elementObjects = new Object[2];
    int count = 0;

    public int size() {
        return count;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public Enumeration getArrayEnum(final Object[] array, final int size) {
        return new Enumeration() {
            int index = 0;
            public boolean hasMoreElements() {
                return index < size;
            }
            public Object nextElement() {
                return array[index++];
            }
        };
    }

    public Enumeration keys() {
        return getArrayEnum(keyObjects, count);
    }

    public Enumeration elements() {
        return getArrayEnum(elementObjects, count);
    }
    
    public Object get(Object key) {
        for (int i=0; i< count; i++) {
            if (keyObjects[i].equals(key))
                return elementObjects[i];
        }
        return null;
    }

    public Object put(Object key, Object element) {
        for (int i=0; i< count; i++) {
            if (keyObjects[i].equals(key)) {
                Object old = elementObjects[i];
                elementObjects[i] = element;
                return old;
            }
        }
        if (count == keyObjects.length) {
            Object[] newArray = new Object[count*3/2];
            System.arraycopy(keyObjects,0,newArray,0,count);
            keyObjects = newArray;
            newArray = new Object[count*3/2];
            System.arraycopy(elementObjects,0,newArray,0,count);
            elementObjects = newArray;
        }
        keyObjects[count] = key;
        elementObjects[count] = element;
        count++;
        return null;
    }

    public Object remove(Object key) {
        for (int i=0; i< count; i++) {
            if (keyObjects[i].equals(key)) {
                Object old = elementObjects[i];
                count--;
                if (i < count) {
                    keyObjects[i] = keyObjects[count];
                    elementObjects[i] = elementObjects[count];
                }
                return old;
            }
        }
        return null;
    }
}
