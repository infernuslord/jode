/* Conflicts Copyright (C) 1999 Jochen Hoenicke.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; see the file COPYING.  If not, write to
 * the Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 * $Id$
 */

package jode.test;

/**
 * This class tests name conflicts and their resolvation.  Note that every
 * name in this file should be the shortest possible name.
 */
public class Conflicts
{
    int Conflicts;

    class Blah
    {
	Conflicts Inner;
	
	void Conflicts() {
	    Inner = jode.test.Conflicts.this;
	}
    }
    
    class Inner
    {
	int Conflicts;
	Conflicts Inner;

	class Blah 
	    extends Conflicts.Blah 
	{
	    int Blah;
	    
	    void Inner() {
		this.Inner.Inner();
		jode.test.Conflicts.Inner.this.Inner.Inner();
		this.Conflicts();
		jode.test.Conflicts.Inner.this.Conflicts();
	    }

	    Blah() {
		/* empty */
	    }
	    
	    Blah(Conflicts Conflicts) {
		Conflicts.super();
	    }
	}

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

	Conflicts Conflicts(Inner Conflicts) {
	    Conflicts.this$0.Conflicts = 9;
	    return jode.test.Conflicts.this;
	}
    }

    class Second 
	extends Conflicts.Inner.Blah 
    {
	Inner Blah = new Inner();

	class Inner extends Conflicts.Inner
	{
	}

	Conflicts.Inner create() {
	    return jode.test.Conflicts.this.new Inner();
	}

	Second(Conflicts.Inner Blah) {
	    Blah.super();
	}
    }

    public void Inner() {
	/* empty */
    }
    
    public Conflicts() {
	int Conflicts = this.Conflicts;
	Inner Inner = new Inner();
	Inner.Conflicts = 5;
	new Inner().Conflicts(Inner).Inner();
    }
}
