/* NewObject Copyright (C) 1999 Jochen Hoenicke.
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

package net.sf.jode.jvm;

/**
 * This class represents a new object, that may not be initialized yet.
 *
 * @author Jochen Hoenicke
 */
class NewObject {
    Object instance;
    String type;

    public NewObject(String type) {
	this.type = type;
    }

    public String getType() {
	return type;
    }

    public void setObject(Object obj) {
	instance = obj;
    }

    public Object objectValue() {
	return instance;
    }

    public String toString() {
	if (instance == null)
	    return "new "+type;
	else
	    return instance.toString();
    }
}

