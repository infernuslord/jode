package jode.swingui;
import jode.bytecode.ClassInfo;
import com.sun.java.swing.tree.TreeModel;
import com.sun.java.swing.tree.TreePath;
import com.sun.java.swing.event.TreeModelListener;
import java.util.*;

public class PackagesTreeModel implements TreeModel {
    Hashtable cachedChildrens = new Hashtable();

    class TreeElement {
	String fullName;
	String name;

	public TreeElement(String prefix, String name) {
	    this.fullName = prefix+name;
	    this.name = name;
	}

	public String getFullName() {
	    return fullName;
	}

	public String toString() {
	    return name;
	}

	public boolean equals(Object o) {
	    return (o instanceof TreeElement)
		&& fullName == ((TreeElement)o).fullName;
	}

	public int hashCode() {
	    return fullName.hashCode();
	}
    }

    TreeElement root = new TreeElement("","");

    public TreeElement[] getChildrens(TreeElement parent) {
	TreeElement[] result = 
	    (TreeElement[]) cachedChildrens.get(parent);
	if (result == null) {
	    Vector v = new Vector();
	    String prefix = parent == root ? "" : parent.getFullName() + ".";
	    Enumeration enum = 
		ClassInfo.getClassesAndPackages(parent.getFullName());
	    while (enum.hasMoreElements()) {
		v.addElement(new TreeElement(prefix, (String)enum.nextElement()));
	    }
	    result = new TreeElement[v.size()];
	    v.copyInto(result);
	    cachedChildrens.put(parent, result);
	}
	return result;
    }

    public void addTreeModelListener(TreeModelListener l) {
	// we never change
    }
    public void removeTreeModelListener(TreeModelListener l) {
	// we never change
    }
    public void valueForPathChanged(TreePath path, Object newValue) {
	// we don't allow values
    }

    public Object getChild(Object parent, int index) {
	return getChildrens((TreeElement) parent)[index];
    }

    public int getChildCount(Object parent) {
	return getChildrens((TreeElement) parent).length;
    }

    public int getIndexOfChild(Object parent, Object child) {
	TreeElement[] childrens = getChildrens((TreeElement) parent);
	for (int i=0; i< childrens.length; i++) {
	    if (childrens[i] == child)
		return i;
	}
	throw new NoSuchElementException
	    (((TreeElement)parent).getFullName() + "." + child);
    }

    public Object getRoot() {
	return root;
    }

    public boolean isLeaf(Object node) {
	return !ClassInfo.isPackage(((TreeElement) node).getFullName());
    }

    public String getFullName(Object node) {
	return ((TreeElement) node).getFullName();
    }
}
