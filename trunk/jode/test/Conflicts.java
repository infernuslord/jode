package jode.test;

/**
 * This class tests name conflicts and their resolvation.
 */
public class Conflicts {
    int Conflicts;
    
    class Inner {
	int Conflicts;

	void Conflicts() {
	    int Conflicts = 4;
	    Conflicts();
	    new Object() {
		void Inner() {
		    jode.test.Conflicts.this.Inner();
		}
	    };
	    this.Conflicts = Conflicts;
	    Inner();
	    jode.test.Conflicts.this.Conflicts = this.Conflicts;
	}

	jode.test.Conflicts Conflicts(Inner Conflicts) {
	    return jode.test.Conflicts.this;
	}
    }

    public void Inner() {
    }

    class Second {
	class Inner {
	}
	void create() {
	    new Inner();
	    jode.test.Conflicts.this.new Inner();
	}
    }

    
    public Conflicts() {
	int Conflicts = this.Conflicts;
	Inner Inner = new Inner();
	Inner.Conflicts = 5;
	new Inner().Conflicts(Inner).Inner();
    }
}

