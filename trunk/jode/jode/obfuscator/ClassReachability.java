/* 
 * ClassReachability (c) 1998 Jochen Hoenicke
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
import jode.bytecode.ClassInfo;
import jode.bytecode.FieldInfo;
import jode.bytecode.MethodInfo;
import jode.MethodType;
import jode.Type;
import java.lang.reflect.Modifier;
import java.util.*;

public class ClassReachability {
    ClassBundle bundle;
    ClassInfo clazz;

    Vector subclasses;

    public FieldInfo[] fields;
    public MethodInfo[] methods;

    public boolean reachable = false;
    public boolean[] reachFields;
    public boolean[] reachMethods;

    public boolean preserve = false;
    public boolean[] preserveFields;
    public boolean[] preserveMethods;

    public Object[] fromClassFields;
    public Object[] fromClassMethods;

    public ClassReachability(ClassBundle bundle, ClassInfo clazz) {
	this.bundle = bundle;
	this.clazz = clazz;
	fields   = clazz.getFields();
	methods  = clazz.getMethods();
	reachFields = new boolean[fields.length];
	preserveFields = new boolean[fields.length];
	reachMethods = new boolean[methods.length];
	preserveMethods = new boolean[methods.length];
	clazz.loadInfo(clazz.FULLINFO);
    }

    public void addSubclass(ClassReachability subclass) {
	subclasses.addElement(subclass);
    }

    public void markReachableField(int i) {
	reachFields[i] = true;
    }

    public void markReachableField(Type type, String name) {
	for (int i=0; i < fields.length; i++) {
	    if (fields[i].getType().equals(type)
		&& fields[i].getName().equals(name))
		markReachableField(i);
	}
    }

    public void markReachableMethod(int i) {
	reachMethods[i] = true;
	Enumeration enum = subclasses.elements();
	while (enum.hasMoreElements()) {
	    ClassReachability subclass =
		(ClassReachability) enum.nextElement();
	    subclass.markReachableMethod(methods[i].getType(),
					 methods[i].getName());
	}
	/*XXX read code and check reachability*/
    }

    public void markReachableMethod(MethodType type, String name) {
	for (int i=0; i < methods.length; i++) {
	    if (methods[i].getType().equals(type)
		&& methods[i].getName().equals(name))
		markReachableMethod(i);
	}
    }

    public void markReachable() {
	reachable = true;
    }

    private void markPreservedField(int i) {
	reachFields[i] = true;
	preserveFields[i] = true;
    }

    public void markPreservedField(Type type, String name) {
	for (int i=0; i < fields.length; i++) {
	    if (fields[i].getType().equals(type)
		&& fields[i].getName().equals(name))
		markPreservedField(i);
	}
    }

    private void markPreservedMethod(int i) {
	markReachableMethod(i);
	preserveMethods[i] = true;
	Enumeration enum = subclasses.elements();
	while (enum.hasMoreElements()) {
	    ClassReachability subclass =
		(ClassReachability) enum.nextElement();
	    subclass.markPreservedMethod(methods[i].getType(),
					 methods[i].getName());
	}
    }

    public void markPreservedMethod(MethodType type, String name) {
	for (int i=0; i < methods.length; i++) {
	    if (methods[i].getType().equals(type)
		&& methods[i].getName().equals(name))
		markPreservedMethod(i);
	}
    }
    
    public void markPreserved() {
	preserve = true;
    }	

    public void doPreserveRule(int preserveRule) {
	preserve = (clazz.getModifiers() & preserveRule) != 0;
	for (int i=0; i < fields.length; i++) {
	    if (((fields[i].getModifiers() ^ Modifier.PRIVATE)
		 & preserveRule) != 0)
		markPreservedField(i);
	}
	for (int i=0; i < methods.length; i++) {
	    if (((methods[i].getModifiers() ^ Modifier.PRIVATE) 
		 & preserveRule) != 0)
		markPreservedMethod(i);
	}
    }	

    public void checkReachableThroughSuper(ClassInfo superInfo) {
	ClassReachability superReach = 
	    bundle.getLoadedClass(superInfo.getName());
	if (superReach != null) {
	    superReach.addSubclass(this);
	} else {
	    for (int i=0; i< methods.length; i++) {
		if (!methods[i].getType().isStatic()
		    && superInfo.findMethod(methods[i].getName(), 
					    methods[i].getType()) != null) {
		    markPreservedMethod(i);
		}
	    }
	}
    }

    public void postInitialize() {
	checkReachableThroughSuper(clazz.getSuperclass());
	ClassInfo[] ifaces = clazz.getInterfaces();
	for (int i=0; i<ifaces.length; i++)
	    checkReachableThroughSuper(ifaces[i]);
    }
}
