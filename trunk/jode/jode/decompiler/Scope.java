/* 
 * Scope (c) 1998 Jochen Hoenicke
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

/**
 * This interface describes a scope.  The basic scopes are: the package
 * scope, the class scope (one more for each inner class) and the method
 * scope.
 *
 * @author Jochen Hoenicke
 */
public interface Scope {
    public int PACKAGENAME   = 0;
    public int CLASSNAME     = 1;
    public int METHODNAME    = 2;
    public int FIELDNAME     = 3;
    public int AMBIGUOUSNAME = 4;

    public Scope   getParentScope();
    public void    requestName(String name);
    public String  getScopedName(String name, int usageType, int wantedType);
}
