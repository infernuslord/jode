package jode.test;

public class ClassOpTest {
    static void test1() {
	Class c1 = ClassOpTest.class;
	Class c2 = Object.class;
	Class c3 = ClassOpTest.class;
	c1.getClass();
    }

    void test2() {
	Class c2 = Object.class;
	Class c3 = ClassOpTest.class;
    }
}
