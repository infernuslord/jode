// This interface is taken from the Classpath project.  
// Please note the different copyright holder!  
// The changes I did is this comment, the package line, some
// imports from java.util and some minor jdk12 -> jdk11 fixes.
// -- Jochen Hoenicke <jochen@gnu.org>

/////////////////////////////////////////////////////////////////////////////
// Set.java -- A collection that prohibits duplicates
//
// Copyright (c) 1998 by Stuart Ballard (stuart.ballard@mcmail.com)
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU Library General Public License as published
// by the Free Software Foundation, version 2. (see COPYING.LIB)
//
// This program is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Library General Public License for more details.
//
// You should have received a copy of the GNU Library General Public License
// along with this program; if not, write to the Free Software Foundation
// Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
/////////////////////////////////////////////////////////////////////////////

// TO DO:
// ~ Doc comments for everything.

package jode.util;

public interface Set extends Collection {
  boolean add(Object o);
  boolean addAll(Collection c);
  void clear();
  boolean contains(Object o);
  boolean containsAll(Collection c);
  boolean equals(Object o);
  int hashCode();
  boolean isEmpty();
  Iterator iterator();
  boolean remove(Object o);
  boolean removeAll(Collection c);
  boolean retainAll(Collection c);
  int size();
  Object[] toArray();
}
