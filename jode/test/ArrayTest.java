package jode.test;
import java.io.*;
import java.lang.reflect.*;

public class ArrayTest {
    Serializable s;
    Serializable[] u;
    Cloneable c;

    public void test() {
	int[] i = {4,3,2};
	i = new int[] {1, 2, 3};
	int[] j = new int[] {4,5,6};

	int[][] k = {i,j};

	u = k;
	s = i;
	c = i;
    }

    public void typetest() {
	int[] arr = null;
	s = arr;
	c = arr;
	arr[0] = 3;
	arr = arr != null ? arr : new int[4];
    }

    public static void main(String[] param) {
	int[] arr = new int[4];
	Class cls = arr.getClass();
	System.err.println("int[].getClass() is: "+cls);
	System.err.println("int[].getClass().getSuperclass() is: "
			   + cls.getSuperclass());
	Class[] ifaces = cls.getInterfaces();
	System.err.print("int[].getClass().getInterfaces() are: ");
	for (int i = 0; i < ifaces.length; i++) {
	    if (i > 0)
		System.err.print(", ");
	    System.err.print(ifaces[i]);
	}
	System.err.println();

	Field[] fields = cls.getDeclaredFields();
	System.err.print("int[].getClass().getDeclaredFields() are: ");
	for (int i = 0; i < fields.length; i++) {
	    if (i > 0)
		System.err.print(", ");
	    System.err.print(fields[i]);
	}
	System.err.println();

	Method[] methods = cls.getDeclaredMethods();
	System.err.print("int[].getClass().getDeclaredMethods() are: ");
	for (int i = 0; i < methods.length; i++) {
	    if (i > 0)
		System.err.print(", ");
	    System.err.print(methods[i]);
	}
	System.err.println();

	Object o = arr;
	System.err.println("arr instanceof Serializable: "+
			   (o instanceof Serializable));
	System.err.println("arr instanceof Externalizable: "+
			   (o instanceof Externalizable));
	System.err.println("arr instanceof Cloneable: "+
			   (o instanceof Cloneable));
// 	System.err.println("arr instanceof Comparable: "+
// 			   (o instanceof Comparable));
    }
}

