/* AnonymousClass Copyright (C) 1999 Jochen Hoenicke.
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

public class AnonymousClass {
    class Inner {
	public void test() {
	    class Hello {
		Hello() {
		    System.err.println("construct");
		}
		Hello(String info) {
		    System.err.println("construct: "+info);
		}

		void hello() {
		    this.hashCode();
		    Inner.this.hashCode();
		    AnonymousClass.this.hashCode();
		    System.err.println("HelloWorld");
		}
	    };
	    final Hello hi = new Hello();
	    final Hello ho = new Hello("ho");
	    final Object o = new Object() {
		public String toString() {
		    hi.hello();
		    return Integer.toHexString(AnonymousClass.this.hashCode());
		}
	    };
	    Object p = new Object() {
		public String toString() {
		    return o.toString();
		}
	    };
	}
    }
}
