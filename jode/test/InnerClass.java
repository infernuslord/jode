package jode.test;

public class InnerClass {

    private int x;

    class Inner {
        int a = 4;
        private int b;
        Inner() {
            b = x;
        }

        class InnerInner {
            public InnerInner(int c) {
                x = c;
                a = b;
            }

            public int getB() {
                return Inner.this.getB();
            }

            public int getStaticB(InnerInner innerinner) {
                return innerinner.getB();
            }
        }

        int getB() {
            return b;
        }
        

        public InnerInner createInnerInner(int a) {
            return new InnerInner(a);
        }
    }

    class Extended extends Inner.InnerInner{
        Extended(Inner inner) {
            inner.super(3);
        }
    }

    InnerClass() {
        new Inner().createInnerInner(10).getB();
    }
}
