/* IdentifierEvent Copyright (C) 1999 Jochen Hoenicke.
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

package jode.obfuscator;

/**
 *
 * @author Jochen Hoenicke
 */
public class IdentifierEvent extends java.util.EventObject {
    /*  0 -  9: general events */
    public final static int REACHABLE =  0;
    public final static int PRESERVED =  1;

    /* 10 - 19: field events */
    public final static int CONSTANT  = 10;
    /* 20 - 29: method events */
    /* 30 - 39: class events */
    /* 40 - 49: package events */

    public final int id;

    public IdentifierEvent(Identifier source, int id) {
	super(source);
	this.id = id;
    }

    public final int getID() {
	return id;
    }
}
