/* 
 * LocalVariableRangeList (c) 1998 Jochen Hoenicke
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

public class LocalVariableRangeList {

    class MyLocalInfo extends LocalInfo {
        int start;
        int length;
        MyLocalInfo next;
        
        MyLocalInfo(int slot, int s, int l, String n, Type t) {
            super (slot);
            start = s;
            length = l;
            setName(n);
            setType(t);
            next = null;
        }
    }

    MyLocalInfo list = null;
    int slot;

    LocalVariableRangeList(int slot) {
        this.slot = slot;
    }

    private void add(MyLocalInfo li) {
        MyLocalInfo before = null;
        MyLocalInfo after = list;
        while (after != null && after.start < li.start) {
            before = after;
            after = after.next;
        }
        if (after != null && li.start + li.length > after.start) 
            throw new AssertError("non disjoint locals");
        li.next = after;
        if (before == null)
            list = li;
        else
            before.next = li;
    }

    private LocalInfo find(int addr) {
        MyLocalInfo li = list;
        while (li != null && addr >= li.start+li.length)
            li = li.next;
        if (li == null || li.start > addr+1 /* XXX weired XXX */) {
            LocalInfo temp = new LocalInfo(slot);
            return temp;
        }
        return li;
    }

    public void addLocal(int start, int length, 
                         String name, Type type) {
        MyLocalInfo li = new MyLocalInfo(slot,start,length,name,type);
        add (li);
    }

    public LocalInfo getInfo(int addr) {
        return find(addr);
    }
}
