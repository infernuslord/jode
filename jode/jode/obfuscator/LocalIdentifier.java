/* LocalIdentifier Copyright (C) 1999 Jochen Hoenicke.
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

///#ifdef JDK12
///import java.util.Collections;
///import java.util.Iterator;
///#else
import jode.util.Collections;
import jode.util.Iterator;
///#endif

public class LocalIdentifier extends Identifier {
    String name;
    String type;

    public LocalIdentifier(String name, String type, MethodIdentifier mIdent) {
	super(name);
	this.name = name;
	this.type = type;
    }

    public String getName() {
	return name;
    }

    public String getType() {
	return type;
    }

    public Iterator getChilds() {
	return Collections.EMPTY_LIST.iterator();
    }

    public Identifier getParent() {
	return null;
    }

    public String getFullName() {
	return name;
    }

    public String getFullAlias() {
	return getAlias();
    }

    public boolean conflicting(String newAlias) {
	return false;
    }
}
